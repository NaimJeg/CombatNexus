package io.github.naimjeg.combatnexus.effect.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.ApplyMobEffect;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ApplyMobEffect.class)
public abstract class EnchantmentEffectOriginMixin {
    @WrapOperation(method = "apply", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean combatnexus$origin(LivingEntity target, MobEffectInstance effect, Operation<Boolean> original,
            ServerLevel level, int amplifier, EnchantedItemInUse item, Entity entity, Vec3 position) {
        var provenance = new SourceProvenance(item.owner() == null ? null : item.owner().getUUID(),
            BuiltInRegistries.ITEM.getKey(item.itemStack().getItem()), item.inSlot(), null, null);
        return EquipmentCombatAdapter.effectOrigins().invoke(target, provenance, () -> original.call(target, effect));
    }
}
