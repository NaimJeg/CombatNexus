package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.registry.DamagePhaseProcessorRegistry;
import net.minecraft.resources.Identifier;

import java.util.*;

public class DamageModulePipeline {
    private static volatile PipelineSnapshot cached;
    private static PipelineSnapshot snapshot() {
        if (cached != null) return cached;
        var definitions = DamagePhaseProcessorRegistry.snapshot();
        Map<DamagePhase, List<PipelineEntry>> phases = new EnumMap<>(DamagePhase.class);
        for (var phase : DamagePhase.values()) phases.put(phase, definitions.entries(phase).stream()
                .map(e -> new PipelineEntry(e.handler(), e.external(), e.id(), e.order(), e.priority())).toList());
        var builder = new io.github.naimjeg.combatnexus.combat.api.pipeline.DomainPipeline.Builder<DamagePhase, PipelineEntry>(DamagePhase.class);
        phases.forEach((phase, entries) -> entries.forEach(e -> builder.register(e.registryId(), phase, e.priority(), e.external(), e)));
        var result = new PipelineSnapshot(Collections.unmodifiableMap(phases), builder.freeze());

        return cached = result;
    }

    public static void clearCache() {
        cached = null;
    }

    public static DamageApplicationPlan execute(DamageModuleContext ctx) {
        executePipeline(ctx);
        return ctx.executionSummary();
    }

    private static void executePipeline(DamageModuleContext ctx) {
        PipelineSnapshot snapshot = snapshot();

        if (!ctx.isManaged()) {
            return;
        }

        ctx.setCurrentProcessingPhase(DamagePhase.BASE_MODIFICATION);
        if (DodgeDecisionEngine.execute(ctx)) return;
        runPhase(snapshot, DamagePhase.BASE_MODIFICATION, ctx);
        if (finishIfCancelled(ctx)) return;

        runPhase(snapshot, DamagePhase.TYPE_SCALING, ctx);
        if (finishIfCancelled(ctx)) return;

        // Private Phase 8 handoff: all converted/new channels are now final.
        ctx.setCurrentProcessingPhase(DamagePhase.TYPE_SCALING);
        DamageAttributeScalingEngine.execute(ctx);

        // Private Phase 7 pre-pass: providers, freeze, one roll, one effect.
        // Ordinary public CRITICAL_HIT processors/rules always run afterward.
        ctx.setCurrentProcessingPhase(DamagePhase.CRITICAL_HIT);
        CriticalDecisionEngine.execute(ctx);
        runPhase(snapshot, DamagePhase.CRITICAL_HIT, ctx);
        if (finishIfCancelled(ctx)) return;

        runPhase(snapshot, DamagePhase.CONDITIONAL_MULTI, ctx);
        if (finishIfCancelled(ctx)) return;

        runPhase(snapshot, DamagePhase.GLOBAL_ADJUSTMENT, ctx);
        if (finishIfCancelled(ctx)) return;

        ctx.finalizeOffensiveDamage();

        runPhase(snapshot, DamagePhase.MITIGATION_SETUP, ctx);
        if (finishIfCancelled(ctx)) return;

        ctx.calculateDefensiveDamage();

        runPhase(snapshot, DamagePhase.FINAL_OVERRIDE, ctx);
        if (finishIfCancelled(ctx)) return;
    }

    private static boolean finishIfCancelled(DamageModuleContext ctx) {
        if (!ctx.isDamageCancelled()) {
            return false;
        }

        return true;
    }

    private static void runPhase(
            PipelineSnapshot snapshot,
            DamagePhase phase,
            DamageModuleContext ctx
    ) {
        ctx.setCurrentProcessingPhase(phase);

        List<PipelineEntry> processors = snapshot.processors(phase);

        if (processors == null || processors.isEmpty()) {
            return;
        }

        snapshot.execution().execute(phase, definition -> {
            var entry = definition.handler();
            var processor = entry.processor();
            if (!safeCanHandle(entry, phase, ctx)) {
                return;
            }
            safeApply(entry, phase, ctx);
        }, ctx::isDamageCancelled);
    }

    private static boolean safeCanHandle(
            PipelineEntry entry,
            DamagePhase phase,
        DamageModuleContext ctx
    ) {
        DamageModuleContext.MutableStateCheckpoint checkpoint =
                ctx.checkpointMutableState();
        try {
            return entry.processor().canHandle(
                    entry.external() ? DamageRuleContextViews.readOnly(ctx) : ctx
            );
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);

            return false;
        }
    }

    private static void safeApply(
            PipelineEntry entry,
            DamagePhase phase,
        DamageModuleContext ctx
    ) {
        DamageModuleContext.MutableStateCheckpoint checkpoint =
                ctx.checkpointMutableState();
        try {
            entry.processor().apply(
                    contextForProcessorCallback(entry, ctx)
            );
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);

        }
    }

    static DamageRuleContext contextForProcessorCallback(
            PipelineEntry entry,
            DamageRuleContext context
    ) {
        Objects.requireNonNull(entry, "entry");
        Objects.requireNonNull(context, "context");

        return entry.external()
                ? DamageRuleContextViews.restricted(context)
                : context;
    }

    record PipelineSnapshot(
            Map<DamagePhase, List<PipelineEntry>> phaseProcessors,
            io.github.naimjeg.combatnexus.combat.api.pipeline.DomainPipeline<DamagePhase, PipelineEntry> execution
    ) {
        List<PipelineEntry> processors(DamagePhase phase) {
            return phaseProcessors.getOrDefault(phase, List.of());
        }
    }

    record PipelineEntry(
            DamagePhaseProcessor processor,
            boolean external,
            Identifier registryId,
            long insertionOrder,
            int priority
    ) {
    }
}
