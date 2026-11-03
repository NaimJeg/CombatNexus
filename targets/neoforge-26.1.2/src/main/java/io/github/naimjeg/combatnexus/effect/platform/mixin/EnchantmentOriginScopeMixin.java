package io.github.naimjeg.combatnexus.effect.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Capture the holder selected by vanilla iteration, including gameplay-only enchantments. */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentOriginScopeMixin {
    @WrapOperation(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;)V", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentVisitor;accept(Lnet/minecraft/core/Holder;I)V"))
    private static void combatnexus$item(EnchantmentHelper.EnchantmentVisitor visitor, Holder<Enchantment> enchantment,
            int amplifier, Operation<Void> original) {
        EquipmentCombatAdapter.effectOrigins().enchantment(enchantment.unwrapKey().map(key -> key.identifier()).orElse(null),
            () -> original.call(visitor, enchantment, amplifier));
    }
    @WrapOperation(method = "runIterationOnItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;)V", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/item/enchantment/EnchantmentHelper$EnchantmentInSlotVisitor;accept(Lnet/minecraft/core/Holder;ILnet/minecraft/world/item/enchantment/EnchantedItemInUse;)V"))
    private static void combatnexus$slot(EnchantmentHelper.EnchantmentInSlotVisitor visitor, Holder<Enchantment> enchantment,
            int amplifier, EnchantedItemInUse item, Operation<Void> original) {
        EquipmentCombatAdapter.effectOrigins().enchantment(enchantment.unwrapKey().map(key -> key.identifier()).orElse(null),
            () -> original.call(visitor, enchantment, amplifier, item));
    }
}
