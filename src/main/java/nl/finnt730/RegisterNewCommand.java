package nl.finnt730;

import com.jsonstructure.DynamicJson;
import haxe.root.Array;
import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class RegisterNewCommand extends ListenerAdapter  {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getMessage().getContentRaw().startsWith("!register")) {
            String command = event.getMessage().getContentRaw().substring(1).split(" ", 2)[0];
            String messageContent = event.getMessage().getContentRaw().substring(command.length() + 2);

            System.out.println(command);
            System.out.println("Message content: " + messageContent);

            if (command.equalsIgnoreCase("register")) {
                // Parse the message content properly to handle quoted strings
                String[] parsedArgs = Global.parseQuotedString(messageContent);
                
                if (parsedArgs.length < 2) {
                    event.getChannel().sendMessage("Usage: !register <name> \"<data>\" [description] [aliases]").queue();
                    return;
                }
                
                String name = parsedArgs[0];
                String data = parsedArgs[1];
                String description = parsedArgs.length > 2 ? parsedArgs[2] : "No description provided";
                String[] aliases = parsedArgs.length > 3 ? parsedArgs[3].split(",") : new String[0];
//                StringBuilder options = new StringBuilder();
//                for (int i = 4; i < args.length; i++) {
//                    options.append(args[i]).append(" ");
//                }
//                String optionsString = options.toString().trim();

                try {
                    DynamicJson json_exists = JsonStructureLib.createReader().readFile(name + ".json");
                    if(json_exists != null) {
                        event.getChannel().sendMessage("Command with name " + name + " already exists!").queue();
                        return;
                    }
                } catch (Exception e) {
                    // Ignore the exception, it means the file does not exist
                }


                Array<String> _aliases = new Array<>();
                for (String alias : aliases) {
                    if (!alias.isEmpty()) {
                        _aliases.push(alias.trim());
                    }
                }

                var builder = JsonStructureLib.createObjectBuilder();
                var commandObject = builder
                        .addStringField("name", name)
                        .addStringField("description", description)
                        .addStringField("data", data)
                        .addStringArrayField("aliases", _aliases)
//                        .addStringField("options", optionsString)
                        .build();

                JsonStructureLib.writeJsonFile(commandObject, "commands/" + name + ".json", Global.COMMAND_STRUCTURE);
                event.getChannel().sendMessage("command has been registered!").queue();

            } else {
                event.getChannel().sendMessage("Unknown command: " + command).queue();
            }
        }
    }
}
