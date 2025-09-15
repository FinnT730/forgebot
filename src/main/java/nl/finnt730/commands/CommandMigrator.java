package nl.finnt730.commands;

import haxe.root.JsonStructureLib;
import nl.finnt730.DatabaseManager;
import nl.finnt730.Global;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to migrate existing JSON commands to the database
 */
public class CommandMigrator {
    
    public static void migrateAllCommands() {
        DatabaseManager dbManager = DatabaseManager.getInstance();
        
        // Try multiple possible locations for the commands directory
        File commandsDir = null;
        String[] possiblePaths = {
            "commands",                    // Current directory
            "../commands",                 // Parent directory (when running from build/libs)
            "../../commands",              // Two levels up
            "src/main/resources/commands"  // Maven/Gradle standard location
        };
        
        for (String path : possiblePaths) {
            File testDir = new File(path);
            if (testDir.exists() && testDir.isDirectory()) {
                commandsDir = testDir;
                System.out.println("Found commands directory at: " + testDir.getAbsolutePath());
                break;
            }
        }
        
        if (commandsDir == null) {
            System.out.println("Commands directory not found in any expected location, skipping migration");
            return;
        }
        
        File[] jsonFiles = commandsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (jsonFiles == null) {
            System.out.println("No JSON files found in commands directory");
            return;
        }
        
        int migrated = 0;
        int failed = 0;
        
        for (File jsonFile : jsonFiles) {
            try {
                var commandJson = JsonStructureLib.createReader().readFile(jsonFile.getPath());
                String name = commandJson.getString(Global.NAME_KEY, "");
                String description = commandJson.getString(Global.DESC_KEY, "No description provided");
                String data = commandJson.getString(Global.DATA_KEY, "");
                var aliasesArray = commandJson.getStringArray(Global.ALIAS_KEY);
                
                List<String> aliases = new ArrayList<>();
                if (aliasesArray != null) {
                    var iter = aliasesArray.iterator();
                    while (iter.hasNext()) {
                        aliases.add(iter.next());
                    }
                }
                
                if (!name.isEmpty() && !data.isEmpty()) {
                    // Check if command already exists in database
                    if (!dbManager.commandExists(name)) {
                        dbManager.createCommand(name, description, data, aliases);
                        migrated++;
                        System.out.println("Migrated command: " + name);
                    } else {
                        System.out.println("Command already exists in database: " + name);
                    }
                } else {
                    System.out.println("Skipping invalid command file: " + jsonFile.getName());
                }
                
            } catch (Exception e) {
                System.err.println("Failed to migrate command from " + jsonFile.getName() + ": " + e.getMessage());
                e.printStackTrace(); // Print full stack trace for debugging
                failed++;
            }
        }
        
        System.out.println("Migration completed. Migrated: " + migrated + ", Failed: " + failed);
    }
    
    public static void migrateUserDatabase() {
        File userDbFile = new File("userdb.json");
        if (!userDbFile.exists()) {
            System.out.println("User database file not found, skipping user migration");
            return;
        }
        
        try {
            com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();
            java.io.Reader reader = new java.io.FileReader(userDbFile);
            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, UserData>>(){}.getType();
            java.util.Map<String, UserData> userData = gson.fromJson(reader, type);
            reader.close();
            
            if (userData != null) {
                DatabaseManager dbManager = DatabaseManager.getInstance();
                int migrated = 0;
                for (java.util.Map.Entry<String, UserData> entry : userData.entrySet()) {
                    dbManager.setUserPasteSite(entry.getKey(), entry.getValue().pasteSite);
                    migrated++;
                }
                System.out.println("Migrated " + migrated + " user records to database");
            }
        } catch (Exception e) {
            System.err.println("Failed to migrate user database: " + e.getMessage());
        }
    }
    
    private static class UserData {
        String pasteSite = "mclogs";
    }
    
    public static void backupJsonCommands() {
        File commandsDir = new File("commands");
        File backupDir = new File("commands_backup");
        
        if (!commandsDir.exists()) {
            System.out.println("Commands directory not found, nothing to backup");
            return;
        }
        
        if (backupDir.exists()) {
            System.out.println("Backup directory already exists, skipping backup");
            return;
        }
        
        if (commandsDir.renameTo(backupDir)) {
            System.out.println("Commands directory backed up to commands_backup");
        } else {
            System.err.println("Failed to backup commands directory");
        }
    }
}
