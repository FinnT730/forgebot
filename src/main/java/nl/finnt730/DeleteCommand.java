package nl.finnt730;

import haxe.root.Array;
import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;

import static nl.finnt730.Global.ALIAS_KEY;

public final class DeleteCommand extends ReservedCommand {
    private static final String USAGE_HINT = "Usage: !delete <command_name>";

    @Override
    public void handle(MessageReceivedEvent event, Member invoker, String commandContents) {
        String commandName = commandContents.trim();
        if (commandName.isEmpty()) {
            event.getChannel().sendMessage(USAGE_HINT).queue();
            return;
        }
        if (CommandCache.existsIsReal(commandName)) {
            try {
                // Delete the command file
                File fileToDelete = new File(Global.commandOf(commandName));
                if (fileToDelete.exists()) {
                    var arr = (Array<String>)JsonStructureLib.createReader().readFile(Global.commandOf(commandName)).getArray(ALIAS_KEY);
                    var iter = arr.iterator();
                    while (iter.hasNext()) {
                        CommandCache.invalidateOnUpdate(iter.next());
                    }
                    CommandCache.invalidateOnUpdate(commandName);
                    if (fileToDelete.delete()) {
                        event.getChannel().sendMessage("Successfully deleted command `" + commandName + "`!").queue();
                    } else {
                        event.getChannel().sendMessage("Failed to delete command `" + commandName + "`. Please try again.").queue();
                    }
                }
            } catch (Exception e) {
                event.getChannel().sendMessage("Error deleting command `" + commandName + "`: " + e.getMessage()).queue();
            }
        } else {
            event.getChannel().sendMessage("Command `" + commandName + "` not found!").queue();
        }
    }
}
