package io.github.naimjeg.combatnexus.damage.core.template;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.config.DiagnosticMode;
import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleDiagnosticState;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DiagnosticTextSanitizer;
import net.minecraft.resources.Identifier;

/** Rate-limited, opt-out diagnostics for unresolved payload-free references. */
public final class DamageTemplateDiagnostics {
    private DamageTemplateDiagnostics() {}

    public static void unresolved(
            String kind,
            Identifier id,
            long revision,
            String source
    ) {
        if (DamageModuleSettings.diagnosticMode() == DiagnosticMode.OFF) {
            return;
        }
        String safeKind = DiagnosticTextSanitizer.sanitizeLine(kind, 32);
        String safeId = DiagnosticTextSanitizer.sanitizeLine(
                String.valueOf(id), 256);
        if (DamageModuleDiagnosticState.shouldLog(
                DamageModuleDiagnosticState.Domain.TEMPLATE_REFERENCE,
                safeKind + ":" + safeId,
                "resolve",
                Long.toString(revision))) {
            DamageModule.LOGGER.warn(
                    "[DamageModule] Unresolved static template reference skipped; kind={} id={} revision={} source={}",
                    DiagnosticTextSanitizer.sanitizeArguments(
                            safeKind, safeId, revision,
                            DiagnosticTextSanitizer.sanitizeLine(source, 128)));
        }
    }

    public static void incompatible(
            long templateRevision,
            long validatedChannelRevision,
            String source
    ) {
        if (DamageModuleSettings.diagnosticMode() == DiagnosticMode.OFF) {
            return;
        }
        String key = templateRevision + ":" + validatedChannelRevision;
        if (DamageModuleDiagnosticState.shouldLog(
                DamageModuleDiagnosticState.Domain.TEMPLATE_REFERENCE,
                "registry_snapshot", "dependency_incompatible", key)) {
            DamageModule.LOGGER.warn(
                    "[DamageModule] Static template references skipped because the pinned registry snapshot is not server-authoritative; templateRevision={} validatedChannelRevision={} source={}",
                    DiagnosticTextSanitizer.sanitizeArguments(
                            templateRevision,
                            validatedChannelRevision,
                            DiagnosticTextSanitizer.sanitizeLine(source, 128)));
        }
    }
}
