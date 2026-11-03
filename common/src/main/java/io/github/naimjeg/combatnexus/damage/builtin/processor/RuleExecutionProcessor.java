package io.github.naimjeg.combatnexus.damage.builtin.processor;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleOrdering;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleProvider;
import io.github.naimjeg.combatnexus.damage.api.rule.RuntimeDamageRule;
import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageRuleContextViews;
import io.github.naimjeg.combatnexus.damage.core.rule.DamageRuleExecutor;
import io.github.naimjeg.combatnexus.damage.core.rule.DamageRuleStackingResolver;
import io.github.naimjeg.combatnexus.damage.core.rule.DamageRuleStackingResult;
import io.github.naimjeg.combatnexus.damage.core.rule.StackingTrace;
import io.github.naimjeg.combatnexus.damage.core.util.StrictCallbackFailure;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleDiagnosticState;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DiagnosticTextSanitizer;
import io.github.naimjeg.combatnexus.damage.registry.rule.DamageRuleProviders;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RuleExecutionProcessor(DamagePhase phase) implements DamagePhaseProcessor {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean safeSupportsPhase(
            DamageModuleContext ctx,
            DamageRuleProvider provider,
            DamagePhase phase
    ) {
        try {
            return provider.supportsPhase(phase);
        } catch (Exception exception) {
            handleProviderFailure(
                    ctx,
                    provider,
                    phase,
                    "supportsPhase",
                    exception
            );

            return false;
        }
    }

    private static void safeCollect(
            DamageModuleContext ctx,
            DamageRuleContext providerContext,
            DamageRuleProvider provider,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        DamageModuleContext.MutableStateCheckpoint checkpoint =
                ctx.checkpointMutableState();
        List<RuntimeDamageRule> collected = new ArrayList<>();

        try {
            provider.collect(providerContext, phase, collected);
            validateCollectedRules(provider, phase, collected);
            out.addAll(List.copyOf(collected));
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);
            handleProviderFailure(
                    ctx,
                    provider,
                    phase,
                    "collect",
                    exception
            );
        }
    }

    private static void validateCollectedRules(
            DamageRuleProvider provider,
            DamagePhase phase,
            List<RuntimeDamageRule> collected
    ) {
        for (int index = 0; index < collected.size(); index++) {
            RuntimeDamageRule rule = collected.get(index);
            if (rule == null
                    || rule.definition() == null
                    || rule.executionContext() == null) {
                throw new IllegalArgumentException(
                        "Invalid runtime rule from provider="
                                + providerName(provider)
                                + " phase="
                                + phase
                                + " index="
                                + index
                );
            }
        }
    }

    private static void handleProviderFailure(
            DamageModuleContext ctx,
            DamageRuleProvider provider,
            DamagePhase phase,
            String stage,
            Exception exception
    ) {
        if (DamageModuleSettings.strictRuleErrors()) {
            throw new StrictCallbackFailure(
                    "[DamageModule] Rule provider failure at "
                            + stage
                            + ": provider="
                            + providerName(provider)
                            + " phase="
                            + phase,
                    exception
            );
        }

        String providerName = providerName(provider);
        if (DamageModuleDiagnosticState.shouldLog(
                DamageModuleDiagnosticState.Domain.PROVIDER,
                providerName,
                phase + "/" + stage,
                exception.getClass().getName()
        )) {
            LOGGER.error(
                    "[DamageModule] Rule provider failed. phase={} stage={} provider={}. "
                            + "Provider output for this phase was skipped. "
                            + "Set strictRuleErrors=true to fail fast.",
                    phase,
                    DiagnosticTextSanitizer.sanitizeLine(stage),
                    DiagnosticTextSanitizer.sanitizeLine(providerName),
                    exception
            );
        }

        ctx.trace().mutations().rejected(
                "rule_provider/" + stage,
                phase,
                "provider=" + providerName
                        + " threw "
                        + exception.getClass().getSimpleName()
                        + ": "
                        + exception.getMessage()
        );
    }

    private static String providerName(DamageRuleProvider provider) {
        return provider == null
                ? "<null>"
                : provider.getClass().getName();
    }

    static DamageRuleContext contextForProviderCallback(
            DamageRuleProvider provider,
            DamageRuleContext internalContext
    ) {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(internalContext, "internalContext");

        return DamageRuleProviders.isBuiltin(provider)
                ? internalContext
                : DamageRuleContextViews.restricted(internalContext);
    }

    @Override
    public void apply(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor"
        );

        List<RuntimeDamageRule> rules = new ArrayList<>();

        for (DamageRuleProvider provider : DamageRuleProviders.all()) {
            if (provider == null) {
                continue;
            }

            if (!safeSupportsPhase(ctx, provider, phase)) {
                continue;
            }

            safeCollect(
                    ctx,
                    contextForProviderCallback(provider, ctx),
                    provider,
                    phase,
                    rules
            );
        }

        if (rules.isEmpty()) {
            return;
        }

        DamageRuleStackingResult result =
                DamageRuleStackingResolver.resolve(rules);

        rules = new ArrayList<>(result.rules());

        if (ctx.trace().enabled()) {
            for (StackingTrace trace : result.traces()) {
                ctx.trace().rules().stackingDrop(trace);
            }
        }

        rules.sort((first, second) ->
                DamageRuleOrdering.compareSamePhasePriorityDescending(
                        first.definition(),
                        second.definition()
                ));

        for (RuntimeDamageRule rule : rules) {
            DamageRuleExecutor.execute(
                    ctx,
                    phase,
                    rule
            );

            if (ctx.isDamageCancelled()) {
                return;
            }
        }
    }

    @Override
    public boolean canHandle(DamageRuleContext context) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "phase processor predicate"
        );

        return ctx.isManaged();
    }

    @Override
    public int getPriority() {
        return switch (phase) {
            case MITIGATION_SETUP -> 1002;
            default -> 500;
        };
    }
}

