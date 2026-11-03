package io.github.naimjeg.combatnexus.combat.api.behavior;
import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation;
/** Callback-scoped dispatch metadata. Histories must project values rather than retain domain payloads. */
public record SettlementEnvelope(CombatOperation operation, CommandContext.Identity identity, UUID target,
        Optional<CombatSource> origin, ApplicationJournal journal, Object payload) {
    public SettlementEnvelope { Objects.requireNonNull(operation);Objects.requireNonNull(identity);Objects.requireNonNull(target);Objects.requireNonNull(origin);Objects.requireNonNull(journal);Objects.requireNonNull(payload); }
    public <T> Optional<T> result(Class<T> type) { return type.isInstance(payload)?Optional.of(type.cast(payload)):Optional.empty(); }
}
