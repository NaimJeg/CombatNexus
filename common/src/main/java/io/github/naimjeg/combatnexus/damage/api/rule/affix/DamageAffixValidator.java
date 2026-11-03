package io.github.naimjeg.combatnexus.damage.api.rule.affix;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.DamageEntryValidator;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleLimits;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleDiagnosticState;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DiagnosticTextSanitizer;
import net.minecraft.resources.Identifier;

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
                warnOnce(
                        null,
                        null,
                        "null_affix",
                        "[DamageModule] Invalid damage affix ignored. source={} reason=null_affix",
                        source
                );
                continue;
            }

            java.util.Optional<String> structuralProblem =
                    DamageRuleLimits.findItemProblem(
                            List.of(),
                            List.of(affix)
                    );

            if (structuralProblem.isPresent()) {
                warnOnce(
                        affix.id(),
                        affix.slot(),
                        structuralProblem.get(),
                        "[DamageModule] Invalid damage affix ignored. source={} affix={} reason={}",
                        source,
                        affix.id(),
                        structuralProblem.get()
                );
                continue;
            }

            if (affix.entries().isEmpty()) {
                warnOnce(
                        affix.id(),
                        affix.slot(),
                        "no_entries",
                        "[DamageModule] Invalid damage affix ignored. source={} affix={} reason=no_entries",
                        source,
                        affix.id()
                );
                continue;
            }

            List<DamageEntryDefinition> validEntries =
                    DamageEntryValidator.filterValid(
                            affix.entries(),
                            source + "/affix/" + affix.id()
                    );

            if (validEntries.isEmpty()) {
                warnOnce(
                        affix.id(),
                        affix.slot(),
                        "no_valid_entries",
                        "[DamageModule] Invalid damage affix ignored. source={} affix={} reason=no_valid_entries",
                        source,
                        affix.id()
                );
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

    private static void warnOnce(
            Identifier definitionId,
            DamageAffixSlot slot,
            String reason,
            String message,
            Object... arguments
    ) {
        if (DamageModuleDiagnosticState.shouldLog(
                DamageModuleDiagnosticState.Domain.AFFIX_VALIDATION,
                definitionId == null
                        ? "<null_affix>"
                        : definitionId.toString(),
                slot == null ? "<null_slot>" : slot.name(),
                reason
        )) {
            DamageModule.LOGGER.warn(
                    DiagnosticTextSanitizer.sanitizeLine(message),
                    DiagnosticTextSanitizer.sanitizeArguments(arguments)
            );
        }
    }
}
