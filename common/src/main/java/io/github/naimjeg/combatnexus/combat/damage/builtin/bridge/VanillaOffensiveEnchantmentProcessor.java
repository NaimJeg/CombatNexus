package io.github.naimjeg.combatnexus.combat.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.display.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;

import java.util.Locale;

public final class VanillaOffensiveEnchantmentProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;

    private static String traceId(DamageContextView ctx) {
        return ctx.nativeInput().projectile()
                ? "vanilla:projectile_enchantment"
                : "vanilla:melee_enchantment";
    }

    @Override
    public void apply(DamageRuleContext context) {
        var ctx = context;

        NativeDamageInput.Offense snapshot =
                ctx.nativeInput().offense().orElse(null);

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
                ctx.nativeInput().enchantmentBucket(),
                delta,
                traceId
        );

        ctx.recordContribution(
                result,
                () -> VanillaContributionDescriptors.vanillaEnchantmentBase(
                        DamageModuleIds.id(
                                "vanilla_offensive_enchantment/"
                                        + ctx.nativeInput().enchantmentBucket()
                                        .name()
                                        .toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.nativeInput().enchantmentBucket(),
                        delta,
                        traceId
                )
        );
    }

    @Override
    public boolean canHandle(DamageContextView context) {
        var ctx = context;

        NativeDamageInput.Offense snapshot =
                ctx.nativeInput().offense().orElse(null);

        return ctx.nativeInput().rebuildEnchantments()
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
