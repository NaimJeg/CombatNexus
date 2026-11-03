package io.github.naimjeg.combatnexus.combat.spi;

import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.api.action.CombatAction;
import io.github.naimjeg.combatnexus.combat.internal.coordination.CombatActionScope;
import java.util.*;

/** Trusted native attack adapter. Action IDs describe causality and never grant write authority. */
public final class CombatActionAccess {
    private CombatActionAccess() {}
    public static final class Scope implements AutoCloseable {
        private final CombatActionScope delegate;
        private Scope(CombatActionScope delegate) { this.delegate = delegate; }
        @Override public void close() { delegate.close(); }
    }
    public static Scope open(CombatRuntime world, UUID actor) { return new Scope(CombatActionScope.open(world, actor)); }
    public static Optional<io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame> input(UUID session, UUID actor) { return CombatActionScope.input(session, actor); }
    public static Optional<CombatAction> current(UUID session, UUID actor) { return CombatActionScope.current(session, actor); }
}
