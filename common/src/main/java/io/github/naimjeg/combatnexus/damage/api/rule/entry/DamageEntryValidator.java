package io.github.naimjeg.combatnexus.damage.api.rule.entry;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleDefinition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleLimits;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleValidator;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleDiagnosticState;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DiagnosticTextSanitizer;
import net.minecraft.resources.Identifier;

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
                warnOnce(
                        null,
                        null,
                        "null_entry",
                        "[DamageModule] Invalid damage entry ignored. source={} reason=null_entry",
                        source
                );
                continue;
            }

            java.util.Optional<String> structuralProblem =
                    DamageRuleLimits.findEntryProblem(entry);

            if (structuralProblem.isPresent()) {
                warnOnce(
                        entry.id(),
                        entry.slot(),
                        structuralProblem.get(),
                        "[DamageModule] Invalid damage entry ignored. source={} entry={} reason={}",
                        source,
                        entry.id(),
                        structuralProblem.get()
                );
                continue;
            }

            if (entry.rules().isEmpty()) {
                warnOnce(
                        entry.id(),
                        entry.slot(),
                        "no_rules",
                        "[DamageModule] Invalid damage entry ignored. source={} entry={} reason=no_rules",
                        source,
                        entry.id()
                );
                continue;
            }

            List<DamageRuleDefinition> validRules =
                    DamageRuleValidator.filterValid(
                            entry.rules(),
                            source + "/entry/" + entry.id()
                    );

            if (validRules.isEmpty()) {
                warnOnce(
                        entry.id(),
                        entry.slot(),
                        "no_valid_rules",
                        "[DamageModule] Invalid damage entry ignored. source={} entry={} reason=no_valid_rules",
                        source,
                        entry.id()
                );
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

    private static void warnOnce(
            Identifier definitionId,
            DamageEntrySlot slot,
            String reason,
            String message,
            Object... arguments
    ) {
        if (DamageModuleDiagnosticState.shouldLog(
                DamageModuleDiagnosticState.Domain.ENTRY_VALIDATION,
                definitionId == null
                        ? "<null_entry>"
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
