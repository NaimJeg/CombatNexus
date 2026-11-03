package io.github.naimjeg.combatnexus.combat.damage.api.rule;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;

import java.util.List;

public interface DamageRuleProvider {

    default boolean supportsPhase(DamagePhase phase) {
        return true;
    }

    void collect(
            DamageRuleContext ctx,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    );
}
