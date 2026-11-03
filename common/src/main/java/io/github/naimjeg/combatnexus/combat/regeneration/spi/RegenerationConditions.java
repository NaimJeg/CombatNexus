package io.github.naimjeg.combatnexus.combat.regeneration.spi;
import io.github.naimjeg.combatnexus.combat.regeneration.api.*;

import java.util.OptionalLong;
import java.util.Objects;
import java.util.UUID;

/** Captured by the server adapter; damage time uses confirmed loss, never Incoming proposals. */
@FunctionalInterface
public interface RegenerationConditions {
    State read(UUID target);

    record State(boolean alive, boolean naturalRegeneration) {}
}
