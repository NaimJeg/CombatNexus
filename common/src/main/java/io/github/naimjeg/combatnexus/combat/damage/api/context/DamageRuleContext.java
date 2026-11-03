package io.github.naimjeg.combatnexus.combat.damage.api.context;

/**
 * Public rule-facing context.
 * <p>
 * Rules can inspect stable damage state through DamageContextView and mutate
 * damage only through DamageMutationContext.
 */
public interface DamageRuleContext
        extends DamageContextView, DamageMutationContext {
    /** Records the result of a guarded mutation during this calculation callback. */
    void recordContribution(DamageMutationResult result,
            java.util.function.Supplier<io.github.naimjeg.combatnexus.combat.damage.api.display.DamageContributionDescriptor> descriptor);
    DamageMutationResult tryAddVanillaBaseReconstructedDamage(
            io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageChannel channel,
            io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageApplicationBucket bucket, float value, String sourceId);
    void beforeApplication(io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Step step);
}
