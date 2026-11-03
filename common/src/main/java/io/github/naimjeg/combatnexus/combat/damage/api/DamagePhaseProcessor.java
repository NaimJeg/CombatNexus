package io.github.naimjeg.combatnexus.combat.damage.api;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;

public interface DamagePhaseProcessor {

    int PRIORITY_VANILLA = 0;
    int PRIORITY_MOD_STANDARD = 100;
    int PRIORITY_OVERRIDE = 1000;

    void apply(DamageRuleContext ctx);

    DamagePhase phase();

    default int getPriority() {
        return PRIORITY_MOD_STANDARD;
    }

    default boolean canHandle(DamageContextView ctx) {
        return ctx.isManaged();
    }
}
