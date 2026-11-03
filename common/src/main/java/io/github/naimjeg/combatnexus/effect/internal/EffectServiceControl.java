package io.github.naimjeg.combatnexus.effect.internal;
import io.github.naimjeg.combatnexus.effect.api.*;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingService;

/** Composition-root lifecycle contract; not a Java security sandbox. */
public interface EffectServiceControl extends EffectService {
    void close();
    void tick(long gameTick);
    void clear(UUID target, Removal reason);
    void reset();
    List<Change> drainChanges();
}
