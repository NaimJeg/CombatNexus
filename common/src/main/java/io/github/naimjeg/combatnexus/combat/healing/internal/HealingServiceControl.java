package io.github.naimjeg.combatnexus.combat.healing.internal;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.healing.api.HealingService;

/** Composition-root lifecycle contract; not a Java security sandbox. */
public interface HealingServiceControl extends HealingService {
    void close();
}
