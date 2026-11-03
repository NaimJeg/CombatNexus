package io.github.naimjeg.combatnexus.combat.damage.builtin.rule.provider;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.*;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageInternalContexts;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.DatapackDamageRuleStore;

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

            out.add(runtimeRule);
        }
    }
}

