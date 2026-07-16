package lokahst.valentines.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import lokahst.valentines.Valentines;
import lokahst.valentines.data.Achievement;
import lokahst.valentines.data.Marriage;
import lokahst.valentines.data.MarriageProposal;
import lokahst.valentines.data.PlayerData;

import java.text.SimpleDateFormat;
import java.util.*;

public class GuiManager {

    private final Valentines plugin;
    private final Map<UUID, String> playerSearchMode = new HashMap<>();
    private final Map<UUID, Integer> ageEditorValues = new HashMap<>();

    public GuiManager(Valentines plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui.main-menu-title");
        Inventory gui = Bukkit.createInventory(null, 36, title);
        fillWithConfiguredBackground(gui);

        gui.setItem(10, createItem(getConfiguredMaterial("gui.items.main.stats", Material.BOOK),
                plugin.getLanguageManager().getMessage("gui.stats-button-title"),
                plugin.getLanguageManager().getMessageList("gui.stats-button-lore")));

        gui.setItem(12, createItem(getConfiguredMaterial("gui.items.main.marriages", Material.CAKE),
                plugin.getLanguageManager().getMessage("gui.marriages-button-title"),
                plugin.getLanguageManager().getMessageList("gui.marriages-button-lore")));

        gui.setItem(22, createItem(getConfiguredMaterial("gui.items.main.marriage", Material.SUNFLOWER),
                plugin.getLanguageManager().getMessage("gui.marriage-button-title"),
                plugin.getLanguageManager().getMessageList("gui.marriage-button-lore")));

        if (plugin.getConfig().getBoolean("effect.enabled", true)) {
            gui.setItem(14, createItem(getConfiguredMaterial("gui.items.main.settings", Material.REDSTONE),
                    plugin.getLanguageManager().getMessage("gui.settings-button-title"),
                    plugin.getLanguageManager().getMessageList("gui.settings-button-lore")));
        }

        if (plugin.getConfig().getBoolean("leaderboard.enabled", true)) {
            gui.setItem(16, createItem(getConfiguredMaterial("gui.items.main.leaderboard", Material.GOLDEN_APPLE),
                    plugin.getLanguageManager().getMessage("gui.leaderboard-button-title"),
                    plugin.getLanguageManager().getMessageList("gui.leaderboard-button-lore")));
        }

        if (plugin.getConfig().getBoolean("achievements.enabled", true)) {
            gui.setItem(20, createItem(getConfiguredMaterial("gui.items.main.achievements", Material.ROSE_BUSH),
                    plugin.getLanguageManager().getMessage("gui.achievements-button-title"),
                    plugin.getLanguageManager().getMessageList("gui.achievements-button-lore")));
        }

        gui.setItem(24, createItem(getConfiguredMaterial("gui.items.main.player-search", Material.COMPASS),
                plugin.getLanguageManager().getMessage("gui.player-search-button-title"),
                plugin.getLanguageManager().getMessageList("gui.player-search-button-lore")));

        gui.setItem(31, createItem(getConfiguredMaterial("gui.items.main.close", Material.BARRIER), "&cClose", List.of("&7Close this menu")));

        player.openInventory(gui);
    }

    public void openStatsMenu(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui.stats-menu-title");
        Inventory gui = Bukkit.createInventory(null, 36, title);
        fillWithConfiguredBackground(gui);

        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());

        Map<String, String> hugPlaceholders = Map.of("count", String.valueOf(data.getHugsReceived()));
        gui.setItem(10, createItem(Material.EMERALD,
                plugin.getLanguageManager().getMessage("gui.hugs-title"),
                plugin.getLanguageManager().getMessageList("gui.hugs-lore", hugPlaceholders)));

        Map<String, String> kissPlaceholders = Map.of("count", String.valueOf(data.getKissesReceived()));
        gui.setItem(12, createItem(Material.ROSE_BUSH,
                plugin.getLanguageManager().getMessage("gui.kisses-title"),
                plugin.getLanguageManager().getMessageList("gui.kisses-lore", kissPlaceholders)));

