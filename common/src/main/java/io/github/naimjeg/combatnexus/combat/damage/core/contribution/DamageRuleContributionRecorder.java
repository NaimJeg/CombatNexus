package io.github.naimjeg.combatnexus.combat.damage.core.contribution;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModulePreMultiplierBuckets;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionDescriptor;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionSourceKind;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionStatus;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.*;
import io.github.naimjeg.combatnexus.combat.damage.builtin.rule.operation.*;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.combat.damage.registry.PreMultiplierBuckets;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class DamageRuleContributionRecorder {

    private DamageRuleContributionRecorder() {
    }

    public static void recordOperation(
            DamageModuleContext ctx,
            DamagePhase runningPhase,
            RuntimeDamageRule runtimeRule,
            DamageRuleOperation operation,
            DamageMutationResult result,
            int operationIndex
    ) {
        if (ctx == null
                || runtimeRule == null
                || operation == null
                || result == null) {
            return;
        }

        var rule = runtimeRule.definition();
        var owner = runtimeRule.owner();
        var exec = runtimeRule.executionContext();
        var details = new io.github.naimjeg.combatnexus.combat.damage.api.display.ContributionFact.RuleDetails(
                rule.id(), operation.type(), owner.kind(), owner.affixId(), owner.effectiveEntryId(),
                exec == null ? null : exec.providerType(), exec == null ? null : exec.sourceLocation(),
                exec == null ? null : exec.role(), result);
        var fact = new io.github.naimjeg.combatnexus.combat.damage.api.display.ContributionFact(
                ctx.definitions().generation(), operationIndex, contributionId(rule.id(), operation.type(), operationIndex),
                sourceKind(owner), DamageContributionOperationKindMapper.fromRuleOperationType(operation.type()),
                runningPhase, channel(operation), applicationBucket(operation), preMultiplierBucket(operation),
                operation.stackingValue(), DamageContributionStatus.APPLIED,
                owner.displayOwnerId().isPresent() ? owner.displayOwnerId() : rule.stackingGroup(),
                owner.kind() == DamageRuleOwnerKind.AFFIX ? owner.effectiveEntryId() : Optional.empty(), details);
        ctx.contributions().recordFact(result, fact);
    }

    private static DamageContributionSourceKind sourceKind(
            DamageRuleOwner owner
    ) {
        if (owner == null || owner.kind() == null) {
            return DamageContributionSourceKind.RULE;
        }

        return switch (owner.kind()) {
            case AFFIX -> DamageContributionSourceKind.AFFIX;
            case ENTRY -> DamageContributionSourceKind.ENTRY;
            case RULE -> DamageContributionSourceKind.RULE;
        };
    }

    private static Identifier contributionId(
            Identifier ruleId,
            Identifier operationType,
            int operationIndex
    ) {
        String path = "rule/"
                + sanitize(ruleId.getNamespace())
                + "/"
                + sanitize(ruleId.getPath())
                + "/"
                + sanitize(operationType.getPath())
                + "/"
                + Math.max(0, operationIndex);

        return DamageModuleIds.id(path);
    }

    private static String sanitize(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value
                .replace(':', '_')
                .replace('/', '_')
                .replace(' ', '_')
                .toLowerCase(Locale.ROOT);
    }

    private static Optional<Identifier> channel(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddBaseDamageOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelPreMultiplierOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelPostMultiplierOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddTemporaryResistanceOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddChannelMitigationOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof AddTrueDamageOperation op) {
            return Optional.ofNullable(op.channelId());
        }

        if (operation instanceof ConvertDamageOperation op) {
            return Optional.ofNullable(op.toChannel());
        }

        if (operation instanceof GainExtraDamageOperation op) {
            return Optional.ofNullable(op.toChannel());
        }

        return Optional.empty();
    }

    private static Optional<DamageApplicationBucket> applicationBucket(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddBaseDamageOperation op) {
            return Optional.ofNullable(op.applicationBucket());
        }

        if (operation instanceof AddTrueDamageOperation) {
            return Optional.of(DamageApplicationBucket.DN_TRUE_DAMAGE);
        }

        if (operation instanceof GainExtraDamageOperation) {
            return Optional.of(DamageApplicationBucket.DN_RULE_BASE);
        }

        return Optional.empty();
    }

    private static Optional<Identifier> preMultiplierBucket(
            DamageRuleOperation operation
    ) {
        if (operation instanceof AddChannelPreMultiplierOperation op) {
            return op.preMultiplierBucketId()
                    .or(() -> Optional.of(DamageModulePreMultiplierBuckets.GENERIC_DAMAGE));
        }

        if (operation instanceof AddGlobalPreMultiplierOperation op) {
            return op.preMultiplierBucketId()
                    .or(() -> Optional.of(DamageModulePreMultiplierBuckets.GENERIC_DAMAGE));
        }

        return Optional.empty();
    }
}
