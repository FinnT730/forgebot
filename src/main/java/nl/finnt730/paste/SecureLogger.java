package nl.finnt730.paste;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import nl.finnt730.Global;

/**
 * GZipped Raw paste site. Used by TLauncher and CrashDetector
 */
public final class SecureLogger implements PasteSite {

    private static final List<String> API_ENDPOINTS = List.of("https://securelogger.net/save/log?", "https://securelogger.top/save/log?");
    private static final int MAX_GZIPPED_SIZE_BYTES = 11 * 1024 * 1024; // 11MB for gzipped content
    private static final int MAX_LINES = 25000;
    private static final String CLIENT_TYPE = "USER_CODE";
    private static final String API_VERSION = "2.923";

    @Override
    public boolean largeEnough(String content) {
        // First check basic size limits before attempting compression
        if (content.getBytes(StandardCharsets.UTF_8).length > 100 * 1024 * 1024) { // 100MB raw
            return false; // Way too big, no need to compress
        }
        
        // Check line count
        int lines = 1;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                lines++;
                if (lines > MAX_LINES) {
                    return false;
                }
            }
        }
        
        // Try to estimate compressed size without full compression
        try {
            byte[] contentBytes = content.getBytes("cp1251");
            byte[] compressed = Global.compressGZIP(contentBytes);
            return compressed.length <= MAX_GZIPPED_SIZE_BYTES;
        } catch (Exception e) {
            // If compression fails, assume it's too large
            return false;
        }
    }

    @Override
    public boolean supportsGZip() {
        return true; // SecureLogger requires gzip compression
    }

    @Override
    public String getResultURL(String content) {
        try {
            // First verify content is within limits
            if (!largeEnough(content)) {
                return null;
            }
            
            // Build the API URL with parameters
            String params = "version=" + URLEncoder.encode(API_VERSION, StandardCharsets.UTF_8.name()) +
                    "&clientType=" + URLEncoder.encode(CLIENT_TYPE, StandardCharsets.UTF_8.name());
            
            // Try each endpoint until one succeeds
            for (String endpoint : API_ENDPOINTS) {
                try {
                    URL url = new URL(endpoint + params);
                    String response = sendPostRequest(url, content);
                    
                    // Extract the link from the response
                    String link = extractLink(response);
                    if (link != null && !link.isEmpty()) {
                        return link;
                    }
                } catch (Exception e) {
                    // Try the next endpoint if this one fails
                    continue;
                }
            }
            
            return null; // All endpoints failed
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String sendPostRequest(URL url, String content) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);  // 30 seconds
        connection.setReadTimeout(60000);     // 60 seconds
        connection.setRequestProperty("Content-Type", "text/plain; charset=utf-8");
        connection.setRequestProperty("Content-Encoding", "gzip");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] contentBytes = content.getBytes("cp1251");
            byte[] compressedBytes = Global.compressGZIP(contentBytes);
            os.write(compressedBytes);
        }

        try (InputStream is = connection.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    /**
     * Extracts the "link" value from a JSON string.
     *
     * @param jsonString The JSON string containing the "link" field.
     * @return The value of the "link" field, or null if not found.
     */
    public static String extractLink(String jsonString) {
        // Define the key to search for
        String key = "\"link\"";

        // Find the index of the key in the JSON string
        int keyIndex = jsonString.indexOf(key);
        if (keyIndex == -1) {
            // Key not found
            return null;
        }

        // Find the start of the value (after the colon and optional whitespace)
        int valueStart = jsonString.indexOf(':', keyIndex) + 1;
        if (valueStart == 0) {
            // Invalid JSON format
            return null;
        }

        // Trim leading whitespace
        while (valueStart < jsonString.length() && Character.isWhitespace(jsonString.charAt(valueStart))) {
            valueStart++;
        }

        // Check if the value is enclosed in quotes
        if (valueStart >= jsonString.length() || jsonString.charAt(valueStart) != '"') {
            // Value is not a string (unsupported format)
            return null;
        }

        // Find the end of the value (closing quote)
        int valueEnd = jsonString.indexOf('"', valueStart + 1);
        if (valueEnd == -1) {
            // Missing closing quote
            return null;
        }

        // Extract and return the link
        return jsonString.substring(valueStart + 1, valueEnd);
    }
}