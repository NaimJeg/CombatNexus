package io.github.naimjeg.combatnexus.combat.damage.core.settlement;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageFailureReason;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSettlementSnapshot;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSettlementStatus;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageApplicationPlan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;
import java.util.Map;

/**
 * Framework-owned settlement state and the sole ordinary caller authorized to
 * complete a public settlement snapshot.
 */
@ApiStatus.Internal
public final class DamageSettlementState {
    private boolean applicationCompleted;
    private boolean beforePlanEntered;
    private io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result beforeResult = io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result.EMPTY;
    void appendBeforeResult(io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result result) {
        var steps=new java.util.ArrayList<>(beforeResult.steps());
        int offset=steps.size();
        for(var step:result.steps()) steps.add(new io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.StepEvidence(offset+step.index(),step.commandType(),step.result()));
        beforeResult = new io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result(steps,
                beforeResult.continueParent() && result.continueParent(), beforeResult.completed() && result.completed());
    }
    boolean applyBeforePlan() {
        if (beforePlanEntered) return beforeResult.continueParent();
        if (!beforeResult.continueParent()) return false;
        if (calculation == null || calculation.cancelled()) return true;
        beforePlanEntered = true;
        var server = CombatServerRuntime.of(level.getServer());
        try (var applying = server.ticket(origin.lineage().transaction()).enter(
                io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.APPLICATION)) {
            appendBeforeResult(io.github.naimjeg.combatnexus.combat.internal.transaction.BeforeApplicationExecutor.execute(
                    server.world(level).orElseThrow(), calculation.beforeApplication()));
        } catch (RuntimeException | Error failure) {
            appendBeforeResult(new io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result(java.util.List.of(), false, false));
            throw failure;
        }
        return beforeResult.continueParent() && target.isAlive() && !target.isRemoved() && target.level() == level;
    }
    private boolean ambiguousHealthWrite;
    private boolean healthWriteObserved;
    private float confirmedHealthLoss;
    private double shieldLoss;
    private final java.util.List<io.github.naimjeg.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt> equipment = new java.util.ArrayList<>();
    void recordEquipmentReceipt(io.github.naimjeg.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt receipt) {
        equipment.add(java.util.Objects.requireNonNull(receipt));
    }
    private boolean equipmentConsumed() { return equipment.stream().anyMatch(receipt -> receipt.durabilityConsumed() > 0); }
    private final java.util.List<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry> writes = new java.util.ArrayList<>();
    private boolean protectionDecided;
    private Float protectedHealth;
    float protectHealth(float before, float proposed) {
        if (before > 0 && Float.isFinite(proposed) && proposed < before)
            proposed = Math.max(proposed, Math.min(before, origin.minimumHealth()));
        if (protectionDecided) return protectedHealth == null ? proposed : Math.max(proposed, protectedHealth);
        protectionDecided = true;
        if (calculation == null || before <= 0 || !Float.isFinite(proposed) || proposed > 0 || proposed >= before) return proposed;
        var server = CombatServerRuntime.of(level.getServer());
        var world = server.world(level).orElseThrow();
        try (var applying = server.ticket(origin.lineage().transaction()).enter(
                io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.APPLICATION)) {
            var decision = io.github.naimjeg.combatnexus.combat.spi.CombatBehaviorAccess.protect(
                    world, target.getUUID(), calculation.behaviors(), before, proposed, calculation.reactionInputs(), java.util.Optional.of(new io.github.naimjeg.combatnexus.combat.api.CombatSource(origin.source().damageType().identifier(), java.util.Optional.ofNullable(origin.attribution().logicalAttacker()).map(net.minecraft.world.entity.Entity::getUUID), java.util.Optional.empty())));
            if (decision.consumption().isPresent()) {
                writes.add(decision.consumption().orElseThrow());
                protectedHealth = decision.health();
                return protectedHealth;
            }
        }
        return proposed;
    }
    void recordProtection(java.util.List<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry> entries) {
        writes.addAll(entries);
    }
    boolean permitsNativeHitEffects() {
        if (calculation == null) return true;
        return !calculation.cancelled() && !lateIncomingCancellation && beforeResult.completed()
                && (confirmedHealthLoss > 0 || shieldLoss > 0 || equipmentConsumed() || positiveDelta(absorptionBefore, target.getAbsorptionAmount()) > 0);
    }
    private int pendingHealthWrite = -1;
    void beginHealthWrite(float before) {
        pendingHealthWrite = writes.size();
        writes.add(new io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry(
                io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH, java.util.Optional.empty(),
                java.util.OptionalDouble.of(before), java.util.OptionalDouble.empty(), 0,
                io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Observation.UNKNOWN, false));
    }
    void recordHealthWrite(float before, float after, boolean completed, boolean attributable) {
        if (!attributable) {
            ambiguousHealthWrite = true;
            if (pendingHealthWrite >= 0) { pendingHealthWrite = -1; }
            return; // The preallocated UNKNOWN entry remains; net delta cannot prove this write.
        }
        healthWriteObserved = true;
        confirmedHealthLoss += positiveDelta(before, after);
        var entry = io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry.write(
                io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH, before, after, positiveDelta(before, after), completed);
        if (pendingHealthWrite >= 0) { writes.set(pendingHealthWrite, entry); pendingHealthWrite = -1; }
        else writes.add(entry);
    }
    private io.github.naimjeg.combatnexus.combat.shield.api.ShieldCommitReceipt shieldReceipt;
    void recordShieldReceipt(io.github.naimjeg.combatnexus.combat.shield.api.ShieldCommitReceipt receipt) {
        if (receipt.applicationId() != origin.lineage().damageId() || !receipt.target().equals(target.getUUID()))
            throw new IllegalArgumentException("Shield receipt belongs to another application");
        if (shieldReceipt != null) {
            if (!shieldReceipt.equals(receipt)) throw new IllegalStateException("Conflicting shield commit receipts");
            return;
        }
        shieldReceipt = receipt;
        shieldLoss += receipt.absorbed(); writes.addAll(receipt.writes());
    }

