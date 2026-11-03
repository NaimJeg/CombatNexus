package io.github.naimjeg.combatnexus.combat.damage.core.settlement;

import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageRequestSubmissionTracker;
import io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementCompletion;
import io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementCoordinator;
import io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementTracker;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import io.github.naimjeg.combatnexus.combat.api.platform.NativeDamageAccess;
import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;
import java.util.function.BooleanSupplier;
import java.util.List;

/** Internal owner of native invocation scopes; the platform receives only its public contract. */
public final class NativeDamageAdapter implements NativeDamageAccess {
    public static final NativeDamageAccess INSTANCE = new NativeDamageAdapter();
    private NativeDamageAdapter() {}
    private record AdjustmentCall(LivingEntity target, DamageSource source, NativeDamageInput.Adjustment value) {}
    private static final ThreadLocal<AdjustmentCall> ADJUSTMENT = new ThreadLocal<>();
    public io.github.naimjeg.combatnexus.combat.api.platform.NativeAbsorptionStorage.State absorptionState(LivingEntity target) {
        return new io.github.naimjeg.combatnexus.combat.absorption.internal.NativeAbsorptionState(target);
    }
    public float absorptionMaximum(LivingEntity target, io.github.naimjeg.combatnexus.combat.api.platform.NativeAbsorptionStorage.State state, float original) {
        return ((io.github.naimjeg.combatnexus.combat.absorption.internal.NativeAbsorptionState)state).maximum(target, original);
    }
    public void absorptionChanged(LivingEntity target, io.github.naimjeg.combatnexus.combat.api.platform.NativeAbsorptionStorage.State state) {
        ((io.github.naimjeg.combatnexus.combat.absorption.internal.NativeAbsorptionState)state).reconcile(target);
    }
    public String saveAbsorption(LivingEntity target, io.github.naimjeg.combatnexus.combat.api.platform.NativeAbsorptionStorage.State state) {
        return ((io.github.naimjeg.combatnexus.combat.absorption.internal.NativeAbsorptionState)state).save(target);
    }
    public void restoreAbsorption(LivingEntity target, io.github.naimjeg.combatnexus.combat.api.platform.NativeAbsorptionStorage.State state, String encoded) {
        ((io.github.naimjeg.combatnexus.combat.absorption.internal.NativeAbsorptionState)state).restore(target, encoded);
    }
    public HealthWrite observeHealthWrite(LivingEntity target, Runnable nativeWrite) {
        if (!(target.level() instanceof ServerLevel level) || !level.getServer().isSameThread())
            throw new IllegalStateException("Health write requires the server world thread");
        try (var write = io.github.naimjeg.combatnexus.combat.spi.NativeResourceWrites.health(target)) {
            boolean completed = false;
            RuntimeException failure = null;
            try { nativeWrite.run(); completed = true; } catch (RuntimeException thrown) { failure = thrown; }
            return new HealthWrite(completed, write.uncontended(), failure);
        }
    }
    public void confirmedDeath(LivingEntity target, DamageSource source) {
        if (!(target.level() instanceof ServerLevel level)) return;
        io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(level,
            new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.Death(target.getUUID(),
                java.util.Optional.ofNullable(source.getEntity()).map(net.minecraft.world.entity.Entity::getUUID)
                    .or(() -> io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileFacts.launch(source.getDirectEntity()).map(launch -> launch.shooter().entity())),
                source.typeHolder().unwrapKey().map(k -> k.identifier()).orElse(net.minecraft.resources.Identifier.parse("minecraft:generic")),
                java.util.Optional.ofNullable(source.getDirectEntity()).map(net.minecraft.world.entity.Entity::getUUID),
                DamageSettlementTracker.currentOperation(target, source)));
    }
    public void itemBlocked(LivingEntity target, DamageSource source, net.minecraft.resources.Identifier item, float amount) {
        if (target.level() instanceof ServerLevel level && amount > 0)
            io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(level, new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.ItemBlock(
                target.getUUID(), java.util.Optional.ofNullable(source.getEntity()).map(net.minecraft.world.entity.Entity::getUUID), item, amount));
    }
    public void partHit(net.minecraft.world.entity.Entity part, LivingEntity target, DamageSource source, boolean accepted) {
        if (target.level() instanceof ServerLevel level)
            io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(level, new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.PartHit(
                part.getUUID(), target.getUUID(), source.typeHolder().unwrapKey().orElseThrow().identifier(), accepted));
    }
    public void protection(LivingEntity target, DamageSource source, java.util.List<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry> evidence, boolean protectedTarget, boolean completed) {
        DamageSettlementTracker.recordProtection(target, source, evidence);
        if (target.level() instanceof ServerLevel level && !evidence.isEmpty())
            io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(level, new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.Protection(
                target.getUUID(), DamageSettlementTracker.currentOperation(target, source), source.typeHolder().unwrapKey().orElseThrow().identifier(), protectedTarget,
                new io.github.naimjeg.combatnexus.combat.api.ApplicationJournal(evidence, completed)));
    }
    public boolean managed(DamageSource source) { return DamageSourcePolicy.shouldManage(source); }
    public boolean permitsHitEffects(LivingEntity target, DamageSource source) { return DamageSettlementTracker.permitsNativeHitEffects(target, source); }
    public float penetratedArmor(LivingEntity target, DamageSource source, float armor) { return DamageSettlementTracker.penetratedArmor(target, source, armor); }
    public float consumeShields(LivingEntity target, DamageSource source, float amount) {
        if (amount <= 0 || !Float.isFinite(amount) || !target.isAlive()
                || !(target.level() instanceof ServerLevel level) || !DamageSourcePolicy.shouldManage(source)) return amount;
        var lineage = CombatExecutionScope.currentLineage(level.getServer());
        if (lineage.isEmpty()) return amount;
        var runtime = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer())
            .flatMap(server -> server.world(level)).orElseThrow();
        var type = source.typeHolder().unwrapKey().orElseThrow().identifier();
        try (var reserved = runtime.shields().reserve(target.getUUID(), lineage.get().id(), type, amount, DamageSettlementTracker.channelDamage(target, source))) {
            float remaining = (float)reserved.remainingDamage();
            try { reserved.commit(); }
            finally { reserved.receipt().ifPresent(receipt -> DamageSettlementTracker.recordShieldReceipt(target, source, receipt)); }
            return remaining;
        }
    }
    public void writeHealth(LivingEntity target, DamageSource source, float requested, java.util.function.Consumer<Float> nativeWrite) {
        float before = target.getHealth();
        float value = DamageSettlementTracker.protectHealth(target, source, before, requested);
        try (var write = DamageSettlementTracker.beginHealthWrite(target, source, before)) {
            boolean completed = false;
            try { nativeWrite.accept(value); completed = true; }
            finally { write.finish(target.getHealth(), completed); }
        }
    }
    public boolean sweep(LivingEntity target, DamageSource source, BooleanSupplier nativeCall) {
        try (var scope = new io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.NativeSweepScope(target, source)) {
            return nativeCall.getAsBoolean();
        }
    }
    public <T> T attackOwnership(DamageSource source, java.util.function.Supplier<T> nativeCall) {
        try (var scope = io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.VanillaAttackTakeover.enter(DamageSourcePolicy.shouldManage(source))) {
            return nativeCall.get();
        }
    }
    public void attack(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Entity target, Runnable nativeCall) {
        if (!(player.level() instanceof ServerLevel level)) { nativeCall.run(); return; }
        var runtime = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer())
            .flatMap(server -> server.world(level)).orElseThrow();
        try (var action = io.github.naimjeg.combatnexus.combat.spi.CombatActionAccess.open(runtime, player.getUUID())) {
            var world = io.github.naimjeg.combatnexus.combat.api.CombatAccess.find(level).orElseThrow();
            var identity = io.github.naimjeg.combatnexus.combat.spi.CombatActionAccess.current(world.session(), player.getUUID()).orElseThrow();
            io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(level, new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.Attack(identity,
                io.github.naimjeg.combatnexus.combat.spi.CombatActionAccess.input(world.session(), player.getUUID()).orElseThrow(), target.getUUID(), player.getAttackStrengthScale(0.5F)));
            nativeCall.run();
            io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(level, new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.PostAttack(identity, target.getUUID()));
        }
    }
    public boolean independentCooldown(LivingEntity target, DamageSource source) {
        return DamageRequestSubmissionTracker.independentCooldown(target, source);
    }
    public boolean adjusted(LivingEntity target, DamageSource source, NativeDamageInput.Adjustment adjustment, BooleanSupplier nativeCall) {
        CombatExecutionScope.requireNativeWritePhase();
        if (!(target.level() instanceof ServerLevel level) || !level.getServer().isSameThread())
            throw new IllegalStateException("Native adjustment requires the world thread");
        var previous = ADJUSTMENT.get();
        ADJUSTMENT.set(new AdjustmentCall(target, source, java.util.Objects.requireNonNull(adjustment)));
        try { return nativeCall.getAsBoolean(); }
        finally { if (previous == null) ADJUSTMENT.remove(); else ADJUSTMENT.set(previous); }
    }
    public static List<NativeDamageInput.Adjustment> takeAdjustments(LivingEntity target, DamageSource source) {
        var call = ADJUSTMENT.get();
        if (call == null || call.target() != target || call.source() != source) return List.of();
        ADJUSTMENT.remove();
        return List.of(call.value());
    }
    public boolean hurt(LivingEntity target, ServerLevel level, DamageSource source, float amount, BooleanSupplier nativeCall) {
        CombatExecutionScope.requireNativeWritePhase();
        var environment = DamageRequestSubmissionTracker.matchesActiveSubmission(target, source) ? java.util.Optional.<Boolean>empty()
            : io.github.naimjeg.combatnexus.effect.internal.EnvironmentEffects.pulse(target, source, amount);
        if (environment.isPresent()) return environment.get();
        if (!DamageSourcePolicy.shouldManage(source)) return nativeCall.getAsBoolean();
        try (var captures = io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.NativeDamageCaptures.open()) {
        DamageSettlementTracker.HurtScope scope;
        try { scope = DamageSettlementTracker.openHurt(target, level, source); }
        catch (DamageSettlementTracker.NativeRejected rejected) {
            return false;
        }
        DamageSettlementCompletion completion;
        boolean accepted;

        try {
            accepted = nativeCall.getAsBoolean();
            completion = scope.complete(accepted);
        } catch (RuntimeException | Error throwable) {
            completion = scope.abort();
            scope.close();
            captures.close();
            DamageSettlementCoordinator.handoff(completion, target, source);
            throw throwable;
        } finally {
            scope.close();
        }

        captures.close();
        DamageSettlementCoordinator.handoff(
                completion,
                target,
                source
        );
        return accepted && (completion == null || completion.snapshot().successfulHit());
        } finally { DamageSettlementCoordinator.drainIfSafe(); }
    }
}
