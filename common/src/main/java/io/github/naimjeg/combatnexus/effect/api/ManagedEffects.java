package io.github.naimjeg.combatnexus.effect.api;

import io.github.naimjeg.combatnexus.attribute.api.AttributeSnapshot;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import java.util.*;

/** Source-scoped managed agility; source must distinguish each caster/cast instance. */
public final class ManagedEffects {
    private ManagedEffects() {}
    public static EffectInstance agility(UUID target, Identifier source, double speedBonus, long expiresAtTick) {
        if (!Double.isFinite(speedBonus) || speedBonus < 0 || speedBonus > 10 || expiresAtTick < 0) {
            throw new IllegalArgumentException("Invalid agility contribution");
        }
        var id = Identifier.fromNamespaceAndPath("combatnexus", "agility");
        return new EffectInstance(new EffectSnapshot(target, id, source, 0, OptionalLong.of(expiresAtTick), Set.of()),
                List.of(new EffectInstance.AttributeContribution(Identifier.withDefaultNamespace("movement_speed"),
                        new AttributeSnapshot.Contribution(id, speedBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL))),
                EffectInstance.Ownership.MANAGED);
    }
}
