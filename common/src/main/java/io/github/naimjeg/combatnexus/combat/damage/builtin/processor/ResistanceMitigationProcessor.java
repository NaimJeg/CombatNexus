package io.github.naimjeg.combatnexus.combat.damage.builtin.processor;

import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.core.DamageComponent;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.attribute.api.CombatAttributes;
import net.minecraft.core.Holder;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.Objects;

public class ResistanceMitigationProcessor implements DamagePhaseProcessor {
    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        for (int i = 0; i < ctx.getActiveComponentCount(); i++) {
            DamageComponent component = ctx.getActiveComponent(i);

            float currentDmg = component.getPostMitigationAmount();
            if (currentDmg <= 0.0f) {
                continue;
            }

            Holder<Attribute> channelAttribute =
                    ctx.definitions().damage().channels().data(component.channel).resistanceAttribute();
            Holder<Attribute> categoryAttribute = categoryAttribute(ctx);

            float channelRating =
                    channelAttribute != null
                            ? finiteOrZero(ctx.getVictimAttrOrZero(channelAttribute))
                            : 0.0f;

            float tempRating = finiteOrZero(
                    component.getTemporaryResistanceRating());

            float categoryRating = categoryAttribute != null
                    && !sameAttribute(channelAttribute, categoryAttribute)
                    ? finiteOrZero(ctx.getVictimAttrOrZero(categoryAttribute))
                    : 0.0f;

            float totalRating = totalRating(
                    channelRating, tempRating, categoryRating);

            if (totalRating == 0.0f) {
                continue;
            }

            float kValue = Math.max(
                    0.0001f,
                    ctx.settings().values().formulas().resistanceKValue()
            );

            float reduction = reductionFor(totalRating, kValue);

            ctx.tryAddChannelMitigation(
                    component.channel,
                    reduction,
                    "dn:resistance"
            );

        }
    }

    @Override
    public boolean canHandle(DamageContextView context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        return shouldMitigate(
                ctx.isManaged(),
                ctx.source().is(DamageTypeTags.BYPASSES_EFFECTS),
                ctx.source().is(DamageTypeTags.BYPASSES_RESISTANCE)
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.MITIGATION_SETUP;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.DN_RESISTANCE_MITIGATION;
    }

    static float finiteOrZero(float value) {
        return io.github.naimjeg.combatnexus.formula.ResistanceFormula.finiteOrZero(value);
    }

    static float totalRating(
            float channelRating,
            float temporaryRating,
            float categoryRating
    ) {
        return io.github.naimjeg.combatnexus.formula.ResistanceFormula.totalRating(channelRating, temporaryRating, categoryRating);
    }

    /**
     * Positive ratings use the asymptotic rating / (rating + K) formula and
     * cap at 95% reduction. Negative ratings use rating / K, so they express
     * vulnerability without a gameplay-scale negative reduction cap.
     */
    static float reductionFor(float rating, float kValue) {
        return io.github.naimjeg.combatnexus.formula.ResistanceFormula.reductionFor(rating, kValue);
    }

    static boolean sameAttribute(
            Holder<Attribute> first,
            Holder<Attribute> second
    ) {
        return sameReference(first, second);
    }

    static boolean shouldMitigate(
            boolean managed,
            boolean bypassesEffects,
            boolean bypassesResistance
    ) {
        return managed && !bypassesEffects && !bypassesResistance;
    }

    static boolean sameReference(Object first, Object second) {
        return first != null && second != null
                && (first == second || Objects.equals(first, second));
    }

    private static Holder<Attribute> categoryAttribute(
            DamageModuleContext context
    ) {
        if (context.isProjectileDamage()) {
            return CombatAttributes.resistanceProjectile();
        }
        if (context.isMeleeDamage()) {
            return CombatAttributes.resistanceMelee();
        }
        return null;
    }
}

