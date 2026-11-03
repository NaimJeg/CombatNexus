package io.github.naimjeg.combatnexus.presentation.api;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Read-only client/session cache. No formulas and no ability to send numeric values to the server. */
public final class CombatPanelCache {
    private final UUID player;
    private final UUID session;
    private final long maximumAgeTicks;
    private CombatPanelSnapshot snapshot;
    private long receivedAt;
    private long revision = -1;

    public CombatPanelCache(UUID player, UUID session, long maximumAgeTicks) {
        this.player = Objects.requireNonNull(player, "player");
        this.session = Objects.requireNonNull(session, "session");
        if (maximumAgeTicks < 1) throw new IllegalArgumentException("Invalid maximum snapshot age");
        this.maximumAgeTicks = maximumAgeTicks;
    }

    /** Only a server payload handler may supply snapshots; localTick is the client's receipt clock. */
    public boolean accept(UUID incomingSession, CombatPanelSnapshot incoming, long localTick) {
        Objects.requireNonNull(incoming, "incoming");
        if (!session.equals(incomingSession) || localTick < 0 || !incoming.attributes().entity().equals(player)
                || incoming.attributes().revision() <= revision) return false;
        snapshot = incoming;
        revision = incoming.attributes().revision();
        receivedAt = localTick;
        return true;
    }

    public Optional<CombatPanelSnapshot> current(long localTick) {
        if (snapshot != null && (localTick < receivedAt || localTick - receivedAt >= maximumAgeTicks)) snapshot = null;
        return Optional.ofNullable(snapshot);
    }

    /** Invalidate on death; reconnect/dimension transfer creates a cache bound to the new server session. */
    public void clear() { snapshot = null; }
}
