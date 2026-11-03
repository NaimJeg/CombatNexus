package io.github.naimjeg.combatnexus.presentation.internal;

import io.github.naimjeg.combatnexus.presentation.api.*;

import java.util.*;
import java.util.function.Supplier;

/** Self-only, rate-limited server snapshot delivery policy. */
public final class DefaultCombatPanelService implements CombatPanelService {
    public CombatPanelService businessView() { return this::request; }
    private boolean closed;
    private final Thread owner = Thread.currentThread();
    private void requireOpen() { if (closed || Thread.currentThread() != owner) throw new IllegalStateException("Panel session closed or wrong thread"); }
    public void close() { requireOpen(); reset(); closed = true; }
    private final Map<UUID, Long> lastRequest = new HashMap<>();
    private final long minimumInterval;
    private final int maximumEntries;

    public DefaultCombatPanelService(long minimumInterval, int maximumEntries) {
        if (minimumInterval < 1 || maximumEntries < 1) throw new IllegalArgumentException("Invalid panel limits");
        this.minimumInterval = minimumInterval;
        this.maximumEntries = maximumEntries;
    }

    public Result request(UUID sender, UUID target, long gameTick,
                                                Supplier<CombatPanelSnapshot> capture) {
        requireOpen();
        Objects.requireNonNull(sender, "sender");
        Objects.requireNonNull(capture, "capture");
        if (!sender.equals(target) || gameTick < 0) return Result.reject(Refusal.FORBIDDEN);
        Long previous = lastRequest.get(sender);
        if (previous != null && (gameTick < previous || gameTick - previous < minimumInterval)) return Result.reject(Refusal.RATE_LIMITED);
        lastRequest.put(sender, gameTick);
        CombatPanelSnapshot snapshot;
        try { snapshot = capture.get(); }
        catch (PresentationBudget.Exceeded exceeded) { return Result.reject(Refusal.BUDGET_EXCEEDED); }
        if (!snapshot.attributes().entity().equals(sender) || snapshot.attributes().gameTick() != gameTick) {
            throw new IllegalArgumentException("Panel must use the requested entity and current server snapshot");
        }
        long entries = (long) snapshot.attributes().valueFrame().values().size() + snapshot.shields().size() + snapshot.effects().size() + snapshot.behaviors().size() + snapshot.reactions().size() + snapshot.sourceDescriptions().size();
        for (var block : snapshot.attributes().provenance().sources().values()) entries += block.size();
        if (entries > maximumEntries) return Result.reject(Refusal.BUDGET_EXCEEDED);
        return new Result(Optional.of(snapshot), Refusal.NONE);
    }

    /** Disconnect, death/respawn or dimension transfer invalidates this session's throttle state. */
    public void clear(UUID player) { requireOpen(); lastRequest.remove(player); }
    public void reset() { requireOpen(); lastRequest.clear(); }
}
