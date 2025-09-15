package nl.finnt730.listeners;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import nl.finnt730.paste.PasteSite;

public final class PasteListener extends ListenerAdapter {

    private static final String NOTEBOOK_EMOJI = "📓";
    private static final Emoji NOTEBOOK_EMOJI_OBJ = Emoji.fromUnicode(NOTEBOOK_EMOJI);
    private static final int MAX_MESSAGE_LENGTH = 700;
    private static final List<String> DENYLISTED_EXTENSIONS = List.of("zip", "gz", "7z");//TODO allow GZips to be uploaded to sites supporting them and to allow curseforge log zips to be read

    private static final String ERROR_RESPONSE_FORMAT = "❌ Unable to create paste for %s\n";
    private static final String LOG_EMOJI = "📄";

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // Ignore messages from other bots to prevent recursive reactions
        if (event.getAuthor().isBot()) {
            return;
        }

        Message message = event.getMessage();
        String content = message.getContentRaw();
        var attachments = message.getAttachments();
      
        if (content.length() > MAX_MESSAGE_LENGTH || !attachments.isEmpty()) {
            // Add notebook emoji to the message
            if (attachments.stream().anyMatch(PasteListener::isPasteable)) {
                message.addReaction(NOTEBOOK_EMOJI_OBJ).queue();
            }
        }
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        // Ignore reactions from other bots to prevent recursive paste creations
        if (event.getUser().isBot()) {
            return;
        }

        // Check if the reaction is our notebook emoji
        if (event.getReaction().getEmoji().getName().equals(NOTEBOOK_EMOJI)) {
            // Get the message that was reacted to
            event.getChannel().retrieveMessageById(event.getMessageId()).queue(message -> {
                // Check if the message has already been processed
                if (message.getReactions().stream().anyMatch(MessageReaction::isSelf)) {
                    message.clearReactions().queue();
                    var attachments = message.getAttachments();
                    List<String> attachmentContent = new ArrayList<>();
                    List<String> fileNames = new ArrayList<>();
                    
                    for (Message.Attachment attachment : attachments) {
                        if (isPasteable(attachment)) {
                            try {
                                URL url = new URL(attachment.getUrl());
                                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("GET");

                                try (BufferedReader reader = new BufferedReader(
                                        new InputStreamReader(connection.getInputStream()))) {
                                    StringBuilder content = new StringBuilder();
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        content.append(line).append("\n");
                                    }
                                    attachmentContent.add(content.toString());
                                    fileNames.add(attachment.getFileName());
                                }
                            } catch (Exception e) {
                                event.getChannel().sendMessage("❌ Error reading attachment: " + e.getMessage()).queue();
                            }
                        }
                    }
                    createPaste(attachmentContent, event.getChannel(), fileNames, event.getUserId());
                }
            });
        }
    }

    private static void createPaste(List<String> contentList, MessageChannel channel, 
                                   List<String> fileNames, String userId) {
        CompletableFuture.runAsync(() -> {
            StringBuilder pasteResponse = new StringBuilder();
            boolean firstLog = true;
            
            for (int i = 0; i < contentList.size(); i++) {
                String content = contentList.get(i);
                String fileName = fileNames.get(i);
                
                try {
                    // Extract log name (filename without extension)
                    String logName = Optional.ofNullable(fileName)
                            .map(f -> f.lastIndexOf('.') > 0 ? f.substring(0, f.lastIndexOf('.')) : f)
                            .orElse("log");
                    
                    PasteSite pasteSite = PasteSite.get(userId, content);
                    String pasteUrl = pasteSite.getResultURL(content);
                    
                    if (pasteUrl != null && !pasteUrl.isEmpty()) {
                        String formattedLink = String.format("[%s](<%s>)", logName, pasteUrl);
                        
                        if (firstLog) {
                            pasteResponse.append(LOG_EMOJI).append(" ").append(formattedLink);
                            firstLog = false;
                        } else {
                            pasteResponse.append(" | ").append(formattedLink);
                        }
                    } else {
                        if (!firstLog) pasteResponse.append(" | ");
                        pasteResponse.append(String.format(ERROR_RESPONSE_FORMAT, fileName));
                        firstLog = false;
                    }
                } catch (Exception e) {
                    if (!firstLog) pasteResponse.append(" | ");
                    pasteResponse.append(String.format(ERROR_RESPONSE_FORMAT, fileName));
                    firstLog = false;
                }
            }
            
            channel.sendMessage(pasteResponse.toString()).queue();
        });
    }

    private static boolean isPasteable(Message.Attachment attachment) {
        if (attachment.isVideo() || attachment.isImage()) return false;
        return !DENYLISTED_EXTENSIONS.contains(attachment.getFileExtension());
    }
}