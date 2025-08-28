package nl.finnt730;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.File;

public final class DeleteCommand extends ReservedCommand {

    @Override
    public void handle(MessageReceivedEvent event, String invoker, String commandContents) {
        String rawMessage = event.getMessage().getContentRaw();
        String command = rawMessage.substring(1).split(" ", 2)[0];
        String messageContent = rawMessage.substring(command.length() + 2);

        if (command.equalsIgnoreCase("delete")) {
            // Check if user has admin or manage server permissions
//                if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR) &&
//                    !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
//                    event.getChannel().sendMessage("❌ You don't have permission to delete commands! You need Administrator or Manage Server permissions.").queue();
//                    return;
//                }

            // Parse the command name to delete
            String commandName = messageContent.trim();

            if (commandName.isEmpty()) {
                event.getChannel().sendMessage("Usage: !delete <command_name>").queue();
                return;
            }

            try {
                // Check if the command exists
                var commandFile = JsonStructureLib.createReader().readFile(String.format(Global.COMMANDS_LOCATION, commandName));
                if (commandFile == null) {
                    event.getChannel().sendMessage("❌ Command `" + commandName + "` not found!").queue();
                    return;
                }

                // Verify the command name matches
                String actualName = commandFile.getString("name", "");
                if (!actualName.equals(commandName)) {
                    event.getChannel().sendMessage("❌ Command `" + commandName + "` not found!").queue();
                    return;
                }

                // Delete the command file
                File fileToDelete = new File(String.format(Global.COMMANDS_LOCATION, commandName));
                if (fileToDelete.exists() && fileToDelete.delete()) {
                    CommandCache.invalidateOnUpdate(commandName);
                    event.getChannel().sendMessage("✅ Successfully deleted command `" + commandName + "`!").queue();
                } else {
                    event.getChannel().sendMessage("❌ Failed to delete command `" + commandName + "`. Please try again.").queue();
                }

            } catch (Exception e) {
                event.getChannel().sendMessage("❌ Error deleting command `" + commandName + "`: " + e.getMessage()).queue();
            }
        } else {
            event.getChannel().sendMessage("Unknown command: " + command).queue();
        }
    }
}
