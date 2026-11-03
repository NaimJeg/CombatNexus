package io.github.naimjeg.combatnexus.damage.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DamageTypeTagContractTest {
    @Test
    void damageTypeTagsExposeOnlyCanonicalNames() throws Exception {
        assertFalse(Files.exists(Path.of(
                "src/main/resources/data/combatnexus/tags/damage_type/spear_charge.json"
        )));
        JsonArray canonical = values("is_spear_charge");
        assertFalse(canonical.toString().contains("combatnexus:spear_charge"));
        assertEquals("combatnexus:is_mace_smash",
                DamageModuleTags.DamageTypes.IS_MACE_SMASH.location().toString());
    }

    @Test
    void poisonAndMagicTagsDelegateToNeoForgeConventions() throws Exception {
        assertEquals("[\"#neoforge:is_poison\"]", values("is_poison").toString());
        assertEquals("[\"#neoforge:is_magic\"]", values("is_magic").toString());
    }

    private static JsonArray values(String name) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/combatnexus/tags/damage_type/"
                        + name + ".json"))).getAsJsonObject().getAsJsonArray("values");
    }
}
