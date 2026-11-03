package io.github.naimjeg.combatnexus.combat.internal.transaction;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;

import java.util.*;
import net.minecraft.resources.Identifier;

/** Definition-time ordering and freeze mechanics; callers retain their own registration capabilities. */
@org.jetbrains.annotations.ApiStatus.Internal
public final class CombatListenerRegistry<L> {
    public record Entry<L>(Identifier id, int priority, L listener) {}
    private final Map<Identifier, Entry<L>> mutable = new LinkedHashMap<>();
    private volatile List<Entry<L>> frozen;
    public synchronized void register(Identifier id, int priority, L listener) {
        if (frozen != null) throw new IllegalStateException("Listener registry is frozen");
        if (priority < -10_000 || priority > 10_000) throw new IllegalArgumentException("Invalid listener priority");
        var entry = new Entry<>(Objects.requireNonNull(id), priority, Objects.requireNonNull(listener));
        if (mutable.putIfAbsent(id, entry) != null) throw new IllegalArgumentException("Duplicate listener: " + id);
    }
    public synchronized void freeze() {
        if (frozen != null) throw new IllegalStateException("Listener registry already frozen");
        frozen = mutable.values().stream().sorted(Comparator.<Entry<L>>comparingInt(Entry::priority)
                .reversed().thenComparing(entry -> entry.id().toString())).toList();
    }
    public boolean frozen() { return frozen != null; }
    public List<Entry<L>> entries() {
        var entries = frozen;
        if (entries == null) throw new IllegalStateException("Listener registry is not frozen");
        return entries;
    }
    public synchronized void clear() { mutable.clear(); frozen = null; }
}
