package io.github.naimjeg.combatnexus.combat.api.scheduling;
import java.util.*;
/** Bounded stable due queue. Payloads must be value identities, never live entities or authority. */
public final class DueQueue<T> {
    public record Due<T>(long id, long tick, T value) {}
    private final int capacity;
    private final NavigableSet<Due<T>> tasks = new TreeSet<>(Comparator.<Due<T>>comparingLong(Due::tick).thenComparingLong(Due::id));
    private final Map<Long, Due<T>> byId = new HashMap<>();
    private long sequence;
    public DueQueue(int capacity) { if (capacity < 1) throw new IllegalArgumentException("Invalid capacity"); this.capacity = capacity; }
    public long schedule(long tick, T value) {
        if (tick < 0 || tasks.size() >= capacity) throw new IllegalStateException("Due queue capacity or time invalid");
        var due = new Due<>(++sequence, tick, Objects.requireNonNull(value)); tasks.add(due); byId.put(due.id(), due); return due.id();
    }
    public boolean cancel(long id) { var due = byId.remove(id); return due != null && tasks.remove(due); }
    public List<Due<T>> poll(long tick) {
        List<Due<T>> due = new ArrayList<>();
        while (!tasks.isEmpty() && tasks.first().tick() <= tick) { var next = tasks.pollFirst(); byId.remove(next.id()); due.add(next); }
        return List.copyOf(due);
    }
    public int size() { return tasks.size(); }
    public void clear() { tasks.clear(); byId.clear(); }
}
