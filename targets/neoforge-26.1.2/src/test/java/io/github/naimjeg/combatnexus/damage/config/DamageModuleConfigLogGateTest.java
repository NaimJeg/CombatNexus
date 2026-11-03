package io.github.naimjeg.combatnexus.damage.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageModuleConfigLogGateTest {

    @AfterEach
    void reset() {
        DamageModuleConfig.resetConfigLogGateForTesting();
    }

    @Test
    void identicalEffectiveConfigLogsOnceAndChangedConfigLogsAgain() {
        DamageModuleConfigValues off = DamageModuleConfigValues.defaults();
        DamageModuleConfigValues summary = new DamageModuleConfigValues(
                off.developer(),
                new DiagnosticsSettings(DiagnosticMode.SUMMARY),
                off.tooltips(),
                off.formulas(),
                off.vanillaCompatibility(),
                off.safety()
        );

        assertTrue(DamageModuleConfig.shouldLogBakedConfig(off));
        assertFalse(DamageModuleConfig.shouldLogBakedConfig(off));
        assertTrue(DamageModuleConfig.shouldLogBakedConfig(summary));
        assertFalse(DamageModuleConfig.shouldLogBakedConfig(summary));
    }
}
