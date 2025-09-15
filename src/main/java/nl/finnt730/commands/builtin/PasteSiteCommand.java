package nl.finnt730.commands.builtin;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import nl.finnt730.UserDB;
import nl.finnt730.commands.Command;
import nl.finnt730.commands.CommandCache;
import nl.finnt730.paste.PasteSite;

public final class PasteSiteCommand extends Command {


	@Override
	public void handle(MessageReceivedEvent event, Member invoker, String commandContents) {
        String userid = event.getMessage().getAuthor().getId();
        String currentPrefix = CommandCache.DEFAULT_PREFIX;
        String rawMessage = event.getMessage().getContentRaw();
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