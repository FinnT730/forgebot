package nl.finnt730.commands.builtin.reserved;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import nl.finnt730.DatabaseManager;
import nl.finnt730.commands.CommandCache;

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
                DatabaseManager dbManager = DatabaseManager.getInstance();
                var commandData = dbManager.getCommand(commandName);
                if (commandData.isPresent()) {
                    // Invalidate aliases from cache
                    for (String alias : commandData.get().aliases()) {
                        CommandCache.invalidateOnUpdate(alias);
                    }
                    CommandCache.invalidateOnUpdate(commandName);
                    
                    // Delete from database
                    dbManager.deleteCommand(commandName);
                    event.getChannel().sendMessage("Successfully deleted command `" + commandName + "`!").queue();
                } else {
                    event.getChannel().sendMessage("Command `" + commandName + "` not found!").queue();
                }
            } catch (Exception e) {
                event.getChannel().sendMessage("Error deleting command `" + commandName + "`: " + e.getMessage()).queue();
            }
        } else {
            event.getChannel().sendMessage("Command `" + commandName + "` not found!").queue();
        }
    }
}