        Map<String, String> likePlaceholders = Map.of("count", String.valueOf(data.getLikesReceived()));
        gui.setItem(14, createItem(Material.DIAMOND,
                plugin.getLanguageManager().getMessage("gui.likes-title"),
                plugin.getLanguageManager().getMessageList("gui.likes-lore", likePlaceholders)));

        List<String> profileLore = new ArrayList<>();
        profileLore.add(ChatColor.WHITE + "Total Score: " + ChatColor.LIGHT_PURPLE + data.getTotalScore());
        profileLore.add(ChatColor.WHITE + "Hugs Given: " + ChatColor.LIGHT_PURPLE + data.getHugsGiven());
        profileLore.add(ChatColor.WHITE + "Kisses Given: " + ChatColor.LIGHT_PURPLE + data.getKissesGiven());
        profileLore.add(ChatColor.WHITE + "Likes Given: " + ChatColor.LIGHT_PURPLE + data.getLikesGiven());
        profileLore.add(ChatColor.WHITE + "Age: " + ChatColor.LIGHT_PURPLE + (data.getAge() == null ? "Not set" : data.getAge()));

        gui.setItem(4, createPlayerHead(player, ChatColor.LIGHT_PURPLE + player.getName(), profileLore));

        gui.setItem(18, createItem(Material.CLOCK,
                plugin.getLanguageManager().getMessage("gui.age-title"),
                plugin.getLanguageManager().getMessageList("gui.age-lore", Map.of("age", data.getAge() == null ? "Not set" : String.valueOf(data.getAge())))));

        gui.setItem(20, createItem(Material.PLAYER_HEAD,
                plugin.getLanguageManager().getMessage("gui.my-friends-title"),
                plugin.getLanguageManager().getMessageList("gui.my-friends-lore", Map.of("count", String.valueOf(data.getFriends().size())))));

        gui.setItem(24, createItem(Material.LEVER,
                plugin.getLanguageManager().getMessage("gui.friend-requests-toggle-title"),
                plugin.getLanguageManager().getMessageList("gui.friend-requests-toggle-lore", Map.of("state", data.isAllowFriendRequests() ? "Enabled" : "Disabled"))));
        gui.setItem(26, createItem(Material.NAME_TAG,
                plugin.getLanguageManager().getMessage("gui.friend-chat-add-title"),
                plugin.getLanguageManager().getMessageList("gui.friend-chat-add-lore")));

        Marriage marriage = plugin.getMarriageManager().getMarriage(player.getUniqueId());
        if (marriage != null) {
            UUID partnerUuid = marriage.getPartner(player.getUniqueId());
            PlayerData partnerData = plugin.getPlayerDataManager().getOrLoadPlayerData(partnerUuid);
            Map<String, String> marriagePlaceholders = new HashMap<>();
            marriagePlaceholders.put("partner", partnerData.getName());
            marriagePlaceholders.put("days", String.valueOf(marriage.getDaysMarried()));
            gui.setItem(16, createItem(getConfiguredMaterial("gui.items.main.marriage", Material.SUNFLOWER),
                    plugin.getLanguageManager().getMessage("gui.marriage-title"),
                    plugin.getLanguageManager().getMessageList("gui.marriage-lore", marriagePlaceholders)));
        } else {
            gui.setItem(16, createItem(Material.APPLE,
                    plugin.getLanguageManager().getMessage("gui.single-title"),
                    plugin.getLanguageManager().getMessageList("gui.single-lore")));
        }

        String moodDisplay = getMoodDisplay(data.getMood());
        gui.setItem(22, createItem(getConfiguredMaterial("gui.items.stats.mood-display", Material.TOTEM_OF_UNDYING),
                plugin.getLanguageManager().getMessage("gui.mood-display-title"),
                plugin.getLanguageManager().getMessageList("gui.mood-display-lore", Map.of("mood", moodDisplay))));

