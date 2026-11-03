package io.github.naimjeg.combatnexus.combat.damage.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;

/** Ensures every LivingEntity hurt scope has a finally-backed completion. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageSettlementMixin
        implements io.github.naimjeg.combatnexus.combat.api.platform.NativeDamageAccess.LifecycleInstalled {
    @org.spongepowered.asm.mixin.Unique private boolean combatnexus$independentCooldown;

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "hurtServer", at = @org.spongepowered.asm.mixin.injection.At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = org.objectweb.asm.Opcodes.GETFIELD))
    private int combatnexus$readCooldown(LivingEntity entity, Operation<Integer> original) {
        return combatnexus$independentCooldown ? 0 : original.call(entity);
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "hurtServer", at = @org.spongepowered.asm.mixin.injection.At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;invulnerableTime:I", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void combatnexus$writeCooldown(LivingEntity entity, int value, Operation<Void> original) {
        if (!combatnexus$independentCooldown) original.call(entity, value);
    }
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "hurtServer", at = @org.spongepowered.asm.mixin.injection.At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;lastHurt:F", opcode = org.objectweb.asm.Opcodes.PUTFIELD))
    private void combatnexus$writeLastHurt(LivingEntity entity, float value, Operation<Void> original) {
        if (!combatnexus$independentCooldown) original.call(entity, value);
    }

    @WrapMethod(method = "hurtServer(Lnet/minecraft/server/level/"
            + "ServerLevel;Lnet/minecraft/world/damagesource/"
            + "DamageSource;F)Z")
    private boolean combatnexus$trackSettlementLifecycle(
            ServerLevel level,
            DamageSource source,
            float amount,
            Operation<Boolean> original
    ) {
        boolean previous = combatnexus$independentCooldown;
        combatnexus$independentCooldown = EquipmentCombatAdapter.nativeDamage().independentCooldown((LivingEntity)(Object)this, source);
        try { return combatnexus$settle(level, source, amount, original); }
        finally { combatnexus$independentCooldown = previous; }
    }
    @org.spongepowered.asm.mixin.Unique
    private boolean combatnexus$settle(ServerLevel level, DamageSource source, float amount, Operation<Boolean> original) {
        return EquipmentCombatAdapter.nativeDamage().hurt((LivingEntity)(Object)this, level, source, amount,
            () -> original.call(level, source, amount));
    }
}
