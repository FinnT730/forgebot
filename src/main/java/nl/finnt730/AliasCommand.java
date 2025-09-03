package nl.finnt730;

import haxe.root.Array;
import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static nl.finnt730.Global.*;

public final class AliasCommand extends ReservedCommand {
    private static final String USAGE_HINT = "Usage: !alias <existing_command> <new_alias1> <new_alias2>...";
    public AliasCommand() {}

    @Override
    public void handle(MessageReceivedEvent event, Member invoker, String commandContents) {
        var parts = commandContents.split(" ");
        if(parts.length < 2) {
            event.getChannel().sendMessage(USAGE_HINT).queue();
            return;
        }
        String existingCommandName = parts[0];
        if (!CommandCache.existsIsReal(existingCommandName)) {
            event.getChannel().sendMessage("Command " + existingCommandName + " not found!").queue();
            return;
        }
        int added = 0;
        try {
            // File is arbiter of truth so I guess it's fine to do IO every time.
            var command = JsonStructureLib.createReader().readFile(Global.commandOf(existingCommandName));
            Array<String> aliases = command.getStringArray("aliases");
            for (int i = 1; i < parts.length; i++) {
                String alias = parts[i];
                if (aliases.contains(alias) || CommandCache.isTakenAlias(alias)) continue;
                aliases.push(alias);
                added++;
            }
            var builder = JsonStructureLib.createObjectBuilder();
            var updatedCommand = builder
                    .addStringField(NAME_KEY, command.getString(NAME_KEY, ""))
                    .addStringField(DESC_KEY, command.getString(DESC_KEY, ""))
                    .addStringField(DATA_KEY, command.getString(DATA_KEY, ""))
                    .addStringArrayField(ALIAS_KEY, aliases)
                    .build();

            JsonStructureLib.writeJsonFile(updatedCommand, Global.commandOf(existingCommandName), null);
            var iter = aliases.iterator();
            while (iter.hasNext()) {
                CommandCache.addObservedAlias(iter.next());
            }
            event.getChannel().sendMessage("Added " + added + " aliases to command " + existingCommandName + "!").queue();
        } catch (Exception ex) {
            event.getChannel().sendMessage("Something went wrong while trying to add aliases. Try again.").queue();
        }
    }
}
