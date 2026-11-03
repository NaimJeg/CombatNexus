package io.github.naimjeg.combatnexus.presentation.api;

import io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot;
import io.github.naimjeg.combatnexus.combat.shield.api.ShieldPoolSnapshot;
import io.github.naimjeg.combatnexus.effect.api.EffectView;
import io.github.naimjeg.combatnexus.formula.Amounts;
import java.util.List;
import java.util.Objects;

/** Read-only server model used by authenticated panel projection. */
public record CombatPanelSnapshot(AttributeSnapshot attributes, double health, double vanillaAbsorption,
                                   List<ShieldPoolSnapshot> shields, List<EffectView> effects,
                                   List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorView> behaviors,
                                   List<io.github.naimjeg.combatnexus.combat.api.reaction.ReactionFact> reactions,
                                   List<io.github.naimjeg.combatnexus.combat.api.source.SourceDescription> sourceDescriptions) {
    public CombatPanelSnapshot(AttributeSnapshot attributes, double health, double vanillaAbsorption,
            List<ShieldPoolSnapshot> shields, List<EffectView> effects) {
        this(attributes, health, vanillaAbsorption, shields, effects, List.of(), List.of(), List.of());
    }
    public CombatPanelSnapshot {
        behaviors = List.copyOf(behaviors); reactions = List.copyOf(reactions);
        sourceDescriptions=List.copyOf(sourceDescriptions);
        if (behaviors.stream().anyMatch(b -> !b.source().target().equals(attributes.entity()))
                || reactions.stream().anyMatch(f -> !f.target().equals(attributes.entity()))) throw new IllegalArgumentException("Foreign panel behavior");
        Objects.requireNonNull(attributes, "attributes");
        Amounts.nonNegative(health, "health");
        Amounts.nonNegative(vanillaAbsorption, "vanillaAbsorption");
        shields = List.copyOf(shields);
        effects = List.copyOf(effects);
        if (effects.stream().anyMatch(effect -> !effect.target().equals(attributes.entity()))) {
            throw new IllegalArgumentException("Panel effect target mismatch");
        }
    }
}
