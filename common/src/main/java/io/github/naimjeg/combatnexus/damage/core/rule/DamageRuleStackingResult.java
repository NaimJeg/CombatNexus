package io.github.naimjeg.combatnexus.damage.core.rule;

import io.github.naimjeg.combatnexus.damage.api.rule.RuntimeDamageRule;

import java.util.List;

public record DamageRuleStackingResult(
        List<RuntimeDamageRule> rules,
        List<StackingTrace> traces
) {
}
