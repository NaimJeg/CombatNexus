package io.github.naimjeg.combatnexus.combat.api.platform;

import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import io.github.naimjeg.combatnexus.effect.api.EffectService;
import io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementCallback;

/** Startup-issued lifecycle capability. Lookups never create sessions; controls require the world thread. */
public interface WorldLifecycleAccess {
    void projectileLeft(ServerLevel level, net.minecraft.world.entity.Entity projectile);
    void collision(ServerLevel level, net.minecraft.world.entity.projectile.Projectile projectile, net.minecraft.world.phys.HitResult hit, java.util.Optional<UUID> subject);
    void cloneState(net.minecraft.server.level.ServerPlayer original, net.minecraft.server.level.ServerPlayer replacement, boolean death);
    void changedDimension(net.minecraft.server.level.ServerPlayer player);
    void load(ServerLevel level, NativeWorldHooks hooks);
    boolean loaded(ServerLevel level);
    void beginTick(ServerLevel level);
    void tickRecovery(ServerLevel level);
    void clear(ServerLevel level, UUID target, EffectService.Removal reason);
    void close(ServerLevel level);
    String save(ServerLevel level, UUID target);
    void restore(ServerLevel level, UUID target, String saved);
    void trackEquipment(ServerLevel level, UUID target);
    void equipmentChanged(ServerLevel level, UUID target, EquipmentSlot slot, ItemStack stack);
    boolean heal(net.minecraft.world.entity.LivingEntity target, float amount,
        io.github.naimjeg.combatnexus.combat.healing.api.NativeHealingSource source);
    void damageSettled(DamageSettlementCallback callback);
}
