package nl.finnt730.commands.builtin.reserved;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import nl.finnt730.DatabaseManager;

import java.util.ArrayList;
import java.util.List;

import static nl.finnt730.commands.CommandCache.existsIsReal;
import static nl.finnt730.commands.CommandCache.isTakenAlias;

public final class RegisterNewCommand extends ReservedCommand {
    private static final String USAGE_HINT = "Usage: !register <name> <contents>";

    @Override
    public void handle(MessageReceivedEvent event, Member invoker, String commandContents) {
        String[] split;
        int newLineIndex = commandContents.indexOf('\n');
        if (newLineIndex != -1 && newLineIndex < commandContents.indexOf(' ')) {
            split = commandContents.split("\n", 2);
        } else {
            split = commandContents.split(" ", 2);
        }
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
        DatabaseManager dbManager = DatabaseManager.getInstance();
        List<String> emptyAliases = new ArrayList<>();
        dbManager.createCommand(name, "N/A", contents, emptyAliases);
    }
}
