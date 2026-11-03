package io.github.naimjeg.combatnexus.combat.damage.api.rule.affix;

import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryValidator;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleLimits;

import java.util.ArrayList;
import java.util.List;

public final class DamageAffixValidator {
    private DamageAffixValidator() {
    }

    public static List<DamageAffixDefinition> filterValid(
            List<DamageAffixDefinition> affixes,
            String source
    ) {
        if (affixes == null || affixes.isEmpty()) {
            return List.of();
        }

        List<DamageAffixDefinition> result = new ArrayList<>();

        for (DamageAffixDefinition affix : affixes) {
            if (affix == null) {
                continue;
            }

            java.util.Optional<String> structuralProblem =
                    DamageRuleLimits.findItemProblem(
                            List.of(),
                            List.of(affix)
                    );

            if (structuralProblem.isPresent()) {
                continue;
            }

            if (affix.entries().isEmpty()) {
                continue;
            }

            List<DamageEntryDefinition> validEntries =
                    DamageEntryValidator.filterValid(
                            affix.entries(),
                            source + "/affix/" + affix.id()
                    );

            if (validEntries.isEmpty()) {
                continue;
            }

            result.add(new DamageAffixDefinition(
                    affix.id(),
                    affix.display(),
                    affix.slot(),
                    affix.rarity(),
                    validEntries,
                    affix.stacking(),
                    affix.stackingGroup()
            ));
        }

        return List.copyOf(result);
    }

}
