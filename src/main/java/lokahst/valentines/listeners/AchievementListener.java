package lokahst.valentines.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import lokahst.valentines.Valentines;
import lokahst.valentines.events.DivorceEvent;
import lokahst.valentines.events.HugGivenEvent;
import lokahst.valentines.events.HugReceivedEvent;
import lokahst.valentines.events.KissGivenEvent;
import lokahst.valentines.events.KissReceivedEvent;
import lokahst.valentines.events.LikeGivenEvent;
import lokahst.valentines.events.LikeReceivedEvent;
import lokahst.valentines.events.MarriageCreatedEvent;
import lokahst.valentines.events.MoodSetEvent;

public class AchievementListener implements Listener {

    private final Valentines plugin;

    public AchievementListener(Valentines plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHugReceived(HugReceivedEvent event) {
        plugin.getAchievementManager().checkHugReceived(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHugGiven(HugGivenEvent event) {
        plugin.getAchievementManager().checkHugGiven(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKissReceived(KissReceivedEvent event) {
        plugin.getAchievementManager().checkKissReceived(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onKissGiven(KissGivenEvent event) {
        plugin.getAchievementManager().checkKissGiven(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLikeReceived(LikeReceivedEvent event) {
        plugin.getAchievementManager().checkLikeReceived(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLikeGiven(LikeGivenEvent event) {
        plugin.getAchievementManager().checkLikeGiven(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMarriageCreated(MarriageCreatedEvent event) {
        plugin.getAchievementManager().checkMarriage(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDivorce(DivorceEvent event) {
        plugin.getAchievementManager().checkDivorce(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMoodSet(MoodSetEvent event) {
        plugin.getAchievementManager().checkMoodSet(event.getPlayer());
    }
}