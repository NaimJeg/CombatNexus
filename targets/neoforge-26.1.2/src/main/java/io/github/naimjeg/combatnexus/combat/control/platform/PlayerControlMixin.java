package io.github.naimjeg.combatnexus.combat.control.platform;
import io.github.naimjeg.combatnexus.combat.api.action.CombatActionGate;
import static io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction.*;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(net.minecraft.world.entity.player.Player.class)
public abstract class PlayerControlMixin {
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void combatnexus$attack(Entity target, CallbackInfo ci) {
        if (!CombatActionGate.allows((LivingEntity)(Object)this, ATTACK)) ci.cancel();
    }
}
