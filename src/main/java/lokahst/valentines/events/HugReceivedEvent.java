package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class HugReceivedEvent extends ValentinesEvent {
    private final Player sender;

    public HugReceivedEvent(Player receiver, Player sender) {
        super(receiver);
        this.sender = sender;
    }

    public Player getSender() {
        return sender;
    }
}