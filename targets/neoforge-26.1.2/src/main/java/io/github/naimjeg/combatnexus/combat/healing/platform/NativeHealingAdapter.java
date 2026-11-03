package io.github.naimjeg.combatnexus.combat.healing.platform;

import io.github.naimjeg.combatnexus.combat.healing.api.NativeHealingSources;
import io.github.naimjeg.combatnexus.combat.platform.CombatWorldLifecycle;
import net.minecraft.world.entity.LivingEntity;

public final class NativeHealingAdapter {
    private NativeHealingAdapter() {}
    /** True means the original body must not run, including rejected requests. */
    public static boolean intercept(LivingEntity target, float amount) {
        var source = NativeHealingSources.consume(target);
        if (target.level().isClientSide()) return false;
        return CombatWorldLifecycle.access().heal(target, amount, source);
    }
}
