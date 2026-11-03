package io.github.naimjeg.combatnexus.combat.api.behavior;
import com.mojang.serialization.*;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import java.util.*;

/** Bounded typed item opt-in. IDs distinguish independent installed sources within each slot. */
public record EquipmentBindings(List<Binding> bindings) {
    public enum Kind { BEHAVIOR, EFFECT }
    public record Binding(Identifier id, Kind kind, Identifier definition, List<EquipmentSlot> slots) {
        public Binding {
            Objects.requireNonNull(id); Objects.requireNonNull(kind); Objects.requireNonNull(definition); slots = List.copyOf(slots);
            if (id.toString().length() > 96 || definition.toString().length() > 256 || slots.isEmpty()
                    || slots.size() > EquipmentSlot.values().length || Set.copyOf(slots).size() != slots.size())
                throw new IllegalArgumentException("Invalid equipment binding");
        }
    }
    private static final Codec<Identifier> ID = Identifier.CODEC.validate(id -> id.toString().length() <= 256
            ? DataResult.success(id) : DataResult.error(() -> "Binding identifier too long"));
    private static final Codec<EquipmentSlot> SLOT = Codec.STRING.comapFlatMap(value -> Arrays.stream(EquipmentSlot.values())
            .filter(slot -> slot.getName().equals(value)).findFirst().map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Unknown equipment slot: " + value)), EquipmentSlot::getName);
    private static final Codec<Kind> KIND = Codec.STRING.comapFlatMap(value -> {
        try { return DataResult.success(Kind.valueOf(value)); }
        catch (IllegalArgumentException failure) { return DataResult.error(() -> "Unknown binding kind: " + value); }
    }, Enum::name);
    private static final Codec<Binding> BINDING = checked(RecordCodecBuilder.create(instance -> instance.group(
            ID.fieldOf("id").forGetter(Binding::id), KIND.fieldOf("kind").forGetter(Binding::kind),
            ID.fieldOf("definition").forGetter(Binding::definition), SLOT.listOf(1, EquipmentSlot.values().length).fieldOf("slots").forGetter(Binding::slots)
    ).apply(instance, Binding::new)), Set.of("id", "kind", "definition", "slots"));
    public static final Codec<EquipmentBindings> CODEC = checked(
            BINDING.listOf(0, 8).xmap(EquipmentBindings::new, EquipmentBindings::bindings), Set.of());
    private static <A> Codec<A> checked(Codec<A> delegate, Set<String> fields) {
        return new Codec<>() {
            @Override public <T> DataResult<com.mojang.datafixers.util.Pair<A, T>> decode(DynamicOps<T> ops, T input) {
                try {
                    if (!fields.isEmpty()) {
                        var map = ops.getMap(input).getOrThrow();
                        var keys = map.entries().map(entry -> ops.getStringValue(entry.getFirst()).getOrThrow())
                                .collect(java.util.stream.Collectors.toSet());
                        if (!keys.equals(fields)) return DataResult.error(() -> "Expected equipment fields: " + fields);
                    }
                    return delegate.decode(ops, input);
                } catch (IllegalArgumentException | IllegalStateException failure) {
                    return DataResult.error(() -> "Invalid equipment bindings: " + failure.getMessage());
                }
            }
            @Override public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
                return delegate.encode(input, ops, prefix);
            }
        };
    }
    public static final EquipmentBindings EMPTY = new EquipmentBindings(List.of());
    public EquipmentBindings {
        bindings = List.copyOf(bindings);
        if (bindings.size() > 8 || bindings.stream().map(Binding::id).distinct().count() != bindings.size())
            throw new IllegalArgumentException("Equipment binding capacity or duplicate identity");
    }
    @SuppressWarnings("unchecked")
    public static DataComponentType<EquipmentBindings> component() {
        return (DataComponentType<EquipmentBindings>) BuiltInRegistries.DATA_COMPONENT_TYPE
                .get(Identifier.fromNamespaceAndPath("combatnexus", "equipment_bindings")).orElseThrow().value();
    }
}
