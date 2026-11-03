package io.github.naimjeg.combatnexus.combat.internal.coordination;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.effect.api.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Slot ledger survives expiry/dispel so unchanged equipment never replenishes a consumed grant. */
public final class EquipmentBehaviorController {
    private record Key(UUID target, EquipmentSlot slot) {}
    private record Installed(Identifier item, EquipmentBindings.Binding binding, Object definition, boolean applied) {}
    private final CombatRuntime world;
    private final Map<Key, Map<Identifier, Installed>> installed = new LinkedHashMap<>();
    private final Set<UUID> pending = new LinkedHashSet<>();
    private final Set<Key> retry = new LinkedHashSet<>();
    private final Set<Key> watched = new LinkedHashSet<>();
    private long generation = -1;
    public EquipmentBehaviorController(CombatRuntime world) { this.world = world; }
    public void track(UUID target) {
        CombatMutationGuard.requireWorldWritePhase();
        if (pending.size() >= 4096 && !pending.contains(target)) throw new IllegalStateException("Equipment discovery budget exceeded");
        pending.add(target);
    }
    private static io.github.naimjeg.combatnexus.combat.api.source.SourceInstance source(UUID target, EquipmentSlot slot, Identifier binding) {
        String identity = target + "/" + slot.getName() + "/" + binding;
        return new io.github.naimjeg.combatnexus.combat.api.source.SourceInstance(UUID.nameUUIDFromBytes(identity.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
            Identifier.fromNamespaceAndPath("combatnexus", "equipment"), Optional.of(target), slot.getName() + "/" + binding);
    }
    private static Identifier sourceKey(UUID target, EquipmentSlot slot, Identifier binding) { return SourceInstallations.key(source(target, slot, binding)); }
    public void changed(UUID target, EquipmentSlot slot, ItemStack current) {
        CombatMutationGuard.requireWorldWritePhase();
        if (!(world.level().getEntity(target) instanceof LivingEntity living) || !living.isAlive()) return;
        var key = new Key(target, slot);
        var before = installed.getOrDefault(key, Map.of());
        var bindings = current.isEmpty() ? EquipmentBindings.EMPTY : current.getOrDefault(EquipmentBindings.component(), EquipmentBindings.EMPTY);
        var item = BuiltInRegistries.ITEM.getKey(current.getItem());
        Map<Identifier, Installed> desired = new LinkedHashMap<>();
        RuntimeException invalid = null;
        for (var binding : bindings.bindings()) if (invalid == null && binding.slots().contains(slot)) {
            Object definition = switch (binding.kind()) {
                case BEHAVIOR -> world.behaviorDefinitions().get(binding.definition());
                case EFFECT -> world.domainDefinitions().effects().get(binding.definition());
            };
            if (definition == null) { invalid = new IllegalArgumentException("Unknown equipment definition: " + binding.definition()); continue; }
            desired.put(binding.id(), new Installed(item, binding, definition, true));
        }
        if (invalid != null) desired.clear();
        boolean relevant = invalid != null || bindings.bindings().stream().anyMatch(binding -> binding.slots().contains(slot));
        if (relevant) {
            if (!watched.contains(key) && watched.size() >= 4096) throw new IllegalStateException("Equipment observation budget exceeded");
            watched.add(key);
        } else watched.remove(key);
        if (!installed.containsKey(key) && !desired.isEmpty() && installed.size() >= 4096)
            throw new IllegalStateException("Equipment source budget exceeded");
        var ledger = new LinkedHashMap<>(before);
        // Commit ledger progress per source; failed cleanup remains tracked for later reconciliation.
        installed.put(key, ledger);
        retry.remove(key);
        try {
            for (var entry : before.entrySet()) if (!entry.getValue().equals(desired.get(entry.getKey()))) {
                var handle = world.sourceInstallations().handle(target, source(target, slot, entry.getKey()).instance());
                if (handle.isPresent() && !world.sourceInstallations().revoke(UUID.randomUUID(), handle.get()).applied()) throw new IllegalStateException("Equipment source cleanup pending");
                ledger.remove(entry.getKey());
            }
            for (var entry : desired.entrySet()) if (!entry.getValue().equals(ledger.get(entry.getKey()))) {
                var value = entry.getValue();
                // Track cleanup ownership separately from successful installation.
                ledger.put(entry.getKey(), new Installed(value.item(), value.binding(), value.definition(), false));
                var origin = source(target, slot, entry.getKey());
                EffectDefinition effect;
                if (value.definition() instanceof BehaviorDefinition behavior)
                    effect = new EffectDefinition(0, behavior.durationTicks(), EffectService.Stacking.REPLACE, EffectPolicy.DEFAULT, List.of(), behavior.plan(), Set.of());
                else effect = (EffectDefinition)value.definition();
                var display = effect.display().nativeId() == null
                    ? io.github.naimjeg.combatnexus.effect.internal.EffectMappings.display(value.binding().definition()) : effect.display();
                display = display.withProvenance(new io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance(target, item, slot, null, null));
                effect = new EffectDefinition(effect.level(), effect.lifetime(), effect.stacking(), effect.policy(), effect.attributes(), effect.behavior(), effect.restrictions(), display);
                var plan = new io.github.naimjeg.combatnexus.combat.api.source.SourcePlan(value.binding().definition(), Map.of(value.binding().definition(), effect), Map.of(), Map.of(), true);
                boolean applied = world.sourceInstallations().install(new io.github.naimjeg.combatnexus.combat.api.source.SourceInstallation(UUID.randomUUID(), target, origin, plan)).applied();
                if (applied) ledger.put(entry.getKey(), value);
                else retry.add(key);
            }
        } catch (RuntimeException failure) {
            retry.add(key);
            throw failure;
        }
        if (ledger.isEmpty()) installed.remove(key);
        if (invalid != null) throw invalid;
    }
    public void tick(long currentGeneration) {
        CombatMutationGuard.requireWorldWritePhase();
        var refresh = new LinkedHashSet<Key>(retry);
        if (generation != currentGeneration) refresh.addAll(watched);
        generation = currentGeneration;
        for (var target : pending) for (var slot : EquipmentSlot.values()) refresh.add(new Key(target, slot));
        pending.clear();
        for (var key : refresh) {
            if (!(world.level().getEntity(key.target()) instanceof LivingEntity entity) || !entity.isAlive()) continue;
            try { changed(key.target(), key.slot(), entity.getItemBySlot(key.slot())); }
            catch (RuntimeException failure) {
            }
        }
    }
    public record SavedEquipment(EquipmentSlot slot, Identifier item, EquipmentBindings.Binding binding,
            BehaviorDefinition behavior, EffectDefinition effect) {}
    public List<SavedEquipment> save(UUID target) {
        var result = new ArrayList<SavedEquipment>();
        installed.forEach((key, values) -> { if (key.target().equals(target)) values.values().forEach(value -> {
            if (value.applied()) result.add(new SavedEquipment(key.slot(), value.item(), value.binding(),
                    value.definition() instanceof BehaviorDefinition d ? d : null,
                    value.definition() instanceof EffectDefinition d ? d : null));
        }); });
        return List.copyOf(result);
    }
    public Set<Identifier> sources(UUID target) {
        return save(target).stream().map(e -> sourceKey(target, e.slot(), e.binding().id())).collect(java.util.stream.Collectors.toUnmodifiableSet());
    }
    public Set<Identifier> restore(UUID target, List<SavedEquipment> saved) {
        CombatMutationGuard.requireWorldWritePhase();
        var accepted = new HashSet<Identifier>();
        if (!(world.level().getEntity(target) instanceof LivingEntity entity)) return Set.of();
        for (var entry : saved) {
            var stack = entity.getItemBySlot(entry.slot());
            var bindings = stack.getOrDefault(EquipmentBindings.component(), EquipmentBindings.EMPTY);
            Object definition = entry.behavior() != null ? entry.behavior() : entry.effect();
            Object current = entry.binding().kind() == EquipmentBindings.Kind.BEHAVIOR
                    ? world.behaviorDefinitions().get(entry.binding().definition()) : world.domainDefinitions().effects().get(entry.binding().definition());
            if (!Objects.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()), entry.item())
                    || !bindings.bindings().contains(entry.binding()) || !Objects.equals(definition, current)) continue;
            var key = new Key(target, entry.slot());
            installed.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put(entry.binding().id(),
                    new Installed(entry.item(), entry.binding(), definition, true));
            watched.add(key); accepted.add(sourceKey(target, entry.slot(), entry.binding().id()));
        }
        track(target); return Set.copyOf(accepted);
    }
    public void clear(UUID target) { installed.keySet().removeIf(key -> key.target().equals(target)); watched.removeIf(key -> key.target().equals(target)); pending.remove(target); retry.removeIf(key -> key.target().equals(target)); }
    public void close() { installed.clear(); retry.clear(); watched.clear(); pending.clear(); }
}
