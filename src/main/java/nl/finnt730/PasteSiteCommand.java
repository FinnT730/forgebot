package nl.finnt730;

import nl.finnt730.paste.PasteSite;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public final class PasteSiteCommand extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String userid = event.getMessage().getAuthor().getId();
        String currentPrefix = "!"; // In real implementation: get from UserDB
        String rawMessage = event.getMessage().getContentRaw();
        
        boolean startsWithCurrentPrefix = rawMessage.startsWith(currentPrefix + "pastesite");
        
        if (startsWithCurrentPrefix) {
            String[] parts = rawMessage.split(" ", 2);
                        
            if (parts.length < 2) {
                event.getChannel().sendMessage("Usage: " + currentPrefix + "pastesite <site>\n" +
                        "Available sites: " + PasteSite.getAvailableSites()).queue();
                return;
            }
            
            String newPasteSite = parts[1].trim().toLowerCase();
            
            if (!PasteSite.isValidSiteId(newPasteSite)) {
                event.getChannel().sendMessage("Invalid paste site! Valid options are:\n" +
                        PasteSite.getAvailableSites()).queue();
                return;
            }
            
            UserDB.setPasteSite(userid, newPasteSite);
            String siteName = PasteSite.getPure(newPasteSite).getId();
            
            event.getChannel().sendMessage("✅ Paste site changed to `" + newPasteSite + "`\n" +
                    "Your long messages will now use " + siteName + " for pasting!").queue();
        }
    }
}