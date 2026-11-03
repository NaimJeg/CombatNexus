package io.github.naimjeg.combatnexus.combat.damage.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.platform.DamageApplicationAdapter;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityApplicationMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method="stabAttack", at={
        @At(value="INVOKE", target="Lnet/minecraft/world/entity/LivingEntity;causeExtraKnockback(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/phys/Vec3;)V"),
        @At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;stopRiding()V"),
        @At(value="INVOKE", target="Lnet/minecraft/world/item/ItemStack;hurtEnemy(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z")}, cancellable=true, require=3)
    private void combatnexus$rejectedStab(net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.entity.Entity target,
            float baseDamage, boolean dealsDamage, boolean dealsKnockback, boolean dismounts,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> callback,
            @com.llamalad7.mixinextras.sugar.Local(name="dealtDamage") boolean hit) {
        if (dealsDamage && !hit) callback.setReturnValue(false);
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"), require = 1)
    private void combatnexus$hitKnockback(LivingEntity target, double strength, double x, double z,
            Operation<Void> original, ServerLevel level, DamageSource source, float amount) {
        if (EquipmentCombatAdapter.nativeDamage().permitsHitEffects(target, source)) original.call(target, strength, x, z);
    }
    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/effect/MobEffectInstance;onMobHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/damagesource/DamageSource;F)V"), require = 1)
    private void combatnexus$hitEffect(net.minecraft.world.effect.MobEffectInstance effect, ServerLevel level,
            LivingEntity target, DamageSource source, float amount, Operation<Void> original) {
        if (EquipmentCombatAdapter.nativeDamage().permitsHitEffects(target, source)) original.call(effect, level, target, source, amount);
    }

    @WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"), require = 1)
    private float combatnexus$penetration(LivingEntity target, float amount, DamageSource source, float armor,
                                         float toughness, Operation<Float> original) {
        return original.call(target, amount, source, EquipmentCombatAdapter.nativeDamage().penetratedArmor(target, source, armor), toughness);
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target =
            "Lnet/neoforged/neoforge/common/CommonHooks;onLivingDamagePre(Lnet/minecraft/world/entity/LivingEntity;Lnet/neoforged/neoforge/common/damagesource/DamageContainer;)F"), require = 1)
    private float combatnexus$shieldBoundary(LivingEntity target, DamageContainer container, Operation<Float> original) {
        return DamageApplicationAdapter.consumeShields(target, container, original.call(target, container));
    }

    @WrapOperation(method = "actuallyHurt", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"), require = 1)
    private void combatnexus$healthBoundary(LivingEntity target, float value, Operation<Void> original,
                                          ServerLevel level, DamageSource source, float amount) {
        EquipmentCombatAdapter.nativeDamage().writeHealth(target, source, value, next -> original.call(target, next));
    }
}
