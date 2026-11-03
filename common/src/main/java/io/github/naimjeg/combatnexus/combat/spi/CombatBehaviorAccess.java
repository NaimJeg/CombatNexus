package io.github.naimjeg.combatnexus.combat.spi;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;

import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatCallback;
import io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementCallback;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingSettlement;
import java.util.*;

/** Trusted domain/adapter wiring. Does not issue transaction authority: handlers verify the active callback/application. */
public final class CombatBehaviorAccess {
    private CombatBehaviorAccess() {}
    public static final class Permit {
        private final CombatRuntime world;
        private final Thread owner = Thread.currentThread();
        private boolean active = true;
        private Permit(CombatRuntime world) { this.world = world; }
        public void require(CombatRuntime candidate) {
            if (!active || candidate != world || owner != Thread.currentThread())
                throw new IllegalStateException("Behavior wiring authority expired or belongs to another session");
        }
    }
    public record ProtectionDecision(float health, Optional<ApplicationJournal.Entry> consumption) {
        public ProtectionDecision { Objects.requireNonNull(consumption); }
    }
    private static final Object SETTLEMENT_OWNER=new Object();
    public static void completed(CombatRuntime world,io.github.naimjeg.combatnexus.combat.api.transaction.CombatLineage lineage,
            io.github.naimjeg.combatnexus.combat.api.behavior.SettlementEnvelope envelope,List<BehaviorSource> captured) {
        var callback=new CombatCallback<>(lineage,envelope,world.level().getServer());
        var permit=new Permit(world);
        var allowed=envelope.journal().applicationCompleted()?java.util.EnumSet.allOf(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.class):java.util.Set.<io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation>of();
        try(var scope=io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope.callback(SETTLEMENT_OWNER,callback,null,world.level().getServer(),lineage,allowed)) {
            world.dispatchSettlement(callback,captured,permit);
        } finally {permit.active=false;}
    }
    public static void damage(CombatRuntime world, DamageSettlementCallback callback) {
        var permit = new Permit(world);
        try { world.dispatchDamageReaction(callback, permit); } finally { permit.active = false; }
    }
    public static void recordRecoveryDamage(CombatRuntime world, DamageSettlementCallback callback) {
        var permit = new Permit(world);
        try { world.recordRecoveryDamage(callback, permit); } finally { permit.active = false; }
    }
    public static void healing(CombatRuntime world, CombatCallback<HealingSettlement> callback) {
        var permit = new Permit(world);
        try { world.dispatchHealingReaction(callback, permit); } finally { permit.active = false; }
    }
    public static ProtectionDecision protect(CombatRuntime world, UUID target, List<BehaviorSource> captured, float before, float proposed, io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs inputs, java.util.Optional<io.github.naimjeg.combatnexus.combat.api.CombatSource> origin) {
        var permit = new Permit(world);
        try { return world.decideProtection(target, captured, before, proposed, inputs, origin, permit); } finally { permit.active = false; }
    }
}
