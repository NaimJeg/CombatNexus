package io.github.naimjeg.combatnexus.combat.control.platform;
import io.github.naimjeg.combatnexus.combat.api.action.CombatActionGate;
import static io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Mob.class)
public abstract class MobControlMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void combatnexus$attack(net.minecraft.server.level.ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> ci) {
        if (!CombatActionGate.allows((LivingEntity)(Object)this, ATTACK)) ci.setReturnValue(false);
    }
}
