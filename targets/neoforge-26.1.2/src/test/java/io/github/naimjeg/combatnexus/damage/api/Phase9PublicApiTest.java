package io.github.naimjeg.combatnexus.damage.api;

import com.google.gson.JsonPrimitive;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.combatnexus.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.damage.api.rule.*;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Phase9PublicApiTest {
    @Test
    void publicIdsHaveCanonicalNames() {
        assertEquals("combatnexus:damage_type_tag",
                DamageModuleConditionIds.DAMAGE_TYPE_TAG.toString());
        assertEquals("combatnexus:source_tag",
                DamageModuleConditionIds.SOURCE_TAG.toString());
        assertEquals("combatnexus:add_base_damage",
                DamageModuleOperationIds.ADD_BASE_DAMAGE.toString());
        assertEquals("combatnexus:crit_damage",
                DamageModulePreMultiplierBuckets.CRIT_DAMAGE.toString());
        assertEquals("combatnexus:crit_chance",
                DamageModuleAttributes.CRIT_CHANCE.identifier().toString());
        assertNull(findPhysicalDamageAdditive());
    }

    @Test
    void applicationBucketUsesStableNamesAndReportsUnknownValues() {
        for (DamageApplicationBucket bucket : DamageApplicationBucket.values()) {
            JsonPrimitive encoded = DamageApplicationBucket.CODEC
                    .encodeStart(JsonOps.INSTANCE, bucket)
                    .getOrThrow().getAsJsonPrimitive();
            assertEquals(bucket.serializedName(), encoded.getAsString());
            assertEquals(bucket, DamageApplicationBucket.CODEC
                    .parse(JsonOps.INSTANCE, encoded).getOrThrow());
        }
        assertTrue(DamageApplicationBucket.CODEC.parse(
                JsonOps.INSTANCE, new JsonPrimitive("future_bucket"))
                .error().isPresent());
    }

    @Test
    void externalNamespacesAreNeverRewrittenByPublicFactories() {
        Identifier external = Identifier.fromNamespaceAndPath("contentmod", "example");
        assertEquals(external, DamageModuleConditions.sourceTag(external)
                .type().equals(DamageModuleConditionIds.SOURCE_TAG) ? external : null);
        assertEquals(external, io.github.naimjeg.combatnexus.damage.api.damage
                .DamageMetadataKey.stringKey(external).id());
    }

    private static Object findPhysicalDamageAdditive() {
        try {
            return DamageModuleAttributes.class.getField("PHYSICAL_DAMAGE_ADDITIVE");
        } catch (NoSuchFieldException expected) {
            return null;
        }
    }

}
