package io.github.naimjeg.combatnexus.combat.health.platform.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import io.github.naimjeg.combatnexus.combat.api.platform.NativeAbsorptionStorage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAbsorptionMixin implements NativeAbsorptionStorage {
    @Unique private NativeAbsorptionStorage.State combatnexus$absorption;
    @Override public NativeAbsorptionStorage.State combatnexus$absorptionState() {
        if (combatnexus$absorption == null) combatnexus$absorption = EquipmentCombatAdapter.nativeDamage().absorptionState((LivingEntity)(Object)this);
        return combatnexus$absorption;
    }
    @ModifyReturnValue(method = "getMaxAbsorption()F", at = @At("RETURN"), require = 1)
    private float combatnexus$maximum(float original) {
        var entity = (LivingEntity)(Object)this;
        if (entity.level().isClientSide()) return Math.max(original, entity.getAbsorptionAmount());
        return combatnexus$absorption == null ? original : EquipmentCombatAdapter.nativeDamage().absorptionMaximum(entity, combatnexus$absorption, original);
    }
    @Inject(method = "setAbsorptionAmount(F)V", at = @At("TAIL"), require = 1)
    private void combatnexus$consumed(float amount, CallbackInfo callback) {
        if (combatnexus$absorption != null) EquipmentCombatAdapter.nativeDamage().absorptionChanged((LivingEntity)(Object)this, combatnexus$absorption);
    }
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 1)
    private void combatnexus$save(ValueOutput output, CallbackInfo callback) {
        if (combatnexus$absorption != null) output.putString("combatnexus_absorption",
            EquipmentCombatAdapter.nativeDamage().saveAbsorption((LivingEntity)(Object)this, combatnexus$absorption));
    }
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"), require = 1)
    private void combatnexus$restore(ValueInput input, CallbackInfo callback) {
        combatnexus$absorption = EquipmentCombatAdapter.nativeDamage().absorptionState((LivingEntity)(Object)this);
        EquipmentCombatAdapter.nativeDamage().restoreAbsorption((LivingEntity)(Object)this, combatnexus$absorption,
            input.getString("combatnexus_absorption").orElse("[]"));
    }
}
