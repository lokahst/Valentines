package lokahst.valentines.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import lokahst.valentines.Valentines;
import lokahst.valentines.data.PlayerData;

import java.util.*;

public class PlayerDataManager {

    private final Valentines plugin;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();

    public PlayerDataManager(Valentines plugin) {
        this.plugin = plugin;
    }

    public void loadPlayerData(Player player) {
        UUID uuid = player.getUniqueId();
        PlayerData data = plugin.getFileStorage().loadPlayerData(uuid);

        if (data == null) {
            data = PlayerData.createDefault(uuid, player.getName());
            plugin.getFileStorage().savePlayerData(data);
        } else if (!data.getName().equals(player.getName())) {
            data.setName(player.getName());
            plugin.getFileStorage().savePlayerData(data);
        }

        playerDataCache.put(uuid, data);
    }

    public PlayerData getOrLoadPlayerData(UUID uuid) {
        PlayerData cached = playerDataCache.get(uuid);
        if (cached != null) {
            return cached;
        }

        PlayerData loaded = plugin.getFileStorage().loadPlayerData(uuid);
        if (loaded != null) {
            playerDataCache.put(uuid, loaded);
            return loaded;
        }

        String name = Optional.ofNullable(Bukkit.getOfflinePlayer(uuid).getName()).orElse("Unknown");
        PlayerData created = PlayerData.createDefault(uuid, name);
        plugin.getFileStorage().savePlayerData(created);
        playerDataCache.put(uuid, created);
        return created;
    }

    public void unloadPlayerData(UUID uuid) {
        PlayerData data = playerDataCache.remove(uuid);
        if (data != null) {
            data.setLastSeen(System.currentTimeMillis());
            plugin.getFileStorage().savePlayerData(data);
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        return playerDataCache.get(uuid);
    }

    public void incrementStat(UUID uuid, String statType) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            switch (statType) {
                case "hugs" -> data.setHugsReceived(data.getHugsReceived() + 1);
                case "kisses" -> data.setKissesReceived(data.getKissesReceived() + 1);
                case "likes" -> data.setLikesReceived(data.getLikesReceived() + 1);
            }
            plugin.getFileStorage().savePlayerData(data);
        }
    }

    public void incrementGivenStat(UUID uuid, String statType) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            switch (statType) {
                case "hugs" -> data.setHugsGiven(data.getHugsGiven() + 1);
                case "kisses" -> data.setKissesGiven(data.getKissesGiven() + 1);
                case "likes" -> data.setLikesGiven(data.getLikesGiven() + 1);
            }
            plugin.getFileStorage().savePlayerData(data);
        }
    }

    public void savePlayerData(PlayerData data) {
        plugin.getFileStorage().savePlayerData(data);
    }

    public void updateEffectType(UUID uuid, String effectType) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.setEffectType(effectType);
            plugin.getFileStorage().savePlayerData(data);
        }
    }

    public void updateMood(UUID uuid, String mood) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.setMood(mood);
            plugin.getFileStorage().savePlayerData(data);
        }
    }

    public void updateAge(UUID uuid, Integer age) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.setAge(age);
            plugin.getFileStorage().savePlayerData(data);
        }
    }

    public void toggleEffects(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data != null) {
            data.setEffectEnabled(!data.isEffectEnabled());
            plugin.getFileStorage().savePlayerData(data);
        }
    }

    public void toggleMarriageRequests(UUID uuid) {
        PlayerData data = getOrLoadPlayerData(uuid);
        data.setAllowMarriageRequests(!data.isAllowMarriageRequests());
        plugin.getFileStorage().savePlayerData(data);
    }

    public List<PlayerData> getTopPlayers(int limit) {
        return plugin.getFileStorage().getTopPlayers(limit);
    }

    public void saveAllData() {
        for (PlayerData data : playerDataCache.values()) {
            plugin.getFileStorage().savePlayerData(data);
        }
    }
}