package lokahst.valentines.managers;

import lokahst.valentines.Valentines;
import lokahst.valentines.data.Achievement;
import lokahst.valentines.data.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AchievementManager {

    private final Valentines plugin;

    public AchievementManager(Valentines plugin) {
        this.plugin = plugin;
    }

    public void checkAndUnlock(UUID playerUuid, Achievement achievement) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null) {
            return;
        }

        PlayerData data = plugin.getPlayerDataManager().getPlayerData(playerUuid);
        if (data == null) {
            return;
        }

        if (!data.hasAchievement(achievement.getKey())) {
            unlockAchievement(player, data, achievement);
        }
    }

    private void unlockAchievement(Player player, PlayerData data, Achievement achievement) {
        data.addAchievement(achievement.getKey());
        plugin.getPlayerDataManager().savePlayerData(data);

        String name = plugin.getLanguageManager().getMessage("achievements." + achievement.getKey() + ".name");
        String description = plugin.getLanguageManager().getMessage("achievements." + achievement.getKey() + ".description");

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("achievement", name);
        placeholders.put("description", description);
        placeholders.put("player", player.getName());

        String message = plugin.getPrefix() + plugin.getLanguageManager().getMessage("achievements.unlocked", placeholders);
        player.sendMessage(message);

        if (plugin.getConfig().getBoolean("achievements.broadcast", true)) {
            String broadcast = plugin.getPrefix() + plugin.getLanguageManager().getMessage("achievements.broadcast", placeholders);
            Bukkit.broadcastMessage(broadcast);
        }

        if (plugin.getConfig().getBoolean("achievements.sound-enabled", true)) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
    }

    public void checkKissReceived(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.getKissesReceived() == 1) {
            checkAndUnlock(player.getUniqueId(), Achievement.FIRST_KISS_RECEIVED);
        } else if (data.getKissesReceived() == 10) {
            checkAndUnlock(player.getUniqueId(), Achievement.REACH_10_KISSES);
        }
    }

    public void checkHugReceived(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.getHugsReceived() == 1) {
            checkAndUnlock(player.getUniqueId(), Achievement.FIRST_HUG_RECEIVED);
        } else if (data.getHugsReceived() == 10) {
            checkAndUnlock(player.getUniqueId(), Achievement.REACH_10_HUGS);
        }
    }

    public void checkLikeReceived(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.getLikesReceived() == 1) {
            checkAndUnlock(player.getUniqueId(), Achievement.FIRST_LIKE_RECEIVED);
        } else if (data.getLikesReceived() == 10) {
            checkAndUnlock(player.getUniqueId(), Achievement.REACH_10_LIKES);
        }
    }

    public void checkKissGiven(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.getKissesGiven() == 1) {
            checkAndUnlock(player.getUniqueId(), Achievement.FIRST_KISS_GIVEN);
        }
    }

    public void checkHugGiven(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.getHugsGiven() == 1) {
            checkAndUnlock(player.getUniqueId(), Achievement.FIRST_HUG_GIVEN);
        }
    }

    public void checkLikeGiven(Player player) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null) return;

        if (data.getLikesGiven() == 1) {
            checkAndUnlock(player.getUniqueId(), Achievement.FIRST_LIKE_GIVEN);
        }
    }

    public void checkMarriage(Player player) {
        checkAndUnlock(player.getUniqueId(), Achievement.MARRIAGE);
    }

    public void checkDivorce(Player player) {
        checkAndUnlock(player.getUniqueId(), Achievement.DIVORCE);
    }

    public void checkMoodSet(Player player) {
        checkAndUnlock(player.getUniqueId(), Achievement.FIRST_MOOD_SET);
    }

    public void checkMarriageDuration(UUID playerUuid, long daysMarried) {
        if (daysMarried >= 730 && !hasAchievement(playerUuid, Achievement.MARRIED_2_YEARS)) {
            checkAndUnlock(playerUuid, Achievement.MARRIED_2_YEARS);
        } else if (daysMarried >= 365 && !hasAchievement(playerUuid, Achievement.MARRIED_365_DAYS)) {
            checkAndUnlock(playerUuid, Achievement.MARRIED_365_DAYS);
        } else if (daysMarried >= 30 && !hasAchievement(playerUuid, Achievement.MARRIED_30_DAYS)) {
            checkAndUnlock(playerUuid, Achievement.MARRIED_30_DAYS);
        }
    }

    public boolean hasAchievement(UUID playerUuid, Achievement achievement) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(playerUuid);
        if (data == null) return false;
        return data.hasAchievement(achievement.getKey());
    }

    public int getUnlockedCount(UUID playerUuid) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(playerUuid);
        if (data == null) return 0;
        return data.getUnlockedAchievements().size();
    }

    public double getProgressPercentage(UUID playerUuid) {
        int unlocked = getUnlockedCount(playerUuid);
        int total = Achievement.values().length;
        return (double) unlocked / total * 100.0;
    }
}