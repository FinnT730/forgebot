package nl.finnt730;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class PasteCommand extends ListenerAdapter {

    private static final String MCLO_API_URL = "https://api.mclo.gs/1/log";
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
            if (attachments.stream().noneMatch(Message.Attachment::isImage)) {
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
                    if (!attachments.isEmpty() && attachments.stream().noneMatch(it -> it.isImage() || it.isVideo())) {
                        try {
                            URL url = new URL(attachments.getFirst().getUrl());
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");

                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                                StringBuilder content = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    content.append(line).append("\n");
                                }
                                // Create paste with attachment content
                                createPaste(content.toString(), event.getChannel(), event.getUser());
                            }
                        } catch (Exception e) {
                            event.getChannel().sendMessage("❌ Error reading attachment: " + e.getMessage()).queue();
                        }
                    } else {
                        // Use message content if no attachments
                        String content = message.getContentRaw();
                        createPaste(content, event.getChannel(), event.getUser());
                    }
                }
            });
        }
    }

    private void createPaste(String content, net.dv8tion.jda.api.entities.channel.middleman.MessageChannel channel, User user) {
        CompletableFuture.runAsync(() -> {
            try {
                // Prepare the POST data
                String postData = "content=" + URLEncoder.encode(content, StandardCharsets.UTF_8);
                byte[] postDataBytes = postData.getBytes(StandardCharsets.UTF_8);

                // Create connection
                URL url = new URL(MCLO_API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                connection.setDoOutput(true);

                // Send the data
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(postDataBytes);
                }

                // Read the response
                StringBuilder response = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                }

                                 // Parse the response (simple JSON parsing for the URL)
                 String responseStr = response.toString();
                 if (responseStr.contains("\"success\":true")) {
                     // Extract the URLs from the response
                     String pasteUrl = extractUrlFromResponse(responseStr);
                     String rawUrl = extractRawUrlFromResponse(responseStr);
                     if (pasteUrl != null && rawUrl != null) {
                         String sentContent = String.format("[PASTE URL](<%s>) | [RAW URL](<%s>)",
                                 pasteUrl.replace("\\", ""), rawUrl.replace("\\", ""));
                         channel.sendMessage(sentContent).queue();
                     } else {
                         channel.sendMessage("❌ Failed to create paste: Could not extract URLs from response.").queue();
                     }
                 } else {
                     channel.sendMessage("❌ Failed to create paste: " + responseStr).queue();
                 }

            } catch (Exception e) {
                channel.sendMessage("❌ Error creating paste: " + e.getMessage()).queue();
                e.printStackTrace();
            }
        });
    }

     private static String extractUrlFromResponse(String response) {
         // Simple JSON parsing to extract the URL
         // Looking for "url":"https://mclo.gs/..."
         int urlIndex = response.indexOf("\"url\":\"");
         if (urlIndex != -1) {
             urlIndex += 7; // Skip "url":"
             int endIndex = response.indexOf("\"", urlIndex);
             if (endIndex != -1) {
                 return response.substring(urlIndex, endIndex);
             }
         }
         return null;
     }

     private static String extractRawUrlFromResponse(String response) {
         // Simple JSON parsing to extract the raw URL
         // Looking for "raw":"https://api.mclo.gs/1/raw/..."
         int rawIndex = response.indexOf("\"raw\":\"");
         if (rawIndex != -1) {
             rawIndex += 7; // Skip "raw":"
             int endIndex = response.indexOf("\"", rawIndex);
             if (endIndex != -1) {
                 return response.substring(rawIndex, endIndex);
             }
         }
         return null;
     }
}
