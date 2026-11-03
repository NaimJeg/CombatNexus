package io.github.naimjeg.combatnexus.combat.internal.platform;

import io.github.naimjeg.combatnexus.combat.api.platform.*;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.naimjeg.combatnexus.combat.spi.*;
import io.github.naimjeg.combatnexus.combat.healing.spi.HealingApplicationPort;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
import io.github.naimjeg.combatnexus.effect.api.*;
import io.github.naimjeg.combatnexus.effect.spi.EffectContributionPort;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

/** Converts platform notifications to internal, finally-scoped lifecycle permits. */
public final class PlatformWorldLifecycle implements WorldLifecycleAccess {
    public static final WorldLifecycleAccess INSTANCE = new PlatformWorldLifecycle();
    private PlatformWorldLifecycle() {}
    private static void thread(ServerLevel level) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("World lifecycle requires the server thread");
    }
    private static void lifecycleInput(ServerLevel level) {
        thread(level);
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatReadScope.requireWritable();
        if (io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.isCurrentPhase(
                io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.Phase.COMPUTE))
            throw new IllegalStateException("Native lifecycle notifications are forbidden during computation");
    }
    public static CombatRuntime existing(ServerLevel level) {
        thread(level);
        return CombatServerRuntime.existing(level.getServer()).flatMap(server -> server.world(level))
            .orElseThrow(() -> new IllegalStateException("World combat session is not loaded"));
    }
    public boolean loaded(ServerLevel level) {
        thread(level);
        return CombatServerRuntime.existing(level.getServer()).flatMap(server -> server.world(level)).isPresent();
    }
    public void load(ServerLevel level, NativeWorldHooks hooks) {
        thread(level);
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireWorldWritePhase();
        if (loaded(level)) return;
        var nativeHealing = java.util.Objects.requireNonNull(hooks.healing());
        var healing = healingApplication(nativeHealing);
        var attributes = new io.github.naimjeg.combatnexus.attribute.internal.EffectAttributeContributions(level);
        var effects = new EffectContributionPort() {
            public Prepared prepare(EffectService.Change change) { return attributes.prepare(change); }
            public boolean mayExpire(EffectInstance effect) { return hooks.mayExpire(effect); }
            public void committed(EffectService.Change change) { hooks.effectCommitted(change); }
        };
        new CombatRuntime(level, healing, id -> {
            var entity = level.getEntity(id);
            return new io.github.naimjeg.combatnexus.combat.regeneration.spi.RegenerationConditions.State(
                entity instanceof LivingEntity living && living.isAlive(),
                level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.NATURAL_HEALTH_REGENERATION));
        }, effect -> false, effects, 10, 256);
    }
    public static HealingApplicationPort healingApplication(NativeHealingApplication nativeHealing) {
        return new HealingApplicationPort() {
            public LivingEntity target(HealingRequest request) { return nativeHealing.target(request); }
            public Receipt apply(HealingRequest request, double amount) {
                var receipt = nativeHealing.apply(request, amount);
                return new Receipt(State.valueOf(receipt.state().name()), receipt.acceptedAmount(), receipt.healthBefore(),
                    receipt.maximumHealth(), receipt.healthAfter(), receipt.completed());
            }
        };
    }
    public void beginTick(ServerLevel level) { lifecycleInput(level); CombatWorldLifecycleAccess.beginTick(existing(level)); }
    public void tickRecovery(ServerLevel level) { lifecycleInput(level); CombatWorldLifecycleAccess.tickRecovery(existing(level)); }
    public String save(ServerLevel level, UUID target) { lifecycleInput(level); return CombatWorldLifecycleAccess.save(existing(level), target); }
    public void restore(ServerLevel level, UUID target, String saved) { lifecycleInput(level); CombatWorldLifecycleAccess.restore(existing(level), target, saved); }
    public void clear(ServerLevel level, UUID target, EffectService.Removal reason) {
        lifecycleInput(level);
        if (loaded(level)) CombatWorldLifecycleAccess.clear(existing(level), target, reason);
    }
    public void close(ServerLevel level) {
        lifecycleInput(level);
        if (loaded(level)) CombatWorldLifecycleAccess.close(existing(level));
    }
    public void trackEquipment(ServerLevel level, UUID target) { CombatEquipmentAccess.track(existing(level), target); }
    public void equipmentChanged(ServerLevel level, UUID target, EquipmentSlot slot, ItemStack stack) {
        CombatEquipmentAccess.changed(existing(level), target, slot, stack);
    }
    public boolean heal(LivingEntity target, float amount, io.github.naimjeg.combatnexus.combat.healing.api.NativeHealingSource source) {
        if (!(target.level() instanceof ServerLevel level)) return false;
        thread(level);
        if (NativeHealingAccess.route() == NativeHealingAccess.Route.REJECTED_PHASE)
            throw new IllegalStateException("Native healing is forbidden during combat computation/submission");
        if (!Float.isFinite(amount) || amount <= 0) return true;
        if (!target.isAddedToLevel() && !target.isRemoved()) return false;
        NativeHealingAccess.submit(existing(level), target, amount, source);
        return true;
    }
    public void damageSettled(io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementCallback callback) {
        CombatBehaviorAccess.damage(existing(callback.snapshot().level()), callback);
    }
    public void projectileLeft(ServerLevel level, net.minecraft.world.entity.Entity projectile) {
        lifecycleInput(level);
        io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileFacts.launch(projectile).ifPresent(launch->{
            io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer()).flatMap(k->k.world(level)).ifPresent(w->w.decisionSamples().close(launch.action()));
            if(level.getEntity(launch.shooter().entity()) instanceof io.github.naimjeg.combatnexus.combat.state.spi.NativeRuntimeStates states)
                states.combatnexus$states().clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.ACTION_FINISHED,java.util.Optional.of(launch.action()));
        });
    }
    public void collision(ServerLevel level, net.minecraft.world.entity.projectile.Projectile projectile, net.minecraft.world.phys.HitResult hit, java.util.Optional<UUID> subject) {
        lifecycleInput(level);
        if (projectile.level() != level) throw new IllegalArgumentException("Projectile world mismatch");
        io.github.naimjeg.combatnexus.combat.projectile.api.ProjectileFacts.launch(projectile).ifPresent(launch->{
            var direct=hit instanceof net.minecraft.world.phys.EntityHitResult entityHit?java.util.Optional.of(entityHit.getEntity().getUUID()):java.util.Optional.<java.util.UUID>empty();
            io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(level,new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.Collision(launch,hit.getLocation(),direct,subject));
        });
    }
    public void cloneState(net.minecraft.server.level.ServerPlayer original, net.minecraft.server.level.ServerPlayer replacement, boolean death) {
        lifecycleInput(replacement.level());
        if (original.level().getServer() != replacement.level().getServer()) throw new IllegalArgumentException("Player server mismatch");
        var originalState=(io.github.naimjeg.combatnexus.combat.state.spi.NativeCooldownTarget)original;
        var replacementState=(io.github.naimjeg.combatnexus.combat.state.spi.NativeCooldownTarget)replacement;
        originalState.combatnexus$cooldowns().forEach((id, stamp) -> {
            if(!death || stamp.deathPolicy()==io.github.naimjeg.combatnexus.combat.state.api.CooldownStamp.DeathPolicy.RETAIN)
                replacementState.combatnexus$cooldowns().put(id,stamp);
        });
        var states=((io.github.naimjeg.combatnexus.combat.state.spi.NativeRuntimeStates)replacement).combatnexus$states();
        states.restore(((io.github.naimjeg.combatnexus.combat.state.spi.NativeRuntimeStates)original).combatnexus$states().encode(false));
        states.clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.RESPAWN,java.util.Optional.empty());
        if(death)states.clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.DEATH,java.util.Optional.empty());
        var schedules=((io.github.naimjeg.combatnexus.combat.state.spi.NativeSchedules)replacement).combatnexus$schedules();
        schedules.restore(((io.github.naimjeg.combatnexus.combat.state.spi.NativeSchedules)original).combatnexus$schedules().encode(false));
        schedules.clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.RESPAWN,java.util.Optional.empty());
        if(death)schedules.clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.DEATH,java.util.Optional.empty());
    }
    public void changedDimension(net.minecraft.server.level.ServerPlayer player) {
        lifecycleInput(player.level());
        ((io.github.naimjeg.combatnexus.combat.state.spi.NativeRuntimeStates)player).combatnexus$states().clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.DIMENSION_CHANGE,java.util.Optional.empty());
        ((io.github.naimjeg.combatnexus.combat.state.spi.NativeSchedules)player).combatnexus$schedules().clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.DIMENSION_CHANGE,java.util.Optional.empty());
    }

}
