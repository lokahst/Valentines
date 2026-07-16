package lokahst.valentines.managers;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import lokahst.valentines.Valentines;
import lokahst.valentines.data.Marriage;
import lokahst.valentines.data.MarriageProposal;
import lokahst.valentines.events.DivorceEvent;
import lokahst.valentines.events.MarriageCreatedEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MarriageManager {
    
    private final Valentines plugin;
    private final Map<UUID, Marriage> marriageCache = new HashMap<>();
    private final Map<UUID, MarriageProposal> proposalCache = new HashMap<>();
    
    public MarriageManager(Valentines plugin) {
        this.plugin = plugin;
    }
    
    public void loadMarriage(UUID player) {
        Marriage marriage = plugin.getFileStorage().loadMarriage(player);
        if (marriage != null) {
            marriageCache.put(player, marriage);
        }
    }
    
    public Marriage getMarriage(UUID player) {
        return marriageCache.get(player);
    }
    
    public boolean isMarried(UUID player) {
        return marriageCache.containsKey(player);
    }
    
    public UUID getPartner(UUID player) {
        Marriage marriage = marriageCache.get(player);
        return marriage != null ? marriage.getPartner(player) : null;
    }
    
    public void createMarriage(UUID player1, UUID player2) {
        removeProposal(player1);
        removeProposal(player2);
        
        Marriage marriage = new Marriage(player1, player2, System.currentTimeMillis());
        marriageCache.put(player1, marriage);
        marriageCache.put(player2, marriage);
        
        plugin.getFileStorage().saveMarriage(marriage);

        if (plugin.getConfig().getBoolean("marriage.announcement", true)) {
            Player p1 = Bukkit.getPlayer(player1);
            Player p2 = Bukkit.getPlayer(player2);
            
            if (p1 != null && p2 != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player1", p1.getName());
                placeholders.put("player2", p2.getName());
                
                String announcement = plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.announcement", placeholders);
                Bukkit.broadcastMessage(announcement);
            }
        }
    }
    
    public void deleteMarriage(UUID player) {
        Marriage marriage = marriageCache.get(player);
        if (marriage != null) {
            UUID partner = marriage.getPartner(player);
            
            marriageCache.remove(player);
            if (partner != null) {
                marriageCache.remove(partner);
            }
            
            plugin.getFileStorage().deleteMarriage(player);

            Player p1 = Bukkit.getPlayer(player);
            Player p2 = partner != null ? Bukkit.getPlayer(partner) : null;

            if (p1 != null) plugin.getServer().getPluginManager().callEvent(new DivorceEvent(p1, p2));
            if (p2 != null) plugin.getServer().getPluginManager().callEvent(new DivorceEvent(p2, p1));

            if (plugin.getConfig().getBoolean("marriage.announcement", true)) {
                if (p1 != null && p2 != null) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("player1", p1.getName());
                    placeholders.put("player2", p2.getName());

                    String announcement = plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.divorce-announcement", placeholders);
                    Bukkit.broadcastMessage(announcement);
                }
            }
        }
    }
    

    public void sendProposal(Player proposer, Player target) {
        if (!proposer.hasPermission("valentines.marry")) {
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("general.no-permission"));
            return;
        }

        if (target.equals(proposer)) {
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.self"));
            return;
        }

        long proposalCooldown = plugin.getConfig().getLong("marriage.proposal-cooldown", 30);
        if (plugin.getCooldownManager().hasCooldown(proposer.getUniqueId(), "proposal")) {
            long remaining = plugin.getCooldownManager().getRemainingCooldown(proposer.getUniqueId(), "proposal");
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.proposal-cooldown", Map.of("time", String.valueOf(remaining))));
            return;
        }

        if (isMarried(proposer.getUniqueId())) {
            Marriage marriage = getMarriage(proposer.getUniqueId());
            Player partner = marriage == null ? null : Bukkit.getPlayer(marriage.getPartner(proposer.getUniqueId()));
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.already-married", Map.of("partner", partner != null ? partner.getName() : "Unknown")));
            return;
        }

        if (isMarried(target.getUniqueId())) {
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.target-already-married", Map.of("player", target.getName())));
            return;
        }

        if (!plugin.getPlayerDataManager().getOrLoadPlayerData(target.getUniqueId()).isAllowMarriageRequests()) {
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.requests-disabled", Map.of("player", target.getName())));
            return;
        }

        if (getEngagementProposal(proposer.getUniqueId()) != null || getEngagementProposal(target.getUniqueId()) != null) {
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.active-proposal"));
            return;
        }

        if (!plugin.getConfig().getBoolean("marriage.require-confirmation", true)) {
            createMarriage(proposer.getUniqueId(), target.getUniqueId());
            plugin.getServer().getPluginManager().callEvent(new MarriageCreatedEvent(proposer, target));
            plugin.getServer().getPluginManager().callEvent(new MarriageCreatedEvent(target, proposer));
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.accept-sender", Map.of("player", target.getName())));
            target.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.accept-receiver", Map.of("player", proposer.getName())));
            plugin.getEffectManager().playMarriageEffect(proposer.getLocation());
            plugin.getEffectManager().playMarriageEffect(target.getLocation());
            return;
        }

        plugin.getCooldownManager().setCooldown(proposer.getUniqueId(), "proposal", proposalCooldown * 1000);
        createProposal(proposer.getUniqueId(), target.getUniqueId());
        proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.proposal-sent", Map.of("player", target.getName())));
        target.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.proposal-received", Map.of("player", proposer.getName())));
    }

    public void acceptProposal(Player receiver) {
        MarriageProposal proposal = getProposal(receiver.getUniqueId());
        if (proposal == null) {
            receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.no-pending-proposals"));
            return;
        }

        Player proposer = Bukkit.getPlayer(proposal.proposer());
        if (proposer == null) {
            receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.proposer-offline"));
            removeProposal(receiver.getUniqueId());
            return;
        }

        if (isMarried(receiver.getUniqueId()) || isMarried(proposer.getUniqueId())) {
            receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.already-married", Map.of("partner", proposer.getName())));
            removeProposal(receiver.getUniqueId());
            return;
        }

        createMarriage(proposal.proposer(), receiver.getUniqueId());
        plugin.getServer().getPluginManager().callEvent(new MarriageCreatedEvent(receiver, proposer));
        plugin.getServer().getPluginManager().callEvent(new MarriageCreatedEvent(proposer, receiver));

        proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.accept-sender", Map.of("player", receiver.getName())));
        receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.accept-receiver", Map.of("player", proposer.getName())));

        plugin.getEffectManager().playMarriageEffect(receiver.getLocation());
        plugin.getEffectManager().playMarriageEffect(proposer.getLocation());
    }

    public void denyProposal(Player receiver) {
        MarriageProposal proposal = getProposal(receiver.getUniqueId());
        if (proposal == null) {
            receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.no-pending-proposals"));
            return;
        }

        Player proposer = Bukkit.getPlayer(proposal.proposer());
        removeProposal(receiver.getUniqueId());

        if (proposer != null) {
            proposer.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.decline-sender", Map.of("player", receiver.getName())));
        }

        receiver.sendMessage(plugin.getPrefix() + plugin.getLanguageManager().getMessage("marry.decline-receiver", Map.of("player", proposer != null ? proposer.getName() : "Unknown")));
    }

    public void createProposal(UUID proposer, UUID target) {
        MarriageProposal proposal = new MarriageProposal(proposer, target, System.currentTimeMillis());
        proposalCache.put(target, proposal);
        plugin.getFileStorage().saveProposal(proposal);
    }
    
    public MarriageProposal getProposal(UUID target) {
        MarriageProposal proposal = proposalCache.get(target);
        if (proposal == null) {
            proposal = plugin.getFileStorage().loadProposal(target);
            if (proposal != null) {
                proposalCache.put(target, proposal);
            }
        }
        
        if (proposal != null) {
            long timeout = plugin.getConfig().getLong("marriage.proposal-timeout", 45) * 1000;
            if (proposal.isExpired(timeout)) {
                removeProposal(target);
                return null;
            }
        }
        
        return proposal;
    }
    
    public MarriageProposal getEngagementProposal(UUID playerUuid) {
        long timeout = plugin.getConfig().getLong("marriage.proposal-timeout", 45) * 1000;

        for (MarriageProposal proposal : proposalCache.values()) {
            if ((proposal.proposer().equals(playerUuid) || proposal.target().equals(playerUuid)) && !proposal.isExpired(timeout)) {
                return proposal;
            }
        }

        for (MarriageProposal proposal : plugin.getFileStorage().getAllProposals()) {
            if (proposal.isExpired(timeout)) {
                continue;
            }

            if (proposal.proposer().equals(playerUuid) || proposal.target().equals(playerUuid)) {
                proposalCache.put(proposal.target(), proposal);
                return proposal;
            }
        }

        return null;
    }

    public void removeProposal(UUID target) {
        proposalCache.remove(target);
        plugin.getFileStorage().deleteProposal(target);
    }
    
    public List<Marriage> getAllMarriages() {
        return plugin.getFileStorage().getAllMarriages();
    }
}