package nl.finnt730;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public final class AliasCommand extends ReservedCommand {
    private static final String USAGE_HINT = "Usage: !alias <existing_command> <new_alias>";
    public AliasCommand() {}

    @Override
    public void handle(MessageReceivedEvent event, String invoker, String commandContents) {
        var parts = commandContents.split(" ");
        if(parts.length < 2) {
            event.getChannel().sendMessage(USAGE_HINT).queue();
            return;
        }

        String existingCommandName = parts[0];
        String newAlias = parts[1];

        // File is arbiter of truth so I guess it's fine to do IO every time.
        var command = JsonStructureLib.createReader().readFile(Global.commandOf(existingCommandName));
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

        JsonStructureLib.writeJsonFile(updatedCommand, Global.commandOf(existingCommandName), null);
        event.getChannel().sendMessage("Alias " + newAlias + " added to command " + existingCommandName + "!").queue();
    }
}
