package io.github.naimjeg.combatnexus.combat.shield.api;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.config.DefinitionJson;
import net.minecraft.resources.Identifier;
import java.util.*;
/** Read-only current pool state; eligible native types were resolved when the grant was installed. */
public record ShieldPoolSnapshot(CombatSource source, ShieldDefinition definition, double balance,
        OptionalLong expiresAtTick, Set<Identifier> eligibleDamageTypes) {
    public ShieldPoolSnapshot {
        Objects.requireNonNull(source); Objects.requireNonNull(definition); Objects.requireNonNull(expiresAtTick);
        eligibleDamageTypes = Set.copyOf(eligibleDamageTypes);
        DefinitionJson.range(balance, 0, definition.capacity(), "balance");
    }
    public enum RemovalReason { BROKEN, EXPIRED, DISPELLED, REMOVED }
}
