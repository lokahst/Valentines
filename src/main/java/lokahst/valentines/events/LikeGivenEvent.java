package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class LikeGivenEvent extends ValentinesEvent {
    private final Player receiver;

    public LikeGivenEvent(Player sender, Player receiver) {
        super(sender);
        this.receiver = receiver;
    }

    public Player getReceiver() {
        return receiver;
    }
}