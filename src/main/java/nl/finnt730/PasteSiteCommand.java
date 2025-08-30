package nl.finnt730;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import nl.finnt730.paste.PasteSite;

public final class PasteSiteCommand extends ReservedCommand {

	@Override
	public void handle(MessageReceivedEvent event, String invoker, String commandContents) {
		// TODO Auto-generated method stub
        String userid = event.getMessage().getAuthor().getId();
        String currentPrefix = CommandCache.DEFAULT_PREFIX;
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