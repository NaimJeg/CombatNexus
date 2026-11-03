package io.github.naimjeg.combatnexus.effect.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Arrow.class)
public abstract class ArrowEffectOriginMixin {
    @WrapMethod(method = "doPostHurtEffects")
    private void combatnexus$origin(LivingEntity target, Operation<Void> original) {
        var arrow = (Arrow)(Object)this;
        var source = arrow.getEffectSource();
        var item = arrow.getPickupItemStackOrigin();
        var provenance = new SourceProvenance(source == null ? null : source.getUUID(),
            item.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(item.getItem()), null, null, null);
        EquipmentCombatAdapter.effectOrigins().invoke(target, provenance, () -> { original.call(target); return null; });
    }
}
