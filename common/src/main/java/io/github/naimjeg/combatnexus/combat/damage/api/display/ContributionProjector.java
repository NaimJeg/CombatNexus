package io.github.naimjeg.combatnexus.combat.damage.api.display;

import java.util.*;

/** Cold formatting only; never samples an entity or a newer definition generation. */
public final class ContributionProjector {
    private ContributionProjector() {}
    public static DamageContributionDescriptor project(ContributionFact fact) {
        var details = fact.details();
        List<String> lines = new ArrayList<>();
        lines.add("rule=" + details.rule()); lines.add("operation=" + details.operation());
        fact.channel().ifPresent(value -> lines.add("channel=" + value));
        fact.applicationBucket().ifPresent(value -> lines.add("application_bucket=" + value));
        fact.preMultiplierBucket().ifPresent(value -> lines.add("pre_multiplier_bucket=" + value));
        lines.add("owner_kind=" + details.ownerKind());
        details.affix().ifPresent(value -> lines.add("affix=" + value));
        details.entry().ifPresent(value -> lines.add("entry=" + value));
        if (details.provider() != null) {
            lines.add("provider=" + details.provider()); lines.add("source_location=" + details.location());
            lines.add("role=" + details.role());
        }
        lines.add("result=" + details.result());
        return new DamageContributionDescriptor(fact.id(), fact.sourceKind(), fact.operationKind(), fact.phase(),
                fact.channel(), fact.applicationBucket(), fact.preMultiplierBucket(), fact.value(), fact.status(),
                fact.displayGroup(), fact.displaySubgroup(), Optional.empty(), lines, true);
    }
}
