package io.github.naimjeg.combatnexus.combat.control.platform;

import io.github.naimjeg.combatnexus.combat.platform.CombatWorldLifecycle;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The 26.1.2 world clock advances inside tick(), after NeoForge LevelTick.Pre. */
@Mixin(ServerLevel.class)
public abstract class WorldLifecycleMixin {
    @Inject(method = "tickTime", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/storage/ServerLevelData;setGameTime(J)V", shift = At.Shift.AFTER))
    private void combatnexus$clock(CallbackInfo ci) {
        CombatWorldLifecycle.clockAdvanced((ServerLevel)(Object)this);
    }
    @Inject(method = "tick", at = @At(value = "CONSTANT", args = "stringValue=tickPending"))
    private void combatnexus$ready(java.util.function.BooleanSupplier haveTime, CallbackInfo ci) {
        CombatWorldLifecycle.tickReady((ServerLevel)(Object)this);
    }
}
