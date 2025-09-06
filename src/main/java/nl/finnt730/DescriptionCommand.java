package nl.finnt730;

import net.dv8tion.jda.api.Permission;
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
            DatabaseManager dbManager = DatabaseManager.getInstance();
            var commandData = dbManager.getCommand(command);
            if (commandData.isEmpty()) {
                event.getChannel().sendMessage("Command `" + command + "` not found!").queue();
                return;
            }

            // Get the old description for confirmation
            String oldDescription = commandData.get().description();
            String data = commandData.get().data();
            var aliases = commandData.get().aliases();

            // Update the command in database
            dbManager.updateCommand(command, messageContent, data, aliases);

            event.getChannel().sendMessage("Updated description for `" + command + "`!\n" +
                    "**Old description:** " + oldDescription + "\n" +
                    "**New description:** " + messageContent).queue();

        } catch (Exception e) {
            event.getChannel().sendMessage("Error updating description for `" + command + "`: " + e.getMessage()).queue();
        }

    }
}
