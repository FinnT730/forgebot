package nl.finnt730;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class EchoCommand extends Command {
    private final String echo;
    public EchoCommand(String echo) { this.echo = echo;}

    @Override
    public void handle(MessageReceivedEvent event, String invoker, String commandContents) {
        event.getChannel().sendMessage(echo).queue();
    }
}
