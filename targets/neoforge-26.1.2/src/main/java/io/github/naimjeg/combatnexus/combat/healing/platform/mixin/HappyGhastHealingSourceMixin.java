package io.github.naimjeg.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.happyghast.HappyGhast")
public abstract class HappyGhastHealingSourceMixin {
    @WrapOperation(method = "continuousHeal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/happyghast/HappyGhast;heal(F)V"), require = 1)
    private void combatnexus$continuousHeal(net.minecraft.world.entity.animal.happyghast.HappyGhast target, float amount, Operation<Void> original) {
        NativeHealingCalls.call(target, amount, original, Identifier.withDefaultNamespace("happy_ghast_regeneration"), HealingRequest.Reason.NATURAL_REGENERATION, target);
    }
}
