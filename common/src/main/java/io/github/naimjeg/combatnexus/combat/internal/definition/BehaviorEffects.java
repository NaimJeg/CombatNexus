package io.github.naimjeg.combatnexus.combat.internal.definition;
import io.github.naimjeg.combatnexus.combat.api.behavior.*;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.effect.api.*;
import net.minecraft.resources.Identifier;
import java.util.*;
/** Explicit installation entry for equipment, code and commands. Definitions never activate themselves. */
public final class BehaviorEffects {
    private BehaviorEffects() {}
    public static boolean install(CombatRuntime world, UUID target, Identifier definition, Identifier source) {
        var spec = world.behaviorDefinitions().get(definition);
        if (spec == null) throw new IllegalArgumentException("Unknown behavior definition: " + definition);
        return world.effects().apply(new EffectInstance(new EffectSnapshot(target, definition, source, 0,
                OptionalLong.of(Math.addExact(world.level().getGameTime(), spec.durationTicks())), Set.of()), List.of(),
                EffectInstance.Ownership.MANAGED, spec.plan()), EffectService.Stacking.REPLACE);
    }
}
