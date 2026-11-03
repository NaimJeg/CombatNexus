package io.github.naimjeg.combatnexus.combat.control.platform;
import io.github.naimjeg.combatnexus.combat.api.action.CombatActionGate;
import static io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LivingEntity.class)
public abstract class LivingControlMixin {
    @Inject(method = {"setYHeadRot", "setYBodyRot"}, at = @At("HEAD"), cancellable = true)
    private void combatnexus$look(float rotation, CallbackInfo ci) {
        var entity = (LivingEntity)(Object)this;
        if (entity.isAddedToLevel() && !CombatActionGate.allows(entity, TURN)) ci.cancel();
    }
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private net.minecraft.world.phys.Vec3 combatnexus$walk(net.minecraft.world.phys.Vec3 input) {
        return CombatActionGate.allows((LivingEntity)(Object)this, WALK) ? input : net.minecraft.world.phys.Vec3.ZERO;
    }
    @Inject(method = "jumpFromGround", at = @At("HEAD"), cancellable = true)
    private void combatnexus$jump(CallbackInfo ci) { if (!CombatActionGate.allows((LivingEntity)(Object)this, JUMP)) ci.cancel(); }
    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void combatnexus$knockback(double power, double x, double z, CallbackInfo ci) {
        if (!CombatActionGate.allows((LivingEntity)(Object)this, EXTERNAL_MOVEMENT)) ci.cancel();
    }
}
