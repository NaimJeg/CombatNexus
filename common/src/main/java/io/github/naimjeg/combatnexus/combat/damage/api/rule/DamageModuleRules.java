package io.github.naimjeg.combatnexus.combat.damage.api.rule;

import io.github.naimjeg.combatnexus.combat.damage.api.rule.builder.DamageRuleBuilder;
import net.minecraft.resources.Identifier;

public final class DamageModuleRules {

    private DamageModuleRules() {
    }

    public static DamageRuleBuilder offensive(Identifier id) {
        return DamageRuleBuilder.offensive(id);
    }

    public static DamageRuleBuilder defensive(Identifier id) {
        return DamageRuleBuilder.defensive(id);
    }

    public static DamageRuleBuilder any(Identifier id) {
        return DamageRuleBuilder.any(id);
    }
}
