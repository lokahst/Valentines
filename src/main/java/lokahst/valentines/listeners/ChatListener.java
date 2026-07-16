package lokahst.valentines.listeners;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import lokahst.valentines.Valentines;

import java.util.List;
import java.util.regex.Pattern;

public class ChatListener implements Listener {
    
    private final Valentines plugin;
    private final Pattern[] wordPatterns;
    
    public ChatListener(Valentines plugin) {
        this.plugin = plugin;
        List<String> words = plugin.getConfig().getStringList("words");
        this.wordPatterns = new Pattern[words.size()];
        for (int i = 0; i < words.size(); i++) {
            this.wordPatterns[i] = Pattern.compile("(?i)" + Pattern.quote(words.get(i)));
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!plugin.getConfig().getBoolean("symbol-change", true)) {
            return;
        }
        
        String message = event.getMessage();
        String symbol = plugin.getConfig().getString("symbol", "♥");
        String symbolColor = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("symbol-color", "&c"));
        String replacement = symbolColor + symbol + ChatColor.RESET;
        
        for (Pattern pattern : wordPatterns) {
            message = pattern.matcher(message).replaceAll(replacement);
        }
        
        event.setMessage(message);
    }
}