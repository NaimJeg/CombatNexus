package io.github.naimjeg.combatnexus.effect.platform.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance;
import io.github.naimjeg.combatnexus.combat.platform.EquipmentCombatAdapter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconOriginMixin {
    @WrapOperation(method = "applyEffects", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean combatnexus$origin(Player target, MobEffectInstance effect, Operation<Boolean> original,
            Level level, BlockPos pos, int levels, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> primary, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> secondary) {
        var provenance = new SourceProvenance(null, null, null, null,
            new SourceProvenance.Block(level.dimension().identifier(), pos.getX(), pos.getY(), pos.getZ()));
        return EquipmentCombatAdapter.effectOrigins().invoke(target, provenance, () -> original.call(target, effect));
    }
}
