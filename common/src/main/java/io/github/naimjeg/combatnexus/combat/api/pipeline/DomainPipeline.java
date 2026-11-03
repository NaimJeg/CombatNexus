package io.github.naimjeg.combatnexus.combat.api.pipeline;

import java.util.*;
import java.util.function.*;
import net.minecraft.resources.Identifier;

/** Immutable ordered definitions. Domain adapters own phases, authority, checkpoints and failure policy. */
public final class DomainPipeline<P extends Enum<P>, H> {
    public record Entry<P, H>(Identifier id, P phase, int priority, boolean external, long order, H handler) {}
    private final Map<P, List<Entry<P, H>>> phases;
    private DomainPipeline(Map<P, List<Entry<P, H>>> phases) { this.phases = Map.copyOf(phases); }
    public List<Entry<P, H>> entries(P phase) { return phases.getOrDefault(phase, List.of()); }
    public void execute(P phase, Consumer<Entry<P, H>> invocation, BooleanSupplier stopped) {
        for (var entry : entries(phase)) {
            if (stopped.getAsBoolean()) return;
            invocation.accept(entry);
        }
    }
    public static final class Builder<P extends Enum<P>, H> {
        private final Class<P> phaseType;
        private final Map<Identifier, Entry<P, H>> entries = new LinkedHashMap<>();
        private long sequence;
        private boolean frozen;
        public Builder(Class<P> phaseType) { this.phaseType = Objects.requireNonNull(phaseType); }
        public synchronized PipelineRegistration register(Identifier id, P phase, int priority, boolean external, H handler) {
            requireOpen();
            Objects.requireNonNull(id); Objects.requireNonNull(handler);
            if (id.toString().length() > 256) throw new IllegalArgumentException("Pipeline ID too long");
            if (phase == null || phase.getDeclaringClass() != phaseType) throw new IllegalArgumentException("Invalid domain phase");
            var entry = new Entry<>(id, phase, priority, external, sequence++, handler);
            if (entries.putIfAbsent(id, entry) != null) throw new IllegalArgumentException("Duplicate pipeline ID: " + id);
            return () -> { synchronized (this) { requireOpen(); entries.remove(id, entry); } };
        }
        public synchronized DomainPipeline<P, H> freeze() {
            requireOpen(); frozen = true;
            Comparator<Entry<P, H>> order = Comparator.<Entry<P, H>>comparingInt(Entry::priority).reversed()
                    .thenComparing(e -> e.id().toString());
            Map<P, List<Entry<P, H>>> phases = new EnumMap<>(phaseType);
            for (P phase : phaseType.getEnumConstants()) phases.put(phase, entries.values().stream()
                    .filter(e -> e.phase() == phase).sorted(order).toList());
            return new DomainPipeline<>(phases);
        }
        private void requireOpen() { if (frozen) throw new IllegalStateException("Pipeline definitions are frozen"); }
    }
}
