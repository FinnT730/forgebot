package nl.finnt730;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

//TODO move to actual DB
public final class UserDB {
    private static final String DB_FILE = "userdb.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DEFAULT_PASTE_SITE = "mclogs";

    // Internal structure for user data (only pasteSite now)
    private static class UserData {
        String pasteSite = DEFAULT_PASTE_SITE;
    }

    /**
     * Get paste site for a user
     * @param userid The user ID to look up
     * @return The pasteSite value, or default if not found
     */
    public static String pasteSite(String userid) {
        Map<String, UserData> db = loadDB();
        UserData user = db.get(userid);
        return user == null ? DEFAULT_PASTE_SITE : user.pasteSite;
    }

    /**
     * Set paste site for a user
     * @param userid The user ID to update
     * @param value The paste site value to set
     */
    public static void setPasteSite(String userid, String value) {
        Map<String, UserData> db = loadDB();
        UserData user = db.computeIfAbsent(userid, k -> new UserData());
        user.pasteSite = value;
        saveDB(db);
    }

    // Internal database operations
    private static Map<String, UserData> loadDB() {
        File file = new File(DB_FILE);
        if (!file.exists()) {
            return new HashMap<>();
        }

        try (Reader reader = new FileReader(file)) {
            Map<String, UserData> data = GSON.fromJson(reader, new TypeToken<Map<String, UserData>>(){}.getType());
            return data == null ? new HashMap<>() : data;
        } catch (Exception e) {
            System.err.println("Error loading user database: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private static void saveDB(Map<String, UserData> db) {
        try (Writer writer = new FileWriter(DB_FILE)) {
            GSON.toJson(db, writer);
        } catch (IOException e) {
            System.err.println("Error saving user database: " + e.getMessage());
        }
    }
}
