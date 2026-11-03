package io.github.naimjeg.combatnexus.combat.damage.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observe the actual superclass argument, preserving vanilla's reduction exactly once. */
@Mixin(Armadillo.class)
public abstract class ArmadilloDamageMixin {
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void combatnexus$input(ServerLevel level, DamageSource source, float amount,
            CallbackInfoReturnable<Boolean> callback, @Share("incoming") LocalFloatRef incoming) {
        incoming.set(amount);
    }
    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/entity/animal/Animal;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean combatnexus$adjustment(Armadillo target, ServerLevel level, DamageSource source, float amount,
            Operation<Boolean> original, @Share("incoming") LocalFloatRef incoming) {
        if (incoming.get() == amount) return original.call(target, level, source, amount);
        return EquipmentCombatAdapter.nativeDamage().adjusted(target, source,
            new NativeDamageInput.Adjustment(Identifier.parse("minecraft:armadillo"), incoming.get(), amount),
            () -> original.call(target, level, source, amount));
    }
}
