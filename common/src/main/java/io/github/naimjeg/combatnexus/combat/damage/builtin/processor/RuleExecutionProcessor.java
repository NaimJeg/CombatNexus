package io.github.naimjeg.combatnexus.combat.damage.builtin.processor;

import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleOrdering;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleProvider;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.RuntimeDamageRule;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageRuleContextViews;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.DamageRuleExecutor;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.DamageRuleStackingResolver;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.DamageRuleStackingResult;
import io.github.naimjeg.combatnexus.combat.damage.registry.rule.DamageRuleProviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record RuleExecutionProcessor(DamagePhase phase) implements DamagePhaseProcessor {
    private static boolean safeSupportsPhase(
            DamageModuleContext ctx,
            DamageRuleProvider provider,
            DamagePhase phase
    ) {
        try {
            return provider.supportsPhase(phase);
        } catch (Exception exception) {
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
    public boolean canHandle(DamageContextView context) {
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

