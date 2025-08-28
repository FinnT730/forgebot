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
    private static final String DEFAULT_PREFIX = "!";
    
    // Internal structure for user data
    private static class UserData {
        String pasteSite = DEFAULT_PASTE_SITE;
        String prefix = DEFAULT_PREFIX;
    }

    /**
     * Generic getter that retrieves any field for a user
     * @param userid The user ID to look up
     * @param key The field name to retrieve ("pasteSite" or "prefix")
     * @return The field value, or default if not found
     */
    public static String get(String userid, String key) {
        Map<String, UserData> db = loadDB();
        UserData user = db.get(userid);
        
        if (user == null) {
            return "pastesite".equals(key) ? DEFAULT_PASTE_SITE : 
                   "prefix".equals(key) ? DEFAULT_PREFIX : null;
        }
        
        return "pastesite".equals(key) ? user.pasteSite : 
               "prefix".equals(key) ? user.prefix : null;
    }
    
    /**
     * Generic setter that sets any field for a user
     * @param userid The user ID to update
     * @param key The field name to set ("pasteSite" or "prefix")
     * @param value The value to set
     */
    public static void set(String userid, String key, String value) {
        Map<String, UserData> db = loadDB();
        UserData user = db.computeIfAbsent(userid, k -> new UserData());
        
        if ("pastesite".equals(key)) {
            user.pasteSite = value;
        } else if ("prefix".equals(key)) {
            user.prefix = value;
        }
        
        saveDB(db);
    }
    
    // Specific methods
    public static String pasteSite(String userid) {
        return get(userid, "pastesite");
    }
    
    public static String prefix(String userid) {
        return get(userid, "prefix");
    }
    
    public static void setPasteSite(String userid, String value) {
        set(userid, "pastesite", value);
    }
    
    public static void setPrefix(String userid, String value) {
        set(userid, "prefix", value);
    }

    // Internal database operations
    private static Map<String, UserData> loadDB() {
        File file = new File(DB_FILE);
        if (!file.exists()) {
            return new HashMap<>();
        }
        
        try (Reader reader = new FileReader(file)) {
            return GSON.fromJson(reader, new TypeToken<Map<String, UserData>>(){}.getType());
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