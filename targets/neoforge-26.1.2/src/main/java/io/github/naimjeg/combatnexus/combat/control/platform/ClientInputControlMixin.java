package io.github.naimjeg.combatnexus.combat.control.platform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(net.minecraft.client.player.ClientInput.class)
public abstract class ClientInputControlMixin {
    @Inject(method = "getMoveVector", at = @At("HEAD"), cancellable = true)
    private void combatnexus$walk(CallbackInfoReturnable<net.minecraft.world.phys.Vec2> ci) {
        if (!io.github.naimjeg.combatnexus.presentation.platform.client.CombatControlClient.allowed(
                io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction.WALK)) ci.setReturnValue(net.minecraft.world.phys.Vec2.ZERO);
    }
}
