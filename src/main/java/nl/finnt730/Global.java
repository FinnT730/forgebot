package nl.finnt730;

import com.jsonstructure.JsonStructure;
import haxe.root.JsonStructureLib;

public final class Global {

    public static JsonStructure commandStructure = JsonStructureLib.createStructure()
            .addStringField("name")
            .addOptionalStringField("description")
            .addStringField("data")
            .addOptionalArrayField("aliases", "string", JsonStructureLib.createStructure().addStringField("alias"))
            .addOptionalArrayField("options", "object", JsonStructureLib.createStructure());

}
