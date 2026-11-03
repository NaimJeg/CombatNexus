package io.github.naimjeg.combatnexus.combat.damage.platform.settlement;

import io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementTracker;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageFailureReason;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageApplicationPlan;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/** Translates loader events without owning settlement state. */
public final class NeoForgeDamageSettlement {
    private NeoForgeDamageSettlement() {}
    private record Incoming(LivingIncomingDamageEvent event) implements DamageSettlementTracker.Incoming {
        public LivingEntity getEntity() { return event.getEntity(); }
        public DamageSource getSource() { return event.getSource(); }
        public Object getContainer() { return event.getContainer(); }
        public boolean isCanceled() { return event.isCanceled(); }
        public void setCanceled(boolean value) { event.setCanceled(value); }
    }
    public static boolean beginIncoming(LivingIncomingDamageEvent event, DamageOrigin origin) {
        return DamageSettlementTracker.beginIncoming(new Incoming(event), origin);
    }
    public static boolean rejectAdmission(LivingIncomingDamageEvent event, DamageOrigin origin, DamageFailureReason reason) {
        return DamageSettlementTracker.rejectAdmission(new Incoming(event), origin, reason);
    }
    public static void recordCalculated(LivingIncomingDamageEvent event, DamageApplicationPlan summary) {
        DamageSettlementTracker.recordCalculated(new Incoming(event), summary);
    }
    public static void enforceAdmissionRejection(LivingIncomingDamageEvent event) {
        DamageSettlementTracker.enforceAdmissionRejection(new Incoming(event));
    }
    public static void markIncomingDispatchComplete(LivingIncomingDamageEvent event) {
        DamageSettlementTracker.markIncomingDispatchComplete(new Incoming(event));
    }
    public static void capturePre(LivingDamageEvent.Pre event) {
        DamageSettlementTracker.capturePre(event.getContainer(), event.getEntity());
    }
    public static void capturePost(LivingDamageEvent.Post event) {
        DamageSettlementTracker.capturePost(event.getEntity(), event.getSource(), event.getInflictedDamage());
    }
}