    private final DamageOrigin origin;
    private final LivingEntity target;
    private final ServerLevel level;
    private final float incomingHealth;
    private final float incomingAbsorption;

    private DamageSettlementPhase phase = DamageSettlementPhase.OPEN;
    private DamageApplicationPlan calculation;
    DamageApplicationPlan calculation() { return calculation; }
    private float healthBefore;
    private float absorptionBefore;
    private boolean lateIncomingCancellation;
    private DamageSettlementCompletion completion;
    private final io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue.Slot notification;
    private boolean notificationQueued;

    void enqueue(Runnable delivery) {
        if (notificationQueued) return;
        if (notification != null) notification.publish(delivery);
        notificationQueued = true;
    }

    DamageSettlementState(
            DamageOrigin origin,
            LivingEntity target,
            ServerLevel level
    ) {
        this.origin = Objects.requireNonNull(origin, "origin");
        this.target = Objects.requireNonNull(target, "target");
        this.level = Objects.requireNonNull(level, "level");
        var kernel = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(level.getServer());
        this.notification = kernel.hasTicket(origin.lineage().transaction())
                ? kernel.reserveCompletion(origin.lineage().transaction()) : kernel.rejectionCompletion().orElse(null);
        this.incomingHealth = safeAmount(target.getHealth());
        this.incomingAbsorption = safeAmount(target.getAbsorptionAmount());
        this.healthBefore = incomingHealth;
        this.absorptionBefore = incomingAbsorption;
    }

    io.github.naimjeg.combatnexus.combat.api.transaction.CombatLineage lineage() { return origin.lineage().transaction(); }

    DamageSettlementPhase phase() {
        return phase;
    }

    void calculated(DamageApplicationPlan summary) {
        requirePhase(DamageSettlementPhase.OPEN, "calculate");
        this.calculation = Objects.requireNonNull(summary, "summary");
        this.phase = DamageSettlementPhase.CALCULATED;
    }

