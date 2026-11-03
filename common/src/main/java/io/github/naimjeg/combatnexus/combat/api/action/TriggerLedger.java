package io.github.naimjeg.combatnexus.combat.api.action;
import java.util.*;
/** Synchronous operation/action/root counters expire with the tick; lifetime source counters are explicitly removed. */
public final class TriggerLedger {
    private record Key(long source, String reaction, TriggerLimit.Scope scope, Object identity) {}
    private final Map<Key, Integer> counts = new HashMap<>();
    private final int capacity;
    private long tick = -1;
    public TriggerLedger(int capacity) { if (capacity < 1) throw new IllegalArgumentException("Invalid trigger capacity"); this.capacity = capacity; }
    public Optional<String> claim(long now, long source, String reaction, TriggerLimit limit, long operation, long root,
            UUID target, Optional<CombatAction> action) {
        if (now < tick) throw new IllegalArgumentException("Trigger clock moved backwards");
        if (now != tick) { counts.keySet().removeIf(k -> k.scope() != TriggerLimit.Scope.SOURCE); tick = now; }
        if (limit.scope() == TriggerLimit.Scope.ACTION && action.isEmpty()) return Optional.of("missing_action_evidence");
        Object identity = switch (limit.scope()) {
            case OPERATION -> operation;
            case TARGET -> List.of(root, target);
            case ACTION -> action.orElseThrow().id();
            case ROOT -> root;
            case SOURCE -> source;
        };
        var key = new Key(source, reaction, limit.scope(), identity);
        int used = counts.getOrDefault(key, 0);
        if (used >= limit.maximum()) return Optional.of("trigger_limit");
        if (used == 0 && counts.size() >= capacity) return Optional.of("trigger_capacity");
        counts.put(key, used + 1); return Optional.empty();
    }
    /** Opaque compensation record; cannot manufacture new allowance or authority. */
    public static final class SourceCheckpoint {
        private final long tick;
        private final Map<Key, Integer> counts;
        private SourceCheckpoint(long tick, Map<Key, Integer> counts) { this.tick = tick; this.counts = Map.copyOf(counts); }
    }
    public SourceCheckpoint checkpoint(long source) {
        Map<Key, Integer> saved = new HashMap<>();
        counts.forEach((key, count) -> { if (key.source() == source) saved.put(key, count); });
        return new SourceCheckpoint(tick, saved);
    }
    public void restore(SourceCheckpoint saved) {
        var relevant = saved.counts.entrySet().stream().filter(e -> saved.tick == tick || e.getKey().scope() == TriggerLimit.Scope.SOURCE).toList();
        long additional = relevant.stream().filter(e -> !counts.containsKey(e.getKey())).count();
        if (counts.size() + additional > capacity) throw new IllegalStateException("Trigger restoration capacity exceeded");
        relevant.forEach(e -> counts.merge(e.getKey(), e.getValue(), Math::max));
    }
    public Map<String, Integer> persistentCounts(long source) {
        var result = new HashMap<String, Integer>();
        counts.forEach((key, value) -> { if (key.source() == source && key.scope() == TriggerLimit.Scope.SOURCE) result.put(key.reaction(), value); });
        return Map.copyOf(result);
    }
    public void restoreCounts(long source, Map<String, Integer> saved) {
        if (counts.size() + saved.size() > capacity) throw new IllegalStateException("Trigger restoration capacity exceeded");
        saved.forEach((reaction, value) -> {
            if (value < 0 || value > 64) throw new IllegalArgumentException("Invalid persistent trigger count");
            counts.put(new Key(source, reaction, TriggerLimit.Scope.SOURCE, source), value);
        });
    }
    public void remove(long source) { counts.keySet().removeIf(k -> k.source() == source); }
    public void clear() { counts.clear(); tick = -1; }
}
