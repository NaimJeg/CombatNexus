package io.github.naimjeg.combatnexus.combat.control.platform;
import io.github.naimjeg.combatnexus.combat.api.action.CombatActionGate;
import static io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Entity.class)
public abstract class EntityControlMixin {
    @Inject(method = "saveWithoutId", at = @At("HEAD"))
    private void combatnexus$save(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        if ((Object)this instanceof LivingEntity living) io.github.naimjeg.combatnexus.combat.platform.CombatPersistence.save(living);
    }

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void combatnexus$turn(double yaw, double pitch, CallbackInfo ci) {
        if ((Object)this instanceof LivingEntity living && living.isAddedToLevel() && !CombatActionGate.allows(living, TURN)) ci.cancel();
    }
    @Inject(method = "setYRot", at = @At("HEAD"), cancellable = true)
    private void combatnexus$yaw(float value, CallbackInfo ci) { if ((Object)this instanceof LivingEntity living && living.isAddedToLevel() && !CombatActionGate.allows(living, TURN)) ci.cancel(); }
    @Inject(method = "setXRot", at = @At("HEAD"), cancellable = true)
    private void combatnexus$pitch(float value, CallbackInfo ci) { if ((Object)this instanceof LivingEntity living && living.isAddedToLevel() && !CombatActionGate.allows(living, TURN)) ci.cancel(); }
}
