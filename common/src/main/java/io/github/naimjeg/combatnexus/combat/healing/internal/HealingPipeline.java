package io.github.naimjeg.combatnexus.combat.healing.internal;
import io.github.naimjeg.combatnexus.combat.healing.spi.HealingApplicationPlan;
import io.github.naimjeg.combatnexus.combat.api.pipeline.*;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import io.github.naimjeg.combatnexus.attribute.api.CombatAttributes;
import io.github.naimjeg.combatnexus.formula.HealingFormula;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;

/** Healing owns its numerical stages; transaction and native receipt classification stay in the service. */
public final class HealingPipeline {
    private static final DomainPipeline<HealingPhase, Consumer<HealingCalculation>> PIPELINE = create();
    private HealingPipeline() {}
    private static DomainPipeline<HealingPhase, Consumer<HealingCalculation>> create() {
        var builder = new DomainPipeline.Builder<HealingPhase, Consumer<HealingCalculation>>(HealingPhase.class);
        var sourceId = Identifier.fromNamespaceAndPath("combatnexus", "healing_power");
        builder.register(sourceId, HealingPhase.SOURCE, 0, false, ctx -> {
            var attribute = ctx.participants().find(io.github.naimjeg.combatnexus.combat.api.input.ParticipantInputs.Role.HEALER)
                    .map(frame -> frame.attributes().valueFrame().values().get(CombatAttributes.HEALING_POWER.identifier()));
            ctx.amount(sourceId, HealingContribution.Kind.SOURCE, ctx.amount() * attribute.map(a -> a.effective()).orElse(1.0));
        });
        var id = Identifier.fromNamespaceAndPath("combatnexus", "healing_received");
        builder.register(id, HealingPhase.RECEIVED, 0, false, ctx -> {
            var attribute = ctx.input().attributes().valueFrame().values().get(CombatAttributes.HEALING_RECEIVED.identifier());
            ctx.amount(id, HealingContribution.Kind.RECEIVED, HealingFormula.modified(ctx.amount(), 1, attribute == null ? 1 : attribute.effective()));
        });
        return builder.freeze();
    }
    public static HealingApplicationPlan calculate(HealingRequest request, io.github.naimjeg.combatnexus.combat.api.input.ParticipantInputs participants,
            Map<Identifier, HealingRuleDefinition> rules) {
        var input = participants.target();
        var workspace = new HealingCalculation(request, participants);
        var ordered = rules.entrySet().stream().sorted(Comparator
                .<Map.Entry<Identifier, HealingRuleDefinition>>comparingInt(entry -> entry.getValue().priority()).reversed()
                .thenComparing(Map.Entry::getKey)).toList();
        for (var phase : HealingPhase.values()) {
            for (var participant : participants.roles().values().stream().distinct().toList())
                for (var source : participant.behaviors()) for (var component : source.plan().components())
                    if (component instanceof io.github.naimjeg.combatnexus.combat.api.behavior.NumericalBehavior contribution) {
                        try { workspace.invokeContribution(ctx -> contribution.healing(phase, ctx, source)); }
                        catch (RuntimeException failure) {}
                    }
            if (phase == HealingPhase.CONTRIBUTIONS || phase == HealingPhase.LIMITS) {
                workspace.invoke(ctx -> {
                    var matching = ordered.stream().filter(entry -> entry.getValue().phase() == phase && entry.getValue().matches(request, input)).toList();
                    if (phase == HealingPhase.LIMITS && matching.isEmpty()) return;
                    for (var entry : matching) if (entry.getValue().additive() != 0) ctx.additive(entry.getKey(), entry.getValue().additive());
                    ctx.amount(Identifier.fromNamespaceAndPath("combatnexus", "healing_nonnegative"), HealingContribution.Kind.CLAMP, Math.max(0, ctx.amount()));
                    for (var entry : matching) if (entry.getValue().multiplier() != 1) ctx.amount(entry.getKey(), ctx.amount() * entry.getValue().multiplier());
                });
            } else PIPELINE.execute(phase, e -> workspace.invoke(e.handler()), () -> false);
            io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.calculate(phase, workspace);
        }
        return new HealingApplicationPlan(request, workspace.amount(), input.definitionGeneration(), Optional.of(input), workspace.contributions(), workspace.beforeApplication());
    }
}
