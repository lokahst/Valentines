package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class HugGivenEvent extends ValentinesEvent {
    private final Player receiver;

    public HugGivenEvent(Player sender, Player receiver) {
        super(sender);
        this.receiver = receiver;
    }

    public Player getReceiver() {
        return receiver;
    }
}