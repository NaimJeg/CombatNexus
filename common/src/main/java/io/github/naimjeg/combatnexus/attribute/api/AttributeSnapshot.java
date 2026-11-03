package io.github.naimjeg.combatnexus.attribute.api;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Server-computed values and original modifier identities, suitable for read-only presentation. */
public final class AttributeSnapshot {
    private final AttributeValueFrame values;
    private final AttributeProvenance provenance;
    public AttributeSnapshot(AttributeValueFrame values, AttributeProvenance provenance) {
        this.values = java.util.Objects.requireNonNull(values);
        this.provenance = java.util.Objects.requireNonNull(provenance);
        if (!values.entity().equals(provenance.entity()) || values.gameTick() != provenance.gameTick()
                || values.revision() != provenance.revision() || !values.values().keySet().equals(provenance.sources().keySet()))
            throw new IllegalArgumentException("Value and provenance capture mismatch");
    }
    public UUID entity() { return values.entity(); }
    public long gameTick() { return values.gameTick(); }
    public long revision() { return values.revision(); }
    public AttributeValueFrame valueFrame() { return values; }
    public AttributeProvenance provenance() { return provenance; }
    /** Assemble the combined read view only for callers requesting explanations. */
    public Map<Identifier, Value> values() {
        var combined = new java.util.LinkedHashMap<Identifier, Value>();
        values.values().forEach((id, value) -> combined.put(id,
                new Value(value.base(), value.effective(), provenance.sources().get(id))));
        return Map.copyOf(combined);
    }
    public float valueOrZero(Identifier attribute) {
        var value = values.values().get(attribute);
        if (value == null) return 0;
        float result = (float) value.effective();
        return Float.isFinite(result) ? result : 0;
    }

    public record Contribution(Identifier source, double amount, AttributeModifier.Operation operation) {
        public Contribution {
            java.util.Objects.requireNonNull(source, "source");
            java.util.Objects.requireNonNull(operation, "operation");
            if (!Double.isFinite(amount)) throw new IllegalArgumentException("Non-finite contribution");
        }
    }

    public record Value(double base, double effective, List<Contribution> contributions) {
        public Value {
            if (!Double.isFinite(base) || !Double.isFinite(effective)) {
                throw new IllegalArgumentException("Non-finite attribute value");
            }
            contributions = List.copyOf(contributions);
        }
    }
}
