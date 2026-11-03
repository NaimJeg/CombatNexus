package io.github.naimjeg.combatnexus.damage.builtin.rule.provider;

import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.rule.*;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.damage.core.rule.DatapackDamageRuleStore;

import java.util.List;

public final class DatapackDamageRuleProvider implements DamageRuleProvider {

    public static int ruleCount() {
        return DatapackDamageRuleStore.ruleCount();
    }

    @Override
    public void collect(
            DamageRuleContext context,
            DamagePhase phase,
            List<RuntimeDamageRule> out
    ) {
        DamageModuleContext ctx = DamageInternalContexts.require(
                context,
                "datapack rule provider"
        );

        for (DamageRuleDefinition rule
                : ctx.datapackRuleSnapshot().rules(phase)) {
            RuntimeDamageRule runtimeRule = new RuntimeDamageRule(
                    rule,
                    RuleExecutionContext.datapackRule(rule.role())
            );

            ctx.trace().rules().collected(
                    phase,
                    rule,
                    runtimeRule.executionContext()
            );

            out.add(runtimeRule);
        }
    }
}

