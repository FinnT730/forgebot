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
            DynamicJson json = JsonStructureLib.createReader().readFile("env.json");
            String botToken = json.getString("botToken", "");

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