        gui.setItem(31, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        player.openInventory(gui);
    }

    public void openAgeEditor(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());
        int currentAge = ageEditorValues.getOrDefault(player.getUniqueId(), data.getAge() != null ? data.getAge() : 18);
        currentAge = Math.max(13, Math.min(99, currentAge));
        ageEditorValues.put(player.getUniqueId(), currentAge);

        Inventory gui = Bukkit.createInventory(null, 27, plugin.getLanguageManager().getMessage("gui.age-editor-title"));
        fillWithConfiguredBackground(gui);
        gui.setItem(10, createItem(Material.RED_DYE, plugin.getLanguageManager().getMessage("gui.age-decrease-title"), List.of("&fCurrent: &d" + currentAge)));
        gui.setItem(13, createItem(Material.CLOCK, plugin.getLanguageManager().getMessage("gui.age-current-title", Map.of("age", String.valueOf(currentAge))), new ArrayList<>()));
        gui.setItem(16, createItem(Material.LIME_DYE, plugin.getLanguageManager().getMessage("gui.age-increase-title"), List.of("&fCurrent: &d" + currentAge)));
        gui.setItem(22, createItem(Material.EMERALD_BLOCK, plugin.getLanguageManager().getMessage("gui.age-save-title"), new ArrayList<>()));
        gui.setItem(21, createItem(Material.BARRIER, plugin.getLanguageManager().getMessage("gui.age-unset-title"), new ArrayList<>()));
        gui.setItem(23, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        player.openInventory(gui);
    }

    public void adjustAgeDraft(UUID uuid, int delta) {
        int current = ageEditorValues.getOrDefault(uuid, 18);
        current = Math.max(13, Math.min(99, current + delta));
        ageEditorValues.put(uuid, current);
    }

    public int getAgeDraft(UUID uuid) {
        return ageEditorValues.getOrDefault(uuid, 18);
    }

    public void clearAgeDraft(UUID uuid) {
        ageEditorValues.remove(uuid);
    }

