package io.github.naimjeg.combatnexus.combat.shield.internal;
import io.github.naimjeg.combatnexus.combat.shield.api.*;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;
import io.github.naimjeg.combatnexus.config.DefinitionJson;
import net.minecraft.resources.Identifier;
import java.util.*;
public final class ShieldDefinitions {
    private ShieldDefinitions() {}
    public static ShieldGrantReceipt grant(CombatRuntime world, UUID target, Identifier definition, CombatSource source,
            double initialBalance, int durationTicks, CombatParent parent) {
        DefinitionJson.range(durationTicks, 1, 1200000, "duration_ticks");
        var value = world.domainDefinitions().shields().get(definition);
        if (value == null) throw new IllegalArgumentException("Unknown shield definition: " + definition);
        return world.shields().replace(target, new ShieldGrant(source, value, initialBalance,
                OptionalLong.of(Math.addExact(world.level().getGameTime(), durationTicks))), parent);
    }
}
