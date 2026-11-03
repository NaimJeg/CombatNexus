package io.github.naimjeg.combatnexus.combat.health.platform.mixin;
import io.github.naimjeg.combatnexus.combat.projectile.spi.NativeArrow;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(AbstractArrow.class)
public interface ArrowAccessMixin extends NativeArrow {
    @Accessor("baseDamage") double combatnexus$baseDamage();
}
