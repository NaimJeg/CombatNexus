package io.github.naimjeg.combatnexus.damage.diagnostics;

import io.github.naimjeg.combatnexus.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.combatnexus.damage.core.settlement.DamageSettlementMixinStatus;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleLifecycleLog;
import io.github.naimjeg.combatnexus.damage.registry.PreMultiplierBuckets;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.damage.registry.rule.DamageRuleConditionTypes;
import net.minecraft.resources.Identifier;

public final class DamageModuleStartupSelfCheck {

    private DamageModuleStartupSelfCheck() {
    }

    public static void run() {
        requireConditionCodec(
                DamageModuleConditionIds.ATTACKER_ENTITY_TYPE_TAG
        );

        requirePreMultiplierRegistryFrozen();
        requireGenericDamageBucket();
        requireTrueDamageBucketFlags();
        requireSettlementMixinApplied();

        DamageModuleLifecycleLog.startupSelfCheckPassed();
    }

    private static void requireConditionCodec(Identifier id) {
        try {
            DamageRuleConditionTypes.codec(id);
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "Missing DamageModule condition codec registration: " + id,
                    e
            );
        }
    }

    private static void requireTrueDamageBucketFlags() {
        DamageApplicationBucket bucket = DamageApplicationBucket.DN_TRUE_DAMAGE;

        if (bucket.affectedByChannelPreMultiplier()) {
            throw new IllegalStateException(
                    "DN_TRUE_DAMAGE must not be affected by channel pre multipliers."
            );
        }

        if (bucket.affectedByGlobalPreMultiplier()) {
            throw new IllegalStateException(
                    "DN_TRUE_DAMAGE must not be affected by global pre multipliers."
            );
        }

        if (bucket.affectedByPostMultiplier()) {
            throw new IllegalStateException(
                    "DN_TRUE_DAMAGE must not be affected by post multipliers."
            );
        }

        if (bucket.affectedByMitigation()) {
            throw new IllegalStateException(
                    "DN_TRUE_DAMAGE must not be affected by mitigation."
            );
        }
    }

    private static void requirePreMultiplierRegistryFrozen() {
        PreMultiplierBucketRegistry.requireFrozen();
    }

    private static void requireGenericDamageBucket() {
        if (PreMultiplierBuckets.genericDamage() < 0) {
            throw new IllegalStateException(
                    "GENERIC_DAMAGE pre-multiplier bucket was not registered."
            );
        }

        if (PreMultiplierBuckets.genericDamage()
                >= PreMultiplierBucketRegistry.bucketCount()) {
            throw new IllegalStateException(
                    "GENERIC_DAMAGE pre-multiplier bucket id is outside registry bounds: "
                            + PreMultiplierBuckets.genericDamage()
            );
        }
    }

    private static void requireSettlementMixinApplied() {
        if (!DamageSettlementMixinStatus.isApplied()) {
            throw new IllegalStateException(
                    "LivingEntity damage settlement mixin was not applied"
            );
        }
    }
}

