package io.github.naimjeg.combatnexus.combat.control.platform;

import io.github.naimjeg.combatnexus.combat.api.action.CombatActionGate;
import io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction;
import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(targets = "net.minecraft.world.entity.monster.illager.SpellcasterIllager$SpellcasterUseSpellGoal")
public abstract class SpellControlMixin {
    @Shadow @Final private SpellcasterIllager this$0;
    @Inject(method = {"canUse", "canContinueToUse"}, at = @At("HEAD"), cancellable = true)
    private void combatnexus$start(CallbackInfoReturnable<Boolean> cir) {
        if (!CombatActionGate.allows(this$0, RestrictedAction.CAST)) cir.setReturnValue(false);
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void combatnexus$cast(CallbackInfo ci) {
        if (!CombatActionGate.allows(this$0, RestrictedAction.CAST)) ci.cancel();
    }
}
