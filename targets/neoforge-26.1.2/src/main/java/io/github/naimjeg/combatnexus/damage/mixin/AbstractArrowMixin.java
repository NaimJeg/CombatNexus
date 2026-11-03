package io.github.naimjeg.combatnexus.damage.mixin;

import io.github.naimjeg.combatnexus.damage.bridge.vanilla.ProjectileDamageCapture;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin {

    @Shadow
    private double baseDamage;
    @Unique
    private int combatnexus$preCritDamage = -1;
    @Unique
    private float combatnexus$postEnchantDamage = 0.0f;

    @Shadow
    public abstract @Nullable ItemStack getWeaponItem();

    @Shadow
    public abstract boolean isCritArrow();

    @Inject(
            method = "onHitEntity",
            at = @At("HEAD")
    )
    private void combatnexus$resetProjectileCaptureState(
            EntityHitResult hitResult,
            CallbackInfo ci
    ) {
        this.combatnexus$preCritDamage = -1;
        this.combatnexus$postEnchantDamage = 0.0f;
    }

    @Inject(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/projectile/arrow/AbstractArrow;isCritArrow()Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void combatnexus$capturePreProjectileCrit(
            EntityHitResult hitResult,
            CallbackInfo ci,
            Entity entity,
            float pow,
            double arrowDamage,
            Entity currentOwner,
            DamageSource damageSource,
            int damage
    ) {
        this.combatnexus$preCritDamage = damage;
        this.combatnexus$postEnchantDamage = (float) arrowDamage;
    }

    @Inject(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )
    private void combatnexus$captureFinalProjectileDamage(
            EntityHitResult hitResult,
            CallbackInfo ci,
            Entity entity,
            float pow,
            double arrowDamage,
            Entity currentOwner,
            DamageSource damageSource,
            int damage,
            boolean isEnderman,
            int remainingFireTicks
    ) {
        int preCritDamage =
                this.combatnexus$preCritDamage >= 0
                        ? this.combatnexus$preCritDamage
                        : damage;

        float postEnchantDamage =
                this.combatnexus$postEnchantDamage > 0.0f
                        ? this.combatnexus$postEnchantDamage
                        : (float) arrowDamage;

        ProjectileDamageCapture.capture(
                (AbstractArrow) (Object) this,
                entity,
                damageSource,
                this.getWeaponItem(),
                (float) this.baseDamage,
                postEnchantDamage,
                preCritDamage,
                damage,
                this.isCritArrow()
        );

        this.combatnexus$preCritDamage = -1;
        this.combatnexus$postEnchantDamage = 0.0f;
    }

    @Inject(
            method = "onHitEntity",
            at = @At("RETURN")
    )
    private void combatnexus$clearProjectileCaptureState(
            EntityHitResult hitResult,
            CallbackInfo ci
    ) {
        this.combatnexus$preCritDamage = -1;
        this.combatnexus$postEnchantDamage = 0.0f;
    }
}
