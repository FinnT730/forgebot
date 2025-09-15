package nl.finnt730.commands.builtin;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import nl.finnt730.DatabaseManager;
import nl.finnt730.commands.Command;
import nl.finnt730.commands.CommandCache;

public final class FindCommand extends Command {
    private static final String USAGE_HINT = "Usage: find <target> <pageNum>";
    private static final int PAGE_SIZE = 7;
    @Override
    public void handle(MessageReceivedEvent event, Member invoker, String commandContents) {
        if (commandContents.isEmpty()) {
            event.getChannel().sendMessage(USAGE_HINT).queue();
            return;
        }
        int pageNum = 0;
        String[] split = commandContents.split(" ", 2);
        if (split.length > 1) {
            if (split[1].trim().matches("[0-9]+")) {
                pageNum = Integer.parseInt(split[1].trim());
            }
        }
        var keys = CommandCache.getAllLoadedNames();
        var result = keys.stream().filter((str) -> str.matches(split[0]) || str.contains(split[0])).sorted().toList();
        int startIndex = pageNum * PAGE_SIZE;
        StringBuilder builder = new StringBuilder();
        builder.append("Found %d results, showing %d-%d\n".formatted(result.size(), startIndex, startIndex + PAGE_SIZE));
        for (int i = startIndex; (i < startIndex + PAGE_SIZE) && i < result.size(); i++) {
            builder.append(result.get(i)).append("\n");
        }
        event.getChannel().sendMessage(builder.toString()).queue();
    }
}
