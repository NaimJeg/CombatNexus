package io.github.naimjeg.combatnexus.combat.health.platform.mixin;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingRequest;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import com.llamalad7.mixinextras.injector.wrapoperation.*;

@Mixin(EnderDragon.class)
public abstract class DragonCrystalHealingMixin {
    @Shadow public EndCrystal nearestCrystal;
    @WrapOperation(method="checkCrystals",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/boss/enderdragon/EnderDragon;setHealth(F)V"),require=1)
    private void combatnexus$crystal(EnderDragon dragon,float amount,Operation<Void> original) {
        if(!(dragon.level() instanceof ServerLevel level)) { original.call(dragon,amount);return; }
        var world=CombatAccess.find(level);
        if(world.isEmpty()) { original.call(dragon,amount);return; }
        world.get().healing().submit(new HealingRequest(UUID.randomUUID(),dragon.getUUID(),new CombatSource(Identifier.parse("minecraft:end_crystal"),
                Optional.ofNullable(nearestCrystal).map(EndCrystal::getUUID),Optional.empty()),HealingRequest.Reason.CRYSTAL,Math.max(0,amount-dragon.getHealth()),Optional.empty()));
    }
}