    public void openFriendsMenu(Player viewer, UUID profileOwnerUuid) {
        PlayerData ownerData = plugin.getPlayerDataManager().getOrLoadPlayerData(profileOwnerUuid);
        String title = plugin.getLanguageManager().getMessage("gui.friends-menu-title", Map.of("player", ownerData.getName()));
        Inventory gui = Bukkit.createInventory(null, 54, title);
        fillWithConfiguredBackground(gui);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int slot = 0;
        for (Map.Entry<UUID, Long> entry : ownerData.getFriends().entrySet()) {
            if (slot >= 45) {
                break;
            }
            PlayerData friendData = plugin.getPlayerDataManager().getOrLoadPlayerData(entry.getKey());
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("friend", friendData.getName());
            placeholders.put("since", dateFormat.format(new Date(entry.getValue())));
            gui.setItem(slot, createPlayerHead(Bukkit.getOfflinePlayer(entry.getKey()),
                    plugin.getLanguageManager().getMessage("gui.friend-entry-title", placeholders),
                    plugin.getLanguageManager().getMessageList("gui.friend-entry-lore", placeholders)));
            slot++;
        }

        gui.setItem(49, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        viewer.openInventory(gui);
    }

    public void openMoodMenu(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui.mood-title");
        Inventory gui = Bukkit.createInventory(null, 36, title);
        fillWithConfiguredBackground(gui);
        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());
        String currentMood = data.getMood();
        gui.setItem(11, createItem(currentMood.equals("very-good") ? Material.LIME_WOOL : Material.GREEN_WOOL, plugin.getLanguageManager().getMessage("gui.mood-very-good-title") + (currentMood.equals("very-good") ? " " + plugin.getLanguageManager().getMessage("gui.effect-selected") : ""), plugin.getLanguageManager().getMessageList("gui.mood-very-good-lore")));
        gui.setItem(12, createItem(currentMood.equals("good") ? Material.LIME_WOOL : Material.YELLOW_WOOL, plugin.getLanguageManager().getMessage("gui.mood-good-title") + (currentMood.equals("good") ? " " + plugin.getLanguageManager().getMessage("gui.effect-selected") : ""), plugin.getLanguageManager().getMessageList("gui.mood-good-lore")));
        gui.setItem(13, createItem(currentMood.equals("neutral") ? Material.LIME_WOOL : Material.WHITE_WOOL, plugin.getLanguageManager().getMessage("gui.mood-neutral-title") + (currentMood.equals("neutral") ? " " + plugin.getLanguageManager().getMessage("gui.effect-selected") : ""), plugin.getLanguageManager().getMessageList("gui.mood-neutral-lore")));
        gui.setItem(14, createItem(currentMood.equals("bad") ? Material.LIME_WOOL : Material.ORANGE_WOOL, plugin.getLanguageManager().getMessage("gui.mood-bad-title") + (currentMood.equals("bad") ? " " + plugin.getLanguageManager().getMessage("gui.effect-selected") : ""), plugin.getLanguageManager().getMessageList("gui.mood-bad-lore")));
        gui.setItem(15, createItem(currentMood.equals("very-bad") ? Material.LIME_WOOL : Material.RED_WOOL, plugin.getLanguageManager().getMessage("gui.mood-very-bad-title") + (currentMood.equals("very-bad") ? " " + plugin.getLanguageManager().getMessage("gui.effect-selected") : ""), plugin.getLanguageManager().getMessageList("gui.mood-very-bad-lore")));
        gui.setItem(31, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        player.openInventory(gui);
    }

    public void openLeaderboardMenu(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui.leaderboard-title");
        Inventory gui = Bukkit.createInventory(null, 54, title);
        fillWithConfiguredBackground(gui);
        List<PlayerData> topPlayers = plugin.getPlayerDataManager().getTopPlayers(45);
        for (int i = 0; i < topPlayers.size() && i < 45; i++) {
            PlayerData data = topPlayers.get(i);
            List<String> lore = List.of("&fTotal Love Score: &d" + data.getTotalScore(), "&fHugs: &d" + data.getHugsReceived(), "&fKisses: &d" + data.getKissesReceived(), "&fLikes: &d" + data.getLikesReceived());
            OfflinePlayer topPlayer = Bukkit.getOfflinePlayer(data.getUuid());
            gui.setItem(i, createPlayerHead(topPlayer, plugin.getLanguageManager().getMessage("gui.leaderboard-entry-title", Map.of("rank", String.valueOf(i + 1), "player", data.getName())), lore));
        }
        gui.setItem(49, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        player.openInventory(gui);
    }


    public void openMarriageMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 36, plugin.getLanguageManager().getMessage("gui.marriage-menu-title"));
        fillWithConfiguredBackground(gui);

        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());
        MarriageProposal proposal = plugin.getMarriageManager().getProposal(player.getUniqueId());
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("state", data.isAllowMarriageRequests() ? plugin.getLanguageManager().getMessage("gui.enabled-state") : plugin.getLanguageManager().getMessage("gui.disabled-state"));
        placeholders.put("count", proposal == null ? "0" : "1");

