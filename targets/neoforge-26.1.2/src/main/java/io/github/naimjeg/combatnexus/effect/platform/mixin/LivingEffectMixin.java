package io.github.naimjeg.combatnexus.effect.platform.mixin;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.network.protocol.game.*;
import io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions;
import io.github.naimjeg.combatnexus.effect.api.EffectInstance;
import io.github.naimjeg.combatnexus.effect.platform.NativeEffectBridge;
import io.github.naimjeg.combatnexus.combat.api.platform.NativeEffectProjection;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingEffectMixin implements NativeEffectProjection {
    @Shadow @Final private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;
    @Shadow private boolean effectsDirty;
    @Shadow private void refreshDirtyAttributes() { throw new AssertionError(); }
    @Shadow protected abstract void onEffectAdded(MobEffectInstance effect, Entity source);
    @Shadow protected abstract void onEffectUpdated(MobEffectInstance effect, boolean refreshAttributes, Entity source);
    @Shadow protected abstract void onEffectsRemoved(Collection<MobEffectInstance> effects);
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = {"onEffectAdded", "onEffectUpdated"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;addAttributeModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeMap;I)V"))
    private void combatnexus$managedAttributes(MobEffect effect, net.minecraft.world.entity.ai.attributes.AttributeMap attributes, int amplifier,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (!NativeEffectBridge.managed(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect))) original.call(effect, attributes, amplifier);
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = {"onEffectUpdated", "onEffectsRemoved"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;removeAttributeModifiers(Lnet/minecraft/world/entity/ai/attributes/AttributeMap;)V"))
    private void combatnexus$managedAttributeRemoval(MobEffect effect, net.minecraft.world.entity.ai.attributes.AttributeMap attributes,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        if (!NativeEffectBridge.managed(net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect))) original.call(effect, attributes);
    }
    @Unique private LivingEntity combatnexus$self() { return (LivingEntity)(Object)this; }
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
    private void combatnexus$add(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> ci) {
        if (!combatnexus$self().level().isClientSide() && NativeEffectBridge.managed(effect.getEffect()))
            ci.setReturnValue(NativeEffectBridge.add(combatnexus$self(), effect, source, false));
    }
    @Inject(method = "forceAddEffect", at = @At("HEAD"), cancellable = true)
    private void combatnexus$force(MobEffectInstance effect, Entity source, CallbackInfo ci) {
        if (!combatnexus$self().level().isClientSide() && NativeEffectBridge.managed(effect.getEffect())) {
            NativeEffectBridge.add(combatnexus$self(), effect, source, true); ci.cancel();
        }
    }
    @Inject(method = "removeEffect", at = @At("HEAD"), cancellable = true)
    private void combatnexus$remove(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> ci) {
        if (NativeEffectBridge.ready(combatnexus$self()) && NativeEffectBridge.managed(effect))
            ci.setReturnValue(NativeEffectBridge.remove(combatnexus$self(), effect, true));
    }
    @Inject(method = "removeEffectNoUpdate", at = @At("HEAD"), cancellable = true)
    private void combatnexus$removeQuiet(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> ci) {
        if (NativeEffectBridge.ready(combatnexus$self()) && NativeEffectBridge.managed(effect)) {
            var old = activeEffects.get(effect);
            ci.setReturnValue(NativeEffectBridge.remove(combatnexus$self(), effect, false) ? old : null);
        }
    }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "removeAllEffects")
    private boolean combatnexus$clear(com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        boolean removed = false;
        if (NativeEffectBridge.ready(combatnexus$self()))
            for (var effect : List.copyOf(activeEffects.keySet()))
                if (NativeEffectBridge.managed(effect)) removed |= NativeEffectBridge.remove(combatnexus$self(), effect, true);
        return original.call() || removed;
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "removeAllEffects",
        at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;onEffectRemoved(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean combatnexus$nativeClear(LivingEntity entity, MobEffectInstance effect,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        // Managed removals already went through their owner. Preserve cancelled managed layers.
        return NativeEffectBridge.managed(effect.getEffect()) || original.call(entity, effect);
    }
    @Inject(method = "tickEffects", at = @At("HEAD"))
    private void combatnexus$project(CallbackInfo ci) { combatnexus$projectEffects(false); }
    @Inject(method = "baseTick", at = @At("HEAD"))
    private void combatnexus$sampleEnvironment(CallbackInfo ci) {
        if (NativeEffectBridge.ready(combatnexus$self())) {
            NativeEffectBridge.readyInputs(combatnexus$self());
            io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter.effects().sampleEnvironment(combatnexus$self());
        }
    }
    @Inject(method = "triggerOnDeathMobEffects", at = @At("HEAD"))
    private void combatnexus$deathCallbacks(ServerLevel level, Entity.RemovalReason reason, CallbackInfo ci) {
        NativeEffectBridge.removed(combatnexus$self(), reason);
    }
    @Inject(method = "hurtServer", at = @At("RETURN"))
    private void combatnexus$unprojectedHurt(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount, CallbackInfoReturnable<Boolean> ci) {
        if (ci.getReturnValue()) for (var effect : CombatWorldSessions.managedEffects(level, combatnexus$self().getUUID()))
            if (effect.display().nativeId() == null)
                io.github.naimjeg.combatnexus.effect.api.EffectTriggers.dispatch(combatnexus$self(), effect,
                    io.github.naimjeg.combatnexus.effect.api.EffectCapability.Trigger.HURT, source, amount);
    }
    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void combatnexus$discardLegacy(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        if (!combatnexus$self().level().isClientSide()) activeEffects.keySet().removeIf(NativeEffectBridge::managed);
    }
    @Inject(method = "getEffect", at = @At("RETURN"), cancellable = true)
    private void combatnexus$readEffect(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> ci) {
        if (!combatnexus$self().level().isClientSide() && NativeEffectBridge.managed(effect) && ci.getReturnValue() != null)
            ci.setReturnValue(new MobEffectInstance(ci.getReturnValue()));
    }
    @Override public void combatnexus$projectEffects(boolean synchronize) {
        var entity = combatnexus$self(); if (!(entity.level() instanceof ServerLevel level)) return;
        var desired = new HashMap<Holder<MobEffect>, MobEffectInstance>();
        for (EffectInstance effect : CombatWorldSessions.managedEffects(level, entity.getUUID())) if (effect.display().nativeId() != null) {
            var projected = NativeEffectBridge.projection(effect, level.getGameTime()); desired.put(projected.getEffect(), projected);
        }
        for (var type : List.copyOf(activeEffects.keySet())) if (NativeEffectBridge.managed(type) && !desired.containsKey(type)) {
            var removed = activeEffects.remove(type); effectsDirty = true;
            if (synchronize) onEffectsRemoved(List.of(removed));
            if (synchronize) level.getChunkSource().sendToTrackingPlayersAndSelf(entity,
                new ClientboundRemoveMobEffectPacket(entity.getId(), type));
        }
        desired.forEach((type, effect) -> {
            var old = activeEffects.put(type, effect);
            if (old == null || old.getAmplifier() != effect.getAmplifier() || old.isVisible() != effect.isVisible()) effectsDirty = true;
            if (synchronize) {
                var owner = NativeEffectBridge.state(entity, type).map(e -> e.display().owner()).map(level::getEntity).orElse(null);
                if (old == null) onEffectAdded(effect, owner); else onEffectUpdated(effect, false, owner);
                level.getChunkSource().sendToTrackingPlayersAndSelf(entity, new ClientboundUpdateMobEffectPacket(entity.getId(), effect, false));
            }
        });
        if (synchronize) refreshDirtyAttributes();
    }
}
