package io.github.naimjeg.combatnexus.combat.api.transaction;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope;

import java.util.Optional;

/** Read-only completion, with a capability available only during its own registered callback. */
public final class CombatCallback<T> {
    private final CombatLineage lineage;
    private final T result;
    private final Object server;
    @org.jetbrains.annotations.ApiStatus.Internal
    public CombatCallback(CombatLineage lineage, T result, Object server) {
        this.lineage = lineage; this.result = result; this.server = server;
    }
    public CombatLineage lineage() { return lineage; }
    public T result() { return result; }
    public Optional<CombatParent> childAuthority() { return CombatDispatchScope.parentFor(this, server); }
}
