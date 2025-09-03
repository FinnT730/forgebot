package nl.finnt730;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.entities.Member;

import java.io.File;
import java.util.*;

import static nl.finnt730.Global.*;

public class CommandCache {
    private static final Map<String, Command> cache = new HashMap<>();
    private static final String DEFAULT_PREFIX = "!";
    private static final Set<String> realCommands = new HashSet<>();
    // Mark what is a taken alias so we don't overlap a command name with an alias.
    // Is okay if it's wiped per reboot since it'll build itself as needed.
    private static final Set<String> observedAliases = new HashSet<>();

    // Init reserved commands
    static {
        cache.put("register", new RegisterNewCommand());
        cache.put("alias", new AliasCommand());
        cache.put("delete", new DeleteCommand());
        cache.put("description", new DescriptionCommand());
    }
    public static CommandContext getOrDefault(Member user, String rawContent) {
        String actualCommand = null; // String command name, e.g. "register" or "optifine"
        String additionalData = null; // Remainder of message
        if (rawContent.startsWith(DEFAULT_PREFIX)) {
            var temp = rawContent.substring(1).split(" ");
            actualCommand = temp[0];
            additionalData = rawContent.substring(actualCommand.length() + 1).trim();
        }

        if (actualCommand == null) return CommandContext.NONE;
        if (existsInCache(actualCommand)) return new CommandContext(cache.get(actualCommand), additionalData);
        CommandContext result = null;
        // Check if it's a real command name
        if (existsIsReal(actualCommand)) {
            try {
                var cmdJson = JsonStructureLib.createReader().readFile(Global.commandOf(actualCommand));
                if (cmdJson != null && cmdJson.getString(NAME_KEY, "_null").equals(actualCommand)) {
                    String data = cmdJson.getString(DATA_KEY, "");
                    // todo will need adaptation for things that aren't EchoCommands, but atm everything is... so.... :)
                    result = new CommandContext(new EchoCommand(data), null);
                }
            } catch (Exception ignored) {} // if finn is ignoring exceptions then so will I
        }
        // Check if it's an alias
        if (result == null) {
            try {
                var opt = existsAsAlias(actualCommand);
                if (opt.isPresent()) {
                    observedAliases.add(actualCommand);
                    result = opt.get();
                }
            } catch (Exception ignored) {}
        }

        if (result != null) {
            cache.put(actualCommand, result.command());
            return result;
        }
        return CommandContext.NOT_FOUND;
    }


    public static void invalidateOnUpdate(String command) {
        cache.remove(command);
        observedAliases.remove(command);
        realCommands.remove(command);
    }

    public static boolean existsInCache(String command) { return cache.containsKey(command);}

    public static boolean existsIsReal(String commandName) {
        if(realCommands.contains(commandName) || new File(Global.commandOf(commandName)).exists()) {
            realCommands.add(commandName);
            return true;
        } return false;
    }

    public static boolean isTakenAlias(String command) {
        return observedAliases.contains(command) || existsAsAlias(command).isPresent();
    }

    public static void addObservedAlias(String name) {
        observedAliases.add(name);
    }

    public static Optional<CommandContext> existsAsAlias(String aliasName) {
        var files = new java.io.File("commands").listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (var file : files) {
                var cmd = JsonStructureLib.createReader().readFile(file.getPath());
                var aliases = cmd.getStringArray(ALIAS_KEY);
                if (aliases.contains(aliasName)) {
                    String data = cmd.getString(DATA_KEY, "");
                    // todo same as above
                    observedAliases.add(aliasName);
                    return Optional.of(new CommandContext(new EchoCommand(data), null));
                }
            }
        }
        return Optional.empty();
    }
}
