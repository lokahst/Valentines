package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class MarriageCreatedEvent extends ValentinesEvent {
    private final Player partner;

    public MarriageCreatedEvent(Player player, Player partner) {
        super(player);
        this.partner = partner;
    }

    public Player getPartner() {
        return partner;
    }
}