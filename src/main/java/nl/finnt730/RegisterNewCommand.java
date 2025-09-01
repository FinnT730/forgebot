package nl.finnt730;

import haxe.root.Array;
import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import static nl.finnt730.CommandCache.existsIsReal;
import static nl.finnt730.CommandCache.isTakenAlias;
import static nl.finnt730.Global.*;

public final class RegisterNewCommand extends ReservedCommand {
    private static final String USAGE_HINT = "Usage: !register <name> <contents>";

    @Override
    public void handle(MessageReceivedEvent event, Member invoker, String commandContents) {
        String[] split = commandContents.split(" ", 2);
        if (split.length < 2) {
            event.getChannel().sendMessage(USAGE_HINT).queue();
            return;
        }
        String command = split[0];
        String contents = split[1];
        if (existsIsReal(command) || isTakenAlias(command)) {
            event.getChannel().sendMessage("Command with name " + command + " already exists!").queue();
            return;
        }
        buildSimple(command, contents);
        event.getChannel().sendMessage("Command has been registered!").queue();
    }

    private void buildSimple(String name, String contents) {
        build(name, "N/A", contents, new Array<>(), null);
    }

    // Feal: I don't think it's worth it to support such a robust registration.
    // Typically we don't know what we want an alias to be at registration time and the description isn't even used anywhere (yet)
    // I will leave this method here in case we want alternate registration methods.
    private void build(String cmdName, String desc, String data, Array<String> aliases, String options) {
        var builder = JsonStructureLib.createObjectBuilder();
        var commandObject = builder
                .addStringField(NAME_KEY, cmdName)
                .addStringField(DESC_KEY, desc)
                .addStringField(DATA_KEY, data)
                .addStringArrayField(ALIAS_KEY, aliases)
//                        .addStringField("options", optionsString)
                .build();

        JsonStructureLib.writeJsonFile(commandObject, Global.commandOf(cmdName), Global.COMMAND_STRUCTURE);
    }
}
