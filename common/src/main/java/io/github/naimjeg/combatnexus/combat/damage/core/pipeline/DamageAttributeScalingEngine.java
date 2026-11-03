package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.core.DamageComponent;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.combatnexus.attribute.api.CombatAttributes;
import io.github.naimjeg.combatnexus.combat.damage.registry.PreMultiplierBuckets;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Private one-shot handoff after all TYPE_SCALING callbacks have completed. */
final class DamageAttributeScalingEngine {
    private static final String TRACE_PREFIX = "attribute:";

    private DamageAttributeScalingEngine() {
    }

    static void execute(DamageModuleContext context) {
        if (!context.claimAttributeScaling()) {
            throw new IllegalStateException(
                    "Damage attribute scaling already executed for this transaction");
        }

        float vulnerability = finiteOrZero(context.getVictimAttrOrZero(CombatAttributes.vulnerableDamageAdditive()));
        if (vulnerability != 0.0f) {
            context.tryAddGlobalPreMultiplier(PreMultiplierBuckets.vulnerableDamage(), vulnerability,
                    traceId(CombatAttributes.vulnerableDamageAdditive()));
        }
        int componentCount = context.getActiveComponentCount();
        for (int index = 0; index < componentCount; index++) {
            DamageComponent component = context.getActiveComponent(index);
            DamageAttributeMappings.ChannelMapping mapping =
                    DamageAttributeMappings.forChannel(component.channel);
            if (mapping == null) {
                continue;
            }
            float value = finiteOrZero(
                    context.getLogicalAttackerAttrOrZero(mapping.attribute()));
            if (value == 0.0f) {
                continue;
            }
            int bucket = PreMultiplierBucketRegistry.getPreMultiplierBucketId(
                    mapping.bucketId());
            context.tryAddChannelPreMultiplier(
                    component.channel,
                    bucket,
                    value,
                    traceId(mapping.attribute())
            );
        }

        Holder<Attribute> categoryAttribute = switch (context.attackCategory()) {
            case MELEE -> CombatAttributes.meleeDamageAdditive();
            case PROJECTILE -> CombatAttributes.projectileDamageAdditive();
            case NONE -> null;
        };
        if (categoryAttribute == null) {
            return;
        }
        float categoryValue = finiteOrZero(
                context.getLogicalAttackerAttrOrZero(categoryAttribute));
        if (categoryValue == 0.0f) {
            return;
        }
        context.tryAddGlobalPreMultiplier(
                PreMultiplierBuckets.genericDamage(),
                categoryValue,
                traceId(categoryAttribute)
        );
    }

    static float finiteOrZero(float value) {
        return Float.isFinite(value) ? value : 0.0f;
    }

    private static String traceId(Holder<Attribute> attribute) {
        return TRACE_PREFIX + attribute.getKey().identifier();
    }
}
