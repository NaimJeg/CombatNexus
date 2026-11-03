package io.github.naimjeg.combatnexus.effect.platform.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.*;
import io.github.naimjeg.combatnexus.effect.platform.NativeEffectBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.effect.HealOrHarmMobEffect")
public abstract class HealHarmEffectMixin {
    @Inject(method = "applyInstantenousEffect", at = @At("HEAD"), cancellable = true)
    private void combatnexus$instant(ServerLevel level, Entity direct, Entity owner, LivingEntity target, int amplifier, double intensity, CallbackInfo ci) {
        if (NativeEffectBridge.ready(target)) {
            NativeEffectBridge.instant(BuiltInRegistries.MOB_EFFECT.wrapAsHolder((MobEffect)(Object)this), target, direct, owner, amplifier, intensity); ci.cancel();
        }
    }
}
