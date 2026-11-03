package io.github.naimjeg.combatnexus.combat.damage.core.lifecycle;

import java.util.function.Consumer;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageModulePipeline;
import io.github.naimjeg.combatnexus.combat.damage.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.combatnexus.combat.damage.core.critical.CriticalDecisionProviders;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.ExternalItemRuleSources;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.combatnexus.combat.damage.core.template.DamageTemplateRegistry;
import io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementCallbacks;
import io.github.naimjeg.combatnexus.combat.damage.core.util.JvmFatalErrors;
import io.github.naimjeg.combatnexus.combat.damage.core.lifecycle.DamageModuleStartupSelfCheck;
import io.github.naimjeg.combatnexus.combat.damage.registry.PreMultiplierBuckets;

/** Shared registration, freeze, validation and failure cleanup sequence. */
final class DamageModuleInitialization {
    private DamageModuleInitialization() {}
    static void initialize(Consumer<DamageModuleRegistrationSession> registration) {
        DamageModuleRegistrationAccess access =
                DamageModuleLifecycle.beginRegistering();
        DamageModuleRegistrationSession session = null;

        try {
            PreMultiplierBuckets.register(access);

            session = new DamageModuleRegistrationSession(access);

            try {
                registration.accept(session);
            } finally {
                session.close();
            }

            PreMultiplierBucketRegistry.freeze(access);
            DamageAttributionResolvers.freeze(access);
            ExternalItemRuleSources.freeze(access);
            CriticalDecisionProviders.freeze(access);
            DamageSettlementCallbacks.freeze(access);
            io.github.naimjeg.combatnexus.combat.damage.registry.DamagePhaseProcessorRegistry.freeze(access);
            DamageTemplateRegistry.freeze(access);
            DamageModuleLifecycle.freezeRegistration(access);

            DamageModuleStartupSelfCheck.run();

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
    }
}
