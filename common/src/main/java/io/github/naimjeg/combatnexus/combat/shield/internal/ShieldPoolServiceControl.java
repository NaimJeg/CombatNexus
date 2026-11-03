package io.github.naimjeg.combatnexus.combat.shield.internal;
import io.github.naimjeg.combatnexus.combat.shield.spi.ShieldPoolService;
import io.github.naimjeg.combatnexus.combat.shield.api.*;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingService;

/** Composition-root lifecycle contract; not a Java security sandbox. */
public interface ShieldPoolServiceControl extends ShieldPoolService {
    void close();
    void tick(long gameTick);
    void clear(UUID target);
    void reset();
    List<Change> drainChanges();
}
