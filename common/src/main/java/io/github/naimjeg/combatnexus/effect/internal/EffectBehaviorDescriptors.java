package io.github.naimjeg.combatnexus.effect.internal;

import io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorComponent;
import io.github.naimjeg.combatnexus.combat.internal.registration.BehaviorTypes;
import io.github.naimjeg.combatnexus.effect.api.EffectBehaviorDescriptor;
import io.github.naimjeg.combatnexus.presentation.api.TooltipBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.*;

public final class EffectBehaviorDescriptors {
    private record Binding<T extends BehaviorComponent>(Class<T> type, EffectBehaviorDescriptor<T> descriptor) {
        TooltipBlock describe(BehaviorComponent value, int level) { return Objects.requireNonNull(descriptor.describe(type.cast(value), level)); }
    }
    private static final Map<Identifier, Binding<?>> BINDINGS = new LinkedHashMap<>();
    private static boolean frozen;
    static { BuiltinEffectBehaviorDescriptors.register(); }
    private EffectBehaviorDescriptors() {}
    public static synchronized <T extends BehaviorComponent> void register(Identifier id, Class<T> type, EffectBehaviorDescriptor<T> descriptor) {
        if (frozen) throw new IllegalStateException("Effect descriptors frozen");
        if (BehaviorTypes.registered(id).implementation() != type) throw new IllegalArgumentException("Effect descriptor type mismatch: " + id);
        if (BINDINGS.putIfAbsent(Objects.requireNonNull(id), new Binding<>(Objects.requireNonNull(type), Objects.requireNonNull(descriptor))) != null)
            throw new IllegalArgumentException("Duplicate effect descriptor: " + id);
    }
    public static synchronized void freeze() {
        BINDINGS.forEach((id, binding) -> {
            if (BehaviorTypes.registered(id).implementation() != binding.type()) throw new IllegalStateException("Effect descriptor type mismatch: " + id);
        });
        frozen = true;
    }
    public static TooltipBlock describe(BehaviorComponent value, int level) {
        var type = BehaviorTypes.type(value);
        var binding = BINDINGS.get(type.id());
        if (binding == null) return new TooltipBlock(Component.empty(), List.of(Component.translatable("tooltip.combatnexus.effect.unknown_behavior")), List.of());
        if (binding.type() != value.getClass()) throw new IllegalStateException("Effect descriptor type mismatch: " + type.id());
        return binding.describe(value, level);
    }
}
