package io.github.naimjeg.combatnexus.damage.core.config;

import io.github.naimjeg.combatnexus.damage.config.DamageModuleConfig;
import io.github.naimjeg.combatnexus.damage.config.DamageModuleConfigValues;
import io.github.naimjeg.combatnexus.damage.config.DiagnosticMode;
import io.github.naimjeg.combatnexus.damage.config.DamageSafetySettings;
import io.github.naimjeg.combatnexus.damage.config.VanillaReductionCompatibilityMode;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleLogKind;

public final class DamageModuleSettings {

    private DamageModuleSettings() {
    }

    public static DamageModuleConfigValues current() {
        return DamageModuleConfig.current();
    }

    public static boolean testCommandsEnabled() {
        return current().developer().testCommandsEnabled();
    }

    public static boolean strictProcessorErrors() {
        return current().developer().strictProcessorErrors();
    }

    public static boolean strictRuleErrors() {
        return current().developer().strictRuleErrors();
    }

    public static VanillaReductionCompatibilityMode vanillaReductionMode() {
        return current().vanillaCompatibility().mode();
    }

    public static boolean suppressVanillaArmorReduction() {
        return current().vanillaCompatibility().shouldSuppressArmor();
    }

    public static boolean suppressVanillaEnchantmentReduction() {
        return current().vanillaCompatibility().shouldSuppressEnchantments();
    }

    public static boolean suppressVanillaMobEffectReduction() {
        return current().vanillaCompatibility().shouldSuppressMobEffects();
    }

    public static boolean suppressVanillaInnateResistanceReduction() {
        return current().vanillaCompatibility().shouldSuppressInnateResistance();
    }

    public static DiagnosticMode diagnosticMode() {
        return current().diagnostics().diagnosticMode();
    }

    public static boolean compatibilityDiagnosticsEnabled() {
        return current().diagnostics().compatibilityDiagnosticsEnabled();
    }

    public static boolean summaryTraceEnabled() {
        return current().diagnostics().summaryTraceEnabled();
    }

    public static boolean fullTraceEnabled() {
        return current().diagnostics().fullTraceEnabled();
    }

    public static boolean transactionTrackingEnabled() {
        return current().diagnostics().transactionTrackingEnabled();
    }

    public static DamageSafetySettings safety() {
        return current().safety();
    }

    public static int maxRecursionDepth() {
        return safety().maxRecursionDepth();
    }

    public static int maxDerivedRequestsPerRoot() {
        return safety().maxDerivedRequestsPerRoot();
    }

    public static int maxManagedRequestsPerServerTick() {
        return safety().maxManagedRequestsPerServerTick();
    }

    public static boolean shouldEmitServer(DamageModuleLogKind kind) {
        return current().diagnostics().shouldEmitServer(kind);
    }

}
