package io.github.naimjeg.combatnexus.damage.config;

import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleLogKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiagnosticsSettingsTest {

    @Test
    void diagnosticModesHaveExpectedServerOutputMatrix() {
        assertMode(DiagnosticMode.OFF, false, false, false);
        assertMode(DiagnosticMode.COMPATIBILITY, true, false, false);
        assertMode(DiagnosticMode.SUMMARY, true, true, false);
        assertMode(DiagnosticMode.FULL_TRACE, true, true, true);
    }

    @Test
    void defaultIsOffAndLogKindsContainOnlyDiagnosticDetailCategories() {
        assertEquals(
                DiagnosticMode.OFF,
                DiagnosticsSettings.defaults().diagnosticMode()
        );
        assertEquals(3, DamageModuleLogKind.values().length);
    }

    private static void assertMode(
            DiagnosticMode mode,
            boolean compatibility,
            boolean summary,
            boolean detail
    ) {
        DiagnosticsSettings settings = new DiagnosticsSettings(mode);
        assertEquals(compatibility, settings.shouldEmitServer(
                DamageModuleLogKind.COMPATIBILITY
        ));
        assertEquals(summary, settings.shouldEmitServer(
                DamageModuleLogKind.TRACE_SUMMARY
        ));
        assertEquals(detail, settings.shouldEmitServer(
                DamageModuleLogKind.TRACE_DETAIL
        ));
        assertEquals(compatibility, settings.compatibilityDiagnosticsEnabled());
        assertEquals(summary, settings.summaryTraceEnabled());
        assertEquals(detail, settings.fullTraceEnabled());
        if (mode == DiagnosticMode.OFF) {
            assertFalse(settings.transactionTrackingEnabled());
        } else {
            assertTrue(settings.transactionTrackingEnabled());
        }
    }
}
