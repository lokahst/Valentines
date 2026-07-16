package lokahst.valentines.data;

public enum Achievement {
    FIRST_KISS_RECEIVED("first_kiss_received"),
    FIRST_HUG_RECEIVED("first_hug_received"),
    FIRST_LIKE_RECEIVED("first_like_received"),
    FIRST_KISS_GIVEN("first_kiss_given"),
    FIRST_HUG_GIVEN("first_hug_given"),
    FIRST_LIKE_GIVEN("first_like_given"),
    REACH_10_KISSES("reach_10_kisses"),
    REACH_10_HUGS("reach_10_hugs"),
    REACH_10_LIKES("reach_10_likes"),
    MARRIAGE("marriage"),
    DIVORCE("divorce"),
    MARRIED_30_DAYS("married_30_days"),
    MARRIED_365_DAYS("married_365_days"),
    MARRIED_2_YEARS("married_2_years"),
    FIRST_MOOD_SET("first_mood_set"),
    MAKE_A_FRIEND("make_a_friend"),
    FRIENDS_FOR_3_MONTHS("friends_for_3_months");

    private final String key;

    Achievement(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static Achievement fromKey(String key) {
        for (Achievement achievement : values()) {
            if (achievement.key.equals(key)) {
                return achievement;
            }
        }
        return null;
    }
}