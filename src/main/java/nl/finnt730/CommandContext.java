package nl.finnt730;

public record CommandContext(Command command, String additionalData) {
    public static final CommandContext NONE = new CommandContext(null, null);
    public static final CommandContext NOT_FOUND = new CommandContext(null, null);
}