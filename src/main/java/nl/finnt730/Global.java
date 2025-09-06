package nl.finnt730;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

import com.jsonstructure.JsonStructure;

import haxe.root.JsonStructureLib;
import net.dv8tion.jda.api.entities.Role;

public final class Global {
    private static final String COMMANDS_LOCATION = "commands/%s.json";
    public static final String NAME_KEY = "name";
    public static final String DESC_KEY = "description";
    public static final String DATA_KEY = "data";
    public static final String ALIAS_KEY = "aliases";
    public static final String OPTION_KEY = "options";
    public static final String MANAGER_ROLE = "Bot Manager";

    private Global() {}

    public static final JsonStructure COMMAND_STRUCTURE = JsonStructureLib.createStructure()
            .addStringField(NAME_KEY)
            .addOptionalStringField(DESC_KEY)
            .addStringField(DATA_KEY)
            .addOptionalArrayField(ALIAS_KEY, "string", JsonStructureLib.createStructure().addStringField("alias"))
            .addOptionalArrayField(OPTION_KEY, "object", JsonStructureLib.createStructure());

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

    public static String commandOf(String cmdName) {
        return String.format(COMMANDS_LOCATION, cmdName);
    }
    public static boolean isManager(Role role) {return role.getName().equals(MANAGER_ROLE);}
    
    public static byte[] compressGZIP(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(data);
        } // <--- closing ensures trailer is written
        return baos.toByteArray();
    }
    
    
}
