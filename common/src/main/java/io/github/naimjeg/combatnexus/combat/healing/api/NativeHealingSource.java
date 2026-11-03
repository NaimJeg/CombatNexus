package io.github.naimjeg.combatnexus.combat.healing.api;

import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import net.minecraft.resources.Identifier;
import java.util.Objects;
import java.util.Optional;

/** Attribution only: does not grant permission to submit or write health. */
public record NativeHealingSource(CombatSource source, HealingRequest.Reason reason) {
    public static final NativeHealingSource UNKNOWN = new NativeHealingSource(
            new CombatSource(Identifier.fromNamespaceAndPath("combatnexus", "unknown_external_healing"),
                    Optional.empty(), Optional.empty()), HealingRequest.Reason.UNKNOWN_EXTERNAL);
    public NativeHealingSource {
        Objects.requireNonNull(source);
        Objects.requireNonNull(reason);
    }
}
