package io.github.naimjeg.combatnexus.effect.api;

import java.util.Objects;

/** A sampled condition owns one stable (target, effect, source) installation. */
public record EffectBinding(EffectInstance effect, boolean satisfied, net.minecraft.resources.Identifier stacking) {
    public EffectBinding(EffectInstance effect, boolean satisfied, EffectService.Stacking stacking) { this(effect, satisfied, stacking.id()); }
    public EffectBinding {
        Objects.requireNonNull(effect); Objects.requireNonNull(stacking);
        if (effect.ownership() != EffectInstance.Ownership.MANAGED
                || effect.snapshot().lifetime() == EffectLifetime.Kind.INSTANT)
            throw new IllegalArgumentException("A condition binding requires a managed active effect");
    }
}
