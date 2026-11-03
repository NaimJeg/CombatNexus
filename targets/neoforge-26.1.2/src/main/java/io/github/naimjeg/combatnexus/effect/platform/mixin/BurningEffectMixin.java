package io.github.naimjeg.combatnexus.effect.platform.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import io.github.naimjeg.combatnexus.effect.platform.NativeEffectBridge;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(Entity.class)
public abstract class BurningEffectMixin {
    @Shadow private int remainingFireTicks;
    @Unique private static final Identifier combatnexus$burning = Identifier.parse("combatnexus:burning");
    @Inject(method = "setRemainingFireTicks", at = @At("HEAD"))
    private void combatnexus$duration(int ticks, CallbackInfo ci) {
        if ((Object)this instanceof LivingEntity entity && NativeEffectBridge.ready(entity))
            EquipmentCombatAdapter.effects().duration(entity, combatnexus$burning, ticks);
    }
    @Inject(method = "getRemainingFireTicks", at = @At("HEAD"), cancellable = true)
    private void combatnexus$readDuration(CallbackInfoReturnable<Integer> ci) {
        if ((Object)this instanceof LivingEntity entity && NativeEffectBridge.ready(entity)) {
            int value = EquipmentCombatAdapter.effects().remaining(entity, combatnexus$burning);
            ci.setReturnValue(value > 0 ? value : Math.min(0, remainingFireTicks));
        }
    }
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void combatnexus$projectFire(CallbackInfo ci) {
        if ((Object)this instanceof LivingEntity entity && NativeEffectBridge.ready(entity)) {
            int value = EquipmentCombatAdapter.effects().remaining(entity, combatnexus$burning);
            remainingFireTicks = value > 0 ? value : Math.min(0, remainingFireTicks);
        }
    }
    @WrapOperation(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setRemainingFireTicks(I)V"))
    private void combatnexus$noSecondTimer(Entity entity, int ticks, Operation<Void> original) {
        if (!(entity instanceof LivingEntity living) || !NativeEffectBridge.ready(living)) original.call(entity, ticks);
    }
    @Inject(method = "load", at = @At("RETURN"))
    private void combatnexus$noLegacyFire(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        if ((Object)this instanceof LivingEntity) remainingFireTicks = Math.min(0, remainingFireTicks);
    }
}
