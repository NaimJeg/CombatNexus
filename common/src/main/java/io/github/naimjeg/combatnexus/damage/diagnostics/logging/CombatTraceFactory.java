package io.github.naimjeg.combatnexus.damage.diagnostics.logging;

import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;

public final class CombatTraceFactory {

    private CombatTraceFactory() {
    }

    public static CombatTrace create(
            long damageId
    ) {
        if (!DamageModuleSettings.summaryTraceEnabled()) {
            return NoOpCombatTrace.INSTANCE;
        }

        return new Slf4jCombatTrace(damageId);
    }
}

