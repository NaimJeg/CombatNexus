package io.github.naimjeg.combatnexus.combat.platform;
import io.github.naimjeg.combatnexus.combat.api.platform.NativeHealingApplication;

import io.github.naimjeg.combatnexus.combat.healing.api.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.EventHooks;

/** Mirrors 26.1.2 heal: event first, then sample and write native health exactly once. */
public final class WorldHealingPort implements NativeHealingApplication {
    private final ServerLevel level;
    @FunctionalInterface public interface NativeHealthWriter { void write(LivingEntity target, float health); }
    private final NativeHealthWriter writer;
    public WorldHealingPort(ServerLevel level) { this(level, LivingEntity::setHealth); }
    public WorldHealingPort(ServerLevel level, NativeHealthWriter writer) {
        this.level = java.util.Objects.requireNonNull(level);
        this.writer = java.util.Objects.requireNonNull(writer);
    }
    @Override public LivingEntity target(HealingRequest request) {
        return level.getEntity(request.target()) instanceof LivingEntity living && !living.isRemoved() ? living : null;
    }
    @Override public Receipt apply(HealingRequest request, double amount) {
        var target = target(request);
        if (target == null) return Receipt.skipped(State.REJECTED, 0);
        if (!target.isAlive()) return Receipt.skipped(State.DEAD_TARGET, 0);
        if (amount == 0) return Receipt.skipped(State.ZERO_AMOUNT, 0);
        float accepted = EventHooks.onLivingHeal(target, (float) Math.min(amount, Float.MAX_VALUE));
        if (!Float.isFinite(accepted) || accepted <= 0) return Receipt.skipped(State.CANCELLED, 0);
        if (!target.isAlive() || target.isRemoved() || target.level() != level) {
            return Receipt.skipped(State.DEAD_TARGET, accepted);
        }
        // Sampling after callbacks excludes nested heals performed by event subscribers.
        float before = target.getHealth();
        float maximum = target.getMaxHealth();
        float next = (float) Math.min(maximum, (double) before + accepted);
        var write = EquipmentCombatAdapter.nativeDamage().observeHealthWrite(target, () -> writer.write(target, next));
        return new Receipt(write.uncontended() ? State.WRITTEN : State.UNKNOWN_WRITE,
            accepted, before, maximum, target.getHealth(), write.completed() && write.uncontended());
    }
}
