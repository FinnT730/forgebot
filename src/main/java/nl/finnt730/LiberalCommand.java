package nl.finnt730;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

public abstract class LiberalCommand extends Command {
    public LiberalCommand() {
    }

    @Override
    public final boolean canInvoke(Guild guild, Member invoker) {
        return true;
    }
}
