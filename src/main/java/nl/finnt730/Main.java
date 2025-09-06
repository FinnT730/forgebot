package nl.finnt730;

import com.jsonstructure.DynamicJson;
import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.EnumSet;

public final class Main {
    public static void main(String[] args) {
        try {
            // Initialize database and migrate existing commands
            System.out.println("Initializing database...");
            DatabaseManager.getInstance(); // This will create the database and load cache
            
            // Migrate existing JSON commands to database (only if they exist)
            System.out.println("Migrating existing commands...");
            CommandMigrator.migrateAllCommands();
            
            // Migrate user database
            System.out.println("Migrating user database...");
            CommandMigrator.migrateUserDatabase();
            
            // Load environment configuration
            DynamicJson json = JsonStructureLib.createReader().readFile("env.json");
            String botToken = json.getString("botToken", "");

            System.out.println("Starting Discord bot...");
            JDABuilder.createLight(botToken, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGE_POLLS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS))
                    .addEventListeners(new ExecuteCommand())
                    .addEventListeners(new PasteCommand())
                    .enableCache(CacheFlag.ROLE_TAGS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL) // Would do ONLINE but I don't think that will work if you aren't literally set to Online status.
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}