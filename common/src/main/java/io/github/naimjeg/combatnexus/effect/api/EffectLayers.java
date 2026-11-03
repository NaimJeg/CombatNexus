package io.github.naimjeg.combatnexus.effect.api;

import java.util.*;

/** Immutable strongest-first layers; suppressed durations continue to elapse. */
public final class EffectLayers {
    private EffectLayers() {}
    private static long deadline(EffectInstance effect) { return effect.snapshot().expiresAtTick().orElse(Long.MAX_VALUE); }
    private static EffectInstance flat(EffectInstance e) {
        return new EffectInstance(e.snapshot(), e.attributes(), e.ownership(), e.behavior(), e.policy(), List.of(), e.display(), e.stacking());
    }
    public static EffectInstance merge(EffectInstance previous, EffectInstance incoming, long now) {
        if (!previous.snapshot().target().equals(incoming.snapshot().target())
                || !previous.snapshot().effect().equals(incoming.snapshot().effect())
                || (!(previous.display().aggregateSources() && incoming.display().aggregateSources()) && !previous.snapshot().source().equals(incoming.snapshot().source())))
            throw new IllegalArgumentException("Cannot merge unrelated effect identities");
        var candidates = new ArrayList<EffectInstance>();
        candidates.add(flat(previous)); candidates.addAll(previous.hidden());
        candidates.add(flat(incoming)); candidates.addAll(incoming.hidden());
        var selected = select(candidates, now).orElseThrow();
        if (!previous.display().aggregateSources()) return selected;
        var oldDisplay = previous.display(); var input = incoming.display(); var display = selected.display();
        boolean changed = incoming.snapshot().level() > previous.snapshot().level()
            || incoming.snapshot().level() == previous.snapshot().level() && deadline(incoming) > deadline(previous);
        return new EffectInstance(selected.snapshot(), selected.attributes(), selected.ownership(), selected.behavior(), selected.policy(), selected.hidden(),
            new EffectDisplay(display.nativeId(), changed ? input.ambient() : oldDisplay.ambient() && input.ambient(),
                input.particles(), input.icon(), display.owner(), display.aggregateSources(), display.persistent(), display.environment(), display.provenance()), incoming.stacking());
    }
    public static Optional<EffectInstance> active(EffectInstance effect, long now) {
        var candidates = new ArrayList<EffectInstance>();
        candidates.add(flat(effect)); candidates.addAll(effect.hidden());
        return select(candidates, now);
    }
    public static Optional<EffectInstance> withoutSource(EffectInstance effect, net.minecraft.resources.Identifier source, long now) {
        return select(java.util.stream.Stream.concat(java.util.stream.Stream.of(flat(effect)), effect.hidden().stream())
            .filter(layer -> !layer.snapshot().source().equals(source)).toList(), now);
    }
    private static Optional<EffectInstance> select(List<EffectInstance> candidates, long now) {
        var levels = new TreeMap<Integer, EffectInstance>(Comparator.reverseOrder());
        for (var value : candidates) if (deadline(value) > now)
            levels.merge(value.snapshot().level(), value, (a, b) -> deadline(b) > deadline(a) ? b : a);
        var retained = new ArrayList<EffectInstance>();
        long visibleUntil = -1;
        for (var value : levels.values()) if (deadline(value) > visibleUntil) {
            retained.add(flat(value)); visibleUntil = deadline(value);
        }
        if (retained.isEmpty()) return Optional.empty();
        var head = retained.removeFirst();
        return Optional.of(new EffectInstance(head.snapshot(), head.attributes(), head.ownership(), head.behavior(), head.policy(), retained, head.display(), head.stacking()));
    }
    public static EffectInstance rebase(EffectInstance effect, long savedTick, long now) {
        var s = effect.snapshot();
        var expiry = s.expiresAtTick().isPresent()
                ? OptionalLong.of(Math.addExact(now, Math.max(0, s.expiresAtTick().getAsLong() - savedTick))) : OptionalLong.empty();
        return new EffectInstance(new EffectSnapshot(s.target(), s.effect(), s.source(), s.level(), expiry, s.restrictions(), s.lifetime()),
                effect.attributes(), effect.ownership(), effect.behavior(), effect.policy(),
                effect.hidden().stream().map(e -> rebase(e, savedTick, now)).toList(), effect.display(), effect.stacking());
    }
}
