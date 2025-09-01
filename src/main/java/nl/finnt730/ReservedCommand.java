package nl.finnt730;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.function.Consumer;

public abstract class ReservedCommand extends Command {
    public ReservedCommand() {
    }

    @Override
    public final boolean canInvoke(Guild guild, Member invoker) {
        return invoker.getRoles().stream().anyMatch((role) -> role.isHoisted() || Global.isManager(role));
    }
}
