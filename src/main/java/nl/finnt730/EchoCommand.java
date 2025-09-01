package nl.finnt730;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EchoCommand extends Command {
    private final String echo;
    public EchoCommand(String echo) { this.echo = echo;}

    @Override
    public void handle(MessageReceivedEvent event, Member invoker, String commandContents) {
        var result = event.getChannel().sendMessage(echo);
        var target = event.getMessage().getReferencedMessage();
        // if is replying to a TARGET MESSAGE
        if (target != null  && target.getMember() != null) {
            var targetRoles = target.getMember().getRoles();
            // This is not elegant but it does the job.
            boolean targetIsHoisted = targetRoles.stream().anyMatch(Role::isHoisted);
            boolean invokerIsHoisted = invoker.getRoles().stream().anyMatch(Role::isHoisted);
            boolean invokerIsManager = invoker.getRoles().stream().anyMatch(Global::isManager);
            boolean targetIsManager = targetRoles.stream().anyMatch(Global::isManager);
            // Logic:
            /*
            * 1. Both are hoisted = Don't even reply (e.g. Admin to Admin)
            * 2. Target is hoisted, Invoker is not hoisted = Don't even reply (e.g. BillyBob to Admin)
            * 3. Target is not hoisted, Invoker is hoisted = Reply ping unless target is Bot Manager (e.g. Admin to BillyBob)
            * 4. Both are not hoisted = Reply ping if invoker has manager (e.g. BillyBob to BillyBob)
            * */
            if (targetIsHoisted) {
                result.queue(); // Case 1 & 2
            } else {
                result.setMessageReference(target).mentionRepliedUser(
                        (invokerIsHoisted && !targetIsManager) || // Case 3
                        (invokerIsManager && !invokerIsHoisted) // Case 4
                ).queue();
            }
        } else {
            result.queue();
        }
    }
}
