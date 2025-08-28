package nl.finnt730;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.Set;

public final class ExecuteCommand extends ListenerAdapter {
    private static final Set<String> RESERVED_COMMANDS = Set.of("register", "alias", "delete", "description");

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String rawMessage = event.getMessage().getContentRaw();

        // Check if the message starts with "!"
        if (rawMessage.isEmpty() || rawMessage.charAt(0) != '!')
            return;

        String commandName = rawMessage.substring(1).split(" ", 2)[0];
        if (RESERVED_COMMANDS.contains(commandName))
            return;

        try {
            var command = JsonStructureLib.createReader().readFile("commands/" + commandName + ".json");
            if (command != null && command.getString("name", "_null").equals(commandName)) {
                String data = command.getString("data", "");
                event.getChannel().sendMessage(data).queue();
                return;
            }
        } catch (Exception e) {
            // Ignore the exception, it means the file does not exist
        }

        try {
            // If not found, check aliases
            var files = new java.io.File("commands").listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (var file : files) {
                    var cmd = JsonStructureLib.createReader().readFile(file.getPath());
                    var aliases = cmd.getStringArray("aliases");
                    if (aliases.contains(commandName)) {
                        String data = cmd.getString("data", "");
                        event.getChannel().sendMessage(data).queue();
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore the exception
        }

        event.getChannel().sendMessage("Command not found!").queue();

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