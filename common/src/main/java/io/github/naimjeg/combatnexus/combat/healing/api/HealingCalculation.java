package io.github.naimjeg.combatnexus.combat.healing.api;
import io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame;
import io.github.naimjeg.combatnexus.formula.Amounts;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Callback-local numerical workspace: no entities, application permits or services. */
public final class HealingCalculation {
    private final HealingRequest request;
    private final CombatInputFrame input;
    private final io.github.naimjeg.combatnexus.combat.api.input.ParticipantInputs participants;
    private final List<HealingContribution> contributions = new ArrayList<>();
    private final List<io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Step> beforeApplication = new ArrayList<>();
    public io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan beforeApplication() { return new io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan(beforeApplication); }
    public void beforeApplication(io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Step step) {
        if (!open) throw new IllegalStateException("Healing callback has expired");
        if (beforeApplication.size() >= 64) throw new IllegalStateException("Healing plan capacity");
        beforeApplication.add(Objects.requireNonNull(step));
    }
    private double amount;
    private boolean open;
    public HealingCalculation(HealingRequest request, io.github.naimjeg.combatnexus.combat.api.input.ParticipantInputs participants) {
        this.request = Objects.requireNonNull(request); this.participants = Objects.requireNonNull(participants);
        this.input = participants.target(); amount = request.amount();
    }
    public io.github.naimjeg.combatnexus.combat.api.input.ParticipantInputs participants() { return participants; }
    public HealingRequest request() { return request; }
    public CombatInputFrame input() { return input; }
    public double amount() { return amount; }
    public List<HealingContribution> contributions() { return List.copyOf(contributions); }
    public void amount(Identifier source, double value) {
        amount(source, HealingContribution.Kind.ADJUSTMENT, value);
    }
    public void amount(Identifier source, HealingContribution.Kind kind, double value) {
        if (!open) throw new IllegalStateException("Healing callback has expired");
        Amounts.nonNegative(value, "healing contribution");
        record(source, kind, value);
    }
    public void additive(Identifier source, double delta) {
        if (!open) throw new IllegalStateException("Healing callback has expired");
        if (!Double.isFinite(delta)) throw new IllegalArgumentException("Nonfinite healing additive");
        record(source, HealingContribution.Kind.ADDITIVE, amount + delta);
    }
    private void record(Identifier source, HealingContribution.Kind kind, double value) {
        if (contributions.size() >= HealingContribution.MAX_FACTS) throw new IllegalStateException("Healing contribution capacity exceeded");
        contributions.add(new HealingContribution(source, kind, amount, value)); amount = value;
    }
    /** Trusted pipeline invocation; failed calculation restores both amount and its explanation. */
    public void invoke(java.util.function.Consumer<HealingCalculation> handler) {
        invoke(handler, false);
    }
    /** Pipeline-only contribution step; signed additive totals are clamped at the phase boundary. */
    @org.jetbrains.annotations.ApiStatus.Internal
    public void invokeContribution(java.util.function.Consumer<HealingCalculation> handler) { invoke(handler, true); }
    private void invoke(java.util.function.Consumer<HealingCalculation> handler, boolean signed) {
        if (open) throw new IllegalStateException("Nested healing workspace invocation");
        double before = amount; int size = contributions.size(), planSize = beforeApplication.size(); open = true;
        try { handler.accept(this); if (signed) { if (!Double.isFinite(amount)) throw new IllegalArgumentException("Nonfinite contribution total"); }
            else Amounts.nonNegative(amount, "completed healing stage"); }
        catch (RuntimeException failure) { amount = before; contributions.subList(size, contributions.size()).clear(); beforeApplication.subList(planSize, beforeApplication.size()).clear(); throw failure; }
        finally { open = false; }
    }
}
