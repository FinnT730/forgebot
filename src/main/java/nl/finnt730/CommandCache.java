package nl.finnt730;

import haxe.root.JsonStructureLib;

import java.util.*;

public class CommandCache {
    private static final Map<String, Command> cache = new HashMap<>();
    private static final String DEFAULT_PREFIX = "!";

    // Init reserved commands
    static {
        cache.put("register", new RegisterNewCommand());
        cache.put("alias", new AliasCommand());
        cache.put("delete", new DeleteCommand());
        cache.put("description", new DescriptionCommand());
    }
    public static CommandContext getOrDefault(String user, String rawContent) {
        // Parse out the actual command
        String actualCommand = null;
        String additionalData = null;
        if (rawContent.startsWith(DEFAULT_PREFIX)) {
            var temp = rawContent.substring(1).split(" ");
            actualCommand = temp[0];
            additionalData = rawContent.substring(actualCommand.length() + 1).trim();
        }

        if (actualCommand == null) return CommandContext.NONE;
        if (cache.containsKey(actualCommand)) return new CommandContext(cache.get(actualCommand), additionalData);

        CommandContext result = null;
        // Get if not in cache.
        try {
            var cmdJson = JsonStructureLib.createReader().readFile(Global.commandOf(actualCommand));
            if (cmdJson != null && cmdJson.getString("name", "_null").equals(actualCommand)) {
                String data = cmdJson.getString("data", "");
                // todo will need adaptation for things that aren't EchoCommands, but atm everything is... so.... :)
                result = new CommandContext(new EchoCommand(data), null);
            }
        } catch (Exception ignored) {} // if finn is ignoring exceptions then so will I
        try {
            // Find alias if present
            var files = new java.io.File("commands").listFiles((dir, name) -> name.endsWith(".json"));
            if (files != null) {
                for (var file : files) {
                    var cmd = JsonStructureLib.createReader().readFile(file.getPath());
                    var aliases = cmd.getStringArray("aliases");
                    if (aliases.contains(actualCommand)) {
                        String data = cmd.getString("data", "");
                        // todo same here
                        result = new CommandContext(new EchoCommand(data), null);
                    }
                }
            }
        } catch (Exception ignored) {}
        if (result != null) {
            cache.put(actualCommand, result.command());
            return result;
        }
        return CommandContext.NOT_FOUND;
    }


    public static void invalidateOnUpdate(String command) {
        cache.remove(command);
    }
}
