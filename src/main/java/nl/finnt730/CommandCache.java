package nl.finnt730;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import net.dv8tion.jda.api.entities.Member;

public class CommandCache {
    private static final Map<String, Command> cache = new HashMap<>();
    public static final String DEFAULT_PREFIX = "!";
    public static final String HOI4_ESP = "º";//In HOI4 they always use the key next to 1 no matter the layout.
    private static final DatabaseManager dbManager = DatabaseManager.getInstance();

    // Init reserved commands
    static {
        cache.put("register", new RegisterNewCommand());
        cache.put("alias", new AliasCommand());
        cache.put("delete", new DeleteCommand());
        cache.put("pastesite", new PasteSiteCommand());
        cache.put("description", new DescriptionCommand());
    }
    public static CommandContext getOrDefault(Member user, String rawContent) {
        String actualCommand = null; // String command name, e.g. "register" or "optifine"
        String additionalData = null; // Remainder of message
        if (rawContent.startsWith(DEFAULT_PREFIX)||rawContent.startsWith(HOI4_ESP)) {            var temp = rawContent.substring(1).split(" ");
            actualCommand = temp[0];
            additionalData = rawContent.substring(actualCommand.length() + 1).trim();
        }

        if (actualCommand == null) return CommandContext.NONE;
        if (existsInCache(actualCommand)) return new CommandContext(cache.get(actualCommand), additionalData);
        CommandContext result = null;
        
        // Check if it's a real command name
        if (existsIsReal(actualCommand)) {
            Optional<DatabaseManager.CommandData> cmdData = dbManager.getCommand(actualCommand);
            if (cmdData.isPresent()) {
                String data = cmdData.get().data();
                // todo will need adaptation for things that aren't EchoCommands, but atm everything is... so.... :)
                result = new CommandContext(new EchoCommand(data), null);
            }
        }
        
        // Check if it's an alias
        if (result == null) {
            Optional<DatabaseManager.CommandData> cmdData = dbManager.getCommandByAlias(actualCommand);
            if (cmdData.isPresent()) {
                String data = cmdData.get().data();
                // todo same as above
                result = new CommandContext(new EchoCommand(data), null);
            }
        }

        if (result != null) {
            cache.put(actualCommand, result.command());
            return result;
        }
        return CommandContext.NOT_FOUND;
    }


    public static void invalidateOnUpdate(String command) {
        cache.remove(command);
        dbManager.invalidateCache(command);
    }

    public static boolean existsInCache(String command) { 
        return cache.containsKey(command);
    }

    public static boolean existsIsReal(String commandName) {
        return dbManager.commandExists(commandName);
    }

    public static boolean isTakenAlias(String command) {
        return dbManager.isTakenAlias(command);
    }

    public static void addObservedAlias(String name) {
        dbManager.addObservedAlias(name);
    }

    public static Optional<CommandContext> existsAsAlias(String aliasName) {
        Optional<DatabaseManager.CommandData> cmdData = dbManager.getCommandByAlias(aliasName);
        if (cmdData.isPresent()) {
            String data = cmdData.get().data();
            // todo same as above
            return Optional.of(new CommandContext(new EchoCommand(data), null));
        }
        return Optional.empty();
    }
}
