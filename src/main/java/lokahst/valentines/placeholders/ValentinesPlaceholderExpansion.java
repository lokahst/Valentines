package lokahst.valentines.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import lokahst.valentines.Valentines;
import lokahst.valentines.data.Achievement;
import lokahst.valentines.data.Marriage;
import lokahst.valentines.data.MarriageProposal;
import lokahst.valentines.data.PlayerData;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ValentinesPlaceholderExpansion extends PlaceholderExpansion {

    private final Valentines plugin;

    public ValentinesPlaceholderExpansion(Valentines plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "valentines";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String params) {
        if (offlinePlayer == null) {
            return "";
        }

        UUID uuid = offlinePlayer.getUniqueId();
        String parameter = params.toLowerCase(Locale.ROOT);

        if (parameter.startsWith("top_")) {
            return handleTopPlaceholder(parameter);
        }

        if (parameter.startsWith("has_achievement_")) {
            return hasAchievement(uuid, parameter.substring("has_achievement_".length()));
        }

        PlayerData data = getData(uuid);
        Marriage marriage = getMarriage(uuid);
        MarriageProposal proposal = plugin.getMarriageManager().getEngagementProposal(uuid);

        return switch (parameter) {
            case "name" -> data.getName();
            case "hugs", "hugs_received" -> String.valueOf(data.getHugsReceived());
            case "kisses", "kisses_received" -> String.valueOf(data.getKissesReceived());
            case "likes", "likes_received" -> String.valueOf(data.getLikesReceived());
            case "hugs_given" -> String.valueOf(data.getHugsGiven());
            case "kisses_given" -> String.valueOf(data.getKissesGiven());
            case "likes_given" -> String.valueOf(data.getLikesGiven());
            case "total_given" -> String.valueOf(data.getHugsGiven() + data.getKissesGiven() + data.getLikesGiven());
            case "total_received" -> String.valueOf(data.getTotalScore());
            case "total_score" -> String.valueOf(data.getTotalScore());
            case "mood" -> getMoodDisplay(data.getMood());
            case "mood_raw" -> data.getMood();

            case "is_married" -> String.valueOf(marriage != null);
            case "partner" -> getPartnerName(uuid, marriage);
            case "days_married" -> marriage != null ? String.valueOf(marriage.getDaysMarried()) : "0";
            case "relationship_status" -> getRelationshipStatus(marriage, proposal);

            case "is_engaged" -> String.valueOf(proposal != null);
            case "engaged_with" -> getEngagedWith(uuid, proposal);
            case "engagement_days" -> getEngagementDays(proposal);
            case "engagement_hours" -> getEngagementHours(proposal);

            case "achievements_unlocked" -> String.valueOf(data.getUnlockedAchievements().size());
            case "achievements_total" -> String.valueOf(Achievement.values().length);
            case "achievements_remaining" -> String.valueOf(Math.max(0, Achievement.values().length - data.getUnlockedAchievements().size()));
            case "achievements_progress" -> getAchievementProgress(data.getUnlockedAchievements());

            default -> null;
        };
    }

    private PlayerData getData(UUID uuid) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(uuid);
        if (data != null) {
            return data;
        }

        PlayerData loaded = plugin.getFileStorage().loadPlayerData(uuid);
        if (loaded != null) {
            return loaded;
        }

        return PlayerData.createDefault(uuid, "Unknown");
    }

    private Marriage getMarriage(UUID uuid) {
        Marriage marriage = plugin.getMarriageManager().getMarriage(uuid);
        if (marriage != null) {
            return marriage;
        }
        return plugin.getFileStorage().loadMarriage(uuid);
    }

    private String getMoodDisplay(String mood) {
        if (mood == null || mood.isBlank()) {
            return plugin.getLanguageManager().getMessage("mood.unknown");
        }

        String key = "mood." + mood.toLowerCase(Locale.ROOT);
        String moodText = plugin.getLanguageManager().getMessage(key);

        if (moodText.startsWith("Missing message:")) {
            return mood;
        }

        return moodText;
    }

    private String getPartnerName(UUID uuid, Marriage marriage) {
        if (marriage == null) {
            return "None";
        }

        UUID partnerUuid = marriage.getPartner(uuid);
        if (partnerUuid == null) {
            return "None";
        }

        Player online = Bukkit.getPlayer(partnerUuid);
        if (online != null) {
            return online.getName();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(partnerUuid);
        return offline.getName() != null ? offline.getName() : "Unknown";
    }

    private String getEngagedWith(UUID uuid, MarriageProposal proposal) {
        if (proposal == null) {
            return "None";
        }

        UUID other = proposal.proposer().equals(uuid) ? proposal.target() : proposal.proposer();
        Player online = Bukkit.getPlayer(other);
        if (online != null) {
            return online.getName();
        }

        OfflinePlayer offline = Bukkit.getOfflinePlayer(other);
        return offline.getName() != null ? offline.getName() : "Unknown";
    }

    private String getEngagementDays(MarriageProposal proposal) {
        if (proposal == null) {
            return "0";
        }
        long days = Duration.ofMillis(System.currentTimeMillis() - proposal.proposalDate()).toDays();
        return String.valueOf(Math.max(0, days));
    }

    private String getEngagementHours(MarriageProposal proposal) {
        if (proposal == null) {
            return "0";
        }
        long hours = Duration.ofMillis(System.currentTimeMillis() - proposal.proposalDate()).toHours();
        return String.valueOf(Math.max(0, hours));
    }

    private String getAchievementProgress(Set<String> unlocked) {
        int total = Achievement.values().length;
        if (total == 0) {
            return "0.0";
        }

        double progress = ((double) unlocked.size() / total) * 100.0;
        return String.format(Locale.US, "%.1f", progress);
    }

    private String getRelationshipStatus(Marriage marriage, MarriageProposal proposal) {
        if (marriage != null) {
            return "married";
        }

        if (proposal != null) {
            return "engaged";
        }

        return "single";
    }

    private String hasAchievement(UUID uuid, String key) {
        String normalized = key.toLowerCase(Locale.ROOT);
        for (Achievement achievement : Achievement.values()) {
            if (achievement.getKey().equalsIgnoreCase(normalized)) {
                return String.valueOf(getData(uuid).hasAchievement(achievement.getKey()));
            }
        }
        return "false";
    }

    private String handleTopPlaceholder(String parameter) {
        String[] parts = parameter.split("_");
        if (parts.length != 3) {
            return "";
        }

        String type = parts[1];
        int position;
        try {
            position = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            return "";
        }

        if (position < 1 || position > 100) {
            return "";
        }

        List<PlayerData> topPlayers = plugin.getPlayerDataManager().getTopPlayers(position);
        if (topPlayers.size() < position) {
            return "";
        }

        PlayerData entry = topPlayers.get(position - 1);

        return switch (type) {
            case "name" -> entry.getName();
            case "score" -> String.valueOf(entry.getTotalScore());
            case "hugs" -> String.valueOf(entry.getHugsReceived());
            case "kisses" -> String.valueOf(entry.getKissesReceived());
            case "likes" -> String.valueOf(entry.getLikesReceived());
            case "hugs_given" -> String.valueOf(entry.getHugsGiven());
            case "kisses_given" -> String.valueOf(entry.getKissesGiven());
            case "likes_given" -> String.valueOf(entry.getLikesGiven());
            default -> "";
        };
    }
}