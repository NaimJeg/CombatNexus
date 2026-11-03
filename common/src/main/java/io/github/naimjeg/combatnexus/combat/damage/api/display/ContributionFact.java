package io.github.naimjeg.combatnexus.combat.damage.api.display;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.*;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.*;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;

/** Compact frozen numeric/source facts. No maps, live context or formatted tooltip lines. */
public record ContributionFact(long generation, int operationIndex, Identifier id,
        DamageContributionSourceKind sourceKind, DamageContributionOperationKind operationKind,
        DamagePhase phase, Optional<Identifier> channel, Optional<DamageApplicationBucket> applicationBucket,
        Optional<Identifier> preMultiplierBucket, float value, DamageContributionStatus status,
        Optional<Identifier> displayGroup, Optional<Identifier> displaySubgroup, RuleDetails details) {
    public record RuleDetails(Identifier rule, Identifier operation, DamageRuleOwnerKind ownerKind,
            Optional<Identifier> affix, Optional<Identifier> entry, DamageRuleProviderType provider,
            RuleSourceLocation location, DamageRuleRole role, DamageMutationResult result) {}
    public ContributionFact {
        Objects.requireNonNull(id); Objects.requireNonNull(details); Objects.requireNonNull(status);
        if (generation < 1 || operationIndex < 0)
            throw new IllegalArgumentException("Invalid contribution fact");
    }
    public ContributionFact withStatus(DamageContributionStatus status) {
        return new ContributionFact(generation, operationIndex, id, sourceKind, operationKind, phase, channel,
                applicationBucket, preMultiplierBucket, value, status, displayGroup, displaySubgroup, details);
    }
}
