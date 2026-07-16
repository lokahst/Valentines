package lokahst.valentines.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import lokahst.valentines.Valentines;
import lokahst.valentines.data.Marriage;
import lokahst.valentines.data.MarriageProposal;
import lokahst.valentines.data.PlayerData;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileStorage {

    private final Valentines plugin;
    private final ReentrantReadWriteLock dataLock = new ReentrantReadWriteLock();
    private final Map<String, Object> pendingWrites = new ConcurrentHashMap<>();
    private volatile boolean shutdownInProgress = false;
    private final Object fileLock = new Object();

    private File playerDataFile;
    private File marriagesFile;
    private File proposalsFile;
    private File likesFile;

    private FileConfiguration playerDataConfig;
    private FileConfiguration marriagesConfig;
    private FileConfiguration proposalsConfig;
    private FileConfiguration likesConfig;

    public FileStorage(Valentines plugin) {
        this.plugin = plugin;
        initializeFiles();
        startPeriodicSave();
    }

    private void initializeFiles() {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        playerDataFile = new File(dataFolder, "playerdata.yml");
        marriagesFile = new File(dataFolder, "marriages.yml");
        proposalsFile = new File(dataFolder, "proposals.yml");
        likesFile = new File(dataFolder, "likes.yml");

        try {
            if (!playerDataFile.exists()) playerDataFile.createNewFile();
            if (!marriagesFile.exists()) marriagesFile.createNewFile();
            if (!proposalsFile.exists()) proposalsFile.createNewFile();
            if (!likesFile.exists()) likesFile.createNewFile();
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create data files: " + e.getMessage());
        }

        playerDataConfig = YamlConfiguration.loadConfiguration(playerDataFile);
        marriagesConfig = YamlConfiguration.loadConfiguration(marriagesFile);
        proposalsConfig = YamlConfiguration.loadConfiguration(proposalsFile);
        likesConfig = YamlConfiguration.loadConfiguration(likesFile);
    }

    private void startPeriodicSave() {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!shutdownInProgress && !pendingWrites.isEmpty()) {
                    flushPendingWrites();
                }
            }
        }.runTaskTimerAsynchronously(plugin, 100L, 100L);
    }

    private void flushPendingWrites() {
        if (shutdownInProgress) {
            return;
        }

        synchronized (fileLock) {
            dataLock.writeLock().lock();
            try {
                if (!pendingWrites.isEmpty()) {
                    saveAllWithLock();
                    pendingWrites.clear();
                }
            } finally {
                dataLock.writeLock().unlock();
            }
        }
    }

    public void saveAll() {
        synchronized (fileLock) {
            dataLock.writeLock().lock();
            try {
                saveAllWithLock();
            } finally {
                dataLock.writeLock().unlock();
            }
        }
    }

    private void saveAllWithLock() {
        saveFileWithLock(playerDataConfig, playerDataFile, "playerdata");
        saveFileWithLock(marriagesConfig, marriagesFile, "marriages");
        saveFileWithLock(proposalsConfig, proposalsFile, "proposals");
        saveFileWithLock(likesConfig, likesFile, "likes");
    }

    private void saveFileWithLock(FileConfiguration config, File file, String type) {
        try {
            File backupFile = new File(file.getParent(), file.getName() + ".backup");

            if (file.exists() && file.length() > 0) {
                try {
                    Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    plugin.getLogger().warning("Failed to create backup for " + type + ": " + e.getMessage());
                }
            }

            config.save(file);
            plugin.getLogger().fine("Successfully saved " + type + " data");

        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save " + type + " file: " + e.getMessage());

            File backupFile = new File(file.getParent(), file.getName() + ".backup");
            if (backupFile.exists()) {
                try {
                    Files.copy(backupFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    plugin.getLogger().info("Restored " + type + " from backup after save failure");
                } catch (IOException backupError) {
                    plugin.getLogger().severe("Failed to restore " + type + " from backup: " + backupError.getMessage());
                }
            }
        }
    }

    public void savePlayerData(PlayerData data) {
        dataLock.writeLock().lock();
        try {
            String path = "players." + data.getUuid().toString();
            playerDataConfig.set(path + ".name", data.getName());
            playerDataConfig.set(path + ".hugs", data.getHugsReceived());
            playerDataConfig.set(path + ".kisses", data.getKissesReceived());
            playerDataConfig.set(path + ".likes", data.getLikesReceived());
            playerDataConfig.set(path + ".hugs-given", data.getHugsGiven());
            playerDataConfig.set(path + ".kisses-given", data.getKissesGiven());
            playerDataConfig.set(path + ".likes-given", data.getLikesGiven());
            playerDataConfig.set(path + ".effect-type", data.getEffectType());
            playerDataConfig.set(path + ".effect-enabled", data.isEffectEnabled());
            playerDataConfig.set(path + ".mood", data.getMood());
            playerDataConfig.set(path + ".last-seen", data.getLastSeen());
            playerDataConfig.set(path + ".achievements", new ArrayList<>(data.getUnlockedAchievements()));
            Map<String, Long> serializedFriends = new HashMap<>();
            for (Map.Entry<UUID, Long> entry : data.getFriends().entrySet()) {
                serializedFriends.put(entry.getKey().toString(), entry.getValue());
            }
            playerDataConfig.set(path + ".friends", serializedFriends);

            List<String> pendingRequests = new ArrayList<>();
            for (UUID requester : data.getPendingFriendRequests()) {
                pendingRequests.add(requester.toString());
            }
            playerDataConfig.set(path + ".pending-friend-requests", pendingRequests);
            playerDataConfig.set(path + ".allow-friend-requests", data.isAllowFriendRequests());
            playerDataConfig.set(path + ".allow-marriage-requests", data.isAllowMarriageRequests());
            playerDataConfig.set(path + ".age", data.getAge());

            pendingWrites.put("playerdata", System.currentTimeMillis());
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public PlayerData loadPlayerData(UUID uuid) {
        dataLock.readLock().lock();
        try {
            String path = "players." + uuid.toString();
            if (!playerDataConfig.contains(path)) {
                return null;
            }

            List<String> achievements = playerDataConfig.getStringList(path + ".achievements");

            Map<UUID, Long> friends = new HashMap<>();
            ConfigurationSection friendsSection = playerDataConfig.getConfigurationSection(path + ".friends");
            if (friendsSection != null) {
                for (String friendUuid : friendsSection.getKeys(false)) {
                    try {
                        friends.put(UUID.fromString(friendUuid), friendsSection.getLong(friendUuid));
                    } catch (IllegalArgumentException ignored) {
                        plugin.getLogger().warning("Invalid friend UUID in player data: " + friendUuid);
                    }
                }
            }

            Set<UUID> pendingFriendRequests = new HashSet<>();
            for (String requestUuid : playerDataConfig.getStringList(path + ".pending-friend-requests")) {
                try {
                    pendingFriendRequests.add(UUID.fromString(requestUuid));
                } catch (IllegalArgumentException ignored) {
                    plugin.getLogger().warning("Invalid pending request UUID in player data: " + requestUuid);
                }
            }

            return new PlayerData(
                    uuid,
                    playerDataConfig.getString(path + ".name", "Unknown"),
                    playerDataConfig.getInt(path + ".hugs", 0),
                    playerDataConfig.getInt(path + ".kisses", 0),
                    playerDataConfig.getInt(path + ".likes", 0),
                    playerDataConfig.getString(path + ".effect-type", "heart"),
                    playerDataConfig.getBoolean(path + ".effect-enabled", true),
                    playerDataConfig.getString(path + ".mood", "unknown"),
                    playerDataConfig.getLong(path + ".last-seen", System.currentTimeMillis()),
                    playerDataConfig.getInt(path + ".hugs-given", 0),
                    playerDataConfig.getInt(path + ".kisses-given", 0),
                    playerDataConfig.getInt(path + ".likes-given", 0),
                    new HashSet<>(achievements),
                    friends,
                    pendingFriendRequests,
                    playerDataConfig.getBoolean(path + ".allow-friend-requests", true),
                    playerDataConfig.getBoolean(path + ".allow-marriage-requests", true),
                    playerDataConfig.contains(path + ".age") ? playerDataConfig.getInt(path + ".age") : null
            );
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public List<PlayerData> getTopPlayers(int limit) {
        dataLock.readLock().lock();
        try {
            List<PlayerData> players = new ArrayList<>();
            ConfigurationSection playersSection = playerDataConfig.getConfigurationSection("players");

            if (playersSection != null) {
                for (String uuidString : playersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidString);
                        PlayerData data = loadPlayerData(uuid);
                        if (data != null) {
                            players.add(data);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in player data: " + uuidString);
                    }
                }
            }

            players.sort((a, b) -> Integer.compare(b.getTotalScore(), a.getTotalScore()));
            return players.subList(0, Math.min(limit, players.size()));
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void saveMarriage(Marriage marriage) {
        dataLock.writeLock().lock();
        try {
            String id = UUID.randomUUID().toString();
            String path = "marriages." + id;
            marriagesConfig.set(path + ".player1", marriage.player1().toString());
            marriagesConfig.set(path + ".player2", marriage.player2().toString());
            marriagesConfig.set(path + ".date", marriage.marriageDate());

            pendingWrites.put("marriages", System.currentTimeMillis());
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public Marriage loadMarriage(UUID player) {
        dataLock.readLock().lock();
        try {
            ConfigurationSection marriagesSection = marriagesConfig.getConfigurationSection("marriages");
            if (marriagesSection == null) return null;

            for (String id : marriagesSection.getKeys(false)) {
                String path = "marriages." + id;
                try {
                    UUID player1 = UUID.fromString(marriagesConfig.getString(path + ".player1"));
                    UUID player2 = UUID.fromString(marriagesConfig.getString(path + ".player2"));

                    if (player1.equals(player) || player2.equals(player)) {
                        long date = marriagesConfig.getLong(path + ".date");
                        return new Marriage(player1, player2, date);
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in marriage data: " + id);
                }
            }

            return null;
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void deleteMarriage(UUID player) {
        dataLock.writeLock().lock();
        try {
            ConfigurationSection marriagesSection = marriagesConfig.getConfigurationSection("marriages");
            if (marriagesSection == null) return;

            for (String id : marriagesSection.getKeys(false)) {
                String path = "marriages." + id;
                try {
                    UUID player1 = UUID.fromString(marriagesConfig.getString(path + ".player1"));
                    UUID player2 = UUID.fromString(marriagesConfig.getString(path + ".player2"));

                    if (player1.equals(player) || player2.equals(player)) {
                        marriagesConfig.set("marriages." + id, null);
                        pendingWrites.put("marriages", System.currentTimeMillis());
                        break;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in marriage data: " + id);
                }
            }
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public List<Marriage> getAllMarriages() {
        dataLock.readLock().lock();
        try {
            List<Marriage> marriages = new ArrayList<>();
            ConfigurationSection marriagesSection = marriagesConfig.getConfigurationSection("marriages");

            if (marriagesSection != null) {
                for (String id : marriagesSection.getKeys(false)) {
                    String path = "marriages." + id;
                    try {
                        UUID player1 = UUID.fromString(marriagesConfig.getString(path + ".player1"));
                        UUID player2 = UUID.fromString(marriagesConfig.getString(path + ".player2"));
                        long date = marriagesConfig.getLong(path + ".date");

                        marriages.add(new Marriage(player1, player2, date));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid UUID in marriage data: " + id);
                    }
                }
            }

            return marriages;
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void saveProposal(MarriageProposal proposal) {
        dataLock.writeLock().lock();
        try {
            String path = "proposals." + proposal.target().toString();
            proposalsConfig.set(path + ".proposer", proposal.proposer().toString());
            proposalsConfig.set(path + ".date", proposal.proposalDate());

            pendingWrites.put("proposals", System.currentTimeMillis());
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public MarriageProposal loadProposal(UUID target) {
        dataLock.readLock().lock();
        try {
            String path = "proposals." + target.toString();
            if (!proposalsConfig.contains(path)) return null;

            try {
                UUID proposer = UUID.fromString(proposalsConfig.getString(path + ".proposer"));
                long date = proposalsConfig.getLong(path + ".date");

                return new MarriageProposal(proposer, target, date);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid UUID in proposal data: " + target);
                return null;
            }
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void deleteProposal(UUID target) {
        dataLock.writeLock().lock();
        try {
            proposalsConfig.set("proposals." + target.toString(), null);
            pendingWrites.put("proposals", System.currentTimeMillis());
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public List<MarriageProposal> getAllProposals() {
        dataLock.readLock().lock();
        try {
            List<MarriageProposal> proposals = new ArrayList<>();
            ConfigurationSection section = proposalsConfig.getConfigurationSection("proposals");
            if (section == null) {
                return proposals;
            }

            for (String targetString : section.getKeys(false)) {
                String path = "proposals." + targetString;
                try {
                    UUID target = UUID.fromString(targetString);
                    UUID proposer = UUID.fromString(proposalsConfig.getString(path + ".proposer"));
                    long date = proposalsConfig.getLong(path + ".date");
                    proposals.add(new MarriageProposal(proposer, target, date));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in proposal data: " + targetString);
                }
            }

            return proposals;
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void saveLike(UUID liker, UUID liked) {
        dataLock.writeLock().lock();
        try {
            String id = UUID.randomUUID().toString();
            String path = "likes." + id;
            likesConfig.set(path + ".liker", liker.toString());
            likesConfig.set(path + ".liked", liked.toString());
            likesConfig.set(path + ".date", System.currentTimeMillis());

            pendingWrites.put("likes", System.currentTimeMillis());
        } finally {
            dataLock.writeLock().unlock();
        }
    }

    public boolean hasLiked(UUID liker, UUID liked) {
        dataLock.readLock().lock();
        try {
            ConfigurationSection likesSection = likesConfig.getConfigurationSection("likes");
            if (likesSection == null) return false;

            for (String id : likesSection.getKeys(false)) {
                String path = "likes." + id;
                try {
                    UUID likerUuid = UUID.fromString(likesConfig.getString(path + ".liker"));
                    UUID likedUuid = UUID.fromString(likesConfig.getString(path + ".liked"));

                    if (likerUuid.equals(liker) && likedUuid.equals(liked)) {
                        return true;
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in likes data: " + id);
                }
            }

            return false;
        } finally {
            dataLock.readLock().unlock();
        }
    }

    public void shutdown() {
        shutdownInProgress = true;

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        synchronized (fileLock) {
            if (!pendingWrites.isEmpty()) {
                plugin.getLogger().info("Saving pending data before shutdown...");
                dataLock.writeLock().lock();
                try {
                    saveAllWithLock();
                    pendingWrites.clear();
                } finally {
                    dataLock.writeLock().unlock();
                }
            }
        }

        plugin.getLogger().info("FileStorage shutdown complete");
    }
}