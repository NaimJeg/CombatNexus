package io.github.naimjeg.combatnexus.combat.healing.api;

import io.github.naimjeg.combatnexus.formula.Amounts;
import java.util.Objects;

/** Confirmed application result; an event's proposed amount alone cannot produce this result. */
public record HealingSettlement(HealingRequest request, double modifiedAmount, double actualRecovery,
                                double overflow, Outcome outcome, io.github.naimjeg.combatnexus.combat.api.ApplicationJournal journal, java.util.List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> behaviors, java.util.List<HealingContribution> contributions,
                                java.util.Optional<io.github.naimjeg.combatnexus.combat.api.CommandRejection> rejection,
                                io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result beforeApplication,
                                java.util.Optional<io.github.naimjeg.combatnexus.combat.api.CommandContext> context) {
    public HealingSettlement(HealingRequest request, double modifiedAmount, double actualRecovery, double overflow, Outcome outcome,
            io.github.naimjeg.combatnexus.combat.api.ApplicationJournal journal, java.util.List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> behaviors, java.util.List<HealingContribution> contributions,
            java.util.Optional<io.github.naimjeg.combatnexus.combat.api.CommandRejection> rejection) {
        this(request, modifiedAmount, actualRecovery, overflow, outcome, journal, behaviors, contributions, rejection,
                io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result.EMPTY, java.util.Optional.empty());
    }
    public HealingSettlement execution(io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result plan,
            io.github.naimjeg.combatnexus.combat.api.CommandContext identity) {
        return new HealingSettlement(request,modifiedAmount,actualRecovery,overflow,outcome,journal,behaviors,contributions,rejection,plan,java.util.Optional.of(identity));
    }
    public HealingSettlement(HealingRequest request, double modifiedAmount, double actualRecovery, double overflow, Outcome outcome,
            io.github.naimjeg.combatnexus.combat.api.ApplicationJournal journal, java.util.List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> behaviors, java.util.List<HealingContribution> contributions) {
        this(request, modifiedAmount, actualRecovery, overflow, outcome, journal, behaviors, contributions, outcome == Outcome.REJECTED
            ? java.util.Optional.of(new io.github.naimjeg.combatnexus.combat.api.CommandRejection(io.github.naimjeg.combatnexus.combat.api.CommandRejection.Code.TARGET_UNAVAILABLE, "Application target unavailable")) : java.util.Optional.empty());
    }
    public HealingSettlement(HealingRequest request, double modifiedAmount, double actualRecovery, double overflow,
            Outcome outcome, io.github.naimjeg.combatnexus.combat.api.ApplicationJournal journal) {
        this(request, modifiedAmount, actualRecovery, overflow, outcome, journal, java.util.List.of(), java.util.List.of());
    }
    public HealingSettlement {
        Objects.requireNonNull(rejection); Objects.requireNonNull(beforeApplication); Objects.requireNonNull(context);
        behaviors = java.util.List.copyOf(behaviors);
        contributions = java.util.List.copyOf(contributions);
        if (contributions.size() > HealingContribution.MAX_FACTS) throw new IllegalArgumentException("Healing contribution capacity exceeded");
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(outcome, "outcome");
        Objects.requireNonNull(journal, "journal");
        Amounts.nonNegative(modifiedAmount, "modifiedAmount");
        Amounts.nonNegative(actualRecovery, "actualRecovery");
        Amounts.nonNegative(overflow, "overflow");
        if (actualRecovery > modifiedAmount || overflow > modifiedAmount - actualRecovery) {
            throw new IllegalArgumentException("Recovery/overflow exceeds modified amount");
        }
        if (outcome != Outcome.APPLIED && outcome != Outcome.PARTIAL && actualRecovery != 0) {
            throw new IllegalArgumentException("Unapplied healing cannot restore health");
        }
    }

    public enum Outcome { APPLIED, PARTIAL, UNKNOWN, ZERO_INPUT, ZERO_EFFECT, FULL_HEALTH, CANCELLED, DEAD_TARGET, REJECTED }
}
