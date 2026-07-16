package lokahst.valentines.listeners;

import lokahst.valentines.events.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import lokahst.valentines.events.*;
import lokahst.valentines.Valentines;

import java.util.Map;

public class PlayerListener implements Listener {

    private final Valentines plugin;

    public PlayerListener(Valentines plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().loadPlayerData(event.getPlayer());
        plugin.getMarriageManager().loadMarriage(event.getPlayer().getUniqueId());
        plugin.getFriendManager().notifyFriendStatus(event.getPlayer(), true);
        plugin.getFriendManager().checkFriendDurationAchievement(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getFriendManager().notifyFriendStatus(event.getPlayer(), false);
        plugin.getPlayerDataManager().unloadPlayerData(event.getPlayer().getUniqueId());
        plugin.getCooldownManager().clearCooldowns(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getGuiManager().isInSearchMode(player)) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().handlePlayerSearch(player, event.getMessage()));
            return;
        }

        if (plugin.getGuiManager().isInFriendRequestMode(player)) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().handleFriendRequestInput(player, event.getMessage()));
            return;
        }

        if (plugin.getGuiManager().isInMarriageProposalMode(player)) {
            event.setCancelled(true);
            plugin.getServer().getScheduler().runTask(plugin, () -> plugin.getGuiManager().handleMarriageProposalInput(player, event.getMessage()));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        if (!(title.contains("Valentines") || title.contains("❤") || title.contains("Mood") || title.contains("Stats") ||
                title.contains("Leaderboard") || title.contains("Couples") || title.contains("Settings") || title.contains("Profile") ||
                title.contains("Achievement") || title.contains("Friends") || title.contains("Age"))) {
            return;
        }

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType().isAir()) return;
        if (!event.getCurrentItem().hasItemMeta() || event.getCurrentItem().getItemMeta() == null || event.getCurrentItem().getItemMeta().getDisplayName() == null) return;
        String itemName = event.getCurrentItem().getItemMeta().getDisplayName();

        if (title.equals(plugin.getLanguageManager().getMessage("gui.main-menu-title"))) {
            handleMainMenuClick(player, itemName);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.mood-title"))) {
            handleMoodMenuClick(player, itemName);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.age-editor-title"))) {
            handleAgeEditorClick(player, itemName);
        } else if (title.equals(plugin.getLanguageManager().getMessage("gui.stats-menu-title")) || title.equals(plugin.getLanguageManager().getMessage("gui.leaderboard-title")) ||
                title.equals(plugin.getLanguageManager().getMessage("gui.marriages-title")) || title.equals(plugin.getLanguageManager().getMessage("gui.marriage-menu-title")) ||
                title.equals(plugin.getLanguageManager().getMessage("gui.marriage-proposals-menu-title")) || title.equals(plugin.getLanguageManager().getMessage("gui.settings-title")) ||
                title.equals(plugin.getLanguageManager().getMessage("gui.achievements-menu-title")) || title.contains("Profile") || title.contains("Friends")) {
            handleSubMenuClick(player, itemName, title);
        }
    }

    private void handleMainMenuClick(Player player, String itemName) {
        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.stats-button-title"))) plugin.getGuiManager().openStatsMenu(player);
        else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.leaderboard-button-title"))) plugin.getGuiManager().openLeaderboardMenu(player);
        else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.marriages-button-title"))) plugin.getGuiManager().openMarriagesMenu(player);
        else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.marriage-button-title"))) plugin.getGuiManager().openMarriageMenu(player);
        else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.settings-button-title"))) plugin.getGuiManager().openSettingsMenu(player);
        else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.achievements-button-title"))) plugin.getGuiManager().openAchievementsMenu(player);
        else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.player-search-button-title"))) plugin.getGuiManager().startPlayerSearch(player);
        else if (itemName.equalsIgnoreCase("§cClose") || itemName.equalsIgnoreCase("Close")) player.closeInventory();
    }

    private void handleMoodMenuClick(Player player, String itemName) {
        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.back-button"))) {
            plugin.getGuiManager().openStatsMenu(player);
            return;
        }

        String mood = null; String moodMessage = null;
        if (itemName.contains("Very Good")) { mood = "very-good"; moodMessage = "Very Good"; }
        else if (itemName.contains("Good") && !itemName.contains("Very")) { mood = "good"; moodMessage = "Good"; }
        else if (itemName.contains("Neutral")) { mood = "neutral"; moodMessage = "Neutral"; }
        else if (itemName.contains("Bad") && !itemName.contains("Very")) { mood = "bad"; moodMessage = "Bad"; }
        else if (itemName.contains("Very Bad")) { mood = "very-bad"; moodMessage = "Very Bad"; }

        if (mood != null) {
            plugin.getPlayerDataManager().updateMood(player.getUniqueId(), mood);
            plugin.getServer().getPluginManager().callEvent(new MoodSetEvent(player, mood));
            player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("mood.updated", Map.of("mood", moodMessage)));
            plugin.getGuiManager().openMoodMenu(player);
        }
    }

    private void handleAgeEditorClick(Player player, String itemName) {
        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.back-button"))) {
            plugin.getGuiManager().clearAgeDraft(player.getUniqueId());
            plugin.getGuiManager().openStatsMenu(player);
            return;
        }

        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.age-decrease-title"))) {
            plugin.getGuiManager().adjustAgeDraft(player.getUniqueId(), -1);
            plugin.getGuiManager().openAgeEditor(player);
        } else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.age-increase-title"))) {
            plugin.getGuiManager().adjustAgeDraft(player.getUniqueId(), 1);
            plugin.getGuiManager().openAgeEditor(player);
        } else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.age-save-title"))) {
            int value = plugin.getGuiManager().getAgeDraft(player.getUniqueId());
            plugin.getPlayerDataManager().updateAge(player.getUniqueId(), value);
            plugin.getGuiManager().clearAgeDraft(player.getUniqueId());
            player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("age.updated", Map.of("age", String.valueOf(value))));
            plugin.getGuiManager().openStatsMenu(player);
        } else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.age-unset-title"))) {
            plugin.getPlayerDataManager().updateAge(player.getUniqueId(), null);
            plugin.getGuiManager().clearAgeDraft(player.getUniqueId());
            player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("age.removed"));
            plugin.getGuiManager().openStatsMenu(player);
        }
    }

    private void handleSubMenuClick(Player player, String itemName, String title) {
        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.back-button"))) {
            if (title.equals(plugin.getLanguageManager().getMessage("gui.marriage-proposals-menu-title"))) {
                plugin.getGuiManager().openMarriageMenu(player);
            } else {
                plugin.getGuiManager().openMainMenu(player);
            }
            return;
        }

        if (title.equals(plugin.getLanguageManager().getMessage("gui.marriage-menu-title"))) {
            handleMarriageMenuClick(player, itemName);
            return;
        }
        if (title.equals(plugin.getLanguageManager().getMessage("gui.marriage-proposals-menu-title"))) {
            handleMarriageProposalsClick(player, itemName);
            return;
        }
        if (title.equals(plugin.getLanguageManager().getMessage("gui.settings-title"))) {
            handleSettingsClick(player, itemName);
        }
        if (title.equals(plugin.getLanguageManager().getMessage("gui.stats-menu-title"))) {
            if (itemName.equals(plugin.getLanguageManager().getMessage("gui.age-title"))) {
                plugin.getGuiManager().openAgeEditor(player);
            } else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.mood-display-title"))) {
                plugin.getGuiManager().openMoodMenu(player);
            } else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.my-friends-title"))) {
                plugin.getGuiManager().openFriendsMenu(player, player.getUniqueId());
            } else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-requests-toggle-title"))) {
                if (!player.hasPermission("valentines.friends.use") && !player.hasPermission("valentines.friends.*")) {
                    player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("general.no-permission"));
                    return;
                }
                plugin.getFriendManager().toggleFriendRequests(player);
                plugin.getGuiManager().openStatsMenu(player);
            } else if (itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-chat-add-title"))) {
                if (!player.hasPermission("valentines.friends.add") && !player.hasPermission("valentines.friends.*")) {
                    player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("general.no-permission"));
                    return;
                }
                plugin.getGuiManager().startFriendRequestInput(player);
            }
        }
        if (title.contains("Profile")) {
            handlePlayerProfileClick(player, itemName, title);
        }
    }


    private void handleMarriageMenuClick(Player player, String itemName) {
        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.marriage-find-title"))) {
            plugin.getGuiManager().startMarriageProposalInput(player);
            return;
        }

        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.marriage-requests-toggle-title"))) {
            plugin.getPlayerDataManager().toggleMarriageRequests(player.getUniqueId());
            plugin.getGuiManager().openMarriageMenu(player);
            return;
        }

        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.marriage-proposals-title"))) {
            plugin.getGuiManager().openMarriageProposalsMenu(player);
        }
    }

    private void handleMarriageProposalsClick(Player player, String itemName) {
        if (itemName.contains("Accept")) {
            player.closeInventory();
            plugin.getMarriageManager().acceptProposal(player);
        } else if (itemName.contains("Deny")) {
            player.closeInventory();
            plugin.getMarriageManager().denyProposal(player);
        }
    }

    private void handleSettingsClick(Player player, String itemName) {
        if (itemName.contains("Heart")) plugin.getPlayerDataManager().updateEffectType(player.getUniqueId(), "heart");
        else if (itemName.contains("Spiral")) plugin.getPlayerDataManager().updateEffectType(player.getUniqueId(), "spiral");
        else if (itemName.contains("Rainbow")) plugin.getPlayerDataManager().updateEffectType(player.getUniqueId(), "rainbow");
        else if (itemName.contains("Selected") || itemName.contains("Disabled")) plugin.getPlayerDataManager().toggleEffects(player.getUniqueId());
        plugin.getGuiManager().openSettingsMenu(player);
    }

    private void handlePlayerProfileClick(Player sender, String itemName, String title) {
        String cleanTitle = title.replaceAll("§[0-9a-fk-or]", "").replace("❤", "").trim();
        String targetName = cleanTitle.contains("'s Profile") ? cleanTitle.replace("'s Profile", "").trim() : "";
        Player receiver = Bukkit.getPlayer(targetName);

        if (receiver == null || !receiver.isOnline()) {
            sender.closeInventory();
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("general.player-offline"));
            return;
        }

        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.profile-friends-title"))) {
            plugin.getGuiManager().openFriendsMenu(sender, receiver.getUniqueId());
            return;
        }

        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-add-title"))) {
            if (!sender.hasPermission("valentines.friends.add") && !sender.hasPermission("valentines.friends.*")) {
                sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("general.no-permission"));
                return;
            }
            plugin.getFriendManager().sendFriendRequest(sender, receiver);
            plugin.getGuiManager().openPlayerProfile(sender, receiver);
            return;
        }
        if ((itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-accept-title")) ||
                itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-remove-title")) ||
                itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-deny-title"))) &&
                !sender.hasPermission("valentines.friends.use") && !sender.hasPermission("valentines.friends.*")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("general.no-permission"));
            return;
        }
        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-accept-title"))) {
            plugin.getFriendManager().acceptRequest(sender, receiver.getUniqueId());
            plugin.getGuiManager().openPlayerProfile(sender, receiver);
            return;
        }
        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-remove-title"))) {
            plugin.getFriendManager().removeFriend(sender, receiver.getUniqueId());
            plugin.getGuiManager().openPlayerProfile(sender, receiver);
            return;
        }
        if (itemName.equals(plugin.getLanguageManager().getMessage("gui.friend-deny-title"))) {
            plugin.getFriendManager().denyRequest(sender, receiver.getUniqueId());
            plugin.getGuiManager().openPlayerProfile(sender, receiver);
            return;
        }
        if (itemName.contains("Hug")) { sender.closeInventory(); executePlayerCommand(sender, receiver, "hug"); }
        else if (itemName.contains("Kiss")) { sender.closeInventory(); executePlayerCommand(sender, receiver, "kiss"); }
        else if (itemName.contains("Like")) { sender.closeInventory(); executePlayerCommand(sender, receiver, "like"); }
        else if (itemName.contains("Marry")) { sender.closeInventory(); executePlayerCommand(sender, receiver, "marry"); }
    }

    private void executePlayerCommand(Player sender, Player receiver, String action) {
        long cooldownSeconds = plugin.getConfig().getLong("cooldowns." + action, 60);
        if (plugin.getCooldownManager().hasCooldown(sender.getUniqueId(), action)) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(sender.getUniqueId(), action);
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("general.cooldown", Map.of("command", action, "time", String.valueOf(remaining))));
            return;
        }

        switch (action.toLowerCase()) {
            case "hug" -> {
                plugin.getCooldownManager().setCooldown(sender.getUniqueId(), "hug", cooldownSeconds * 1000);
                plugin.getPlayerDataManager().incrementStat(receiver.getUniqueId(), "hugs");
                plugin.getPlayerDataManager().incrementGivenStat(sender.getUniqueId(), "hugs");
                plugin.getServer().getPluginManager().callEvent(new HugReceivedEvent(receiver, sender));
                plugin.getServer().getPluginManager().callEvent(new HugGivenEvent(sender, receiver));
                sendActionMessages(sender, receiver, "hug");
                plugin.getEffectManager().playHugEffect(receiver.getLocation());
            }
            case "kiss" -> {
                plugin.getCooldownManager().setCooldown(sender.getUniqueId(), "kiss", cooldownSeconds * 1000);
                plugin.getPlayerDataManager().incrementStat(receiver.getUniqueId(), "kisses");
                plugin.getPlayerDataManager().incrementGivenStat(sender.getUniqueId(), "kisses");
                plugin.getServer().getPluginManager().callEvent(new KissReceivedEvent(receiver, sender));
                plugin.getServer().getPluginManager().callEvent(new KissGivenEvent(sender, receiver));
                sendActionMessages(sender, receiver, "kiss");
                plugin.getEffectManager().playKissEffect(receiver.getLocation());
            }
            case "like" -> {
                if (plugin.getFileStorage().hasLiked(sender.getUniqueId(), receiver.getUniqueId())) {
                    sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("like.already-liked", Map.of("player", receiver.getName())));
                    return;
                }
                plugin.getCooldownManager().setCooldown(sender.getUniqueId(), "like", cooldownSeconds * 1000);
                plugin.getFileStorage().saveLike(sender.getUniqueId(), receiver.getUniqueId());
                plugin.getPlayerDataManager().incrementStat(receiver.getUniqueId(), "likes");
                plugin.getPlayerDataManager().incrementGivenStat(sender.getUniqueId(), "likes");
                plugin.getServer().getPluginManager().callEvent(new LikeReceivedEvent(receiver, sender));
                plugin.getServer().getPluginManager().callEvent(new LikeGivenEvent(sender, receiver));
                sendActionMessages(sender, receiver, "like");
            }
            case "marry" -> plugin.getMarriageManager().sendProposal(sender, receiver);
        }
    }

    private void sendActionMessages(Player sender, Player receiver, String action) {
        sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage(action + ".sent", Map.of("player", receiver.getName())));
        receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage(action + ".received", Map.of("player", sender.getName())));
    }
}