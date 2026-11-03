package io.github.naimjeg.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.allay.Allay")
public abstract class AllayHealingSourceMixin {
    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/allay/Allay;heal(F)V"), require = 1)
    private void combatnexus$aiStep(net.minecraft.world.entity.animal.allay.Allay target, float amount, Operation<Void> original) {
        NativeHealingCalls.call(target, amount, original, Identifier.withDefaultNamespace("allay_regeneration"), HealingRequest.Reason.NATURAL_REGENERATION, target);
    }
}
