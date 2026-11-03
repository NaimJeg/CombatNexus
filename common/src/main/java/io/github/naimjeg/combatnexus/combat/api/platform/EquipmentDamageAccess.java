package io.github.naimjeg.combatnexus.combat.api.platform;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import java.util.function.Consumer;

/** Startup-issued native equipment adapter. A receipt sink is valid only inside its native call. */
@FunctionalInterface
public interface EquipmentDamageAccess {
    void apply(LivingEntity target, DamageSource source, Consumer<Consumer<EquipmentDamageReceipt>> nativeCall);
}
