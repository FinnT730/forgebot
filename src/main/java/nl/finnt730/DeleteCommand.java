package nl.finnt730;

import haxe.root.Array;
import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;

public final class DeleteCommand extends ReservedCommand {
    private static final String USAGE_HINT = "Usage: !delete <command_name>";
    @Override
    public void handle(MessageReceivedEvent event, String invoker, String commandContents) {
        String rawMessage = event.getMessage().getContentRaw();
        String command = rawMessage.substring(1).split(" ", 2)[0];
        String messageContent = rawMessage.substring(command.length() + 2);

        // Check if user has admin or manage server permissions
//                if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR) &&
//                    !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
//                    event.getChannel().sendMessage("❌ You don't have permission to delete commands! You need Administrator or Manage Server permissions.").queue();
//                    return;
//                }

        // Parse the command name to delete
        String commandName = messageContent.trim();

        if (commandName.isEmpty()) {
            event.getChannel().sendMessage(USAGE_HINT).queue();
            return;
        }

        try {
            // Check if the command exists
            var commandFile = JsonStructureLib.createReader().readFile(Global.commandOf(commandName));
            if (commandFile == null) {
                event.getChannel().sendMessage("Command `" + commandName + "` not found!").queue();
                return;
            }

            // Verify the command name matches
            String actualName = commandFile.getString("name", "");
            if (!actualName.equals(commandName)) {
                event.getChannel().sendMessage("Command `" + commandName + "` not found!").queue();
                return;
            }

            // Delete the command file
            File fileToDelete = new File(Global.commandOf(commandName));
            if (fileToDelete.exists()) {
                var arr = (Array<String>)JsonStructureLib.createReader().readFile(Global.commandOf(commandName)).getArray("aliases");
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
    }
}
