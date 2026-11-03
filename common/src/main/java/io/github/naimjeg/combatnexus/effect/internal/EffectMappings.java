package io.github.naimjeg.combatnexus.effect.internal;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.effect.api.*;

/** Startup capabilities referenced by definitions and fact requests. */
public final class EffectMappings {
    private static final Map<Identifier, EffectDisplay> DISPLAYS = new LinkedHashMap<>();
    private static final Map<Identifier, EffectCondition> CONDITIONS = new LinkedHashMap<>();
    private static boolean frozen;
    private EffectMappings() {}
    public static void display(Identifier id, EffectDisplay value) {
        if (frozen) throw new IllegalStateException("Effect mappings frozen");
        if (DISPLAYS.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate effect display " + id);
    }
    public static void condition(Identifier id, EffectCondition value) {
        if (frozen) throw new IllegalStateException("Effect mappings frozen");
        if (CONDITIONS.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate effect condition " + id);
    }
    public static EffectDisplay display(Identifier id) { return DISPLAYS.getOrDefault(id, EffectDisplay.NONE); }
    public static boolean projected(Identifier id) { return DISPLAYS.values().stream().anyMatch(d -> id.equals(d.nativeId())); }
    public static boolean satisfied(EffectFact fact) {
        var condition = CONDITIONS.get(fact.condition());
        if (condition == null) throw new IllegalArgumentException("Unknown effect condition " + fact.condition());
        return condition.test(fact.values());
    }
    public static void freeze() { frozen = true; }
}
