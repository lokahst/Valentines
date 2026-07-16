package lokahst.valentines.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import lokahst.valentines.Valentines;
import lokahst.valentines.data.Achievement;
import lokahst.valentines.data.PlayerData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FriendManager {

    public static final int MAX_FRIENDS = 50;
    private static final long THREE_MONTHS_MS = 90L * 24L * 60L * 60L * 1000L;

    private final Valentines plugin;

    public FriendManager(Valentines plugin) {
        this.plugin = plugin;
    }

    public boolean sendFriendRequest(Player sender, Player target) {
        if (sender.getUniqueId().equals(target.getUniqueId())) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.chat-add-self"));
            return false;
        }

        PlayerData senderData = plugin.getPlayerDataManager().getOrLoadPlayerData(sender.getUniqueId());
        PlayerData targetData = plugin.getPlayerDataManager().getOrLoadPlayerData(target.getUniqueId());

        if (senderData.getFriends().containsKey(target.getUniqueId())) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.already-friends"));
            return false;
        }

        if (!targetData.isAllowFriendRequests()) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.requests-disabled"));
            return false;
        }

        if (senderData.getFriends().size() >= MAX_FRIENDS || targetData.getFriends().size() >= MAX_FRIENDS) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.max-reached"));
            return false;
        }

        if (targetData.getPendingFriendRequests().contains(sender.getUniqueId())) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.request-already-sent"));
            return false;
        }

        targetData.getPendingFriendRequests().add(sender.getUniqueId());
        plugin.getPlayerDataManager().savePlayerData(targetData);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.request-sent", placeholders));

        placeholders.put("player", sender.getName());
        target.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.request-received", placeholders));
        return true;
    }

    public boolean acceptRequest(Player receiver, UUID requesterUuid) {
        PlayerData receiverData = plugin.getPlayerDataManager().getOrLoadPlayerData(receiver.getUniqueId());
        PlayerData requesterData = plugin.getPlayerDataManager().getOrLoadPlayerData(requesterUuid);

        if (!receiverData.getPendingFriendRequests().contains(requesterUuid)) {
            receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.no-pending"));
            return false;
        }

        if (receiverData.getFriends().size() >= MAX_FRIENDS || requesterData.getFriends().size() >= MAX_FRIENDS) {
            receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.max-reached"));
            return false;
        }

        receiverData.getPendingFriendRequests().remove(requesterUuid);
        long now = System.currentTimeMillis();
        receiverData.getFriends().put(requesterUuid, now);
        requesterData.getFriends().put(receiver.getUniqueId(), now);

        plugin.getPlayerDataManager().savePlayerData(receiverData);
        plugin.getPlayerDataManager().savePlayerData(requesterData);

        Player requester = Bukkit.getPlayer(requesterUuid);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", requesterData.getName());
        receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.request-accepted", placeholders));

        if (requester != null) {
            placeholders.put("player", receiver.getName());
            requester.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.accept-notify", placeholders));
        }

        plugin.getAchievementManager().checkAndUnlock(receiver.getUniqueId(), Achievement.MAKE_A_FRIEND);
        plugin.getAchievementManager().checkAndUnlock(requesterUuid, Achievement.MAKE_A_FRIEND);
        return true;
    }

    public boolean denyRequest(Player receiver, UUID requesterUuid) {
        PlayerData receiverData = plugin.getPlayerDataManager().getOrLoadPlayerData(receiver.getUniqueId());
        if (!receiverData.getPendingFriendRequests().remove(requesterUuid)) {
            receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.no-pending"));
            return false;
        }

        plugin.getPlayerDataManager().savePlayerData(receiverData);
        Player requester = Bukkit.getPlayer(requesterUuid);
        if (requester != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", receiver.getName());
            requester.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.request-denied-notify", placeholders));
        }

        receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.request-denied"));
        return true;
    }

    public boolean removeFriend(Player player, UUID friendUuid) {
        PlayerData playerData = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());
        PlayerData friendData = plugin.getPlayerDataManager().getOrLoadPlayerData(friendUuid);

        if (playerData.getFriends().remove(friendUuid) == null) {
            player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.not-friends"));
            return false;
        }

        friendData.getFriends().remove(player.getUniqueId());
        plugin.getPlayerDataManager().savePlayerData(playerData);
        plugin.getPlayerDataManager().savePlayerData(friendData);

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", friendData.getName());
        player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("friends.removed", placeholders));
        return true;
    }

    public void toggleFriendRequests(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());
        data.setAllowFriendRequests(!data.isAllowFriendRequests());
        plugin.getPlayerDataManager().savePlayerData(data);

        String key = data.isAllowFriendRequests() ? "friends.toggle-enabled" : "friends.toggle-disabled";
        player.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage(key));
    }

    public void notifyFriendStatus(Player player, boolean joined) {
        if (!plugin.getConfig().getBoolean("friends.join-leave-notifications", true)) {
            return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getOrLoadPlayerData(player.getUniqueId());
        for (UUID friendUuid : playerData.getFriends().keySet()) {
            Player friend = Bukkit.getPlayer(friendUuid);
            if (friend == null || !friend.isOnline()) {
                continue;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("friend", player.getName());
            String messagePath = joined ? "friends.join-notify" : "friends.leave-notify";
            friend.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage(messagePath, placeholders));
        }
    }

    public void checkFriendDurationAchievement(UUID playerUuid) {
        PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(playerUuid);
        long now = System.currentTimeMillis();
        for (long since : data.getFriends().values()) {
            if (now - since >= THREE_MONTHS_MS) {
                plugin.getAchievementManager().checkAndUnlock(playerUuid, Achievement.FRIENDS_FOR_3_MONTHS);
                return;
            }
        }
    }
}