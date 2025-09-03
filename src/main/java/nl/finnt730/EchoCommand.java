package nl.finnt730;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.List;
import java.util.Locale;

public class EchoCommand extends Command {
    private final String echo;
    private static int TRIAGE_AUTHORITY_LEVEL = -1;
    public EchoCommand(String echo) { this.echo = echo;}

    @Override
    public void handle(MessageReceivedEvent event, Member invoker, String commandContents) {
        var result = event.getChannel().sendMessage(echo);
        var target = event.getMessage().getReferencedMessage();
        if (TRIAGE_AUTHORITY_LEVEL == -1) {
            var guildRoles = event.getGuild().getRoles();
            for (Role guildRole : guildRoles) {
                if (guildRole.getName().toLowerCase(Locale.ROOT).startsWith("triage")) {
                    TRIAGE_AUTHORITY_LEVEL = guildRole.getPosition();
                    break;
                }
            }
        }
        // if is replying to a TARGET MESSAGE
        if (target != null  && target.getMember() != null) {
            int targetAuthority = highest(target.getMember().getRoles());
            if (targetAuthority >= TRIAGE_AUTHORITY_LEVEL) {
                result.queue(); // Don't reply to triage or higher.
            } else { // Always reply and ping those who aren't at least triage.
                result.setMessageReference(target).mentionRepliedUser(true).queue();
            }
        } else {
            result.queue();
        }
    }

    private int highest(List<Role> roles) {
        int i = -1;
        for (Role role : roles) {
            i = Math.max(role.getPosition(), i);
        }
        return i;
    }
}
