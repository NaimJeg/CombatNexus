package io.github.naimjeg.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.golem.IronGolem")
public abstract class IronGolemHealingSourceMixin {
    @WrapOperation(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/golem/IronGolem;heal(F)V"), require = 1)
    private void combatnexus$mobInteract(net.minecraft.world.entity.animal.golem.IronGolem target, float amount, Operation<Void> original, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        NativeHealingCalls.call(target, amount, original, Identifier.withDefaultNamespace("iron_ingot"), HealingRequest.Reason.ITEM, player);
    }
}
