package io.github.naimjeg.combatnexus.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.display.DamageContributionSourceKind;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageSourcePolicy;
import io.github.naimjeg.combatnexus.damage.registry.ModAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class VanillaThornsAttributeBridgeProcessor
        implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;
    private static final String TRACE_ID = "vanilla:thorns_attribute";

    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        LivingEntity owner = thornsOwner(ctx);
        if (owner == null) {
            return;
        }

        float bonus = thornsBonus(owner);
        if (!Float.isFinite(bonus) || bonus <= EPSILON) {
            return;
        }

        DamageMutationResult result = ctx.tryAddBaseDamage(
                ctx.getInitialChannel(),
                ctx.getInitialBaseBucket(),
                bonus,
                TRACE_ID
        );

        ctx.contributions().record(
                result,
                () -> VanillaContributionDescriptors.vanillaBase(
                        DamageModuleIds.id("vanilla_thorns_attribute"),
                        DamageContributionSourceKind.VANILLA_ENCHANTMENT,
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.getInitialBaseBucket(),
                        bonus,
                        TRACE_ID
                )
        );
    }

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        if (!ctx.isManaged()
                || !DamageSourcePolicy.isVanillaThorns(ctx.source())) {
            return false;
        }

        LivingEntity owner = thornsOwner(ctx);
        return owner != null && thornsBonus(owner) > EPSILON;
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_THORNS_ATTRIBUTE_BRIDGE;
    }

    private static LivingEntity thornsOwner(DamageModuleContext ctx) {
        Entity sourceEntity = ctx.source().getEntity();
        return sourceEntity instanceof LivingEntity living ? living : null;
    }

    private static float thornsBonus(LivingEntity owner) {
        if (!owner.getAttributes().hasAttribute(ModAttributes.THORNS)) {
            return 0.0f;
        }

        return (float) owner.getAttributeValue(ModAttributes.THORNS);
    }
}
