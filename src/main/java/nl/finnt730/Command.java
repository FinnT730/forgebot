package nl.finnt730;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class Command {
    public Command() {}
    public abstract void handle(MessageReceivedEvent event, String invoker, String commandContents);
}
