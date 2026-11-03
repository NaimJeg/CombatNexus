package io.github.naimjeg.combatnexus.damage.config;

import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleLogKind;

/** Controls diagnostic detail only; it does not suppress operational warnings or errors. */
public enum DiagnosticMode {
    OFF,
    COMPATIBILITY,
    SUMMARY,
    FULL_TRACE;

    public boolean compatibilityEnabled() {
        return ordinal() >= COMPATIBILITY.ordinal();
    }

    public boolean summaryEnabled() {
        return ordinal() >= SUMMARY.ordinal();
    }

    public boolean fullTraceEnabled() {
        return this == FULL_TRACE;
    }

    public boolean allows(DamageModuleLogKind kind) {
        DamageModuleLogKind effectiveKind =
                kind == null ? DamageModuleLogKind.TRACE_DETAIL : kind;

        return switch (effectiveKind) {
            case COMPATIBILITY -> compatibilityEnabled();
            case TRACE_SUMMARY -> summaryEnabled();
            case TRACE_DETAIL -> fullTraceEnabled();
        };
    }
}
