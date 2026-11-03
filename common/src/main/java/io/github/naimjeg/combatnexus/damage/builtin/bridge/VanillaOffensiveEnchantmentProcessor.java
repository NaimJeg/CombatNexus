package io.github.naimjeg.combatnexus.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.bridge.vanilla.VanillaDamageCapture;
import io.github.naimjeg.combatnexus.damage.core.contribution.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class VanillaOffensiveEnchantmentProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;

    private static String traceId(DamageModuleContext ctx) {
        return ctx.vanillaSourceProfile().projectile()
                ? "vanilla:projectile_enchantment"
                : "vanilla:melee_enchantment";
    }

    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        VanillaDamageCapture.OffensiveSnapshot snapshot =
                ctx.getVanillaSnapshot();

        if (snapshot == null) {
            return;
        }

        float delta = snapshot.enchantDelta();

        if (!Float.isFinite(delta) || Math.abs(delta) <= EPSILON) {
            return;
        }

        String traceId = traceId(ctx);

        DamageMutationResult result = ctx.tryAddBaseDamage(
                ctx.getInitialChannel(),
                ctx.getVanillaOffensiveEnchantmentBucket(),
                delta,
                traceId
        );

        ctx.contributions().record(
                result,
                () -> VanillaContributionDescriptors.vanillaEnchantmentBase(
                        DamageModuleIds.id(
                                "vanilla_offensive_enchantment/"
                                        + ctx.getVanillaOffensiveEnchantmentBucket()
                                        .name()
                                        .toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.getVanillaOffensiveEnchantmentBucket(),
                        delta,
                        traceId
                )
        );
    }

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        VanillaDamageCapture.OffensiveSnapshot snapshot =
                ctx.getVanillaSnapshot();

        return ctx.shouldRebuildVanillaOffensiveEnchantment()
                && snapshot != null
                && Math.abs(snapshot.enchantDelta()) > EPSILON;
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_OFFENSIVE_ENCHANTMENT;
    }
}
