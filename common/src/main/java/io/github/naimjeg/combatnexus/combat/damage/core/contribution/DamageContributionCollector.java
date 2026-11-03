package io.github.naimjeg.combatnexus.combat.damage.core.contribution;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionDescriptor;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionStatus;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class DamageContributionCollector {

    private record Entry(DamageContributionStatus status, DamageContributionDescriptor descriptor,
                         io.github.naimjeg.combatnexus.combat.damage.api.display.ContributionFact fact) {
        DamageContributionDescriptor project() {
            return fact == null ? descriptor.withStatus(status)
                    : io.github.naimjeg.combatnexus.combat.damage.api.display.ContributionProjector.project(fact.withStatus(status));
        }
    }
    private final List<Entry> entries =
            new ArrayList<>();

    public void recordApplied(DamageContributionDescriptor descriptor) {
        recordWithStatus(
                descriptor,
                DamageContributionStatus.APPLIED
        );
    }

    public void recordRejected(DamageContributionDescriptor descriptor) {
        recordWithStatus(
                descriptor,
                DamageContributionStatus.REJECTED
        );
    }

    public void recordNoOp(DamageContributionDescriptor descriptor) {
        recordWithStatus(
                descriptor,
                DamageContributionStatus.NO_OP
        );
    }

    public void record(
            DamageMutationResult result,
            Supplier<DamageContributionDescriptor> descriptor
    ) {
        if (result == null || descriptor == null) {
            return;
        }

        var frozen = descriptor.get();
        if (frozen != null) entries.add(new Entry(statusFromResult(result), frozen, null));
    }

    public void recordFact(DamageMutationResult result,
            io.github.naimjeg.combatnexus.combat.damage.api.display.ContributionFact fact) {
        entries.add(new Entry(statusFromResult(result), null, fact));
    }
    public List<io.github.naimjeg.combatnexus.combat.damage.api.display.ContributionFact> facts() {
        return entries.stream().filter(entry -> entry.fact() != null)
                .map(entry -> entry.fact().withStatus(entry.status())).toList();
    }

    public List<DamageContributionDescriptor> entries() {
        return entries.stream().map(entry -> entry.project()).toList();
    }

    public List<DamageContributionDescriptor> applied() {
        return filtered(DamageContributionStatus.APPLIED);
    }

    public List<DamageContributionDescriptor> rejected() {
        return filtered(DamageContributionStatus.REJECTED);
    }

    public List<DamageContributionDescriptor> noOps() {
        return filtered(DamageContributionStatus.NO_OP);
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @ApiStatus.Internal
    public int checkpoint() {
        return entries.size();
    }

    @ApiStatus.Internal
    public void rollback(int checkpoint) {
        if (checkpoint < 0 || checkpoint > entries.size()) {
            throw new IllegalArgumentException(
                    "Invalid contribution checkpoint: " + checkpoint
            );
        }
        entries.subList(checkpoint, entries.size()).clear();
    }

    private void recordWithStatus(
            DamageContributionDescriptor descriptor,
            DamageContributionStatus status
    ) {
        if (descriptor == null) {
            return;
        }

        entries.add(new Entry(status, descriptor, null));
    }

    private List<DamageContributionDescriptor> filtered(
            DamageContributionStatus status
    ) {
        return entries.stream()
                .filter(entry -> entry.status() == status)
                .map(entry -> entry.project())
                .toList();
    }

    private static DamageContributionStatus statusFromResult(
            DamageMutationResult result
    ) {
        if (result.applied()) {
            return DamageContributionStatus.APPLIED;
        }

        if (result.rejected()) {
            return DamageContributionStatus.REJECTED;
        }

        return DamageContributionStatus.NO_OP;
    }
}