        gui.setItem(11, createItem(Material.COMPASS,
                plugin.getLanguageManager().getMessage("gui.marriage-find-title"),
                plugin.getLanguageManager().getMessageList("gui.marriage-find-lore")));
        gui.setItem(13, createItem(data.isAllowMarriageRequests() ? Material.LIME_DYE : Material.GRAY_DYE,
                plugin.getLanguageManager().getMessage("gui.marriage-requests-toggle-title"),
                plugin.getLanguageManager().getMessageList("gui.marriage-requests-toggle-lore", placeholders)));
        gui.setItem(15, createItem(proposal == null ? Material.PAPER : Material.SUNFLOWER,
                plugin.getLanguageManager().getMessage("gui.marriage-proposals-title"),
                plugin.getLanguageManager().getMessageList("gui.marriage-proposals-lore", placeholders)));
        gui.setItem(31, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));

        player.openInventory(gui);
    }

    public void openMarriageProposalsMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 27, plugin.getLanguageManager().getMessage("gui.marriage-proposals-menu-title"));
        fillWithConfiguredBackground(gui);

        MarriageProposal proposal = plugin.getMarriageManager().getProposal(player.getUniqueId());
        if (proposal == null) {
            gui.setItem(13, createItem(Material.BARRIER,
                    plugin.getLanguageManager().getMessage("gui.no-marriage-proposals-title"),
                    plugin.getLanguageManager().getMessageList("gui.no-marriage-proposals-lore")));
        } else {
            PlayerData proposerData = plugin.getPlayerDataManager().getOrLoadPlayerData(proposal.proposer());
            Map<String, String> placeholders = Map.of("player", proposerData.getName());
            gui.setItem(11, createItem(Material.EMERALD_BLOCK,
                    plugin.getLanguageManager().getMessage("gui.marriage-accept-title", placeholders),
                    plugin.getLanguageManager().getMessageList("gui.marriage-accept-lore", placeholders)));
            gui.setItem(15, createItem(Material.BARRIER,
                    plugin.getLanguageManager().getMessage("gui.marriage-deny-title", placeholders),
                    plugin.getLanguageManager().getMessageList("gui.marriage-deny-lore", placeholders)));
        }

        gui.setItem(31, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        player.openInventory(gui);
    }

    public void openMarriagesMenu(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui.marriages-title");
        Inventory gui = Bukkit.createInventory(null, 54, title);
        fillWithConfiguredBackground(gui);
        List<Marriage> marriages = plugin.getMarriageManager().getAllMarriages();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (int i = 0; i < marriages.size() && i < 45; i++) {
            Marriage marriage = marriages.get(i);
            PlayerData p1 = plugin.getPlayerDataManager().getOrLoadPlayerData(marriage.player1());
            PlayerData p2 = plugin.getPlayerDataManager().getOrLoadPlayerData(marriage.player2());
            Map<String, String> placeholders = Map.of("player1", p1.getName(), "player2", p2.getName(), "days", String.valueOf(marriage.getDaysMarried()), "date", dateFormat.format(new Date(marriage.marriageDate())));
            gui.setItem(i, createItem(Material.CAKE, plugin.getLanguageManager().getMessage("gui.marriage-couple-title", placeholders), plugin.getLanguageManager().getMessageList("gui.marriage-couple-lore", placeholders)));
        }
        gui.setItem(49, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        player.openInventory(gui);
    }

    public void openSettingsMenu(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui.settings-title");
        Inventory gui = Bukkit.createInventory(null, 36, title);
        fillWithConfiguredBackground(gui);
        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());
        String currentEffect = data.getEffectType();
        gui.setItem(10, createItem(currentEffect.equals("heart") ? Material.REDSTONE_BLOCK : Material.RED_WOOL, plugin.getLanguageManager().getMessage("gui.effect-heart") + (currentEffect.equals("heart") ? " " + plugin.getLanguageManager().getMessage("gui.effect-selected") : ""), new ArrayList<>()));
        gui.setItem(12, createItem(currentEffect.equals("spiral") ? Material.REDSTONE_BLOCK : Material.YELLOW_WOOL, plugin.getLanguageManager().getMessage("gui.effect-spiral") + (currentEffect.equals("spiral") ? " " + plugin.getLanguageManager().getMessage("gui.effect-selected") : ""), new ArrayList<>()));
        boolean rainbowSelected = currentEffect.equals("rainbow") || currentEffect.equals("cloud");
        gui.setItem(14, createItem(rainbowSelected ? Material.REDSTONE_BLOCK : Material.PINK_WOOL, plugin.getLanguageManager().getMessage("gui.effect-rainbow") + (rainbowSelected ? " " + plugin.getLanguageManager().getMessage("gui.effect-selected") : ""), new ArrayList<>()));
        gui.setItem(16, createItem(data.isEffectEnabled() ? Material.LIME_DYE : Material.GRAY_DYE, data.isEffectEnabled() ? plugin.getLanguageManager().getMessage("gui.effect-selected") : plugin.getLanguageManager().getMessage("gui.effect-disabled"), new ArrayList<>()));
        gui.setItem(31, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        player.openInventory(gui);
    }

    public void openAchievementsMenu(Player player) {
        String title = plugin.getLanguageManager().getMessage("gui.achievements-menu-title");
        Inventory gui = Bukkit.createInventory(null, 36, title);
        fillWithConfiguredBackground(gui);
        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());
        Achievement[] achievements = Achievement.values();
        for (int i = 0; i < achievements.length && i < 45; i++) {
            Achievement achievement = achievements[i];
            boolean hasAchievement = data.hasAchievement(achievement.getKey());
            Material material = hasAchievement ? Material.LIME_DYE : Material.GRAY_DYE;
            String status = hasAchievement ? "&a✔ " : "&7✘ ";
            String name = plugin.getLanguageManager().getMessage("achievements." + achievement.getKey() + ".name");
            String description = plugin.getLanguageManager().getMessage("achievements." + achievement.getKey() + ".description");
            gui.setItem(i, createItem(material, status + "&d" + name, List.of("&f" + description, "", hasAchievement ? "&aUnlocked!" : "&7Locked")));
        }
        int unlocked = plugin.getAchievementManager().getUnlockedCount(player.getUniqueId());
        int total = Achievement.values().length;
        double percentage = plugin.getAchievementManager().getProgressPercentage(player.getUniqueId());
        gui.setItem(31, createItem(Material.BOOK, "&d&lAchievement Progress", List.of("&fUnlocked: &d" + unlocked + "&f/&d" + total, "&fProgress: &d" + String.format("%.1f", percentage) + "%")));
        gui.setItem(30, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        player.openInventory(gui);
    }

    public void openPlayerProfile(Player viewer, Player target) {
        Inventory gui = Bukkit.createInventory(null, 36, plugin.getLanguageManager().getMessage("gui.player-profile-title", Map.of("player", target.getName())));
        fillWithConfiguredBackground(gui);
        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(target.getUniqueId());
        PlayerData viewerData = plugin.getPlayerDataManager().getOrLoadPlayerData(viewer.getUniqueId());

        List<String> profileLore = new ArrayList<>();
        profileLore.add(ChatColor.WHITE + "Total Score: " + ChatColor.LIGHT_PURPLE + data.getTotalScore());
        profileLore.add(ChatColor.WHITE + "Hugs: " + ChatColor.LIGHT_PURPLE + data.getHugsReceived());
        profileLore.add(ChatColor.WHITE + "Kisses: " + ChatColor.LIGHT_PURPLE + data.getKissesReceived());
        profileLore.add(ChatColor.WHITE + "Likes: " + ChatColor.LIGHT_PURPLE + data.getLikesReceived());
        profileLore.add(ChatColor.WHITE + "Age: " + ChatColor.LIGHT_PURPLE + (data.getAge() == null ? "Hidden" : data.getAge()));
        profileLore.add(ChatColor.WHITE + "Account Type: " + ChatColor.LIGHT_PURPLE + getAccountType(target));
        gui.setItem(4, createPlayerHead(target, ChatColor.LIGHT_PURPLE + target.getName() + "'s Profile", profileLore));

        Map<String, String> placeholders = Map.of("player", target.getName());
        gui.setItem(10, createItem(Material.EMERALD, plugin.getLanguageManager().getMessage("gui.hug-button-title", placeholders), plugin.getLanguageManager().getMessageList("gui.hug-button-lore", placeholders)));
        gui.setItem(12, createItem(Material.ROSE_BUSH, plugin.getLanguageManager().getMessage("gui.kiss-button-title", placeholders), plugin.getLanguageManager().getMessageList("gui.kiss-button-lore", placeholders)));
        gui.setItem(14, createItem(Material.DIAMOND, plugin.getLanguageManager().getMessage("gui.like-button-title", placeholders), plugin.getLanguageManager().getMessageList("gui.like-button-lore", placeholders)));

        if (!plugin.getMarriageManager().isMarried(viewer.getUniqueId()) && !plugin.getMarriageManager().isMarried(target.getUniqueId())) {
            gui.setItem(16, createItem(getConfiguredMaterial("gui.items.main.leaderboard", Material.GOLDEN_APPLE), plugin.getLanguageManager().getMessage("gui.marry-button-title", placeholders), plugin.getLanguageManager().getMessageList("gui.marry-button-lore", placeholders)));
        }

        gui.setItem(20, createItem(Material.PLAYER_HEAD,
                plugin.getLanguageManager().getMessage("gui.profile-friends-title"),
                plugin.getLanguageManager().getMessageList("gui.profile-friends-lore", Map.of("count", String.valueOf(data.getFriends().size())))));

        if (!target.equals(viewer)) {
            Material friendMat;
            String friendTitle;
            List<String> lore;
            if (viewerData.getFriends().containsKey(target.getUniqueId())) {
                friendMat = Material.REDSTONE;
                friendTitle = plugin.getLanguageManager().getMessage("gui.friend-remove-title");
                lore = plugin.getLanguageManager().getMessageList("gui.friend-remove-lore", Map.of("player", target.getName()));
            } else if (viewerData.getPendingFriendRequests().contains(target.getUniqueId())) {
                friendMat = Material.LIME_DYE;
                friendTitle = plugin.getLanguageManager().getMessage("gui.friend-accept-title");
                lore = plugin.getLanguageManager().getMessageList("gui.friend-accept-lore", Map.of("player", target.getName()));
                gui.setItem(25, createItem(Material.BARRIER,
                        plugin.getLanguageManager().getMessage("gui.friend-deny-title"),
                        plugin.getLanguageManager().getMessageList("gui.friend-deny-lore", Map.of("player", target.getName()))));
            } else {
                friendMat = Material.PAPER;
                friendTitle = plugin.getLanguageManager().getMessage("gui.friend-add-title");
                lore = plugin.getLanguageManager().getMessageList("gui.friend-add-lore", Map.of("player", target.getName()));
            }
            gui.setItem(24, createItem(friendMat, friendTitle, lore));
        }

        gui.setItem(22, createItem(getConfiguredMaterial("gui.items.stats.mood-display", Material.TOTEM_OF_UNDYING), ChatColor.LIGHT_PURPLE + target.getName() + "'s Mood", List.of(ChatColor.WHITE + "Current mood: " + getMoodDisplay(data.getMood()))));
        gui.setItem(31, createItem(Material.ARROW, plugin.getLanguageManager().getMessage("gui.back-button"), new ArrayList<>()));
        viewer.openInventory(gui);
    }

    private String getMoodDisplay(String mood) {
        return switch (mood) {
            case "very-good" -> ChatColor.GREEN + "😄 Very Good";
            case "good" -> ChatColor.YELLOW + "😊 Good";
            case "neutral" -> ChatColor.WHITE + "😐 Neutral";
            case "bad" -> ChatColor.RED + "😞 Bad";
            case "very-bad" -> ChatColor.DARK_RED + "😢 Very Bad";
            default -> ChatColor.GRAY + "❓ Unknown";
        };
    }

    private String getAccountType(Player target) {
        try {
            if (target.getPlayerProfile() != null && target.getPlayerProfile().getTextures() != null && target.getPlayerProfile().getTextures().getSkin() != null) return "Premium";
        } catch (Exception ignored) {
        }
        return "Non-Premium/Offline";
    }

    private ItemStack createPlayerHead(Player target, String name, List<String> lore) { return createPlayerHead((OfflinePlayer) target, name, lore); }
    private ItemStack createPlayerHead(OfflinePlayer target, String name, List<String> lore) {
        ItemStack item = createItem(Material.PLAYER_HEAD, name, lore);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(target);
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    public void startPlayerSearch(Player player) {
        playerSearchMode.put(player.getUniqueId(), "searching");
        player.closeInventory();
        player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("player-search.prompt"));
    }

    public void startFriendRequestInput(Player player) {
        playerSearchMode.put(player.getUniqueId(), "friend_request");
        player.closeInventory();
        player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.chat-add-prompt"));
    }

    public void startMarriageProposalInput(Player player) {
        playerSearchMode.put(player.getUniqueId(), "marriage_proposal");
        player.closeInventory();
        player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.chat-propose-prompt"));
    }

    public boolean isInSearchMode(Player player) {
        return "searching".equals(playerSearchMode.get(player.getUniqueId()));
    }

    public boolean isInFriendRequestMode(Player player) {
        return "friend_request".equals(playerSearchMode.get(player.getUniqueId()));
    }

    public boolean isInMarriageProposalMode(Player player) {
        return "marriage_proposal".equals(playerSearchMode.get(player.getUniqueId()));
    }

    public void handlePlayerSearch(Player searcher, String playerName) {
        playerSearchMode.remove(searcher.getUniqueId());
        Player target = Bukkit.getPlayer(playerName);
        if (target == null || !target.isOnline()) {
            searcher.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("player-search.not-found", Map.of("player", playerName)));
            return;
        }
        if (target.equals(searcher)) {
            openStatsMenu(searcher);
            return;
        }
        searcher.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("player-search.searching", Map.of("player", target.getName())));
        openPlayerProfile(searcher, target);
    }

    public void handleFriendRequestInput(Player sender, String playerName) {
        playerSearchMode.remove(sender.getUniqueId());
        Player target = Bukkit.getPlayer(playerName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.chat-add-not-found", Map.of("player", playerName)));
            return;
        }

        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.chat-add-self"));
            return;
        }

        plugin.getFriendManager().sendFriendRequest(sender, target);
    }

    public void handleMarriageProposalInput(Player sender, String playerName) {
        playerSearchMode.remove(sender.getUniqueId());
        Player target = Bukkit.getPlayer(playerName);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.chat-propose-not-found", Map.of("player", playerName)));
            return;
        }

        plugin.getMarriageManager().sendProposal(sender, target);
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
            meta.setLore(coloredLore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private Material getConfiguredMaterial(String path, Material defaultMaterial) {
        String materialName = plugin.getConfig().getString(path, defaultMaterial.name());
        Material configured = Material.matchMaterial(materialName == null ? "" : materialName);
        if (configured == null || configured.isAir()) {
            return defaultMaterial;
        }
        return configured;
    }

    private void fillWithConfiguredBackground(Inventory gui) {
        if (!plugin.getConfig().getBoolean("gui.background.enabled", true)) {
            return;
        }

        String materialName = plugin.getConfig().getString("gui.background.material", "PINK_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(materialName == null ? "" : materialName);
        if (material == null || material.isAir()) {
            material = Material.PINK_STAINED_GLASS_PANE;
        }

        String name = plugin.getConfig().getString("gui.background.name", "&7");
        ItemStack filler = createItem(material, name, List.of());
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null || gui.getItem(i).getType().isAir()) {
                gui.setItem(i, filler);
            }
        }
    }
}