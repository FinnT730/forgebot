package nl.finnt730;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class PrefixCommand extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Get user ID first - we'll need it for UserDB
        String userid = event.getMessage().getAuthor().getId();
        
        // Get current prefix from UserDB (this will return "!" if none set)
        String currentPrefix = UserDB.prefix(userid);
        String rawMessage = event.getMessage().getContentRaw();
        
        // Check if message starts with either "!" or current prefix followed by "prefix"
        boolean startsWithExclamation = rawMessage.startsWith("!prefix");
        boolean startsWithCurrentPrefix = rawMessage.startsWith(currentPrefix + "prefix");
        
        // Only process if it starts with one of the valid prefixes
        if (startsWithExclamation || startsWithCurrentPrefix) {
            String[] parts = rawMessage.split(" ");
            
            // Determine the actual command prefix used
            String commandPrefix = startsWithExclamation ? "!" : currentPrefix;
            
            // Extract the new prefix - it should be after the command
            int prefixIndex = 1;
            if (startsWithExclamation) {
                // For "!prefix new", the command is at index 0
                prefixIndex = 1;
            } else {
                // For "$prefix new", the command is at index 0
                prefixIndex = 1;
            }
            
            if (parts.length < prefixIndex + 1) {
                event.getChannel().sendMessage("Usage: " + commandPrefix + "prefix <new_prefix>").queue();
                return;
            }
            
            String newPrefix = parts[prefixIndex].trim();
            
            // Basic validation
            if (newPrefix.isEmpty()) {
                event.getChannel().sendMessage("Prefix cannot be empty!").queue();
                return;
            }
            
            if (newPrefix.length() > 5) {
                event.getChannel().sendMessage("Prefix cannot be longer than 5 characters!").queue();
                return;
            }
            
            // Update the prefix in UserDB
            UserDB.setPrefix(userid, newPrefix);
            
            // Confirm the change
            event.getChannel().sendMessage("✅ Prefix changed to `" + newPrefix + "`\n" +
                    "You can now use `" + newPrefix + "command` to run commands!").queue();
        }
    }
}