    void rejectAdmission(DamageFailureReason reason) {
        requirePhase(DamageSettlementPhase.OPEN, "reject admission for");
        Objects.requireNonNull(reason, "reason");
        phase = DamageSettlementPhase.ADMISSION_REJECTED;
        completion = new DamageSettlementCompletion(
                DamageSettlementSnapshot.completeInternal(
                        origin,
                        target,
                        level,
                        Map.of(),
                        0.0f,
                        0.0f,
                        0.0f,
                        0.0f,
                        incomingHealth,
                        incomingHealth,
                        incomingAbsorption,
                        incomingAbsorption,
                        false,
                        true,
                        false,
                        DamageSettlementStatus.NOT_APPLIED,
                        reason,
                        null,
                        io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageApplicationEvidence.NONE
                ),
                this
        );
    }

    void markLateIncomingCancellation() {
        if (phase == DamageSettlementPhase.CALCULATED) {
            lateIncomingCancellation = true;
        }
    }

    void capturePre(float health, float absorption) {
        requirePhase(DamageSettlementPhase.CALCULATED, "capture pre-apply");
        healthBefore = safeAmount(health);
        absorptionBefore = safeAmount(absorption);
        phase = DamageSettlementPhase.PRE_APPLY_CAPTURED;
    }

    DamageSettlementCompletion capturePost(float inflictedDamage, float observedHealth, float observedAbsorption) {
        requirePhase(
                DamageSettlementPhase.PRE_APPLY_CAPTURED,
                "capture post-apply"
        );

        applicationCompleted = !ambiguousHealthWrite && beforeResult.completed();
        float inflicted = safeAmount(inflictedDamage);
        float healthAfter = safeAmount(observedHealth);
        float absorptionAfter = safeAmount(observedAbsorption);

        float actualHealthDamage = positiveDelta(
                healthBefore,
                healthAfter
        );
        float actualAbsorptionDamage = positiveDelta(
                absorptionBefore,
                absorptionAfter
        );

        writes.add(new io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry(
                io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Resource.ABSORPTION, java.util.Optional.empty(),
                java.util.OptionalDouble.of(absorptionBefore), java.util.OptionalDouble.of(absorptionAfter),
                actualAbsorptionDamage, io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Observation.WHOLE_CALL, true));
        DamageFailureReason reason = terminalReason(inflicted + actualAbsorptionDamage, false);
        DamageSettlementStatus status = reason == null
                ? DamageSettlementStatus.APPLIED
                : DamageSettlementStatus.NOT_APPLIED;
        phase = status == DamageSettlementStatus.APPLIED
                ? DamageSettlementPhase.APPLIED
                : DamageSettlementPhase.NOT_APPLIED;

        completion = new DamageSettlementCompletion(
                snapshot(
                        status,
                        reason,
                        inflicted,
                        actualHealthDamage,
                        actualAbsorptionDamage,
                        healthAfter,
                        absorptionAfter,
                        calculation.cancelled()
                ),
                this
        );
        return completion;
    }

    DamageSettlementCompletion completeWithoutPost(boolean vanillaAccepted, boolean incomingCancelled) {
        if (phase == DamageSettlementPhase.ADMISSION_REJECTED) {
            return completion;
        }
        if (phase == DamageSettlementPhase.APPLIED
                || phase == DamageSettlementPhase.NOT_APPLIED) {
            return completion;
        }

        if (phase != DamageSettlementPhase.CALCULATED
                && phase != DamageSettlementPhase.PRE_APPLY_CAPTURED) {
            return null;
        }

        if (vanillaAccepted) {
            return abort();
        }

        if (incomingCancelled) {
            lateIncomingCancellation = true;
        }
        DamageFailureReason reason = terminalReason(0.0f, true);
        if (reason == null) {
            reason = DamageFailureReason.VANILLA_REJECTED;
        }

        phase = DamageSettlementPhase.NOT_APPLIED;
        completion = new DamageSettlementCompletion(
                snapshot(
                        DamageSettlementStatus.NOT_APPLIED,
                        reason,
                        0.0f,
                        0.0f,
                        0.0f,
                        safeAmount(target.getHealth()),
                        safeAmount(target.getAbsorptionAmount()),
                        calculation.cancelled()
                                || lateIncomingCancellation
                ),
                this
        );
        return completion;
    }

