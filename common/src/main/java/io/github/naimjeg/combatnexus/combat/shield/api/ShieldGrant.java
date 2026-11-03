package io.github.naimjeg.combatnexus.combat.shield.api;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.config.DefinitionJson;
import java.util.*;
/** Command values are separate from a pool's observed balance. No runtime permissions are retained. */
public record ShieldGrant(CombatSource source, ShieldDefinition definition, double initialBalance, OptionalLong expiresAtTick) {
    public ShieldGrant {
        Objects.requireNonNull(source); Objects.requireNonNull(definition); Objects.requireNonNull(expiresAtTick);
        DefinitionJson.range(initialBalance, 0, definition.capacity(), "initial_balance");
        if (expiresAtTick.isPresent() && expiresAtTick.getAsLong() < 0) throw new IllegalArgumentException("Negative expiry");
    }
}
