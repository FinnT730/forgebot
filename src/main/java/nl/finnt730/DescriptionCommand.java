package nl.finnt730;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Objects;

public final class DescriptionCommand extends ReservedCommand {

    @Override
    public void handle(MessageReceivedEvent event, String invoker, String additionalData) {
        String rawMessage = event.getMessage().getContentRaw();
        String command = rawMessage.substring(1).split(" ", 2)[0];
        String messageContent = rawMessage.substring(command.length() + 2);

        if (command.equalsIgnoreCase("description")) {
            // Check if user has admin or manage server permissions
            if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR) &&
                !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
                event.getChannel().sendMessage("❌ You don't have permission to modify command descriptions! You need Administrator or Manage Server permissions.").queue();
                return;
            }

            // Parse the message content properly to handle quoted strings
            String[] parsedArgs = Global.parseQuotedString(messageContent);

            if (parsedArgs.length < 2) {
                event.getChannel().sendMessage("Usage: !description <command_name> \"<new_description>\"").queue();
                return;
            }

            String commandName = parsedArgs[0];
            String newDescription = parsedArgs[1];

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

                // Get the old description for confirmation
                String oldDescription = commandFile.getString("description", "No description");
                String data = commandFile.getString("data", "");
                var aliases = commandFile.getStringArray("aliases");

                // Create updated command object
                var builder = JsonStructureLib.createObjectBuilder();
                var updatedCommandObject = builder
                        .addStringField("name", commandName)
                        .addStringField("description", newDescription)
                        .addStringField("data", data)
                        .addStringArrayField("aliases", aliases)
                        .build();

                // Write the updated command back to file
                JsonStructureLib.writeJsonFile(updatedCommandObject, String.format(Global.COMMANDS_LOCATION, commandName), Global.COMMAND_STRUCTURE);

                event.getChannel().sendMessage("✅ Successfully updated description for command `" + commandName + "`!\n" +
                        "**Old description:** " + oldDescription + "\n" +
                        "**New description:** " + newDescription).queue();

            } catch (Exception e) {
                event.getChannel().sendMessage("❌ Error updating description for command `" + commandName + "`: " + e.getMessage()).queue();
            }
        } else {
            event.getChannel().sendMessage("Unknown command: " + command).queue();
        }
    }
}
