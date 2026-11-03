package io.github.naimjeg.combatnexus.combat.healing.platform;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.Optional;

/** Attribution surrounds only the actual heal invocation, never the whole native tick. */
public final class NativeHealingCalls {
    private NativeHealingCalls() {}
    public static void call(LivingEntity target, float amount, Operation<Void> original,
                            Identifier id, HealingRequest.Reason reason, Entity owner) {
        var ownerId = owner != null && !owner.isRemoved() && owner.level() == target.level() && owner.isAddedToLevel()
                ? Optional.of(owner.getUUID()) : Optional.<java.util.UUID>empty();
        var source = new NativeHealingSource(new CombatSource(id, ownerId, Optional.empty()), reason);
        try (var ignored = NativeHealingSources.open(target, source)) { original.call(target, amount); }
    }
}
