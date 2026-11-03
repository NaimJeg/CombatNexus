package io.github.naimjeg.combatnexus.effect.internal;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.effect.api.*;

/** Startup registry shared by native and custom effect plans. */
public final class EffectCapabilities {
    private static final Map<Identifier, EffectCapability> VALUES = new LinkedHashMap<>();
    private static boolean frozen;
    private EffectCapabilities() {}
    public static synchronized void register(Identifier id, EffectCapability capability) {
        if (frozen) throw new IllegalStateException("Effect capabilities frozen");
        if (VALUES.putIfAbsent(Objects.requireNonNull(id), Objects.requireNonNull(capability)) != null)
            throw new IllegalArgumentException("Duplicate effect capability " + id);
    }
    public static java.util.List<net.minecraft.network.chat.Component> describeRequired(Identifier id, int amplifier) {
        var value = VALUES.get(id);
        if (value == null) throw new IllegalArgumentException("Unknown effect description: " + id);
        return value.describe(amplifier);
    }
    public static void freeze() { frozen = true; }
    public static boolean execute(Identifier id, EffectCapability.Context context) {
        var capability = VALUES.get(id);
        if (capability == null) throw new IllegalArgumentException("Unknown effect capability " + id);
        return capability.execute(context);
    }
    public static java.util.List<net.minecraft.network.chat.Component> describe(Identifier id, int amplifier) {
        var value = VALUES.get(id);
        return value == null ? java.util.List.of(net.minecraft.network.chat.Component.translatable("tooltip.combatnexus.effect.unknown_behavior")) : value.describe(amplifier);
    }
}
