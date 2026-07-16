package lokahst.valentines.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import lokahst.valentines.Valentines;
import lokahst.valentines.data.Marriage;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AnniversaryManager {

    private final Valentines plugin;
    private BukkitTask anniversaryTask;
    private final Set<String> processedToday = new HashSet<>();

    public AnniversaryManager(Valentines plugin) {
        this.plugin = plugin;
    }

    public void startAnniversaryTask() {
        if (!plugin.getConfig().getBoolean("anniversary.enabled", true)) {
            return;
        }

        if (anniversaryTask != null) {
            anniversaryTask.cancel();
        }

        int checkInterval = plugin.getConfig().getInt("anniversary.check-interval", 300) * 20;

        anniversaryTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkAnniversaries();
            }
        }.runTaskTimer(plugin, 0L, checkInterval);
    }

    public void stopAnniversaryTask() {
        if (anniversaryTask != null) {
            anniversaryTask.cancel();
            anniversaryTask = null;
        }
    }

    private void checkAnniversaries() {
        LocalDate today = LocalDate.now();
        List<Marriage> marriages = plugin.getMarriageManager().getAllMarriages();

        for (Marriage marriage : marriages) {
            LocalDate marriageDate = new Date(marriage.marriageDate()).toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate();

            long daysMarried = java.time.temporal.ChronoUnit.DAYS.between(marriageDate, today);

            plugin.getAchievementManager().checkMarriageDuration(marriage.player1(), daysMarried);
            plugin.getAchievementManager().checkMarriageDuration(marriage.player2(), daysMarried);

            if (marriageDate.getMonth() == today.getMonth() &&
                    marriageDate.getDayOfMonth() == today.getDayOfMonth() &&
                    marriageDate.getYear() < today.getYear()) {

                String marriageKey = marriage.player1().toString() + ":" + marriage.player2().toString() + ":" + today;

                if (!processedToday.contains(marriageKey)) {
                    processedToday.add(marriageKey);
                    handleAnniversary(marriage, today.getYear() - marriageDate.getYear());
                }
            }
        }

        if (System.currentTimeMillis() % (24 * 60 * 60 * 1000) < (5 * 60 * 1000)) {
            processedToday.clear();
        }
    }

    private void handleAnniversary(Marriage marriage, int years) {
        Player player1 = Bukkit.getPlayer(marriage.player1());
        Player player2 = Bukkit.getPlayer(marriage.player2());

        String player1Name = player1 != null ? player1.getName() : "Unknown";
        String player2Name = player2 != null ? player2.getName() : "Unknown";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player1", player1Name);
        placeholders.put("player2", player2Name);
        placeholders.put("years", String.valueOf(years));

        String announcementType = plugin.getConfig().getString("anniversary.announcement-type", "global");

        switch (announcementType.toLowerCase()) {
            case "global":
                String globalMessage = plugin.getPrefix() + plugin.getLanguageManager().getMessage("anniversary.global-announcement", placeholders);
                Bukkit.broadcastMessage(globalMessage);
                break;

            case "couple":
                if (player1 != null) {
                    Map<String, String> p1Placeholders = new HashMap<>(placeholders);
                    p1Placeholders.put("partner", player2Name);
                    player1.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("anniversary.couple-message", p1Placeholders));
                }
                if (player2 != null) {
                    Map<String, String> p2Placeholders = new HashMap<>(placeholders);
                    p2Placeholders.put("partner", player1Name);
                    player2.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("anniversary.couple-message", p2Placeholders));
                }
                break;

            case "world":
                List<String> worlds = plugin.getConfig().getStringList("anniversary.announcement-worlds");
                String worldMessage = plugin.getPrefix() + plugin.getLanguageManager().getMessage("anniversary.world-announcement", placeholders);

                for (String worldName : worlds) {
                    if (Bukkit.getWorld(worldName) != null) {
                        for (Player player : Bukkit.getWorld(worldName).getPlayers()) {
                            player.sendMessage(worldMessage);
                        }
                    }
                }
                break;
        }

        if (player1 != null) {
            plugin.getEffectManager().playMarriageEffect(player1.getLocation());
        }
        if (player2 != null) {
            plugin.getEffectManager().playMarriageEffect(player2.getLocation());
        }
    }
}