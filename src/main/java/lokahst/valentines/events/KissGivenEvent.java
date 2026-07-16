package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class KissGivenEvent extends ValentinesEvent {
    private final Player receiver;

    public KissGivenEvent(Player sender, Player receiver) {
        super(sender);
        this.receiver = receiver;
    }

    public Player getReceiver() {
        return receiver;
    }
}