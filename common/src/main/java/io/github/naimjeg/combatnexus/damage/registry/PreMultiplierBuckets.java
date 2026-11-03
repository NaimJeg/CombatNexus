package io.github.naimjeg.combatnexus.damage.registry;

import io.github.naimjeg.combatnexus.damage.api.DamageModulePreMultiplierBuckets;
import io.github.naimjeg.combatnexus.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleRegistrationAccess;
import io.github.naimjeg.combatnexus.damage.core.registry.PreMultiplierBucketRegistry;
import net.minecraft.resources.Identifier;

/** Runtime slot cache for the public pre-multiplier bucket IDs. */
public final class PreMultiplierBuckets {

    private static int critDamage = -1;
    private static int physicalDamage = -1;
    private static int fireDamage = -1;
    private static int coldDamage = -1;
    private static int lightningDamage = -1;
    private static int magicDamage = -1;
    private static int poisonDamage = -1;
    private static int witherDamage = -1;
    private static int kineticDamage = -1;
    private static int genericDamage = -1;

    private static int vanillaDifficulty = -1;
    private static int vanillaSpecialAttack = -1;
    private static int vanillaSpearStab = -1;
    private static int vanillaSpearCharge = -1;
    private static int vanillaSpearAttack = -1;
    private static int vanillaPlayerAttack = -1;
    private static int vanillaProjectile = -1;

    private static boolean registered = false;

    private PreMultiplierBuckets() {
    }

    public static void register(DamageModuleRegistrationAccess access) {
        DamageModuleLifecycle.requireRegistering(
                access,
                "PreMultiplierBuckets.register"
        );

        if (registered) {
            return;
        }

        critDamage = register(
                access, DamageModulePreMultiplierBuckets.CRIT_DAMAGE);
        physicalDamage = register(
                access, DamageModulePreMultiplierBuckets.PHYSICAL_DAMAGE);
        fireDamage = register(
                access, DamageModulePreMultiplierBuckets.FIRE_DAMAGE);
        coldDamage = register(
                access, DamageModulePreMultiplierBuckets.COLD_DAMAGE);
        lightningDamage = register(
                access, DamageModulePreMultiplierBuckets.LIGHTNING_DAMAGE);
        magicDamage = register(
                access, DamageModulePreMultiplierBuckets.MAGIC_DAMAGE);
        poisonDamage = register(
                access, DamageModulePreMultiplierBuckets.POISON_DAMAGE);
        witherDamage = register(
                access, DamageModulePreMultiplierBuckets.WITHER_DAMAGE);
        kineticDamage = register(
                access, DamageModulePreMultiplierBuckets.KINETIC_DAMAGE);
        genericDamage = register(
                access, DamageModulePreMultiplierBuckets.GENERIC_DAMAGE);

        vanillaDifficulty = register(
                access, DamageModulePreMultiplierBuckets.VANILLA_DIFFICULTY);
        vanillaSpecialAttack = register(
                access,
                DamageModulePreMultiplierBuckets.VANILLA_SPECIAL_ATTACK);
        vanillaSpearStab = register(
                access, DamageModulePreMultiplierBuckets.VANILLA_SPEAR_STAB);
        vanillaSpearCharge = register(
                access, DamageModulePreMultiplierBuckets.VANILLA_SPEAR_CHARGE);
        vanillaSpearAttack = register(
                access, DamageModulePreMultiplierBuckets.VANILLA_SPEAR_ATTACK);
        vanillaPlayerAttack = register(
                access,
                DamageModulePreMultiplierBuckets.VANILLA_PLAYER_ATTACK);
        vanillaProjectile = register(
                access, DamageModulePreMultiplierBuckets.VANILLA_PROJECTILE);

        registered = true;
    }

    public static int forChannelDamage(DamageChannel channel) {
        Identifier id = channel.id();

        if (id.equals(DamageChannel.PHYSICAL_ID)) {
            return physicalDamage;
        }

        if (id.equals(DamageChannel.FIRE_ID)) {
            return fireDamage;
        }

        if (id.equals(DamageChannel.COLD_ID)) {
            return coldDamage;
        }

        if (id.equals(DamageChannel.LIGHTNING_ID)) {
            return lightningDamage;
        }

        if (id.equals(DamageChannel.MAGIC_ID)) {
            return magicDamage;
        }

        if (id.equals(DamageChannel.POISON_ID)) {
            return poisonDamage;
        }

        if (id.equals(DamageChannel.WITHER_ID)) {
            return witherDamage;
        }

        if (id.equals(DamageChannel.KINETIC_ID)) {
            return kineticDamage;
        }

        return genericDamage;
    }

    public static int critDamage() {
        return critDamage;
    }

    public static int genericDamage() {
        return genericDamage;
    }

    public static int vanillaDifficulty() {
        return vanillaDifficulty;
    }

    public static int vanillaPlayerAttack() {
        return vanillaPlayerAttack;
    }

    public static int vanillaProjectile() {
        return vanillaProjectile;
    }

    private static int register(
            DamageModuleRegistrationAccess access,
            Identifier id
    ) {
        return PreMultiplierBucketRegistry.registerPreMultiplierBucket(
                access,
                id
        );
    }

}
