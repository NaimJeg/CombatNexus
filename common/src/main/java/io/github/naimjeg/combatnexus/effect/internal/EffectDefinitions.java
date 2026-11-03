package io.github.naimjeg.combatnexus.effect.internal;
import io.github.naimjeg.combatnexus.effect.api.*;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import net.minecraft.resources.Identifier;
import java.util.UUID;
/** Explicit installation; loading a definition never grants an effect. */
public final class EffectDefinitions {
    private EffectDefinitions() {}
    public static boolean install(CombatRuntime world, UUID target, Identifier definition, Identifier source) {
        var value = world.domainDefinitions().effects().get(definition);
        if (value == null) throw new IllegalArgumentException("Unknown effect definition: " + definition);
        return world.effects().apply(value.instantiate(target, definition, source, world.level().getGameTime()), value.stacking());
    }
}
