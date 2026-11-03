package io.github.naimjeg.combatnexus.damage.api.rule;

import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;

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
