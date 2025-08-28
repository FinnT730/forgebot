package nl.finnt730;

import com.jsonstructure.DynamicJson;
import haxe.root.Array;
import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public final class RegisterNewCommand extends ReservedCommand {
    private static final String USAGE_HINT = "Usage: !register <name> \"<data>\" [description] [aliases]";

    @Override
    public void handle(MessageReceivedEvent event, String invoker, String commandContents) {
        String[] split = commandContents.split(" ", 2);
        if (split.length < 2) {
            event.getChannel().sendMessage(USAGE_HINT).queue();
            return;
        }
        String command = split[0];
        String description = split[1].replace("\"", "");
        // Parse the message content properly to handle quoted strings
        String[] parsedArgs = Global.parseQuotedString(description);

        String data = parsedArgs[1];
        description = parsedArgs.length > 2 ? parsedArgs[2] : "No description provided";
        String[] aliases = parsedArgs.length > 3 ? parsedArgs[3].split(",") : new String[0];
//                StringBuilder options = new StringBuilder();
//                for (int i = 4; i < args.length; i++) {
//                    options.append(args[i]).append(" ");
//                }
//                String optionsString = options.toString().trim();

        try {
            DynamicJson json_exists = JsonStructureLib.createReader().readFile(String.format(Global.COMMANDS_LOCATION, command));
            if(json_exists != null) {
                event.getChannel().sendMessage("Command with name " + command + " already exists!").queue();
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
                .addStringField("name", command)
                .addStringField("description", description)
                .addStringField("data", data)
                .addStringArrayField("aliases", _aliases)
//                        .addStringField("options", optionsString)
                .build();

        JsonStructureLib.writeJsonFile(commandObject, String.format(Global.COMMANDS_LOCATION, command), Global.COMMAND_STRUCTURE);
        event.getChannel().sendMessage("Command has been registered!").queue();
    }
}
