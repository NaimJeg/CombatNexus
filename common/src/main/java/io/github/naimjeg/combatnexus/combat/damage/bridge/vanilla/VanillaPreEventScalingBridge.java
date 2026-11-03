package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.display.VanillaContributionDescriptors;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.context.NativeDamageInput;
import net.minecraft.resources.Identifier;

import java.util.Locale;

public final class VanillaPreEventScalingBridge {

    private static final float EPSILON = 0.0001f;

    private VanillaPreEventScalingBridge() {
    }

    public static boolean canApply(
            DamageContextView ctx,
            NativeDamageInput.DeltaKind expectedKind,
            boolean allowPositiveRatio
    ) {
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

        if (delta.kind() != expectedKind) {
            return false;
        }

        float ratio = delta.ratio();

        if (!Float.isFinite(ratio)) {
            return false;
        }

        if (Math.abs(ratio - 1.0f) <= EPSILON) {
            return false;
        }

        /*
         * Player attack scaling currently means vanilla attack-cooldown /
         * attack-strength penalty. It must not absorb positive bonuses such as
         * crit, enchantment, mace, spear, or unknown modded scaling.
         */
        return allowPositiveRatio || !(ratio > 1.0f + EPSILON);
    }

    public static void applyApplicationPreMultiplier(
            DamageRuleContext ctx,
            NativeDamageInput.DeltaKind expectedKind,
            DamageApplicationBucket applicationBucket,
            int preMultiplierBucket,
            Identifier preMultiplierBucketId,
            String traceId,
            boolean allowPositiveRatio
    ) {
        if (!canApply(ctx, expectedKind, allowPositiveRatio)) {
            return;
        }

        float value = multiplierDelta(ctx);

        if (value == 0.0f) {
            return;
        }

        recordApplicationPreMultiplier(
                ctx,
                applicationBucket,
                preMultiplierBucket,
                preMultiplierBucketId,
                value,
                traceId
        );
    }

    public static void applyApplicationPreMultiplierToAll(
            DamageRuleContext ctx,
            NativeDamageInput.DeltaKind expectedKind,
            int preMultiplierBucket,
            Identifier preMultiplierBucketId,
            String traceId,
            boolean allowPositiveRatio,
            DamageApplicationBucket... applicationBuckets
    ) {
        if (!canApply(ctx, expectedKind, allowPositiveRatio)) {
            return;
        }

        float value = multiplierDelta(ctx);

        if (value == 0.0f) {
            return;
        }

        if (applicationBuckets == null) {
            return;
        }

        for (DamageApplicationBucket applicationBucket : applicationBuckets) {
            recordApplicationPreMultiplier(
                    ctx,
                    applicationBucket,
                    preMultiplierBucket,
                    preMultiplierBucketId,
                    value,
                    traceId
            );
        }
    }

    private static void recordApplicationPreMultiplier(
            DamageRuleContext ctx,
            DamageApplicationBucket applicationBucket,
            int preMultiplierBucket,
            Identifier preMultiplierBucketId,
            float value,
            String traceId
    ) {
        if (applicationBucket == null) {
            return;
        }

        DamageMutationResult result = ctx.tryAddApplicationPreMultiplier(
                applicationBucket,
                preMultiplierBucket,
                value,
                traceId
        );

        ctx.recordContribution(
                result,
                () -> VanillaContributionDescriptors.vanillaMultiplier(
                        contributionId(traceId, applicationBucket),
                        DamagePhase.GLOBAL_ADJUSTMENT,
                        ctx.getInitialChannel().id(),
                        applicationBucket,
                        preMultiplierBucketId,
                        value,
                        traceId
                )
        );
    }

    private static float multiplierDelta(DamageContextView ctx) {
        NativeDamageInput.Offense snapshot =
                ctx.nativeInput().offense().orElse(null);

        if (snapshot == null) {
            return 0.0f;
        }

        float ratio = snapshot.preEventDelta().ratio();

        if (!Float.isFinite(ratio)) {
            return 0.0f;
        }

        float value = ratio - 1.0f;

        return Math.abs(value) > EPSILON ? value : 0.0f;
    }

    private static Identifier contributionId(
            String traceId,
            DamageApplicationBucket bucket
    ) {
        String normalizedTrace = traceId == null || traceId.isBlank()
                ? "unknown"
                : traceId;

        int namespaceSeparator = normalizedTrace.indexOf(':');

        if (namespaceSeparator >= 0) {
            normalizedTrace = normalizedTrace.substring(namespaceSeparator + 1);
        }

        normalizedTrace = normalizedTrace
                .replace(':', '_')
                .replace('/', '_')
                .replace(' ', '_')
                .toLowerCase(Locale.ROOT);

        return DamageModuleIds.id(
                "vanilla_pre_event/"
                        + normalizedTrace
                        + "/"
                        + bucket.name().toLowerCase(Locale.ROOT)
        );
    }
}
