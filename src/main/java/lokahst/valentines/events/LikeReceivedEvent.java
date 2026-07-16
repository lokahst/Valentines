package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class LikeReceivedEvent extends ValentinesEvent {
    private final Player sender;

    public LikeReceivedEvent(Player receiver, Player sender) {
        super(receiver);
        this.sender = sender;
    }

    public Player getSender() {
        return sender;
    }
}