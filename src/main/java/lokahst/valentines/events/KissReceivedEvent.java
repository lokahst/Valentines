package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class KissReceivedEvent extends ValentinesEvent {
    private final Player sender;

    public KissReceivedEvent(Player receiver, Player sender) {
        super(receiver);
        this.sender = sender;
    }

    public Player getSender() {
        return sender;
    }
}