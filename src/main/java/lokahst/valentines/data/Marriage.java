package lokahst.valentines.data;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public record Marriage(UUID player1, UUID player2, long marriageDate) {

    public UUID getPartner(UUID player) {
        if (player1.equals(player)) {
            return player2;
        } else if (player2.equals(player)) {
            return player1;
        }
        return null;
    }

    public boolean isMarriedTo(UUID player) {
        return player1.equals(player) || player2.equals(player);
    }

    public int getDaysMarried() {
        long diff = System.currentTimeMillis() - marriageDate;
        return (int) TimeUnit.MILLISECONDS.toDays(diff);
    }
}