package io.github.naimjeg.combatnexus.attribute.api;

import java.util.*;
import net.minecraft.resources.Identifier;

/** Numeric input only; identity is shared with the provenance captured in the same pass. */
public record AttributeValueFrame(UUID entity, long gameTick, long revision, Map<Identifier, Value> values) {
    public AttributeValueFrame {
        Objects.requireNonNull(entity);
        if (revision < 0) throw new IllegalArgumentException("Negative revision");
        values = Map.copyOf(values);
    }
    public record Value(double base, double raw, double effective, Identifier effectiveReason) {
        public Value {
            Objects.requireNonNull(effectiveReason);
            if (!Double.isFinite(base) || !Double.isFinite(raw) || !Double.isFinite(effective))
                throw new IllegalArgumentException("Non-finite attribute");
        }
    }
}
