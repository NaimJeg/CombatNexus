package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleTags;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.jspecify.annotations.Nullable;

public record VanillaDamageSourceProfile(
        @Nullable Identifier damageTypeId,
        String msgId,

        @Nullable Entity attacker,
        @Nullable Entity directAttacker,
        @Nullable LivingEntity livingAttacker,
        LivingEntity victim,

        boolean directLivingAttack,
        boolean playerAttack,
        boolean mobAttack,
        boolean projectile,
        boolean explosion,
        boolean fire,
        boolean magic,
        boolean bypassesArmor,
        boolean bypassesEnchantments,
        boolean bypassesResistance,
        boolean bypassesCooldown
) {

    public static VanillaDamageSourceProfile create(DamageSource source, @Nullable LivingEntity attacker, LivingEntity victim) {
        return create(source, attacker, victim, DamageSourceClassification.isMagic(source));
    }

    public static VanillaDamageSourceProfile create(
            DamageSource source,
            @Nullable LivingEntity attacker,
            LivingEntity victim,
            boolean platformMagic
    ) {
        Entity sourceEntity = source.getEntity();
        Entity directEntity = source.getDirectEntity();

        LivingEntity livingAttacker =
                attacker != null
                        ? attacker
                        : sourceEntity instanceof LivingEntity le ? le : null;

        Identifier damageTypeId = source.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier())
                .orElse(null);

        boolean directLivingAttack =
                livingAttacker != null
                        && sourceEntity == livingAttacker
                        && directEntity == livingAttacker;

        boolean playerAttack =
                isId(damageTypeId, "minecraft", "player_attack");

        boolean mobAttack =
                isId(damageTypeId, "minecraft", "mob_attack")
                        || isId(damageTypeId, "minecraft", "mob_attack_no_aggro");

        boolean projectile =
                source.is(DamageTypeTags.IS_PROJECTILE)
                        || source.is(DamageModuleTags.DamageTypes.IS_PROJECTILE)
                        || source.is(DamageModuleTags.DamageTypes.IS_RANGED)
                        || directEntity instanceof Projectile;

        boolean explosion =
                source.is(DamageTypeTags.IS_EXPLOSION)
                        || source.is(DamageModuleTags.DamageTypes.IS_EXPLOSION);

        boolean fire =
                source.is(DamageTypeTags.IS_FIRE)
                        || source.is(DamageModuleTags.DamageTypes.IS_FIRE);

        boolean magic =
                platformMagic
                        || source.is(DamageModuleTags.DamageTypes.IS_MAGIC);

        return new VanillaDamageSourceProfile(
                damageTypeId,
                source.type().msgId(),

                sourceEntity,
                directEntity,
                livingAttacker,
                victim,

                directLivingAttack,
                playerAttack,
                mobAttack,
                projectile,
                explosion,
                fire,
                magic,

                source.is(DamageTypeTags.BYPASSES_ARMOR),
                source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS),
                source.is(DamageTypeTags.BYPASSES_RESISTANCE),
                source.is(DamageTypeTags.BYPASSES_COOLDOWN)
        );
    }

    private static boolean isId(
            @Nullable Identifier id,
            String namespace,
            String path
    ) {
        return id != null
                && namespace.equals(id.getNamespace())
                && path.equals(id.getPath());
    }

    public boolean shouldApplyMeleeOffensiveMobEffects() {
        return directLivingAttack
                && !projectile
                && (playerAttack || mobAttack);
    }
}
