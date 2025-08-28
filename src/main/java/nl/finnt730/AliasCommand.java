package nl.finnt730;

import haxe.root.Array;
import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class AliasCommand extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String rawMessage = event.getMessage().getContentRaw();
        if (rawMessage.startsWith("!alias")) {
            String[] parts = rawMessage.split(" ");
            if(parts.length < 3) {
                event.getChannel().sendMessage("Usage: !alias <existing_command> <new_alias>").queue();
                return;
            }

            String existingCommandName = parts[1];
            String newAlias = parts[2];

            var command = JsonStructureLib.createReader().readFile("commands/" + existingCommandName + ".json");
            if (command == null || command.getString("name", "_null").equals("_null")) {
                event.getChannel().sendMessage("Command " + existingCommandName + " not found!").queue();
                return;
            }

            var aliases = command.getStringArray("aliases");
            if (aliases.contains(newAlias)) {
                event.getChannel().sendMessage("Alias " + newAlias + " already exists for command " + existingCommandName + "!").queue();
                return;
            }

            aliases.push(newAlias);
            var builder = JsonStructureLib.createObjectBuilder();
            var updatedCommand = builder
                    .addStringField("name", command.getString("name", ""))
                    .addStringField("description", command.getString("description", ""))
                    .addStringField("data", command.getString("data", ""))
                    .addStringArrayField("aliases", aliases)
                    .build();

            JsonStructureLib.writeJsonFile(updatedCommand, "commands/" + existingCommandName + ".json", null);
            event.getChannel().sendMessage("Alias " + newAlias + " added to command " + existingCommandName + "!").queue();
        }
    }
}