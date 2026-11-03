package io.github.naimjeg.combatnexus.combat.platform;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/** Version-specific damage-container projection after cancellable Pre listeners. */
public final class DamageApplicationAdapter {
    private DamageApplicationAdapter() {}
    public static float consumeShields(LivingEntity target, DamageContainer container, float amount) {
        float remaining = EquipmentCombatAdapter.nativeDamage().consumeShields(target, container.getSource(), amount);
        container.setNewDamage(remaining);
        return remaining;
    }
}
