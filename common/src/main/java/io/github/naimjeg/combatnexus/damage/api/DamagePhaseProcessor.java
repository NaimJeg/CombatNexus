package io.github.naimjeg.combatnexus.damage.api;

import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;

public interface DamagePhaseProcessor {

    int PRIORITY_VANILLA = 0;
    int PRIORITY_MOD_STANDARD = 100;
    int PRIORITY_OVERRIDE = 1000;

    void apply(DamageRuleContext ctx);

    DamagePhase phase();

    default int getPriority() {
        return PRIORITY_MOD_STANDARD;
    }

    default boolean canHandle(DamageRuleContext ctx) {
        return ctx.isManaged();
    }
}
