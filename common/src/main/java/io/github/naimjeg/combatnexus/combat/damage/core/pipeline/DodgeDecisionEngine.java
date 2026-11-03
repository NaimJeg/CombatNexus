package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.attribute.api.CombatAttributes;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind;

/** One target-local decision before any public computation callback or resource application. */
final class DodgeDecisionEngine {
    private DodgeDecisionEngine() {}
    static boolean execute(DamageModuleContext context) {
        if ((context.requestKind() != DamageRequestKind.PRIMARY && context.requestKind() != DamageRequestKind.SWEEP)
                || context.attackCategory() == DamageAttackCategory.NONE && context.actionId().isEmpty()) return false;
        float chance = context.getVictimAttrOrZero(CombatAttributes.dodgeChance());
        if (!Float.isFinite(chance) || chance <= 0) return false;
        if (chance < 1 && context.victim().getRandom().nextFloat() >= chance) return false;
        context.setCurrentProcessingPhase(io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase.FINAL_OVERRIDE);
        if (!context.tryCancelDamage("combatnexus:dodge").applied())
            throw new IllegalStateException("Dodge must be decided before damage locks");
        context.lineage().transaction().rejectChildren("combatnexus:dodge");
        return true;
    }
}
