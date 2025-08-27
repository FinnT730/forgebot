package nl.finnt730;

import com.jsonstructure.JsonStructure;
import haxe.root.JsonStructureLib;

public final class Global {
    private Global() {}

    public static final JsonStructure COMMAND_STRUCTURE = JsonStructureLib.createStructure()
            .addStringField("name")
            .addOptionalStringField("description")
            .addStringField("data")
            .addOptionalArrayField("aliases", "string", JsonStructureLib.createStructure().addStringField("alias"))
            .addOptionalArrayField("options", "object", JsonStructureLib.createStructure());

}
