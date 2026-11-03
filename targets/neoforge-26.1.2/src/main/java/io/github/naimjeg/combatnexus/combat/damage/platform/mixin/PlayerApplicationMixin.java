package io.github.naimjeg.combatnexus.combat.damage.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.platform.DamageApplicationAdapter;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class PlayerApplicationMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method="stabAttack", at={
        @At(value="INVOKE", target="Lnet/minecraft/world/entity/player/Player;causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;)V"),
        @At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;stopRiding()V"),
        @At(value="INVOKE", target="Lnet/minecraft/world/entity/player/Player;itemAttackInteraction(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/damagesource/DamageSource;Z)V")}, cancellable=true, require=3)
    private void combatnexus$rejectedStab(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.entity.Entity target,
            float baseDamage, boolean dealsDamage, boolean dealsKnockback, boolean dismounts,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> callback,
            @com.llamalad7.mixinextras.sugar.Local(name="wasHurt") boolean hit) {
        if (dealsDamage && !hit) callback.setReturnValue(false);
    }

    @WrapOperation(method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;FLnet/minecraft/world/phys/AABB;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"), require = 1)
    private boolean combatnexus$sweep(LivingEntity target, ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        return EquipmentCombatAdapter.nativeDamage().sweep(target, source, () -> original.call(target, level, source, amount));
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "attack")
    private void combatnexus$action(net.minecraft.world.entity.Entity target, Operation<Void> original) {
        EquipmentCombatAdapter.nativeDamage().attack((Player)(Object)this, target, () -> original.call(target));
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/neoforge/common/CommonHooks;fireCriticalHit(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;ZF)Lnet/neoforged/neoforge/event/entity/player/CriticalHitEvent;"), require = 1)
    private net.neoforged.neoforge.event.entity.player.CriticalHitEvent combatnexus$critOwnership(
            Player player, net.minecraft.world.entity.Entity target, boolean critical, float multiplier,
            Operation<net.neoforged.neoforge.event.entity.player.CriticalHitEvent> original,
            @com.llamalad7.mixinextras.sugar.Local DamageSource source) {
        return EquipmentCombatAdapter.nativeDamage().attackOwnership(source, () -> original.call(player, target, critical, multiplier));
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F"), require = 1)
    private float combatnexus$shieldBoundary(LivingEntity target, DamageContainer container, Operation<Float> original) {
        return DamageApplicationAdapter.consumeShields(target, container, original.call(target, container));
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"), require = 1)
    private void combatnexus$healthBoundary(Player target, float value, Operation<Void> original,
                                          ServerLevel level, DamageSource source, float amount) {
        EquipmentCombatAdapter.nativeDamage().writeHealth(target, source, value, next -> original.call(target, next));
    }
}
