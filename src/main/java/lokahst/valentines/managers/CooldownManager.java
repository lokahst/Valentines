package lokahst.valentines.managers;

import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import lokahst.valentines.Valentines;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();
    private BukkitTask cleanupTask;

    public CooldownManager(Valentines plugin) {
        startCleanupTask(plugin);
    }

    public void startCleanupTask(Valentines plugin) {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }

        cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredCooldowns();
            }
        }.runTaskTimerAsynchronously(plugin, 6000L, 6000L);
    }

    public void stopCleanupTask() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
            cleanupTask = null;
        }
    }

    private void cleanupExpiredCooldowns() {
        long currentTime = System.currentTimeMillis();
        int removedEntries = 0;
        int removedPlayers = 0;

        Iterator<Map.Entry<UUID, Map<String, Long>>> playerIterator = cooldowns.entrySet().iterator();

        while (playerIterator.hasNext()) {
            Map.Entry<UUID, Map<String, Long>> playerEntry = playerIterator.next();
            Map<String, Long> playerCooldowns = playerEntry.getValue();

            Iterator<Map.Entry<String, Long>> cooldownIterator = playerCooldowns.entrySet().iterator();
            while (cooldownIterator.hasNext()) {
                Map.Entry<String, Long> cooldownEntry = cooldownIterator.next();
                if (currentTime >= cooldownEntry.getValue()) {
                    cooldownIterator.remove();
                    removedEntries++;
                }
            }

            if (playerCooldowns.isEmpty()) {
                playerIterator.remove();
                removedPlayers++;
            }
        }

        if (removedEntries > 0) {
            System.out.println("[Valentines] Cleaned up " + removedEntries + " expired cooldowns and " + removedPlayers + " empty player entries");
        }
    }

    public void setCooldown(UUID player, String action, long duration) {
        cooldowns.computeIfAbsent(player, k -> new ConcurrentHashMap<>()).put(action, System.currentTimeMillis() + duration);
    }

    public boolean hasCooldown(UUID player, String action) {
        Map<String, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) {
            return false;
        }

        Long cooldownEnd = playerCooldowns.get(action);
        if (cooldownEnd == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime >= cooldownEnd) {
            playerCooldowns.remove(action);

            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(player);
            }
            return false;
        }

        return true;
    }

    public long getRemainingCooldown(UUID player, String action) {
        Map<String, Long> playerCooldowns = cooldowns.get(player);
        if (playerCooldowns == null) {
            return 0;
        }

        Long cooldownEnd = playerCooldowns.get(action);
        if (cooldownEnd == null) {
            return 0;
        }

        long currentTime = System.currentTimeMillis();
        long remaining = cooldownEnd - currentTime;

        if (remaining <= 0) {
            playerCooldowns.remove(action);
            if (playerCooldowns.isEmpty()) {
                cooldowns.remove(player);
            }
            return 0;
        }

        return Math.max(0, remaining / 1000);
    }

    public void clearCooldowns(UUID player) {
        cooldowns.remove(player);
    }

    public int getTotalCooldownCount() {
        return cooldowns.values().stream().mapToInt(Map::size).sum();
    }

    public int getPlayerCount() {
        return cooldowns.size();
    }
}