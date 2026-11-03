package io.github.naimjeg.combatnexus.combat.api.input;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** Request-scoped input captured before extension callbacks; mutable values never escape. */
public final class CombatEntityFacts {
    private final float health, maxHealth;
    private final boolean onFire;
    private final FrozenFacts external;
    private final ItemStack weapon;
    private final int chargeTicks;
    private final float attackStrength;
    private final Map<EquipmentSlot, ItemStack> equipment = new EnumMap<>(EquipmentSlot.class);
    private final io.github.naimjeg.combatnexus.effect.api.EffectFacts effects;
    private CombatEntityFacts(LivingEntity entity) {
        weapon=entity.getWeaponItem().copy();chargeTicks=entity.getTicksUsingItem();
        attackStrength=entity instanceof net.minecraft.world.entity.player.Player player?player.getAttackStrengthScale(0.5F):1;
        health = entity.getHealth(); maxHealth = entity.getMaxHealth(); onFire = entity.isOnFire();
        for (var slot : EquipmentSlot.values()) equipment.put(slot, entity.getItemBySlot(slot).copy());
        effects = io.github.naimjeg.combatnexus.effect.api.EffectFacts.capture(entity);
        external = io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.captureFacts(entity);
    }
    public static CombatEntityFacts capture(LivingEntity entity) { return new CombatEntityFacts(entity); }
    public float health() { return health; }
    public float maxHealth() { return maxHealth; }
    public boolean onFire() { return onFire; }
    public FrozenFacts external() { return external; }
    public ItemStack weapon(){return weapon.copy();}
    public int chargeTicks(){return chargeTicks;}
    public float attackStrength(){return attackStrength;}
    public ItemStack equipment(EquipmentSlot slot) { return equipment.getOrDefault(slot, ItemStack.EMPTY).copy(); }
    public List<MobEffectInstance> effects() { return effects.values(); }
    public List<io.github.naimjeg.combatnexus.effect.api.EffectView> effectViews() { return effects.views(); }
    public boolean hasEffect(net.minecraft.resources.Identifier effect) { return effects.hasEffect(effect); }
    public boolean hasEffect(Holder<MobEffect> effect) { return effects.hasEffect(effect); }
    public OptionalInt amplifier(Holder<MobEffect> effect) {
        return effects.amplifier(effect);
    }
}
