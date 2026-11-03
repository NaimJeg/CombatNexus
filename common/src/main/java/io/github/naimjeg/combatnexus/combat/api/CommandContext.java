package io.github.naimjeg.combatnexus.combat.api;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;

/** Frozen identity and provenance, never execution authority. Rejection before admission has no operation identity. */
public record CommandContext(UUID request, UUID target, CombatOperation operation, Optional<CombatSource> source,
        Optional<Identity> identity) {
    public CommandContext {
        Objects.requireNonNull(request); Objects.requireNonNull(target); Objects.requireNonNull(operation);
        Objects.requireNonNull(source); Objects.requireNonNull(identity);
    }
    public record Identity(long operation, long root, OptionalLong parent, int depth) {}
}
