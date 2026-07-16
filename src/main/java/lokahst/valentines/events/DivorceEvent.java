package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class DivorceEvent extends ValentinesEvent {
    private final Player partner;

    public DivorceEvent(Player player, Player partner) {
        super(player);
        this.partner = partner;
    }

    public Player getPartner() {
        return partner;
    }
}