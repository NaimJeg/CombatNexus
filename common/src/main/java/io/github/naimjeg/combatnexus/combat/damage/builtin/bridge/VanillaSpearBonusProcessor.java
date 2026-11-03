package io.github.naimjeg.combatnexus.combat.damage.builtin.bridge;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.DamageProcessorPriorities;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;

import io.github.naimjeg.combatnexus.combat.damage.api.display.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;

import java.util.Locale;

public final class VanillaSpearBonusProcessor implements DamagePhaseProcessor {

    private static final float EPSILON = 0.0001f;

    private static boolean isSpearBonus(NativeDamageInput.DeltaKind kind) {
        return kind == NativeDamageInput.DeltaKind.SPEAR_STAB_BONUS
                || kind == NativeDamageInput.DeltaKind.SPEAR_CHARGE_BONUS
                || kind == NativeDamageInput.DeltaKind.SPEAR_ATTACK_BONUS;
    }

    private static String traceId(NativeDamageInput.DeltaKind kind) {
        return switch (kind) {
            case SPEAR_STAB_BONUS -> "vanilla:special/spear_stab";
            case SPEAR_CHARGE_BONUS -> "vanilla:special/spear_charge";
            case SPEAR_ATTACK_BONUS -> "vanilla:special/spear_attack";
            default -> throw new IllegalArgumentException(
                    "Unsupported spear bonus delta kind: " + kind
            );
        };
    }

    @Override
    public boolean canHandle(DamageContextView context) {
        var ctx = context;

        if (!ctx.nativeInput().rebuildPreEvent()) {
            return false;
        }

        NativeDamageInput.Offense snapshot =
                ctx.nativeInput().offense().orElse(null);

        if (snapshot == null) {
            return false;
        }

        NativeDamageInput.Delta delta =
                snapshot.preEventDelta();

        return isSpearBonus(delta.kind())
                && Float.isFinite(delta.delta())
                && Math.abs(delta.delta()) > EPSILON;
    }

    @Override
    public void apply(DamageRuleContext context) {
        var ctx = context;

        NativeDamageInput.Delta delta =
                ctx.nativeInput().offense().orElse(null).preEventDelta();

        DamageApplicationBucket bucket =
                DamageApplicationBucket.VANILLA_WEAPON_SPECIAL;

        String traceId = traceId(delta.kind());

        DamageMutationResult result = ctx.tryAddVanillaBaseReconstructedDamage(
                ctx.getInitialChannel(),
                bucket,
                delta.delta(),
                traceId
        );

        ctx.recordContribution(
                result,
                () -> VanillaContributionDescriptors.vanillaBase(
                        DamageModuleIds.id(
                                "vanilla_spear_bonus/"
                                        + delta.kind().name().toLowerCase(Locale.ROOT)
                        ),
                        DamagePhase.BASE_MODIFICATION,
                        ctx.getInitialChannel().id(),
                        bucket,
                        delta.delta(),
                        traceId
                )
        );
    }

    @Override
    public DamagePhase phase() {
        return DamagePhase.BASE_MODIFICATION;
    }

    @Override
    public int getPriority() {
        return DamageProcessorPriorities.VANILLA_WEAPON_SPECIAL_BONUS;
    }
}
