package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.mutation.DamageMutationGuard;

final class DamageContextMutations implements DamageMutationContext {
    private final DamageMutationApplier applier;
    private final DamagePhaseState phaseState;

    DamageContextMutations(
            DamagePacketState packet,
            DamageCombatState combat,
            DamagePipelineResult result,
            DamagePhaseState phaseState
    ) {
        this.applier = new DamageMutationApplier(packet, combat, result);
        this.phaseState = phaseState;
    }

    @Override
    public DamageMutationResult tryAddBaseDamage(
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId
    ) {
        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.BASE_MODIFICATION);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        return addBaseDamageWithoutPhaseCheck(
                "addBaseDamage",
                channel,
                bucket,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryAddBaseDamage(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return tryAddBaseDamage(
                channel,
                DamageApplicationBucket.DN_RULE_BASE,
                value,
                sourceId
        );
    }

    DamageMutationResult addBaseDamageWithoutPhaseCheck(
            String action,
            DamageChannel channel,
            DamageApplicationBucket bucket,
            float value,
            String sourceId
    ) {
        DamageMutationResult offenseResult = phaseState.canModifyOffense();

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return channelResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        if (bucket == null) {
            return DamageMutationResult.REJECTED_NULL_APPLICATION_BUCKET;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addBaseDamage(channel, bucket, value);

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddChannelPreMultiplier(
            DamageChannel channel,
            int modifierId,
            float value,
            String sourceId
    ) {
        DamageMutationResult offenseResult = phaseState.canModifyOffense();

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requireMultiplierPhase();

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return channelResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        DamageMutationResult bucketResult =
                DamageMutationGuard.requirePreMultiplierBucket(modifierId);

        if (!bucketResult.applied()) {
            return DamageMutationResult.REJECTED_INVALID_PRE_MULTIPLIER_BUCKET;
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addChannelPreMultiplier(
                channel,
                modifierId,
                value
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddApplicationPreMultiplier(
            DamageApplicationBucket bucket,
            int modifierId,
            float value,
            String sourceId
    ) {
        DamageMutationResult offenseResult = phaseState.canModifyOffense();

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requireMultiplierPhase();

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        if (bucket == null) {
            return DamageMutationResult.REJECTED_NULL_APPLICATION_BUCKET;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        DamageMutationResult bucketResult =
                DamageMutationGuard.requirePreMultiplierBucket(modifierId);

        if (!bucketResult.applied()) {
            return DamageMutationResult.REJECTED_INVALID_PRE_MULTIPLIER_BUCKET;
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addApplicationPreMultiplier(
                bucket,
                modifierId,
                value
        );

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddGlobalPreMultiplier(
            int modifierId,
            float value,
            String sourceId
    ) {
        DamageMutationResult offenseResult = phaseState.canModifyOffense();

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requireMultiplierPhase();

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        DamageMutationResult bucketResult =
                DamageMutationGuard.requirePreMultiplierBucket(modifierId);

        if (!bucketResult.applied()) {
            return DamageMutationResult.REJECTED_INVALID_PRE_MULTIPLIER_BUCKET;
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addGlobalPreMultiplier(modifierId, value);

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddChannelPostMultiplier(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        DamageMutationResult offenseResult = phaseState.canModifyOffense();

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requireMultiplierPhase();

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return channelResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addChannelPostMultiplier(channel, value);

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddGlobalPostMultiplier(
            float value,
            String sourceId
    ) {
        DamageMutationResult offenseResult = phaseState.canModifyOffense();

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult =
                phaseState.requireGlobalPostMultiplierPhase();

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(value);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        if (value == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addGlobalPostMultiplier(value);

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryConvertDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio,
            String sourceId
    ) {
        DamageMutationResult offenseResult = phaseState.canModifyOffense();

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.TYPE_SCALING);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        if (from == null || to == null) {
            return DamageMutationResult.REJECTED_NULL_CHANNEL;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(ratio);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        float clampedRatio = DamageMutationGuard.clampRatio(ratio);

        if (clampedRatio == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        float movedAmount = applier.convertDamage(
                from,
                to,
                clampedRatio
        );

        if (movedAmount <= 0.0f) {
            return DamageMutationResult.NO_OP_EMPTY_SOURCE;
        }

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryGainExtraDamage(
            DamageChannel from,
            DamageChannel to,
            float ratio,
            String sourceId
    ) {
        DamageMutationResult offenseResult = phaseState.canModifyOffense();

        if (!offenseResult.applied()) {
            return offenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.TYPE_SCALING);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        if (from == null || to == null) {
            return DamageMutationResult.REJECTED_NULL_CHANNEL;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(ratio);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        float safeRatio = DamageMutationGuard.clampNonNegative(ratio);

        if (safeRatio == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        float extraAmount = applier.gainExtraDamage(
                from,
                to,
                safeRatio
        );

        if (extraAmount <= 0.0f) {
            return DamageMutationResult.NO_OP_EMPTY_SOURCE;
        }

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddTrueDamage(
            DamageChannel channel,
            float value,
            String sourceId
    ) {
        return tryAddBaseDamage(
                channel,
                DamageApplicationBucket.DN_TRUE_DAMAGE,
                value,
                sourceId
        );
    }

    @Override
    public DamageMutationResult tryMultiplyArmorEffectiveness(
            float multiplier,
            String sourceId
    ) {
        DamageMutationResult defenseResult = phaseState.canModifyDefense();

        if (!defenseResult.applied()) {
            return defenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.MITIGATION_SETUP);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(multiplier);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        float safeMultiplier = DamageMutationGuard.clampNonNegative(multiplier);

        applier.multiplyArmorEffectiveness(safeMultiplier);

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddTemporaryResistance(
            DamageChannel channel,
            float rating,
            String sourceId
    ) {
        DamageMutationResult defenseResult = phaseState.canModifyDefense();

        if (!defenseResult.applied()) {
            return defenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.MITIGATION_SETUP);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return channelResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(rating);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        if (rating == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        if (!applier.addTemporaryResistance(channel, rating)) {
            return DamageMutationResult.REJECTED_INACTIVE_CHANNEL;
        }

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddChannelMitigation(
            DamageChannel channel,
            float reductionPercent,
            String sourceId
    ) {
        DamageMutationResult defenseResult = phaseState.canModifyDefense();

        if (!defenseResult.applied()) {
            return defenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.MITIGATION_SETUP);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult channelResult =
                DamageMutationGuard.requireChannel(channel);

        if (!channelResult.applied()) {
            return channelResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(reductionPercent);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        float safeReduction =
                DamageMutationGuard.clampMitigationUpperBound(
                        reductionPercent
                );

        if (safeReduction == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        if (!applier.addChannelMitigation(channel, safeReduction)) {
            return DamageMutationResult.REJECTED_INACTIVE_CHANNEL;
        }

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryAddGlobalMitigation(
            float reductionPercent,
            String sourceId
    ) {
        DamageMutationResult defenseResult = phaseState.canModifyDefense();

        if (!defenseResult.applied()) {
            return defenseResult;
        }

        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.MITIGATION_SETUP);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(reductionPercent);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        float safeReduction =
                DamageMutationGuard.clampMitigationUpperBound(
                        reductionPercent
                );

        if (safeReduction == 0.0f) {
            return DamageMutationResult.NO_OP_ZERO;
        }

        applier.addGlobalMitigation(safeReduction);

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryOverrideFinalDamage(
            float amount,
            String sourceId
    ) {
        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.FINAL_OVERRIDE);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        DamageMutationResult finiteResult =
                DamageMutationGuard.requireFinite(amount);

        if (!finiteResult.applied()) {
            return finiteResult;
        }

        float safeAmount = DamageMutationGuard.clampNonNegative(amount);
        applier.overrideFinalDamage(safeAmount);

        return DamageMutationResult.APPLIED;
    }

    @Override
    public DamageMutationResult tryCancelDamage(String sourceId) {
        DamageMutationResult phaseResult = phaseState.requirePhase(DamagePhase.FINAL_OVERRIDE);

        if (!phaseResult.applied()) {
            return phaseResult;
        }

        DamageMutationResult sourceResult = requireSourceId(sourceId);

        if (!sourceResult.applied()) {
            return sourceResult;
        }

        applier.cancelDamage(sourceId);

        return DamageMutationResult.APPLIED;
    }

    private DamageMutationResult requireSourceId(
            String sourceId
    ) {
        if (sourceId != null && !sourceId.isBlank()) {
            return DamageMutationResult.APPLIED;
        }

        return DamageMutationResult.REJECTED_EMPTY_SOURCE_ID;
    }

    DamageMutationResult requirePhase(
            String action,
            DamagePhase expected
    ) {
        return phaseState.requirePhase(expected);
    }

}
