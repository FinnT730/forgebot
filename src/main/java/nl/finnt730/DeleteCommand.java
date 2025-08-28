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
    	String userid = event.getMessage().getAuthor().getId();
    	String prefix = UserDB.prefix(userid);
        String rawMessage = event.getMessage().getContentRaw();
        if (rawMessage.startsWith(prefix+"delete")) {
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
                    event.getChannel().sendMessage("Usage:"+prefix+"delete <command_name>").queue();
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
