package nl.finnt730;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Set;

public final class ExecuteCommand extends ListenerAdapter {
    private static final Set<String> RESERVED_COMMANDS = Set.of("register", "alias", "delete", "description","pastesite");

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return; // Strictly ignore any bot messages.
        String rawMessage = event.getMessage().getContentRaw();
        if (rawMessage.isEmpty()) return;
        String author = event.getAuthor().getName();
        boolean silent = rawMessage.startsWith("s") || rawMessage.startsWith("S");
        var context = CommandCache.getOrDefault(event.getAuthor().getName(), silent ? rawMessage.substring(1) : rawMessage);
        if (context == CommandContext.NONE) {
            return; // Was not a command
        } else if (context == CommandContext.NOT_FOUND) {
            event.getChannel().sendMessage("Command not found!").queue();
        } else {
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