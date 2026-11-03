package io.github.naimjeg.combatnexus.attribute.internal;
import io.github.naimjeg.combatnexus.effect.spi.EffectContributionPort;

import io.github.naimjeg.combatnexus.effect.api.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.resources.Identifier;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** World-owned native attribute port. Each write is reconciled against actual modifier state. */
public final class EffectAttributeContributions implements EffectContributionPort {
    private record Key(UUID target, Identifier effect, Identifier source) {}
    private record Slot(AttributeInstance attribute, Identifier id) {}
    public interface ModifierWriter {
        void install(AttributeInstance attribute, AttributeModifier modifier);
        void remove(AttributeInstance attribute, Identifier id);
    }
    private final ModifierWriter writer;
    private final ServerLevel level;
    private UUID session;
    private final Map<Key, Map<Slot, AttributeModifier>> installed = new HashMap<>();
    public EffectAttributeContributions(ServerLevel level) {
        this(level, new ModifierWriter() {
            public void install(AttributeInstance attribute, AttributeModifier modifier) { attribute.addTransientModifier(modifier); }
            public void remove(AttributeInstance attribute, Identifier id) { attribute.removeModifier(id); }
        });
    }
    public EffectAttributeContributions(ServerLevel level, ModifierWriter writer) {
        this.level = Objects.requireNonNull(level); this.writer = Objects.requireNonNull(writer);
    }
    private static Key key(EffectInstance effect) {
        var s = effect.snapshot(); return new Key(s.target(), s.effect(), s.source());
    }
    @Override public Prepared prepare(EffectService.Change change) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Attribute writes require server thread");
        UUID current = io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions.current(level)
                .orElseThrow(() -> new IllegalStateException("Attribute contribution world session closed"));
        if (session == null) session = current;
        else if (!session.equals(current)) throw new IllegalStateException("Attribute contribution session expired");
        Map<Key, Map<Slot, AttributeModifier>> desired = new LinkedHashMap<>();
        for (var effect : change.before()) desired.put(key(effect), Map.of());
        for (var effect : change.after()) {
            Map<Slot, AttributeModifier> values = new LinkedHashMap<>();
            if (effect.ownership() == EffectInstance.Ownership.MANAGED) {
                if (!(level.getEntity(effect.snapshot().target()) instanceof LivingEntity entity) || !entity.isAlive())
                    throw new IllegalArgumentException("Managed effect requires a living target in this world");
                for (var contribution : effect.attributes()) {
                    var holder = BuiltInRegistries.ATTRIBUTE.get(contribution.attribute()).orElseThrow();
                    var attribute = entity.getAttribute(holder);
                    if (attribute == null) throw new IllegalArgumentException("Target lacks attribute " + contribution.attribute());
                    var value = contribution.modifier();
                    var id = EffectContributionIdentity.id(effect, contribution);
                    var slot = new Slot(attribute, id);
                    if (values.putIfAbsent(slot, new AttributeModifier(id, value.amount(), value.operation())) != null)
                        throw new IllegalArgumentException("Duplicate managed modifier " + id);
                    if (attribute.getModifier(id) != null && !installed.getOrDefault(key(effect), Map.of()).containsKey(slot))
                        throw new IllegalArgumentException("Modifier identity already owned " + id);
                }
            }
            desired.put(key(effect), Map.copyOf(values));
        }
        Map<Key, Map<Slot, AttributeModifier>> before = new LinkedHashMap<>();
        desired.keySet().forEach(k -> before.put(k, Map.copyOf(installed.getOrDefault(k, Map.of()))));
        return new Prepared() {
            public Receipt apply() { return write(desired); }
            public Receipt reconcile() { return write(before); }
            private Receipt write(Map<Key, Map<Slot, AttributeModifier>> goal) {
                if (!io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions.current(level)
                        .filter(current -> current.equals(session)).isPresent())
                    return new Receipt(List.of(), false, new IllegalStateException("Attribute application session expired"));
                List<Write> writes = new ArrayList<>();
                try {
                    for (var entry : goal.entrySet()) {
                        Key key = entry.getKey();
                        var actual = installed.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
                        Set<Slot> slots = new LinkedHashSet<>(actual.keySet());
                        slots.addAll(entry.getValue().keySet());
                        for (var slot : slots) {
                            var wanted = entry.getValue().get(slot);
                            try {
                                var current = slot.attribute().getModifier(slot.id());
                                if (!Objects.equals(current, wanted)) {
                                    if (current != null) writer.remove(slot.attribute(), slot.id());
                                    if (wanted != null) writer.install(slot.attribute(), wanted);
                                }
                            } finally {
                                var observed = slot.attribute().getModifier(slot.id());
                                if (observed == null) actual.remove(slot); else actual.put(slot, observed);
                                writes.add(new Write(slot.id().toString(), observed != null));
                            }
                        }
                        if (actual.isEmpty()) installed.remove(key);
                    }
                    return new Receipt(writes, true, null);
                } catch (RuntimeException failure) {
                    return new Receipt(writes, false, failure);
                }
            }
        };
    }
}
