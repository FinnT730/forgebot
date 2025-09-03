package nl.finnt730;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class PasteCommand extends ListenerAdapter {

    private static final String MCLO_API_URL = "https://api.mclo.gs/1/log";
    private static final String NOTEBOOK_EMOJI = "📓";
    private static final Emoji NOTEBOOK_EMOJI_OBJ = Emoji.fromUnicode(NOTEBOOK_EMOJI);
    private static final int MAX_MESSAGE_LENGTH = 700;
    private static final List<String> BLACKLISTED_EXTENSIONS = List.of("zip", "gz", "7z"); // if there are others add them, idk why discord doesn't just have isText

    private static final String ERROR_RESPONSE_FORMAT = "Unable to create paste for %s\n";
    private static final String PASTE_RESPONSE_FORMAT = "%s: [Paste](<%s>) | [Raw](<%s>)\n";

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
            if (attachments.stream().anyMatch(PasteCommand::isPasteable)) {
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
                                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                                connection.setRequestMethod("GET");

                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                                    StringBuilder content = new StringBuilder(10_000);
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        content.append(line).append("\n");
                                    }
                                    // Create paste with attachment content
                                    attachmentContent.add(content.toString());
                                    fileNames.add(attachment.getFileName());

                                }
                            } catch (Exception e) {
                                event.getChannel().sendMessage("❌ Error reading attachment: " + e.getMessage()).queue();
                            }
                        }
                    }
                    createPaste(attachmentContent, event.getChannel(), message, fileNames);
                }
            });
        }
    }

    private static void createPaste(List<String> contentList, MessageChannel channel, Message message, List<String> fileName) {
        CompletableFuture.runAsync(() -> {
            StringBuilder pasteResponse = new StringBuilder();
            for (int i = 0; i < contentList.size(); i++) {
                try {
                    String rawResponse = tryGetResponse(contentList.get(i));
                    String pasteLinks = tryFormatLinks(rawResponse, fileName.get(i));
                    pasteResponse.append(pasteLinks);
                } catch (IOException e) {
                    pasteResponse.append(String.format(ERROR_RESPONSE_FORMAT, fileName.get(i)));
                }
            }
            channel.sendMessage(pasteResponse.toString()).queue();
        });
    }
    private static String tryFormatLinks(String rawContent, String fileName) throws IOException {
        if (rawContent.contains("\"success\":true")) {
            // Extract the URLs from the response
            String pasteUrl = extractUrlFromResponse(rawContent);
            String rawUrl = extractRawUrlFromResponse(rawContent);
            if (pasteUrl != null && rawUrl != null) {
                return PASTE_RESPONSE_FORMAT.formatted(
                        fileName, pasteUrl.replace("\\", ""),
                        rawUrl.replace("\\", ""));
            } else {
                throw new IOException(); // me when i do exception based control flow
            }
        } else {
            throw new IOException();
        }
    }
    private static String tryGetResponse(String content) throws IOException {
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
        return response.toString();
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

    private static boolean isPasteable(Message.Attachment attachment) {
        if (attachment.isVideo() || attachment.isImage()) return false;
        return !BLACKLISTED_EXTENSIONS.contains(attachment.getFileExtension());
    }
}
