package nl.finnt730;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Objects;

public final class DescriptionCommand extends ReservedCommand {
    private static final String USAGE_HINT = "Usage: !description <command_name> \"<new_description>\"";

    @Override
    public void handle(MessageReceivedEvent event, Member invoker, String additionalData) {
        String[] split = additionalData.split(" ", 2);
        if (split.length < 2) {
            event.getChannel().sendMessage(USAGE_HINT).queue();
            return;
        }
        String command = split[0];
        String messageContent = split[1];
        // Check if user has admin or manage server permissions
        if (!Objects.requireNonNull(event.getMember()).hasPermission(Permission.ADMINISTRATOR) &&
            !event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
            event.getChannel().sendMessage("You don't have permission to modify command descriptions! You need Administrator or Manage Server permissions.").queue();
            return;
        }

        try {
            // Check if the command exists
            var commandFile = JsonStructureLib.createReader().readFile(Global.commandOf(command));
            if (commandFile == null) {
                event.getChannel().sendMessage("Command `" + command + "` not found!").queue();
                return;
            }

            // Verify the command name matches
            String actualName = commandFile.getString("name", "");
            if (!actualName.equals(command)) {
                event.getChannel().sendMessage("Command `" + command + "` not found!").queue();
                return;
            }

            // Get the old description for confirmation
            String oldDescription = commandFile.getString("description", "No description");
            String data = commandFile.getString("data", "");
            var aliases = commandFile.getStringArray("aliases");

            // Create updated command object
            var builder = JsonStructureLib.createObjectBuilder();
            var updatedCommandObject = builder
                    .addStringField("name", command)
                    .addStringField("description", messageContent)
                    .addStringField("data", data)
                    .addStringArrayField("aliases", aliases)
                    .build();

            // Write the updated command back to file
            JsonStructureLib.writeJsonFile(updatedCommandObject, Global.commandOf(command), Global.COMMAND_STRUCTURE);

            event.getChannel().sendMessage("Updated description for `" + command + "`!\n" +
                    "**Old description:** " + oldDescription + "\n" +
                    "**New description:** " + messageContent).queue();

        } catch (Exception e) {
            event.getChannel().sendMessage("Error updating description for `" + command + "`: " + e.getMessage()).queue();
        }

    }
}
