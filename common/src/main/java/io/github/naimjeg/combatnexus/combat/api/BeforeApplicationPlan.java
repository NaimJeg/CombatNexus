package io.github.naimjeg.combatnexus.combat.api;
import java.util.*;
/** Ordered domain requests frozen by calculation. */
public record BeforeApplicationPlan(List<Step> steps) {
    public static final BeforeApplicationPlan EMPTY = new BeforeApplicationPlan(List.of());
    public BeforeApplicationPlan { steps = List.copyOf(steps); }
    public enum RejectionPolicy { CONTINUE, ABORT_PARENT }
    public record Step(DomainCommand command, RejectionPolicy rejectionPolicy) {
        public Step { Objects.requireNonNull(command); Objects.requireNonNull(rejectionPolicy); }
    }
    public record StepEvidence(int index, net.minecraft.resources.Identifier commandType, CommandOutcome<?> result) {
        public ApplicationJournal journal() { return result.journal(); }
        public Optional<CommandContext> context() { return result.context(); }
        public Optional<CommandRejection> rejection() { return result.rejection(); }
    }
    public record Result(List<StepEvidence> steps, boolean continueParent, boolean completed) {
        public static final Result EMPTY = new Result(List.of(), true, true);
        public Result { steps = List.copyOf(steps); }
    }
}
