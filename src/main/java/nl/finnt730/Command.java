package nl.finnt730;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command {
    public Command() {}

    /**
     * Handles whatever action a command should perform
     * @param invoker username of the person who invoked the command
     * @param commandContents contents of invoking message AFTER the command name. "!java boots" will have the contents of "boots"
     */
    public abstract void handle(MessageReceivedEvent event, Member invoker, String commandContents);

    /**
     * By default anyone can invoke a command. Overrides should restrict as needed.
     */
    public boolean canInvoke(Guild guild, Member invoker) {
        return true;
    }
}
