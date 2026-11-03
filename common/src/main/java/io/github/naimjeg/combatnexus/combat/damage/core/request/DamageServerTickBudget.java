package io.github.naimjeg.combatnexus.combat.damage.core.request;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;

import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatAdmissionBudget;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import io.github.naimjeg.combatnexus.combat.config.CombatSafetySettings;

/** Damage vocabulary adapter; live servers use CombatServerRuntime's shared budget. */
final class DamageServerTickBudget {
    private final CombatAdmissionBudget budget = new CombatAdmissionBudget();
    DamageAdmissionResult tryAdmit(DamageLineage lineage, int currentTick, CombatSafetySettings settings) {
        return fromCombat(budget.admit(lineage.transaction(), currentTick, settings.combatLimits()));
    }
    int count(int currentTick) { return budget.count(currentTick); }
    static DamageAdmissionResult fromCombat(CombatAdmissionBudget.Result result) {
        if (result.admitted()) return DamageAdmissionResult.admitted(result.derivedCount(), result.tickCount());
        DamageFailureReason reason = switch (result.rejection()) {
            case DEPTH -> DamageFailureReason.MAX_RECURSION_DEPTH;
            case ROOT_BUDGET -> DamageFailureReason.ROOT_DERIVATION_LIMIT;
            case TICK_BUDGET -> DamageFailureReason.SERVER_TICK_BUDGET_EXHAUSTED;
            case COMPLETION_CAPACITY -> DamageFailureReason.COMPLETION_CAPACITY_EXHAUSTED;
            case NONE -> throw new IllegalStateException("Admitted result");
        };
        return DamageAdmissionResult.rejected(reason, result.derivedCount(), result.tickCount());
    }
}
