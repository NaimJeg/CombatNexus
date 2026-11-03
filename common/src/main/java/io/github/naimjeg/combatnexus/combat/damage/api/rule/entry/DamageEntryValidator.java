package io.github.naimjeg.combatnexus.combat.damage.api.rule.entry;

import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleLimits;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleValidator;

import java.util.ArrayList;
import java.util.List;

public final class DamageEntryValidator {
    private DamageEntryValidator() {
    }

    public static List<DamageEntryDefinition> filterValid(
            List<DamageEntryDefinition> entries,
            String source
    ) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<DamageEntryDefinition> result = new ArrayList<>();

        for (DamageEntryDefinition entry : entries) {
            if (entry == null) {
                continue;
            }

            java.util.Optional<String> structuralProblem =
                    DamageRuleLimits.findEntryProblem(entry);

            if (structuralProblem.isPresent()) {
                continue;
            }

            if (entry.rules().isEmpty()) {
                continue;
            }

            List<DamageRuleDefinition> validRules =
                    DamageRuleValidator.filterValid(
                            entry.rules(),
                            source + "/entry/" + entry.id()
                    );

            if (validRules.isEmpty()) {
                continue;
            }

            result.add(new DamageEntryDefinition(
                    entry.id(),
                    entry.display(),
                    entry.slot(),
                    validRules,
                    entry.stacking(),
                    entry.stackingGroup()
            ));
        }

        return List.copyOf(result);
    }

}
