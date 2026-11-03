package io.github.naimjeg.combatnexus.combat.healing.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.healing.platform.NativeHealingCalls;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.server.level.ServerPlayer")
public abstract class ServerPlayerHealingSourceMixin {
    @WrapOperation(method = "tickRegeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"), require = 1)
    private void combatnexus$tickRegeneration(net.minecraft.server.level.ServerPlayer target, float amount, Operation<Void> original) {
        NativeHealingCalls.call(target, amount, original, Identifier.withDefaultNamespace("peaceful_regeneration"), HealingRequest.Reason.NATURAL_REGENERATION, target);
    }
}
