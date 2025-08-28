package nl.finnt730;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class PasteSiteCommand extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Get user ID first - we'll need it for UserDB
        String userid = event.getMessage().getAuthor().getId();
        
        // Get current prefix from UserDB (this will return "!" if none set)
        String currentPrefix = UserDB.prefix(userid);
        String rawMessage = event.getMessage().getContentRaw();
        
        boolean startsWithCurrentPrefix = rawMessage.startsWith(currentPrefix + "pastesite");
        
        // Only process if it starts with one of the valid prefixes
        if (startsWithCurrentPrefix) {
            String[] parts = rawMessage.split(" ");
                        
            // Extract the new paste site - it should be after the command
            int siteIndex = 1;

            
            if (parts.length < siteIndex + 1) {
                event.getChannel().sendMessage("Usage: " + currentPrefix + "pastesite <site>\n" +
                        "Available sites: mclogs, pastesdev, securelogger, mmd").queue();
                return;
            }
            
            String newPasteSite = parts[siteIndex].trim().toLowerCase();
            
            // Validate the paste site
            if (!isValidPasteSite(newPasteSite)) {
                event.getChannel().sendMessage("Invalid paste site! Valid options are: mclogs, gnomebot, pastesdev, securelogger, mmd").queue();
                return;
            }
            
            // Update the paste site in UserDB
            UserDB.setPasteSite(userid, newPasteSite);
            
            // Confirm the change
            event.getChannel().sendMessage("✅ Paste site changed to `" + newPasteSite + "`\n" +
                    "Your long messages will now use " + getSiteName(newPasteSite) + " for pasting!").queue();
        }
    }
    
    private static boolean isValidPasteSite(String site) {
        return "mclogs".equals(site) || 
               "pastesdev".equals(site) || 
               "gnomebot".equals(site) || 
               "securelogger".equals(site) || 
               "mmd".equals(site);
    }
    
    private static String getSiteName(String site) {
        switch (site) {
            case "mclogs": return "MCLogs";
            case "gnomebot": return "gnomebot.dev";
            case "pastesdev": return "Pastes.dev";
            case "securelogger": return "SecureLogger";
            case "mmd": return "MMD Paste";
            default: return site;
        }
    }
}