    DamageSettlementCompletion abort() {
        if (completion != null) return completion;
        if (calculation != null && (confirmedHealthLoss > 0 || shieldLoss > 0 || equipmentConsumed())) {
            // Confirmed writes survive exceptions. The evidence explicitly marks incomplete application.
            phase = DamageSettlementPhase.APPLIED;
            completion = new DamageSettlementCompletion(snapshot(DamageSettlementStatus.APPLIED, null,
                    confirmedHealthLoss, confirmedHealthLoss, 0, safeAmount(target.getHealth()),
                    safeAmount(target.getAbsorptionAmount()), false), this);
            return completion;
        }
        if (calculation == null) calculation = new DamageApplicationPlan(0, Map.of(), false, false, null, io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs.EMPTY);
        phase = DamageSettlementPhase.NOT_APPLIED;
        completion = new DamageSettlementCompletion(snapshot(DamageSettlementStatus.NOT_APPLIED,
                DamageFailureReason.INTERNAL_ERROR, 0, 0, 0, safeAmount(target.getHealth()),
                safeAmount(target.getAbsorptionAmount()), false), this);
        return completion;
    }

    void markPublished() {
        if (phase != DamageSettlementPhase.ADMISSION_REJECTED
                && phase != DamageSettlementPhase.APPLIED
                && phase != DamageSettlementPhase.NOT_APPLIED) {
            throw new IllegalStateException(
                    "Cannot publish settlement in phase " + phase
            );
        }
        phase = DamageSettlementPhase.PUBLISHED;
    }

    private DamageFailureReason terminalReason(
            float inflicted,
            boolean noPost
    ) {
        if (!beforeResult.completed()) return DamageFailureReason.INTERNAL_ERROR;
        if (calculation.cancelled()) {
            return DamageFailureReason.CANCELLED;
        }
        if (calculation.resolvedDamage() <= 0.0f) {
            return DamageFailureReason.ZERO_DAMAGE;
        }
        if (lateIncomingCancellation) {
            return DamageFailureReason.LATE_INCOMING_CANCELLATION;
        }
        if (!noPost && inflicted <= 0.0f && shieldLoss <= 0 && !equipmentConsumed()) {
            return DamageFailureReason.ZERO_AFTER_PRE;
        }
        if (noPost) {
            return DamageFailureReason.VANILLA_REJECTED;
        }
        return null;
    }

    private DamageSettlementSnapshot snapshot(
            DamageSettlementStatus status,
            DamageFailureReason reason,
            float appliedDamage,
            float healthDamage,
            float absorptionDamage,
            float healthAfter,
            float absorptionAfter,
            boolean cancelled
    ) {
        return DamageSettlementSnapshot.completeInternal(
                origin,
                target,
                level,
                calculation.resolvedChannelDamage(),
                calculation.resolvedDamage(),
                appliedDamage,
                healthDamage,
                absorptionDamage,
                healthBefore,
                healthAfter,
                absorptionBefore,
                absorptionAfter,
                calculation.critical(),
                calculation.criticalDecision(),
                cancelled,
                true,
                status,
                reason,
                calculation.cancelSourceId(),
                new io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageApplicationEvidence(
                        healthWriteObserved, confirmedHealthLoss, shieldLoss, calculation.reactionInputs(), applicationCompleted,
                        new io.github.naimjeg.combatnexus.combat.api.ApplicationJournal(writes, applicationCompleted), calculation.behaviors(), calculation.action(), beforeResult, equipment)
        );
    }

    private void requirePhase(
            DamageSettlementPhase expected,
            String action
    ) {
        if (phase != expected) {
            throw new IllegalStateException(
                    "Cannot " + action + " settlement in phase " + phase
            );
        }
    }

    private static float safeAmount(float amount) {
        return Float.isFinite(amount) ? Math.max(0.0f, amount) : 0.0f;
    }

    private static float positiveDelta(float before, float after) {
        return safeAmount(before - after);
    }
}
