package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;
import io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageAttribution;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageAttributionProvenance;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageLineage;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageMetadata;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageSourceDescriptor;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageTriggerPolicy;
import io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.*;
import io.github.naimjeg.combatnexus.combat.damage.core.attribution.DamageAttributionResolvers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;
import java.util.Set;

/**
 * Factory for constructing DamageModuleContext instances from platform input snapshots.
 *
 * <p>This class owns the vanilla bridge setup required before the pipeline can run:
 * attacker/victim extraction, vanilla offensive snapshot consumption, source profile
 * creation, mob-effect bridge analysis, and bridge-plan construction
 * logging.</p>
 *
 * <p>The event handler remains responsible for recursion protection and final
 * VanillaDamageCapture cleanup.</p>
 */
public final class DamageModuleContextFactory {
    private DamageModuleContextFactory() {
    }

    /**
     * Creates a context for a server-side LivingIncomingDamageEvent.
     *
     * @return a DamageModuleContext, or {@code null} when the event should not be
     * processed by DamageModule.
     */
    public static DamageModuleContext tryCreate(
            DamageInput event,
            DamageResultWriter writer,
            DamageOrigin origin, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions
    ) {
        if (event == null) {
            return null;
        }

        LivingEntity victim = event.victim();
        if (victim == null || victim.level().isClientSide()) {
            return null;
        }

        if (!DamageSourcePolicy.shouldManage(event.source())) {
            return null;
        }

        LivingEntity attacker = origin.attribution().logicalAttacker();

        return createServerContext(
                event,
                writer,
                attacker,
                victim,
                origin, definitions
        );
    }

    private static DamageModuleContext createServerContext(
            DamageInput event,
            DamageResultWriter writer,
            LivingEntity attacker,
            LivingEntity victim,
            DamageOrigin origin, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions
    ) {
        VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot =
                VanillaDamageCapture.consumeOffensiveSnapshot(
                        event.source(),
                        victim,
                        event.originalAmount()
                );

        VanillaDamageSourceProfile sourceProfile =
                VanillaDamageSourceProfile.create(
                        event.source(),
                        attacker,
                        victim,
                        DamageSourceClassification.isMagic(event.source())
                );

        io.github.naimjeg.combatnexus.effect.api.VanillaEffectContributions.OffensiveMobEffectBreakdown mobEffectBreakdown =
                VanillaMobEffectBridge.computeOffensiveBreakdown(sourceProfile);

        VanillaBridgePlan bridgePlan =
                VanillaBridgePlan.from(
                        event.originalAmount(),
                        sourceProfile,
                        vanillaSnapshot,
                        mobEffectBreakdown.observedDelta(),
                        mobEffectBreakdown.enabledDelta()
                );

        return createFromBridgePlan(
                event,
                writer,
                attacker,
                victim,
                origin, definitions,
                sourceProfile,
                vanillaSnapshot,
                bridgePlan
        );
    }

    private static DamageModuleContext createFromBridgePlan(
            DamageInput event,
            DamageResultWriter writer,
            LivingEntity attacker,
            LivingEntity victim,
            DamageOrigin origin, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions,
            VanillaDamageSourceProfile sourceProfile,
            VanillaDamageCapture.OffensiveSnapshot vanillaSnapshot,
            VanillaBridgePlan bridgePlan
    ) {
        return new DamageModuleContext(DamageModuleContextSpec.of(
                event,
                writer,
                attacker,
                victim,
                origin, definitions,
                sourceProfile,
                bridgePlan.initialBaseAmount(),
                vanillaSnapshot,
                bridgePlan.rebuildOffensiveMobEffects(),
                bridgePlan.rebuildOffensiveEnchantment(),
                bridgePlan.rebuildPreEventDelta(),
                bridgePlan.offensiveMobEffectDelta(),
                bridgePlan.initialBaseBucket(),
                bridgePlan.offensiveMobEffectBucket(),
                bridgePlan.offensiveEnchantmentBucket()
        ));
    }

    private static DamageRequestKind nativeRequestKind(
            DamageAttribution attribution
    ) {
        return attribution.directEntity() == null
                && attribution.logicalAttacker() == null
                && attribution.effectOwner() == null
                ? DamageRequestKind.ENVIRONMENTAL
                : DamageRequestKind.PRIMARY;
    }

    public static DamageOrigin nativeOrigin(
            DamageInput event
    ) {
        Entity rawAttacker = event.source().getEntity();
        LivingEntity vanillaAttacker = rawAttacker
                instanceof LivingEntity livingAttacker
                ? livingAttacker
                : null;
        DamageAttribution vanillaDefault = DamageAttribution.defaults(
                event.source().getDirectEntity(),
                vanillaAttacker,
                rawAttacker,
                vanillaAttacker
        );
        DamageAttribution attribution =
                DamageAttributionResolvers.normalizeVanillaDefault(
                        (ServerLevel) event.victim().level(),
                        vanillaDefault
                );
        DamageRequestKind kind = DamageSourcePolicy.isVanillaThorns(event.source())
                ? DamageRequestKind.THORNS : io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.NativeSweepScope.matches(event.victim(), event.source())
                ? DamageRequestKind.SWEEP : nativeRequestKind(attribution);
        var kernel = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(((ServerLevel) event.victim().level()).getServer());
        var execution = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.currentLineage(kernel.server());
        DamageLineage lineage = execution.map(DamageLineage::execution).orElseGet(DamageLineage::newRoot);
        var triggerPolicy = io.github.naimjeg.combatnexus.combat.damage.core.request.DamageExecutionPolicy.constrain(
                lineage.transaction(), DamageTriggerPolicy.defaultsFor(kind));
        float originalAmount = event.originalAmount();
        float safeAmount = Float.isFinite(originalAmount)
                ? Math.max(0.0f, originalAmount)
                : 0.0f;

        DamageOrigin origin = new DamageOrigin(
                lineage,
                kind,
                attribution,
                DamageSourceDescriptor.from(event.source()),
                safeAmount,
                Optional.empty(),
                Set.of(),
                triggerPolicy,
                DamageMetadata.empty()
        );
        return origin.withResolvedAttribution(
                attribution,
                DamageAttributionProvenance.vanillaDefault()
        );
    }
}

