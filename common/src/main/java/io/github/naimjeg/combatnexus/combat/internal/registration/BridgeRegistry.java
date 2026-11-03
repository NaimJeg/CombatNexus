package io.github.naimjeg.combatnexus.combat.internal.registration;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability;

/** Mutable only during collection; validation happens before publication. */
final class BridgeRegistry {
    private final List<BridgeCapability> entries = new ArrayList<>();
    void register(BridgeCapability capability) {
        Objects.requireNonNull(capability);
        if (entries.stream().anyMatch(e -> e.id().equals(capability.id())
                && e.position().equals(capability.position()) && e.executor().equals(capability.executor())))
            throw new IllegalArgumentException("Duplicate bridge declaration: " + capability.id());
        entries.add(capability);
    }
    List<BridgeCapability> freeze() {
        var replacements = new HashSet<List<Object>>();
        for (var entry : entries) {
            if (entry.mode() == BridgeCapability.Mode.REPLACE
                    && !replacements.add(List.of(entry.id(), entry.position())))
                throw new IllegalStateException("Conflicting bridge replacement: " + entry.id() + " at " + entry.position());
        }
        return List.copyOf(entries);
    }
}
