package io.github.naimjeg.combatnexus.damage.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.config.DamageModuleConfig;
import io.github.naimjeg.combatnexus.damage.config.DiagnosticMode;
import io.github.naimjeg.combatnexus.damage.config.VanillaReductionCompatibilityMode;
import org.slf4j.Logger;

public final class DamageModuleLifecycleLog {

    private static final Logger LOGGER = LogUtils.getLogger();

    private DamageModuleLifecycleLog() {
    }

    public static void commonSetupComplete(
            DiagnosticMode diagnosticMode,
            boolean testCommandsEnabled,
            int preMultiplierBucketCount
    ) {
        LOGGER.info(
                "[DamageModule] diagnosticMode={}, testCommands={}",
                diagnosticMode,
                testCommandsEnabled
        );

        LOGGER.info(
                "[DamageModule] Damage processor registry frozen with {} pre-multiplier bucket slots.",
                preMultiplierBucketCount
        );
    }

    public static void configBaked(
            DiagnosticMode diagnosticMode,
            boolean enableTestCommands,
            boolean strictProcessorErrors,
            boolean strictRuleErrors,
            VanillaReductionCompatibilityMode vanillaReductionCompatibilityMode,
            boolean suppressArmor,
            boolean suppressEnchantments,
            boolean suppressMobEffects,
            boolean suppressInnateResistance,
            float asymptoticKValue,
            float resistanceKValue,
            float ratingPerProtScore
    ) {
        LOGGER.info(
                "[DamageModule] Config baked: diagnosticMode={}, "
                        + "testCommands={}, strictProcessorErrors={}, "
                        + "strictRuleErrors={}, vanillaReductionMode={}, "
                        + "suppressArmor={}, suppressEnchantments={}, "
                        + "suppressMobEffects={}, suppressInnateResistance={}, "
                        + "ArmorK={}, ResK={}, ProtScoreRatio={}",
                diagnosticMode,
                enableTestCommands,
                strictProcessorErrors,
                strictRuleErrors,
                vanillaReductionCompatibilityMode,
                suppressArmor,
                suppressEnchantments,
                suppressMobEffects,
                suppressInnateResistance,
                asymptoticKValue,
                resistanceKValue,
                ratingPerProtScore
        );
    }

    public static void startupSelfCheckPassed() {
        LOGGER.info("[DamageModule] Startup self-check passed.");
    }

    public static void safetyConfig(
            int maxRecursionDepth,
            int maxDerivedRequestsPerRoot,
            int maxManagedRequestsPerServerTick
    ) {
        LOGGER.info(
                "[DamageModule] Damage safety: maxRecursionDepth={}, "
                        + "maxDerivedRequestsPerRoot={}, "
                        + "maxManagedRequestsPerServerTick={}",
                maxRecursionDepth,
                maxDerivedRequestsPerRoot,
                maxManagedRequestsPerServerTick
        );
    }

    /** One startup-only developer diagnostic; never emitted per damage. */
    public static void attributeConsumptionStatus() {
        LOGGER.debug(
                "[DamageModule] Attribute consumers active: critical, "
                        + "channel/category damage, channel/category resistance, "
                        + "thorns. Reserved and currently unconsumed: "
                        + "vulnerable_damage_additive, dodge_chance, "
                        + "healing_received."
        );
    }

    public static void channelsLoaded(int channelCount, long contentRevision) {
        LOGGER.info(
                "[DamageModule] Loaded {} damage channels; contentRevision={}",
                channelCount,
                contentRevision
        );
    }

    public static void datapackRulesLoaded(
            int accepted,
            int rejected,
            long ruleRevision,
            long channelRevision
    ) {
        LOGGER.info(
                "[DamageModule] Loaded {} global datapack damage rules; rejected={} ruleRevision={} validatedChannelRevision={} ready=true",
                accepted, rejected, ruleRevision, channelRevision
        );
    }

    public static void templatesLoaded(
            int entries,
            int affixes,
            long templateRevision,
            long channelRevision
    ) {
        LOGGER.info(
                "[DamageModule] Published static templates; entries={} affixes={} templateRevision={} validatedChannelRevision={} ready=true",
                entries, affixes, templateRevision, channelRevision
        );
    }

    public static void externalProcessorRegistered(DamagePhaseProcessor processor) {
        LOGGER.info(
                "[DamageModule] Registered external damage phase processor: {} phase={} priority={}",
                processor.getClass().getName(),
                processor.phase(),
                processor.getPriority()
        );
    }

    public static void pipelinePhase(Object phase) {
        if (!DamageModuleConfig.current()
                .diagnostics()
                .shouldLogFullServerTrace()) {
            return;
        }

        LOGGER.info("[DamageModule] Pipeline phase {}:", phase);
    }

    public static void pipelineProcessor(
            String processorName,
            int priority,
            String kind
    ) {
        if (!DamageModuleConfig.current()
                .diagnostics()
                .shouldLogFullServerTrace()) {
            return;
        }

        LOGGER.info(
                "  - {} priority={} kind={}",
                processorName,
                priority,
                kind
        );
    }

}

