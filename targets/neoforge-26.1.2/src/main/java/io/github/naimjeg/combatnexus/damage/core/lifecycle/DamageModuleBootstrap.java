package io.github.naimjeg.combatnexus.damage.core.lifecycle;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.event.DamageModuleRegisterEvent;
import io.github.naimjeg.combatnexus.damage.config.DamageModuleConfig;
import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModulePipeline;
import io.github.naimjeg.combatnexus.damage.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.combatnexus.damage.core.critical.CriticalDecisionProviders;
import io.github.naimjeg.combatnexus.damage.core.rule.ExternalItemRuleSources;
import io.github.naimjeg.combatnexus.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.combatnexus.damage.core.template.DamageTemplateRegistry;
import io.github.naimjeg.combatnexus.damage.core.settlement.DamageSettlementCallbacks;
import io.github.naimjeg.combatnexus.damage.core.util.JvmFatalErrors;
import io.github.naimjeg.combatnexus.damage.diagnostics.DamageModuleStartupSelfCheck;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleLifecycleLog;
import io.github.naimjeg.combatnexus.damage.registry.PreMultiplierBuckets;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Framework-owned lifecycle coordinator.
 *
 * <p>NeoForge discovers this package-private MOD-bus subscriber directly.
 * There is no public install/progression entry point that another mod can call
 * before the real DamageModule bootstrap.</p>
 */
@EventBusSubscriber(modid = DamageModule.MODID)
final class DamageModuleBootstrap {

    private static final AtomicBoolean COMMON_SETUP_CLAIMED =
            new AtomicBoolean();

    private DamageModuleBootstrap() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        if (!claimCommonSetup()) {
            return;
        }

        event.enqueueWork(DamageModuleBootstrap::runCommonSetup);
    }

    static boolean claimCommonSetup() {
        return COMMON_SETUP_CLAIMED.compareAndSet(false, true);
    }

    private static void runCommonSetup() {
        DamageModuleConfig.bakeConfig();

        DamageModuleRegistrationAccess access =
                DamageModuleLifecycle.beginRegistering();
        DamageModuleRegistrationSession session = null;

        try {
            PreMultiplierBuckets.register(access);

            session = new DamageModuleRegistrationSession(access);

            try {
                DamageModule.LOGGER.info(
                        "[DamageModule] Posting DamageModuleRegisterEvent"
                );
                NeoForge.EVENT_BUS.post(
                        new DamageModuleRegisterEvent(session)
                );
            } finally {
                session.close();
            }

            PreMultiplierBucketRegistry.freeze(access);
            DamageAttributionResolvers.freeze(access);
            ExternalItemRuleSources.freeze(access);
            CriticalDecisionProviders.freeze(access);
            DamageSettlementCallbacks.freeze(access);
            DamageTemplateRegistry.freeze(access);
            DamageModuleLifecycle.freezeRegistration(access);

            DamageModuleStartupSelfCheck.run();

            DamageModuleLifecycleLog.safetyConfig(
                    DamageModuleSettings.maxRecursionDepth(),
                    DamageModuleSettings.maxDerivedRequestsPerRoot(),
                    DamageModuleSettings.maxManagedRequestsPerServerTick()
            );
            DamageModuleLifecycleLog.attributeConsumptionStatus();

            DamageModulePipeline.clearCache();
            DamageModuleLifecycle.running();
        } catch (Throwable throwable) {
            JvmFatalErrors.rethrowIfFatal(throwable);

            if (session != null) {
                session.close();
            }

            DamageModuleLifecycle.failBootstrap(access);
            throw throwable;
        }

        DamageModuleLifecycleLog.commonSetupComplete(
                DamageModuleSettings.diagnosticMode(),
                DamageModuleSettings.testCommandsEnabled(),
                PreMultiplierBucketRegistry.bucketCount()
        );
    }

}
