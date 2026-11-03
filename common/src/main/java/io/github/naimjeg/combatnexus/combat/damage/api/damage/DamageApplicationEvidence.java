package io.github.naimjeg.combatnexus.combat.damage.api.damage;

/** Native health-write evidence is separate from whole-call observed deltas. */
public record DamageApplicationEvidence(boolean healthWriteObserved, float healthLoss, double shieldLoss, io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs reactionInputs, boolean applicationCompleted, io.github.naimjeg.combatnexus.combat.api.ApplicationJournal journal, java.util.List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> behaviors, java.util.Optional<io.github.naimjeg.combatnexus.combat.api.action.CombatAction> action, io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result beforeApplication, java.util.List<EquipmentDamageReceipt> equipment) {
    public static final DamageApplicationEvidence NONE = new DamageApplicationEvidence(false, 0, 0, io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs.EMPTY, false, io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.EMPTY, java.util.List.of(), java.util.Optional.empty(), io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result.EMPTY, java.util.List.of());
    public DamageApplicationEvidence {
        equipment = java.util.List.copyOf(equipment);
        behaviors = java.util.List.copyOf(behaviors);
        if (!Float.isFinite(healthLoss) || healthLoss < 0 || !Double.isFinite(shieldLoss) || shieldLoss < 0) {
            throw new IllegalArgumentException("Invalid application evidence");
        }
    }
}
