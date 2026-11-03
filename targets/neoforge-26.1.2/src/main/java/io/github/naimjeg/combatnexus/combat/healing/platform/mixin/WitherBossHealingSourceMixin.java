package io.github.naimjeg.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.boss.wither.WitherBoss")
public abstract class WitherBossHealingSourceMixin {
    @WrapOperation(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;heal(F)V"), require = 2)
    private void combatnexus$customServerAiStep(net.minecraft.world.entity.boss.wither.WitherBoss target, float amount, Operation<Void> original) {
        NativeHealingCalls.call(target, amount, original, Identifier.withDefaultNamespace("wither_regeneration"), HealingRequest.Reason.NATURAL_REGENERATION, target);
    }
}
