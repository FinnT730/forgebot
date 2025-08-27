package nl.finnt730;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.File;
import java.util.Objects;

public final class DeleteCommand extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().startsWith("!delete")) {
            String command = event.getMessage().getContentRaw().substring(1).split(" ")[0];
            String messageContent = event.getMessage().getContentRaw().substring(command.length() + 2);

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
                    var commandFile = JsonStructureLib.createReader().readFile("commands/" + commandName + ".json");
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
                    File fileToDelete = new File("commands/" + commandName + ".json");
                    if (fileToDelete.exists() && fileToDelete.delete()) {
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
}
