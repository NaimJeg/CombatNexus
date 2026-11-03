package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

/** Incoming handoff is an application action, never evidence of a native resource commit. */
public final class DamageApplicationCoordinator {
    private DamageApplicationCoordinator() {}
    public static DamageApplicationPlan calculateAndHandoff(DamageModuleContext context) {
        DamageApplicationPlan plan = DamageModulePipeline.execute(context);
        if (context.isManaged()) context.handoff(plan);
        return plan;
    }
}
