package io.github.naimjeg.combatnexus.damage.core.pipeline;

import io.github.naimjeg.combatnexus.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.damage.api.DamageModulePreMultiplierBuckets;
import io.github.naimjeg.combatnexus.damage.registry.ModAttributes;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DamageAttributeMappingsTest {
    @Test
    void sevenChannelMappingsAreExactUniqueAndImmutable() {
        List<DamageAttributeMappings.ChannelMapping> mappings =
                DamageAttributeMappings.channelMappings();
        assertEquals(7, mappings.size());
        assertMapping(DamageChannel.FIRE_ID, "fire_damage_additive",
                DamageModulePreMultiplierBuckets.FIRE_DAMAGE);
        assertMapping(DamageChannel.COLD_ID, "cold_damage_additive",
                DamageModulePreMultiplierBuckets.COLD_DAMAGE);
        assertMapping(DamageChannel.LIGHTNING_ID,
                "lightning_damage_additive",
                DamageModulePreMultiplierBuckets.LIGHTNING_DAMAGE);
        assertMapping(DamageChannel.MAGIC_ID, "magic_damage_additive",
                DamageModulePreMultiplierBuckets.MAGIC_DAMAGE);
        assertMapping(DamageChannel.WITHER_ID, "wither_damage_additive",
                DamageModulePreMultiplierBuckets.WITHER_DAMAGE);
        assertMapping(DamageChannel.POISON_ID, "poison_damage_additive",
                DamageModulePreMultiplierBuckets.POISON_DAMAGE);
        assertMapping(DamageChannel.KINETIC_ID, "kinetic_damage_additive",
                DamageModulePreMultiplierBuckets.KINETIC_DAMAGE);

        assertEquals(7, new HashSet<>(mappings.stream()
                .map(DamageAttributeMappings.ChannelMapping::channelId).toList()).size());
        assertEquals(7, new HashSet<>(mappings.stream()
                .map(DamageAttributeMappings.ChannelMapping::attributeId).toList()).size());
        assertEquals(7, new HashSet<>(mappings.stream()
                .map(DamageAttributeMappings.ChannelMapping::bucketId).toList()).size());
        assertThrows(UnsupportedOperationException.class, mappings::clear);
    }

    @Test
    void physicalUntypedAndCustomChannelsHaveNoBuiltinAttribute() {
        assertNull(DamageAttributeMappings.forChannel(
                new DamageChannel(DamageChannel.PHYSICAL_ID, 1)));
        assertNull(DamageAttributeMappings.forChannel(
                new DamageChannel(DamageChannel.UNTYPED_ID, 0)));
        assertNull(DamageAttributeMappings.forChannel(new DamageChannel(
                net.minecraft.resources.Identifier.fromNamespaceAndPath(
                        "contentmod", "custom"), 2)));
    }

    @Test
    void nonFiniteAttributeValuesContributeZero() {
        assertEquals(0.0f,
                DamageAttributeScalingEngine.finiteOrZero(Float.NaN));
        assertEquals(0.0f,
                DamageAttributeScalingEngine.finiteOrZero(
                        Float.POSITIVE_INFINITY));
        assertEquals(-0.25f,
                DamageAttributeScalingEngine.finiteOrZero(-0.25f));
    }

    private static void assertMapping(
            net.minecraft.resources.Identifier channel,
            String attributePath,
            net.minecraft.resources.Identifier bucket
    ) {
        var mapping = DamageAttributeMappings.forChannel(
                new DamageChannel(channel, 1));
        assertNotNull(mapping);
        assertEquals(net.minecraft.resources.Identifier.fromNamespaceAndPath(
                "combatnexus", attributePath), mapping.attributeId());
        assertEquals(bucket, mapping.bucketId());
    }
}
