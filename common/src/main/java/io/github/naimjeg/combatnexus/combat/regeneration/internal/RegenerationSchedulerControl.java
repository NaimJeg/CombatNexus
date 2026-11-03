package io.github.naimjeg.combatnexus.combat.regeneration.internal;
import io.github.naimjeg.combatnexus.combat.regeneration.api.*;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingService;

/** Composition-root lifecycle contract; not a Java security sandbox. */
public interface RegenerationSchedulerControl extends RegenerationScheduler {
    void recordDamage(UUID target, long gameTick);
    void tick(long gameTick, HealingService healing);
    void clear(UUID target);
    void reset();
    void close();
}
