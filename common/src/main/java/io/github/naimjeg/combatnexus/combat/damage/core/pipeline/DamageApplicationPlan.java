package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.api.critical.CriticalDecisionSnapshot;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable internal handoff from the pipeline to a public request submission.
 */
public record DamageApplicationPlan(
        float resolvedDamage,
        Map<Identifier, Float> resolvedChannelDamage,
        boolean critical,
        CriticalDecisionSnapshot criticalDecision,
        boolean cancelled,
        @Nullable String cancelSourceId,
        io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs reactionInputs,
        float penetratedArmor, float offensiveTotal, long definitionGeneration,
        java.util.List<io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorSource> behaviors,
        java.util.Optional<io.github.naimjeg.combatnexus.combat.api.action.CombatAction> action,
        io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan beforeApplication
) {
    public DamageApplicationPlan {
        behaviors = java.util.List.copyOf(behaviors);
        resolvedDamage = Float.isFinite(resolvedDamage)
                ? Math.max(0.0f, resolvedDamage)
                : 0.0f;
        resolvedChannelDamage = resolvedChannelDamage == null
                ? Map.of()
                : Collections.unmodifiableMap(
                        new LinkedHashMap<>(resolvedChannelDamage)
                );
        criticalDecision = criticalDecision == null
                ? CriticalDecisionSnapshot.unresolved()
                : criticalDecision;
    }

    public DamageApplicationPlan(
            float resolvedDamage,
            Map<Identifier, Float> resolvedChannelDamage,
            boolean critical,
            boolean cancelled,
            @Nullable String cancelSourceId,
            io.github.naimjeg.combatnexus.combat.api.behavior.ReactionInputs reactionInputs
    ) {
        this(resolvedDamage, resolvedChannelDamage, critical,
                CriticalDecisionSnapshot.unresolved(), cancelled, cancelSourceId, reactionInputs, Float.NaN, resolvedDamage, 0, java.util.List.of(), java.util.Optional.empty(), io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.EMPTY);
    }
}
