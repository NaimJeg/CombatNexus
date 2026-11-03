package io.github.naimjeg.combatnexus.combat.api.input;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot;

/** One participant's frozen input. Identity is descriptive and cannot grant execution authority. */
public record CombatInputFrame(UUID sessionId, Identifier dimension, UUID entity, long revision,
        long gameTick, long definitionGeneration, AttributeSnapshot attributes, CombatEntityFacts facts, java.util.List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> behaviors) {
    public CombatInputFrame {
        behaviors = java.util.List.copyOf(behaviors);
        Objects.requireNonNull(sessionId); Objects.requireNonNull(dimension); Objects.requireNonNull(entity);
        Objects.requireNonNull(attributes); Objects.requireNonNull(facts);
        if (behaviors.size() > 64 || behaviors.stream().anyMatch(s -> !s.target().equals(entity) || !s.session().equals(sessionId)))
            throw new IllegalArgumentException("Behavior capture identity or capacity mismatch");
        if (!entity.equals(attributes.entity()) || revision != attributes.revision() || gameTick != attributes.gameTick())
            throw new IllegalArgumentException("Input capture identity mismatch");
    }
}
