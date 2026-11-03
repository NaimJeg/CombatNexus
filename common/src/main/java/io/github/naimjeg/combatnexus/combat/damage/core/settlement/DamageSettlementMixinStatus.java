package io.github.naimjeg.combatnexus.combat.damage.core.settlement;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

/** Low-cost runtime proof that the settlement mixin transformed LivingEntity. */
@ApiStatus.Internal
public final class DamageSettlementMixinStatus {

    private DamageSettlementMixinStatus() {
    }

    public static boolean isApplied() {
        return io.github.naimjeg.combatnexus.combat.api.platform.NativeDamageAccess.LifecycleInstalled.class.isAssignableFrom(LivingEntity.class);
    }

}
