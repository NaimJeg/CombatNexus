package io.github.naimjeg.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.animal.equine.Llama")
public abstract class LlamaHealingSourceMixin {
    @WrapOperation(method = "handleEating", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/equine/Llama;heal(F)V"), require = 1)
    private void combatnexus$handleEating(net.minecraft.world.entity.animal.equine.Llama target, float amount, Operation<Void> original, net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack item) {
        NativeHealingCalls.call(target, amount, original, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item.getItem()), HealingRequest.Reason.ITEM, player);
    }
}
