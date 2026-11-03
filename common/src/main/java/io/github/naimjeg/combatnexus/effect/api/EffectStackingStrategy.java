package io.github.naimjeg.combatnexus.effect.api;

/** Pure resolution of an existing installation and a new application. */
@FunctionalInterface
public interface EffectStackingStrategy {
    EffectInstance resolve(EffectInstance previous, EffectInstance incoming, long now);
}
