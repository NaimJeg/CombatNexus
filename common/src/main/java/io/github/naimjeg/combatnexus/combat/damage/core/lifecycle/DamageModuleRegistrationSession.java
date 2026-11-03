package io.github.naimjeg.combatnexus.combat.damage.core.lifecycle;

import com.mojang.serialization.MapCodec;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageAttributionResolver;
import io.github.naimjeg.combatnexus.combat.damage.api.critical.CriticalDecisionProvider;
import io.github.naimjeg.combatnexus.combat.damage.api.event.DamageModuleRegistrar;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleProvider;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleValidator;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.provider.StaticDamageRuleProvider;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.source.EquippedItemRuleSource;
import io.github.naimjeg.combatnexus.combat.damage.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.combatnexus.combat.damage.core.critical.CriticalDecisionProviders;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.ExternalItemRuleSources;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.combatnexus.combat.damage.core.template.DamageTemplateRegistry;
import io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementCallbacks;
import io.github.naimjeg.combatnexus.combat.damage.api.event.DamageSettlementListener;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.registry.DamagePhaseProcessorRegistry;
import io.github.naimjeg.combatnexus.combat.damage.registry.rule.DamageRuleConditionTypes;
import io.github.naimjeg.combatnexus.combat.damage.registry.rule.DamageRuleOperationTypes;
import io.github.naimjeg.combatnexus.combat.damage.registry.rule.DamageRuleProviders;
import net.minecraft.resources.Identifier;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

final class DamageModuleRegistrationSession
        implements DamageModuleRegistrar {

    private final DamageModuleRegistrationAccess access;
    private final AtomicBoolean open = new AtomicBoolean(true);

    DamageModuleRegistrationSession(DamageModuleRegistrationAccess access) {
        this.access = Objects.requireNonNull(access, "access");
        DamageModuleLifecycle.requireRegistering(
                access,
                "createRegistrationSession"
        );
    }

    @Override
    public void registerCondition(
            Identifier id,
            MapCodec<? extends DamageRuleCondition> codec
    ) {
        requireOpen("registerCondition");
        DamageRuleConditionTypes.register(access, id, codec);
    }

    @Override
    public void registerOperation(
            Identifier id,
            MapCodec<? extends DamageRuleOperation> codec
    ) {
        requireOpen("registerOperation");
        DamageRuleOperationTypes.register(access, id, codec);
    }

    @Override
    public int registerPreMultiplierBucket(Identifier id) {
        requireOpen("registerPreMultiplierBucket");
        return PreMultiplierBucketRegistry.registerPreMultiplierBucket(
                access,
                id
        );
    }

    @Override
    public void registerRuleProvider(DamageRuleProvider provider) {
        requireOpen("registerRuleProvider");
        DamageRuleProviders.register(access, provider);
    }

    @Override
    public void registerGlobalRule(DamageRuleDefinition rule) {
        requireOpen("registerGlobalRule");
        DamageRuleValidator.requireValid(
                rule,
                "java_api/register_global_rule"
        );
        DamageRuleProviders.register(
                access,
                new StaticDamageRuleProvider(rule)
        );
    }

    @Override
    public io.github.naimjeg.combatnexus.combat.api.pipeline.PipelineRegistration registerPhaseProcessor(Identifier id, DamagePhaseProcessor processor) {
        requireOpen("registerPhaseProcessor");
        return DamagePhaseProcessorRegistry.registerExternal(access, id, processor);
    }

    @Override
    public void registerAttributionResolver(
            Identifier id,
            int priority,
            DamageAttributionResolver resolver
    ) {
        requireOpen("registerAttributionResolver");
        DamageAttributionResolvers.register(access, id, priority, resolver);
    }

    @Override
    public void registerEquippedItemRuleSource(
            Identifier id,
            int priority,
            EquippedItemRuleSource source
    ) {
        requireOpen("registerEquippedItemRuleSource");
        ExternalItemRuleSources.register(access, id, priority, source);
    }

    @Override
    public void registerCriticalDecisionProvider(
            Identifier id,
            int priority,
            CriticalDecisionProvider provider
    ) {
        requireOpen("registerCriticalDecisionProvider");
        CriticalDecisionProviders.register(access, id, priority, provider);
    }

    @Override
    public void registerSettlementListener(
            Identifier id,
            int priority,
            DamageSettlementListener listener
    ) {
        requireOpen("registerSettlementListener");
        DamageSettlementCallbacks.register(access, id, priority, listener);
    }

    @Override
    public void registerEntryTemplate(
            Identifier id,
            DamageEntryDefinition definition
    ) {
        requireOpen("registerEntryTemplate");
        DamageTemplateRegistry.registerEntry(access, id, definition);
    }

    @Override
    public void registerAffixTemplate(
            Identifier id,
            DamageAffixDefinition definition
    ) {
        requireOpen("registerAffixTemplate");
        DamageTemplateRegistry.registerAffix(access, id, definition);
    }

    void close() {
        open.set(false);
    }

    boolean isOpen() {
        return open.get();
    }

    private void requireOpen(String action) {
        if (!open.get()) {
            throw new IllegalStateException(
                    "DamageModule registration event has ended; saved "
                            + "registrars cannot be reused. action="
                            + action
            );
        }

        DamageModuleLifecycle.requireRegistering(access, action);
    }
}
