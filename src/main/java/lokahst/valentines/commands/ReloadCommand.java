package lokahst.valentines.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import lokahst.valentines.Valentines;

public class ReloadCommand implements CommandExecutor {

    private final Valentines plugin;

    public ReloadCommand(Valentines plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("valentines.reload")) {
            sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("general.no-permission"));
            return true;
        }

        plugin.reloadPlugin();
        sender.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("plugin.reload"));
        return true;
    }
}