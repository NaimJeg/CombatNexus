package io.github.naimjeg.combatnexus.effect.api;
/** Classification and removal policy are independent from execution ownership. */
public record EffectPolicy(Category category, boolean dispellable, java.util.Set<net.minecraft.resources.Identifier> tags) {
    public EffectPolicy(Category category, boolean dispellable) { this(category, dispellable, java.util.Set.of()); }
    public enum Category { BENEFICIAL, HARMFUL, NEUTRAL }
    public static final EffectPolicy DEFAULT = new EffectPolicy(Category.NEUTRAL, true);
    public EffectPolicy { java.util.Objects.requireNonNull(category); tags = java.util.Set.copyOf(tags); }
}
