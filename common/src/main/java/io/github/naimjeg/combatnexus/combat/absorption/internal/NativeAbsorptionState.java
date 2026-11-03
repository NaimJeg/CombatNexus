package io.github.naimjeg.combatnexus.combat.absorption.internal;

import io.github.naimjeg.combatnexus.combat.api.platform.NativeAbsorptionStorage;
import net.minecraft.world.entity.LivingEntity;

/** Bound to one native entity; platform storage cannot access the underlying ledger. */
public final class NativeAbsorptionState implements NativeAbsorptionStorage.State {
    private final LivingEntity owner;
    private AbsorptionLedger ledger = new AbsorptionLedger();
    private String invalid;
    public NativeAbsorptionState(LivingEntity owner) { this.owner = java.util.Objects.requireNonNull(owner); }
    private void requireOwner(LivingEntity target) {
        if (owner != target) throw new IllegalArgumentException("Absorption state belongs to another entity");
        if (target.level() instanceof net.minecraft.server.level.ServerLevel level && !level.getServer().isSameThread())
            throw new IllegalStateException("Absorption state requires the world thread");
    }
    public static AbsorptionLedger ledger(LivingEntity target) {
        var state = (NativeAbsorptionState)((NativeAbsorptionStorage)target).combatnexus$absorptionState();
        state.requireOwner(target);
        if (state.invalid != null) throw new IllegalStateException("Native absorption state failed restoration");
        return state.ledger;
    }
    public float maximum(LivingEntity target, float original) {
        requireOwner(target);
        return (float)Math.min(Float.MAX_VALUE, original + ledger.balance());
    }
    public void reconcile(LivingEntity target) { requireOwner(target); ledger.reconcile(target.getAbsorptionAmount()); }
    public String save(LivingEntity target) { requireOwner(target); return invalid != null ? invalid : ledger.encode(); }
    public void restore(LivingEntity target, String encoded) {
        requireOwner(target);
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireNativeWritePhase();
        ledger = new AbsorptionLedger(); invalid = null;
        try { ledger.restore(encoded); }
        catch (RuntimeException failure) {
            invalid = encoded;
        }
    }
}
