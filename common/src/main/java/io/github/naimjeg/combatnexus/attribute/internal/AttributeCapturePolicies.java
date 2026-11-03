package io.github.naimjeg.combatnexus.attribute.internal;

import java.util.*;
import io.github.naimjeg.combatnexus.attribute.api.EffectiveAttributePolicy;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Attribute-owned read policy table, published once by startup registration. */
public final class AttributeCapturePolicies {
    private static List<EffectiveAttributePolicy> policies = List.of();
    private static boolean installed;
    private AttributeCapturePolicies() {}
    public static synchronized void install(List<EffectiveAttributePolicy> entries) {
        if (installed) throw new IllegalStateException("Attribute capture policies already frozen");
        policies = List.copyOf(entries); installed = true;
    }
    public static EffectiveAttributePolicy.Reading capture(LivingEntity entity, Holder<Attribute> attribute,
            double raw, double nativeEffective) {
        for (var policy : policies) {
            try (var read = io.github.naimjeg.combatnexus.combat.api.transaction.CombatReadScope.open()) {
                var reading = policy.capture(entity, attribute, raw, nativeEffective);
                if (reading.isPresent()) return reading.get();
            } catch (Exception failure) {
            }
        }
        return new EffectiveAttributePolicy.Reading(nativeEffective, Identifier.withDefaultNamespace("entity_attribute_value"));
    }
}
