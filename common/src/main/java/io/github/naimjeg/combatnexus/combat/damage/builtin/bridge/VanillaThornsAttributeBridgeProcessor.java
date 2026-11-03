package io.github.naimjeg.combatnexus.combat.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionSourceKind;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.display.VanillaContributionDescriptors;

import io.github.naimjeg.combatnexus.attribute.api.CombatAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class VanillaThornsAttributeBridgeProcessor
        implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;
    private static final String TRACE_ID = "vanilla:thorns_attribute";

    @Override
    public void apply(DamageRuleContext context) {
        var ctx = context;

        LivingEntity owner = thornsOwner(ctx);
        if (owner == null) {
            return;
        }

        float bonus = thornsBonus(ctx, owner);
        if (!Float.isFinite(bonus) || bonus <= EPSILON) {
            return;
        }

        DamageMutationResult result = ctx.tryAddBaseDamage(
                ctx.getInitialChannel(),
                ctx.nativeInput().initialBaseBucket(),
                bonus,
                TRACE_ID
        );

        ctx.recordContribution(
                result,
                () -> VanillaContributionDescriptors.vanillaBase(
                        DamageModuleIds.id("vanilla_thorns_attribute"),
                        DamageContributionSourceKind.VANILLA_ENCHANTMENT,
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        ctx.nativeInput().initialBaseBucket(),
                        bonus,
                        TRACE_ID
                )
        );
    }

    @Override
    public boolean canHandle(DamageContextView context) {
        var ctx = context;

        if (!ctx.isManaged()
                || !ctx.source().is(net.minecraft.world.damagesource.DamageTypes.THORNS)) {
            return false;
        }

        LivingEntity owner = thornsOwner(ctx);
        return owner != null && thornsBonus(ctx, owner) > EPSILON;
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_THORNS_ATTRIBUTE_BRIDGE;
    }

    private static LivingEntity thornsOwner(DamageContextView ctx) {
        Entity sourceEntity = ctx.source().getEntity();
        return sourceEntity instanceof LivingEntity living ? living : null;
    }

    private static float thornsBonus(DamageContextView context, LivingEntity owner) {
        return context.attributeSnapshot(owner.getUUID())
                .map(snapshot -> snapshot.valueOrZero(CombatAttributes.THORNS.identifier()))
                .orElse(0.0f);
    }
}
