package io.github.naimjeg.combatnexus.combat.health.platform.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import io.github.naimjeg.combatnexus.combat.api.platform.NativeHealthTarget;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHealthAccessMixin implements NativeHealthTarget {
    @Shadow private boolean checkTotemDeathProtection(DamageSource source) { throw new AssertionError(); }
    @Shadow protected abstract SoundEvent getDeathSound();
    @Shadow protected abstract float getSoundVolume();
    @Shadow protected boolean dead;
    @org.spongepowered.asm.mixin.Unique private final java.util.List<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry> combatnexus$protectionWrites = new java.util.ArrayList<>();
    @Override public boolean combatnexus$nativeProtection(DamageSource source) { io.github.naimjeg.combatnexus.combat.api.transaction.CombatMutationGuard.requireNativeWritePhase(); return checkTotemDeathProtection(source); }
    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method="checkTotemDeathProtection")
    private boolean combatnexus$observeProtection(DamageSource source, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original) {
        combatnexus$protectionWrites.clear();
        var target = (LivingEntity)(Object)this;
        boolean protectedTarget = false, completed = false;
        try {
            protectedTarget = original.call(source);
            // A component found in the final hand is not proof that the use-totem event allowed it.
            protectedTarget = protectedTarget && combatnexus$protectionWrites.stream().anyMatch(entry ->
                entry.resource() == io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH
                    && entry.completed() && entry.after().isPresent() && entry.after().getAsDouble() > 0);
            completed = true;
            return protectedTarget;
        }
        finally {
            var evidence = java.util.List.copyOf(combatnexus$protectionWrites);
            io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter.nativeDamage()
                .protection(target, source, evidence, protectedTarget, completed);
        }
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="checkTotemDeathProtection", at=@org.spongepowered.asm.mixin.injection.At(value="INVOKE",target="Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"),require=1)
    private void combatnexus$protectionHealth(LivingEntity target, float amount, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        float before = target.getHealth(); boolean completed = false;
        try { original.call(target, amount); completed = true; }
        finally { combatnexus$protectionWrites.add(io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry.write(
            io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Resource.HEALTH, before, target.getHealth(), Math.abs(target.getHealth()-before), completed)); }
    }
    @Override public java.util.List<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry> combatnexus$protectionEvidence() { return java.util.List.copyOf(combatnexus$protectionWrites); }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method="checkTotemDeathProtection", at=@org.spongepowered.asm.mixin.injection.At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;shrink(I)V"),require=1)
    private void combatnexus$consume(net.minecraft.world.item.ItemStack stack,int amount,com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
        var source=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        int before=stack.getCount();boolean completed=false;
        try { original.call(stack,amount);completed=true; }
        finally { combatnexus$protectionWrites.add(io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry.write(
                io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Resource.PROTECTION,source,before,stack.getCount(),Math.max(0,before-stack.getCount()),completed)); }
    }
    @Override public boolean combatnexus$deathConfirmed() { return dead; }
    @Override public void combatnexus$deathSound() {
        var sound = getDeathSound();
        var entity = (LivingEntity)(Object)this;
        if (sound != null) entity.playSound(sound, getSoundVolume(), entity.getVoicePitch());
    }
}
