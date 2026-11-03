package io.github.naimjeg.combatnexus.damage.core.rule;

import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleStacking;
import net.minecraft.resources.Identifier;

public record StackingTrace(
        DamagePhase phase,
        Identifier kept,
        Identifier dropped,
        DamageRuleStacking policy,
        float keptValue,
        float droppedValue
) {
}
