package nl.finnt730;

import java.util.EnumSet;

import com.jsonstructure.DynamicJson;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public final class Main {
    public static void main(String[] args) {
        try {
            DynamicJson json = JsonStructureLib.createReader().readFile("env.json");
            String botToken = json.getString("botToken", "");

            JDABuilder.createLight(botToken, EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGE_POLLS, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS))
                    .addEventListeners(new RegisterNewCommand())
                    .addEventListeners(new ExecuteCommand())
                    .addEventListeners(new AliasCommand())
                    .addEventListeners(new DeleteCommand())
                    .addEventListeners(new DescriptionCommand())
                    .addEventListeners(new PasteSiteCommand())
                    .addEventListeners(new PasteCommand())
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}