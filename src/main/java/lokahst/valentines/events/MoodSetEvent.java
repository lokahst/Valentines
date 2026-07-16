package lokahst.valentines.events;

import org.bukkit.entity.Player;

public class MoodSetEvent extends ValentinesEvent {
    private final String mood;

    public MoodSetEvent(Player player, String mood) {
        super(player);
        this.mood = mood;
    }

    public String getMood() {
        return mood;
    }
}