package nl.finnt730;

import com.jsonstructure.JsonStructure;
import haxe.root.JsonStructureLib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

public final class Global {
    private Global() {}

    public static final JsonStructure COMMAND_STRUCTURE = JsonStructureLib.createStructure()
            .addStringField("name")
            .addOptionalStringField("description")
            .addStringField("data")
            .addOptionalArrayField("aliases", "string", JsonStructureLib.createStructure().addStringField("alias"))
            .addOptionalArrayField("options", "object", JsonStructureLib.createStructure());

    /**
     * Parse a string that may contain quoted arguments, handling spaces within quotes properly
     * @param input The input string to parse
     * @return Array of parsed arguments
     */
    public static String[] parseQuotedString(String input) {
        var result = new ArrayList<String>();
        StringBuilder currentArg = new StringBuilder();
        boolean inQuotes = false;
        boolean escapeNext = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (escapeNext) {
                currentArg.append(c);
                escapeNext = false;
                continue;
            }

            if (c == '\\') {
                escapeNext = true;
                continue;
            }

            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            if (c == ' ' && !inQuotes) {
                if (!currentArg.isEmpty()) {
                    result.add(currentArg.toString().trim());
                    currentArg.setLength(0);
                }
            } else {
                currentArg.append(c);
            }
        }

        // Add the last argument if there is one
        if (!currentArg.isEmpty()) {
            result.add(currentArg.toString().trim());
        }

        return result.toArray(new String[0]);
    }
    
    public static byte[] compressGZIP(byte[] data) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(buffer)) {
            gzip.write(data);
        }
        return buffer.toByteArray();
    }
}
