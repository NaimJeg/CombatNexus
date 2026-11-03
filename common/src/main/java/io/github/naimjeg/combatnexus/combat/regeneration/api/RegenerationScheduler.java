package io.github.naimjeg.combatnexus.combat.regeneration.api;

import java.util.UUID;

/** Fractional game-tick recovery, no offline compensation or direct health writes. */
public interface RegenerationScheduler {
    void replace(RegenerationPlan plan);
    void remove(UUID target, net.minecraft.resources.Identifier source);
}
