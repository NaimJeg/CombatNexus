package io.github.naimjeg.combatnexus.combat.damage.core.rule;

import io.github.naimjeg.combatnexus.combat.damage.api.rule.RuntimeDamageRule;
import java.util.List;

public record DamageRuleStackingResult(List<RuntimeDamageRule> rules) {
    public DamageRuleStackingResult { rules = List.copyOf(rules); }
}
