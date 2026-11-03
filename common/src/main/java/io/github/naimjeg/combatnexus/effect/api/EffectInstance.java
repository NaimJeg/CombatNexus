package io.github.naimjeg.combatnexus.effect.api;

import io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot;
import net.minecraft.resources.Identifier;
import java.util.List;
import java.util.Objects;

/** Observed vanilla modifiers are descriptive only; managed modifiers need one platform executor. */
public record EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, Ownership ownership, io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan behavior, EffectPolicy policy, List<EffectInstance> hidden, EffectDisplay display, Identifier stacking) {
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, Ownership ownership,
            io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan behavior, EffectPolicy policy, List<EffectInstance> hidden, EffectDisplay display) {
        this(snapshot, attributes, ownership, behavior, policy, hidden, display, EffectService.Stacking.REPLACE.id());
    }
    public EffectInstance withStacking(Identifier strategy) { return new EffectInstance(snapshot, attributes, ownership, behavior, policy, hidden, display, strategy); }
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, Ownership ownership,
            io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan behavior, EffectPolicy policy, List<EffectInstance> hidden) {
        this(snapshot, attributes, ownership, behavior, policy, hidden, EffectDisplay.NONE);
    }
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, Ownership ownership,
            io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan behavior, EffectPolicy policy) {
        this(snapshot, attributes, ownership, behavior, policy, List.of());
    }
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, Ownership ownership, io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan behavior) {
        this(snapshot, attributes, ownership, behavior, EffectPolicy.DEFAULT);
    }
    public EffectInstance(EffectSnapshot snapshot, List<AttributeContribution> attributes, Ownership ownership) {
        this(snapshot, attributes, ownership, io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorPlan.NONE);
    }
    public EffectInstance {
        Objects.requireNonNull(stacking);
        Objects.requireNonNull(display);
        Objects.requireNonNull(policy);
        Objects.requireNonNull(behavior);
        if (ownership == Ownership.VANILLA_OBSERVED && !behavior.empty()) throw new IllegalArgumentException("Observed effect cannot own behavior");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(ownership, "ownership");
        attributes = List.copyOf(attributes);
        hidden = List.copyOf(hidden);
        if (hidden.size() > 256 || hidden.stream().anyMatch(e -> !e.hidden().isEmpty()
                || !e.snapshot().target().equals(snapshot.target()) || !e.snapshot().effect().equals(snapshot.effect())
                || (!display.aggregateSources() && !e.snapshot().source().equals(snapshot.source())) || e.snapshot().lifetime() == EffectLifetime.Kind.INSTANT))
            throw new IllegalArgumentException("Invalid hidden effect layers");
        if (snapshot.lifetime() == EffectLifetime.Kind.INSTANT) {
            if (!hidden.isEmpty() || !attributes.isEmpty() || !snapshot.restrictions().isEmpty()
                    || behavior.components().stream().anyMatch(c -> !(c instanceof io.github.naimjeg.combatnexus.combat.api.behavior.InstantBehavior)))
                throw new IllegalArgumentException("Instant effects can only contain instant behaviors");
        } else if (behavior.components().stream().anyMatch(c -> c instanceof io.github.naimjeg.combatnexus.combat.api.behavior.InstantBehavior)) {
            throw new IllegalArgumentException("Instant behaviors require an instant effect");
        }
        if (ownership == Ownership.VANILLA_OBSERVED && !snapshot.restrictions().isEmpty()) {
            throw new IllegalArgumentException("Observed effects cannot install managed restrictions");
        }
    }
    public enum Ownership { VANILLA_OBSERVED, MANAGED }
    public record AttributeContribution(Identifier attribute, AttributeSnapshot.Contribution modifier) {
        public AttributeContribution {
            Objects.requireNonNull(attribute, "attribute");
            Objects.requireNonNull(modifier, "modifier");
        }
    }
}
