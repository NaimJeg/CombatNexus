package io.github.naimjeg.combatnexus.effect.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Consumable.class)
public abstract class ConsumableOriginMixin {
    @WrapMethod(method = "onConsume")
    private ItemStack combatnexus$origin(Level level, LivingEntity user, ItemStack stack, Operation<ItemStack> original) {
        if (level.isClientSide()) return original.call(level, user, stack);
        var provenance = new SourceProvenance(user.getUUID(), BuiltInRegistries.ITEM.getKey(stack.getItem()), null, null, null);
        return EquipmentCombatAdapter.effectOrigins().invoke(user, provenance, () -> original.call(level, user, stack));
    }
}
