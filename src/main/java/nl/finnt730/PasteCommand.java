package nl.finnt730;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import nl.finnt730.paste.PasteSite;

public final class PasteCommand extends ListenerAdapter {

    private static final String NOTEBOOK_EMOJI = "📓";
    private static final Emoji NOTEBOOK_EMOJI_OBJ = Emoji.fromUnicode(NOTEBOOK_EMOJI);
    private static final int MAX_MESSAGE_LENGTH = 700;

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
            if (attachments.stream().noneMatch(it -> it.isImage() || it.isVideo())) {
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
                    
                    // Check if there are non-image/video attachments
                    boolean hasNonMediaAttachments = attachments.stream()
                            .anyMatch(att -> !att.isImage() && !att.isVideo());
                    
                    if (hasNonMediaAttachments) {
                        // Process all non-image/video attachments
                        List<Message.Attachment> nonMediaAttachments = new ArrayList<>();
                        for (Message.Attachment attachment : attachments) {
                            if (!attachment.isImage() && !attachment.isVideo()) {
                                nonMediaAttachments.add(attachment);
                            }
                        }
                        
                        // Create pastes for all non-media attachments
                        createPastesForAttachments(nonMediaAttachments, event.getChannel(), message, event.getUser().getId());
                    } else {
                        // Use message content if no non-media attachments
                        String content = message.getContentRaw();
                        createPaste(content, event.getChannel(), message, event.getUser().getId(), "message");
                    }
                }
            });
        }
    }

    private static void createPastesForAttachments(List<Message.Attachment> attachments, 
                                                 MessageChannel channel, 
                                                 Message message, 
                                                 String userId) {
        CompletableFuture.runAsync(() -> {
            try {
                List<String> pasteLinks = new ArrayList<>();
                StringBuilder errorMessages = new StringBuilder();
                
                for (Message.Attachment attachment : attachments) {
                    try {
                        URL url = new URL(attachment.getUrl());
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        connection.setConnectTimeout(10000);
                        connection.setReadTimeout(30000);

                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(connection.getInputStream()))) {
                            
                            StringBuilder content = new StringBuilder(10_000);
                            String line;
                            while ((line = reader.readLine()) != null) {
                                content.append(line).append("\n");
                            }
                            
                            // Create paste with attachment content
                            String pasteUrl = createPasteForContent(content.toString(), userId, attachment.getFileName());
                            
                            if (pasteUrl != null && !pasteUrl.isEmpty()) {
                                // Use the attachment name as the label (without extension)
                                String label = attachment.getFileName().replaceFirst("[.][^.]+$", "");
                                pasteLinks.add("📝 [" + label + "](<" + pasteUrl + ">)");
                            } else {
                                errorMessages.append("❌ Failed to create paste for ").append(attachment.getFileName()).append("\n");
                            }
                        }
                    } catch (Exception e) {
                        errorMessages.append("❌ Error reading ").append(attachment.getFileName())
                                .append(": ").append(e.getMessage()).append("\n");
                    }
                }
                
                // Send the results
                if (!pasteLinks.isEmpty()) {
                    // Changed from newline to | separator
                    String response = String.join(" | ", pasteLinks);
                    if (errorMessages.length() > 0) {
                        response += "\n" + errorMessages.toString().trim();
                    }
                    
                    channel.sendMessage(response)
                            .setMessageReference(message)
                            .mentionRepliedUser(false)
                            .queue();
                } else if (errorMessages.length() > 0) {
                    channel.sendMessage(errorMessages.toString().trim())
                            .setMessageReference(message)
                            .mentionRepliedUser(false)
                            .queue();
                }
                
            } catch (Exception e) {
                channel.sendMessage("❌ Error processing attachments: " + e.getMessage())
                        .setMessageReference(message)
                        .mentionRepliedUser(false)
                        .queue();
                e.printStackTrace();
            }
        });
    }

    private static String createPasteForContent(String content, String userId, String attachmentName) {
        try {
            // Get the appropriate paste site for the REACTING USER and content
            PasteSite pasteSite = PasteSite.get(userId, content);
            return pasteSite.getResultURL(content);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void createPaste(String content, MessageChannel channel, Message message, 
                                   String userId, String attachmentName) {
        CompletableFuture.runAsync(() -> {
            try {
                // Get the appropriate paste site for the REACTING USER and content
                PasteSite pasteSite = PasteSite.get(userId, content);
                String pasteUrl = pasteSite.getResultURL(content);
                
                if (pasteUrl == null || pasteUrl.isEmpty()) {
                    channel.sendMessage("❌ Failed to create paste: Content too large for any service.")
                            .setMessageReference(message)
                            .mentionRepliedUser(false)
                            .queue();
                    return;
                }
                
                // Format the message based on whether we have an attachment name
                String formattedMessage;
                if ("message".equals(attachmentName)) {
                    formattedMessage = "📝 [Long message](<" + pasteUrl + ">)";
                } else {
                    // Use the attachment name as the label
                    String label = attachmentName.replaceFirst("[.][^.]+$", ""); // Remove extension
                    formattedMessage = "📝 [" + label + "](<" + pasteUrl + ">)";
                }
                
                channel.sendMessage(formattedMessage)
                        .setMessageReference(message)
                        .mentionRepliedUser(false)
                        .queue();
                        
            } catch (Exception e) {
                channel.sendMessage("❌ Error creating paste: " + e.getMessage())
                        .setMessageReference(message)
                        .mentionRepliedUser(false)
                        .queue();
                e.printStackTrace();
            }
        });
    }
}