package io.github.naimjeg.combatnexus.combat.api.platform;

import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.BooleanSupplier;

/** Startup-issued platform bridge. Native callbacks execute synchronously on the world's thread. */
public interface NativeDamageAccess {
    /** Marker installed by the required native lifecycle adapter. Grants no mutation authority. */
    interface LifecycleInstalled {}
    NativeAbsorptionStorage.State absorptionState(LivingEntity target);
    float absorptionMaximum(LivingEntity target, NativeAbsorptionStorage.State state, float original);
    void absorptionChanged(LivingEntity target, NativeAbsorptionStorage.State state);
    String saveAbsorption(LivingEntity target, NativeAbsorptionStorage.State state);
    void restoreAbsorption(LivingEntity target, NativeAbsorptionStorage.State state, String encoded);
    record HealthWrite(boolean completed, boolean uncontended, RuntimeException failure) {}
    HealthWrite observeHealthWrite(LivingEntity target, Runnable nativeWrite);
    void confirmedDeath(LivingEntity target, DamageSource source);
    void itemBlocked(LivingEntity target, DamageSource source, net.minecraft.resources.Identifier item, float amount);
    void partHit(net.minecraft.world.entity.Entity part, LivingEntity target, DamageSource source, boolean accepted);
    void protection(LivingEntity target, DamageSource source, java.util.List<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry> evidence, boolean protectedTarget, boolean completed);
    /** Whether this source participates in framework settlement; opt-out sources retain native ownership. */
    boolean managed(DamageSource source);
    boolean permitsHitEffects(LivingEntity target, DamageSource source);
    float penetratedArmor(LivingEntity target, DamageSource source, float armor);
    float consumeShields(LivingEntity target, DamageSource source, float amount);
    void writeHealth(LivingEntity target, DamageSource source, float requested, java.util.function.Consumer<Float> nativeWrite);
    void attack(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Entity target, Runnable nativeCall);
    boolean sweep(LivingEntity target, DamageSource source, BooleanSupplier nativeCall);
    <T> T attackOwnership(DamageSource source, java.util.function.Supplier<T> nativeCall);
    boolean independentCooldown(LivingEntity target, DamageSource source);
    boolean hurt(LivingEntity target, ServerLevel level, DamageSource source, float amount, BooleanSupplier nativeCall);
    /** Records an already-applied native adjustment; never applies the adjustment again. */
    boolean adjusted(LivingEntity target, DamageSource source, NativeDamageInput.Adjustment adjustment, BooleanSupplier nativeCall);
}
