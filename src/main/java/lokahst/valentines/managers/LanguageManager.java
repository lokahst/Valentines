package lokahst.valentines.managers;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import lokahst.valentines.Valentines;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {
    
    private final Valentines plugin;
    private FileConfiguration langConfig;
    private final Map<String, String> messages = new HashMap<>();
    
    public LanguageManager(Valentines plugin) {
        this.plugin = plugin;
        loadLanguage();
    }
    
    public void loadLanguage() {
        String language = "en";
        File langFile = new File(plugin.getDataFolder(), "lang/" + language + ".yml");
        
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + language + ".yml", false);
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        InputStream defConfigStream = plugin.getResource("lang/" + language + ".yml");
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
            langConfig.setDefaults(defConfig);
        }
        
        loadMessages("", langConfig);
    }
    
    private void loadMessages(String path, ConfigurationSection config) {
        for (String key : config.getKeys(false)) {
            String fullPath = path.isEmpty() ? key : path + "." + key;
            
            if (config.isConfigurationSection(key)) {
                ConfigurationSection section = config.getConfigurationSection(key);
                if (section != null) {
                    loadMessages(fullPath, section);
                }
            } else {
                messages.put(fullPath, config.getString(key));
            }
        }
    }
    
    public String getMessage(String key) {
        String message = messages.get(key);
        if (message == null) {
            return "Missing message: " + key;
        }
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        return message;
    }
    
    public List<String> getMessageList(String key) {
        List<String> result = new ArrayList<>();
        if (langConfig.isList(key)) {
            List<String> list = langConfig.getStringList(key);
            for (String line : list) {
                result.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        } else {
            String message = getMessage(key);
            if (message.contains("\n")) {
                String[] lines = message.split("\n");
                for (String line : lines) {
                    result.add(ChatColor.translateAlternateColorCodes('&', line));
                }
            } else {
                result.add(message);
            }
        }
        return result;
    }
    
    public List<String> getMessageList(String key, Map<String, String> placeholders) {
        List<String> result = getMessageList(key);
        List<String> processed = new ArrayList<>();
        
        for (String line : result) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                line = line.replace("{" + entry.getKey() + "}", entry.getValue());
            }
            processed.add(line);
        }
        
        return processed;
    }
    
    public void reloadLanguage() {
        messages.clear();
        loadLanguage();
    }
}