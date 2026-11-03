package io.github.naimjeg.combatnexus.effect.platform.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.damagesource.DamageSource;
import io.github.naimjeg.combatnexus.effect.platform.NativeEffectBridge;
import io.github.naimjeg.combatnexus.effect.api.EffectCapability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(MobEffectInstance.class)
public abstract class EffectInstanceMixin {
    @Inject(method = "tickServer", at = @At("HEAD"), cancellable = true)
    private void combatnexus$noNativeTimer(ServerLevel level, LivingEntity entity, Runnable update, CallbackInfoReturnable<Boolean> ci) {
        if (NativeEffectBridge.managed(((MobEffectInstance)(Object)this).getEffect())) ci.setReturnValue(true);
    }
    @Inject(method = "onMobHurt", at = @At("HEAD"), cancellable = true)
    private void combatnexus$hurt(ServerLevel level, LivingEntity entity, DamageSource source, float amount, CallbackInfo ci) {
        var type = ((MobEffectInstance)(Object)this).getEffect();
        if (NativeEffectBridge.managed(type)) {
            NativeEffectBridge.state(entity, type).ifPresent(e -> NativeEffectBridge.trigger(entity, e, EffectCapability.Trigger.HURT, source, amount));
            ci.cancel();
        }
    }
    @Inject(method = "onMobRemoved", at = @At("HEAD"), cancellable = true)
    private void combatnexus$removed(ServerLevel level, LivingEntity entity, Entity.RemovalReason reason, CallbackInfo ci) {
        if (NativeEffectBridge.managed(((MobEffectInstance)(Object)this).getEffect())) ci.cancel();
    }
}
