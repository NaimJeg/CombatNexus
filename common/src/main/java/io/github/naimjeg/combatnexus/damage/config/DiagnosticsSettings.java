package io.github.naimjeg.combatnexus.damage.config;

import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleLogKind;

public record DiagnosticsSettings(
        DiagnosticMode configuredMode
) {
    public static DiagnosticsSettings defaults() {
        return new DiagnosticsSettings(DiagnosticMode.OFF);
    }

    public DiagnosticMode diagnosticMode() {
        DiagnosticMode configured = configuredMode == null
                ? DiagnosticMode.OFF
                : configuredMode;

        return configured;
    }

    public boolean shouldLogFullServerTrace() {
        return shouldEmitServer(DamageModuleLogKind.TRACE_DETAIL);
    }

    public boolean compatibilityDiagnosticsEnabled() {
        return diagnosticMode().compatibilityEnabled();
    }

    public boolean summaryTraceEnabled() {
        return diagnosticMode().summaryEnabled();
    }

    public boolean fullTraceEnabled() {
        return diagnosticMode().fullTraceEnabled();
    }

    public boolean transactionTrackingEnabled() {
        return compatibilityDiagnosticsEnabled();
    }

    public boolean shouldEmitServer(DamageModuleLogKind kind) {
        return diagnosticMode().allows(kind);
    }
}
