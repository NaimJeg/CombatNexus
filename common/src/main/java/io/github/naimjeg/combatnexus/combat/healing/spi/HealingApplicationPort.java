package io.github.naimjeg.combatnexus.combat.healing.spi;
import io.github.naimjeg.combatnexus.combat.healing.api.*;

import net.minecraft.world.entity.LivingEntity;

/** Native observations only. Events run before sampling the one native health write. */
public interface HealingApplicationPort {
    LivingEntity target(HealingRequest request);
    Receipt apply(HealingRequest request, double modifiedAmount);
    enum State { UNKNOWN_WRITE, WRITTEN, REJECTED, DEAD_TARGET, CANCELLED, ZERO_AMOUNT }
    record Receipt(State state, double acceptedAmount, double healthBefore,
                   double maximumHealth, double healthAfter, boolean completed) {
        public Receipt {
            java.util.Objects.requireNonNull(state);
            if (!Double.isFinite(acceptedAmount) || acceptedAmount < 0
                    || !Double.isFinite(healthBefore) || !Double.isFinite(maximumHealth)
                    || !Double.isFinite(healthAfter))
                throw new IllegalArgumentException("Invalid native healing evidence");
        }
        public static Receipt skipped(State state, double accepted) {
            return new Receipt(state, accepted, 0, 0, 0, true);
        }
    }
}
