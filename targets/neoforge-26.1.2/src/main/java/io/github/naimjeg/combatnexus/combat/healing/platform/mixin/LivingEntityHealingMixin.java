package io.github.naimjeg.combatnexus.combat.healing.platform.mixin;

import io.github.naimjeg.combatnexus.combat.healing.platform.NativeHealingAdapter;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealingMixin {
    @Inject(method = {"setHealth", "setAbsorptionAmount"}, at = @At("HEAD"), require = 1)
    private void combatnexus$guardHealth(float value, CallbackInfo ci) {
        io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireNativeWritePhase();
    }
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true, require = 1)
    private void combatnexus$heal(float amount, CallbackInfo ci) {
        if (NativeHealingAdapter.intercept((LivingEntity) (Object) this, amount)) ci.cancel();
    }
}
