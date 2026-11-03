package io.github.naimjeg.combatnexus.effect.api;

import java.util.*;
import net.minecraft.resources.Identifier;

public record EffectFact(EffectInstance effect, Identifier condition, Map<String, Double> values, Identifier stacking) {
    public EffectFact {
        Objects.requireNonNull(effect); Objects.requireNonNull(condition); Objects.requireNonNull(stacking); values = Map.copyOf(values);
        if (values.values().stream().anyMatch(v -> !Double.isFinite(v))) throw new IllegalArgumentException("Non-finite environment fact");
    }
}
