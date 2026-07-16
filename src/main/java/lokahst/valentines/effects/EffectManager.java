package lokahst.valentines.effects;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import lokahst.valentines.Valentines;
import lokahst.valentines.data.PlayerData;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class EffectManager {

    private static final Color[] RAINBOW_COLORS = {
            Color.AQUA,
            Color.RED,
            Color.FUCHSIA,
            Color.YELLOW,
            Color.LIME,
            Color.ORANGE
    };

    private final Valentines plugin;
    private BukkitTask effectTask;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    public EffectManager(Valentines plugin) {
        this.plugin = plugin;
    }

    public void startEffectTask() {
        if (effectTask != null) {
            effectTask.cancel();
        }

        int updateRate = plugin.getConfig().getInt("effect.update-rate", 3);
        List<String> enabledWorlds = plugin.getConfig().getStringList("enabled-worlds");

        effectTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!enabledWorlds.contains(player.getWorld().getName())) {
                        continue;
                    }

                    if (!player.hasPermission("valentines.effect")) {
                        continue;
                    }

                    PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
                    if (data == null || !data.isEffectEnabled()) {
                        continue;
                    }

                    playEffect(player, data.getEffectType());
                }
            }
        }.runTaskTimer(plugin, 0L, updateRate);
    }

    public void stopEffectTask() {
        if (effectTask != null) {
            effectTask.cancel();
            effectTask = null;
        }
    }

    private void playEffect(Player player, String effectType) {
        Location loc = player.getLocation().add(0, plugin.getConfig().getDouble("effect.start-height", 0.1), 0);
        double radius = plugin.getConfig().getDouble("effect.radius", 1.0);
        double maxHeight = plugin.getConfig().getDouble("effect.max-height", 2.5);
        int density = plugin.getConfig().getInt("effect.particle-density", 2);

        switch (effectType.toLowerCase()) {
            case "heart":
                createHeartEffect(loc, radius, maxHeight, density);
                break;
            case "spiral":
                createSpiralEffect(loc, radius, maxHeight, density);
                break;
            case "rainbow":
            case "cloud":
                createRainbowEffect(loc, radius, maxHeight, density);
                break;
        }
    }

    private void createHeartEffect(Location center, double radius, double maxHeight, int density) {
        int particleCount = Math.max(1, density / 2);
        for (int i = 0; i < particleCount; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double height = random.nextDouble() * maxHeight;

            double x = radius * (Math.sin(angle) * 0.5);
            double z = radius * (Math.cos(angle) * 0.5);

            Location particleLoc = center.clone().add(x, height, z);
            center.getWorld().spawnParticle(Particle.HEART, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private void createSpiralEffect(Location center, double radius, double maxHeight, int density) {
        int particleCount = Math.max(1, density / 2);
        for (int i = 0; i < particleCount; i++) {
            double height = random.nextDouble() * maxHeight;
            double angle = (System.currentTimeMillis() / 200.0 + height * 2) % (2 * Math.PI);

            double x = radius * Math.cos(angle) * 0.5;
            double z = radius * Math.sin(angle) * 0.5;

            Location particleLoc = center.clone().add(x, height, z);
            center.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    private void createRainbowEffect(Location center, double radius, double maxHeight, int density) {

        for (int i = 0; i < density; i++) {

            double angle = random.nextDouble() * Math.PI * 2;
            double distance = random.nextDouble() * radius;

            double x = Math.cos(angle) * distance;
            double z = Math.sin(angle) * distance;
            double y = random.nextDouble() * maxHeight;

            Location particleLoc = center.clone().add(x, y, z);

            Color color = RAINBOW_COLORS[random.nextInt(RAINBOW_COLORS.length)];

            Particle.DustOptions dust =
                    new Particle.DustOptions(color, 1.2F);

            center.getWorld().spawnParticle(
                    Particle.REDSTONE,
                    particleLoc,
                    2,
                    0.03,
                    0.03,
                    0.03,
                    0,
                    dust
            );
        }
    }

    public void playKissEffect(Location location) {
        if (!plugin.getConfig().getBoolean("kiss-effect", true)) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            double x = (random.nextDouble() - 0.5) * 1.2;
            double y = random.nextDouble() * 1.2;
            double z = (random.nextDouble() - 0.5) * 1.2;

            Location particleLoc = location.clone().add(x, y, z);
            location.getWorld().spawnParticle(Particle.HEART, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    public void playHugEffect(Location location) {
        if (!plugin.getConfig().getBoolean("hug-effect", true)) {
            return;
        }

        for (int i = 0; i < 5; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = random.nextDouble();

            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            double y = random.nextDouble() * 1.2;

            Location particleLoc = location.clone().add(x, y, z);
            location.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, particleLoc, 1, 0, 0, 0, 0);
        }
    }

    public void playMarriageEffect(Location location) {
        if (!plugin.getConfig().getBoolean("marriage-effect", true)) {
            return;
        }

        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = random.nextDouble() * 1.5;
            double height = random.nextDouble() * 2.0;

            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);

            Location particleLoc = location.clone().add(x, height, z);
            location.getWorld().spawnParticle(Particle.HEART, particleLoc, 1, 0, 0, 0, 0);
        }
    }
}