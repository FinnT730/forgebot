package nl.finnt730.listeners;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import nl.finnt730.commands.CommandCache;
import nl.finnt730.commands.CommandContext;

import java.util.Set;

public final class CommandListener extends ListenerAdapter {
    private static final Set<String> RESERVED_COMMANDS = Set.of("register", "alias", "delete", "description");

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return; // Strictly ignore any bot messages.
        String rawMessage = event.getMessage().getContentRaw();
        if (rawMessage.isEmpty()) return;
        Member author = event.getMember();
        boolean silent = rawMessage.startsWith("s") || rawMessage.startsWith("S");
        var context = CommandCache.getOrDefault(author, silent ? rawMessage.substring(1) : rawMessage);
        if (context == CommandContext.NONE) {
            return; // Was not a command attempt
        } else if (context == CommandContext.NOT_FOUND) {
            event.getChannel().sendMessage("Command not found!").queue();
        } else {
            if (!context.command().canInvoke(event.getGuild(), author)) return;
            // Silently deny if user does not have permission. Up for discussion.
            context.command().handle(event, author, context.additionalData());
            if (silent) event.getMessage().delete().queue();
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
