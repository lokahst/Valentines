package lokahst.valentines;

import lokahst.valentines.commands.GuiCommand;
import lokahst.valentines.commands.ReloadCommand;
import lokahst.valentines.effects.EffectManager;
import lokahst.valentines.gui.GuiManager;
import lokahst.valentines.listeners.AchievementListener;
import lokahst.valentines.listeners.ChatListener;
import lokahst.valentines.listeners.PlayerListener;
import lokahst.valentines.managers.AchievementManager;
import lokahst.valentines.managers.AnniversaryManager;
import lokahst.valentines.managers.ConfigManager;
import lokahst.valentines.managers.CooldownManager;
import lokahst.valentines.managers.FriendManager;
import lokahst.valentines.managers.LanguageManager;
import lokahst.valentines.managers.MarriageManager;
import lokahst.valentines.managers.PlayerDataManager;
import lokahst.valentines.storage.FileStorage;
import lokahst.valentines.placeholders.ValentinesPlaceholderExpansion;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Valentines extends JavaPlugin {

    private static Valentines instance;
    private FileStorage fileStorage;
    private LanguageManager languageManager;
    private PlayerDataManager playerDataManager;
    private MarriageManager marriageManager;
    private CooldownManager cooldownManager;
    private EffectManager effectManager;
    private GuiManager guiManager;
    private AnniversaryManager anniversaryManager;
    private AchievementManager achievementManager;
    private ConfigManager configManager;
    private FriendManager friendManager;
    private String prefix;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        configManager.initializeConfig();

        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Prefix", "&d&lValentines &7»&r "));
        initializeManagers();
        registerCommands();
        registerListeners();

        if (getConfig().getBoolean("effect.enabled")) {
            effectManager.startEffectTask();
        }
        anniversaryManager.startAnniversaryTask();
        startAutoSaveTask();
        registerPlaceholderExpansion();

        getLogger().info("Valentines v" + getDescription().getVersion() + " has been enabled!");
    }

    @Override
    public void onDisable() {
        if (cooldownManager != null) {
            cooldownManager.stopCleanupTask();
        }

        if (effectManager != null) {
            effectManager.stopEffectTask();
        }

        if (anniversaryManager != null) {
            anniversaryManager.stopAnniversaryTask();
        }

        if (playerDataManager != null) {
            playerDataManager.saveAllData();
        }

        if (fileStorage != null) {
            fileStorage.saveAll();
            fileStorage.shutdown();
        }

        getLogger().info("Valentines plugin has been disabled.");
    }

    private void initializeManagers() {
        this.languageManager = new LanguageManager(this);
        this.fileStorage = new FileStorage(this);
        this.playerDataManager = new PlayerDataManager(this);
        this.marriageManager = new MarriageManager(this);
        this.cooldownManager = new CooldownManager(this);
        this.effectManager = new EffectManager(this);
        this.guiManager = new GuiManager(this);
        this.anniversaryManager = new AnniversaryManager(this);
        this.achievementManager = new AchievementManager(this);
        this.friendManager = new FriendManager(this);
    }

    private void registerPlaceholderExpansion() {
        if (!getConfig().getBoolean("placeholderapi.enabled", true)) {
            return;
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ValentinesPlaceholderExpansion(this).register();
            getLogger().info("Hooked into PlaceholderAPI.");
        }
    }

    private void registerCommands() {
        registerCommand("valentines", new GuiCommand(this));
        registerCommand("vreload", new ReloadCommand(this));
    }

    private void registerCommand(String commandName, CommandExecutor executor) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().warning("Command not found in plugin.yml: " + commandName);
            return;
        }

        command.setExecutor(executor);
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AchievementListener(this), this);

        if (getConfig().getBoolean("symbol-change")) {
            Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        }
    }

    private void startAutoSaveTask() {
        int autoSaveInterval = getConfig().getInt("auto-save", 30);

        new BukkitRunnable() {
            @Override
            public void run() {
                playerDataManager.saveAllData();
                fileStorage.saveAll();
                getLogger().fine("Auto-saved all player data");
            }
        }.runTaskTimerAsynchronously(this, 0L, autoSaveInterval * 60 * 20L);
    }

    public void reloadPlugin() {
        reloadConfig();
        prefix = ChatColor.translateAlternateColorCodes('&', getConfig().getString("Prefix", "&d&lValentines &7»&r "));
        languageManager.reloadLanguage();

        if (getConfig().getBoolean("effect.enabled")) {
            effectManager.startEffectTask();
        } else {
            effectManager.stopEffectTask();
        }

        anniversaryManager.stopAnniversaryTask();
        anniversaryManager.startAnniversaryTask();

        getLogger().info(languageManager.getMessage("plugin.reload"));
    }

    public static Valentines getInstance() {
        return instance;
    }

    public String getPrefix() {
        return prefix;
    }

    public FileStorage getFileStorage() {
        return fileStorage;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public MarriageManager getMarriageManager() {
        return marriageManager;
    }

    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public AnniversaryManager getAnniversaryManager() {
        return anniversaryManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }
}