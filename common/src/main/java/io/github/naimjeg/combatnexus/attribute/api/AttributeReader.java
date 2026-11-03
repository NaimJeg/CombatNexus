package io.github.naimjeg.combatnexus.attribute.api;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.LinkedHashMap;
import java.util.Collection;
import java.util.Set;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/** Reads vanilla's authoritative attribute values; never reapplies Effect modifiers. */
public final class AttributeReader {
    private AttributeReader() {}

    public static float valueOrZero(LivingEntity entity, Holder<Attribute> attribute) {
        return entity == null || attribute == null || !entity.getAttributes().hasAttribute(attribute)
                ? 0.0f : (float) entity.getAttributeValue(attribute);
    }

    /** Caller owns revision changes and the requested attribute set. Server-thread only. */
    public static AttributeSnapshot capture(LivingEntity entity, long revision, Iterable<Holder<Attribute>> requested) {
        return capture(entity, revision, requested, count -> {});
    }

    public static AttributeSnapshot capture(LivingEntity entity, long revision, Iterable<Holder<Attribute>> requested,
                                             java.util.function.IntConsumer budget) {
        if (entity.level().isClientSide()) throw new IllegalArgumentException("Server snapshot required");
        var values = new LinkedHashMap<Identifier, AttributeValueFrame.Value>();
        var sources = new LinkedHashMap<Identifier, java.util.List<AttributeSnapshot.Contribution>>();
        for (var holder : requested) {
            budget.accept(1);
            var instance = entity.getAttribute(holder);
            if (instance == null) continue;
            var id = holder.unwrapKey().orElseThrow().identifier();
            var contributions = new java.util.ArrayList<AttributeSnapshot.Contribution>();
            for (var modifier : instance.getModifiers()) {
                budget.accept(1);
                contributions.add(new AttributeSnapshot.Contribution(modifier.id(), modifier.amount(), modifier.operation()));
            }
            double raw = instance.getValue();
            double nativeEffective = entity.getAttributeValue(holder);
            var reading = io.github.naimjeg.combatnexus.attribute.internal.AttributeCapturePolicies
                    .capture(entity, holder, raw, nativeEffective);
            values.put(id, new AttributeValueFrame.Value(instance.getBaseValue(), raw, reading.effective(), reading.reason()));
            sources.put(id, contributions);
        }
        return new AttributeSnapshot(
                new AttributeValueFrame(entity.getUUID(), entity.level().getGameTime(), revision, values),
                new AttributeProvenance(entity.getUUID(), entity.level().getGameTime(), revision, sources));
    }

    /** Capture every registered attribute for each participant once, before a calculation starts.
     * The caller supplies a request/session revision; this method retains no entities or global state.
     */
    public static java.util.Map<java.util.UUID, AttributeSnapshot> captureParticipants(
            Iterable<LivingEntity> entities, long revision) {
        var requested = net.minecraft.core.registries.BuiltInRegistries.ATTRIBUTE.listElements()
                .<Holder<Attribute>>map(holder -> holder).toList();
        var snapshots = new LinkedHashMap<java.util.UUID, AttributeSnapshot>();
        for (LivingEntity entity : entities) {
            if (entity != null && !snapshots.containsKey(entity.getUUID())) {
                snapshots.put(entity.getUUID(), capture(entity, revision, requested));
            }
        }
        return java.util.Map.copyOf(snapshots);
    }

    public static double valueWithout(
            Holder<Attribute> attribute,
            double baseValue,
            Collection<AttributeModifier> modifiers,
            Set<Identifier> excludedModifierIds
    ) {
        double base = baseValue;

        for (AttributeModifier modifier : modifiers) {
            if (excludedModifierIds.contains(modifier.id())) {
                continue;
            }

            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                base += modifier.amount();
            }
        }

        double result = base;

        for (AttributeModifier modifier : modifiers) {
            if (excludedModifierIds.contains(modifier.id())) {
                continue;
            }

            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                result += base * modifier.amount();
            }
        }

        for (AttributeModifier modifier : modifiers) {
            if (excludedModifierIds.contains(modifier.id())) {
                continue;
            }

            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                result *= 1.0D + modifier.amount();
            }
        }

        return attribute.value().sanitizeValue(result);
    }
}
