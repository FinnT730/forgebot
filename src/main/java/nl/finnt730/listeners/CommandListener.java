package nl.finnt730.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import nl.finnt730.commands.CommandCache;
import nl.finnt730.commands.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public final class CommandListener extends ListenerAdapter {
    private static final Logger logger = LoggerFactory.getLogger("nl.finnt730.commands");
    private static final Set<String> RESERVED_COMMANDS = Set.of("register", "alias", "delete", "description");

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            if (event.getAuthor().isBot()) return; // Strictly ignore any bot messages.
            String rawMessage = event.getMessage().getContentRaw();
            if (rawMessage.isEmpty()) return;
            
            Member author = event.getMember();
            boolean silent = rawMessage.startsWith("s") || rawMessage.startsWith("S");
            String commandText = silent ? rawMessage.substring(1) : rawMessage;
            
            logger.debug("Processing message from user {} in guild {}: {}", 
                author != null ? author.getUser().getName() : "Unknown", 
                event.getGuild() != null ? event.getGuild().getName() : "DM", 
                commandText);
            
            var context = CommandCache.getOrDefault(author, commandText);
            if (context == CommandContext.NONE) {
                return; // Was not a command attempt
            } else if (context == CommandContext.NOT_FOUND) {
                logger.debug("Command not found: {}", commandText);
                event.getChannel().sendMessage("Command not found!").queue();
            } else {
                if (!context.command().canInvoke(event.getGuild(), author)) {
                    logger.debug("User {} does not have permission to execute command: {}", 
                        author != null ? author.getUser().getName() : "Unknown", commandText);
                    return;
                }
                // Silently deny if user does not have permission. Up for discussion.
                logger.info("Executing command '{}' for user {} in guild {}", 
                    commandText, 
                    author != null ? author.getUser().getName() : "Unknown",
                    event.getGuild() != null ? event.getGuild().getName() : "DM");
                context.command().handle(event, author, context.additionalData());
                if (silent) event.getMessage().delete().queue();
            }
        } catch (Exception e) {
            logger.error("Error processing command message", e);
        }

//        var command = JsonStructureLib.createReader().readFile("commands/" + event.getMessage().getContentRaw().substring(1).split(" ")[0] + ".json");
//        if (command == null) {
//            event.getChannel().sendMessage("Command not found!").queue();
//            return;
//        }
//
//        if(command.getString("name", "_null").equals("_null")) {
//            event.getChannel().sendMessage("Command not found!").queue();
//            return;
//        } else {
//            String data = command.getString("data", "");
//            String[] args = event.getMessage().getContentRaw().substring(event.getMessage().getContentRaw().indexOf(" ") + 1).split(" ");
//
//            if (data.isEmpty()) {
//                event.getChannel().sendMessage("No data provided for command " + command.getString("name", "null") + "!").queue();
//                return;
//            }
//
//            // Here you can execute the command with the provided data and args
//            // For example, you could send a message back to the channel
//            event.getChannel().sendMessage("Executing command: " + command.getString("name", null) + " with data: " + data + " and args: " + String.join(", ", args)).queue();
//        }
    }
}
