package nl.finnt730;

/**
 * User database management using SQLite
 */
public final class UserDB {
    private static final String DEFAULT_PASTE_SITE = "mclogs";
    private static final DatabaseManager dbManager = DatabaseManager.getInstance();

    /**
     * Get paste site for a user
     * @param userid The user ID to look up
     * @return The pasteSite value, or default if not found
     */
    public static String pasteSite(String userid) {
        return dbManager.getUserPasteSite(userid);
    }

    /**
     * Set paste site for a user
     * @param userid The user ID to update
     * @param value The paste site value to set
     */
    public static void setPasteSite(String userid, String value) {
        dbManager.setUserPasteSite(userid, value);
    }
}
