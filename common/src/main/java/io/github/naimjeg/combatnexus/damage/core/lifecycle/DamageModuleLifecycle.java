package io.github.naimjeg.combatnexus.damage.core.lifecycle;

import io.github.naimjeg.combatnexus.damage.core.attribution.DamageAttributionResolvers;
import io.github.naimjeg.combatnexus.damage.core.critical.CriticalDecisionProviders;
import io.github.naimjeg.combatnexus.damage.core.rule.ExternalItemRuleSources;

public final class DamageModuleLifecycle {

    private static DamageModuleLifecycleState state =
            DamageModuleLifecycleState.CONSTRUCTING;

    private DamageModuleLifecycle() {
    }

    public static synchronized DamageModuleLifecycleState state() {
        return state;
    }

    static synchronized DamageModuleRegistrationAccess beginRegistering() {
        transitionFrom(
                DamageModuleLifecycleState.CONSTRUCTING,
                DamageModuleLifecycleState.REGISTERING
        );

        return new DamageModuleRegistrationAccess();
    }

    static synchronized void freezeRegistration(
            DamageModuleRegistrationAccess access
    ) {
        requireRegistering(access, "freezeRegistration");
        transitionFrom(
                DamageModuleLifecycleState.REGISTERING,
                DamageModuleLifecycleState.FROZEN
        );
        access.close();
    }

    static synchronized void running() {
        transitionFrom(
                DamageModuleLifecycleState.FROZEN,
                DamageModuleLifecycleState.RUNNING
        );
    }

    public static synchronized void requireRegistering(
            DamageModuleRegistrationAccess access,
            String action
    ) {
        if (access == null) {
            throw new IllegalStateException(
                    "DamageModule registration requires the active event "
                            + "registrar. action="
                            + action
                            + " state="
                            + state
            );
        }

        access.requireActive(action);

        if (state != DamageModuleLifecycleState.REGISTERING) {
            throw new IllegalStateException(
                    "DamageModule registration is only allowed during "
                            + "DamageModuleRegisterEvent. action="
                            + action
                            + " state="
                            + state
            );
        }
    }

    static synchronized void resetForTesting() {
        state = DamageModuleLifecycleState.CONSTRUCTING;
        DamageAttributionResolvers.resetForTesting();
        ExternalItemRuleSources.resetForTesting();
        CriticalDecisionProviders.resetForTesting();
        io.github.naimjeg.combatnexus.damage.core.settlement.DamageSettlementCallbacks
                .resetForTesting();
        io.github.naimjeg.combatnexus.damage.core.template.DamageTemplateRegistry
                .resetForTesting();
        io.github.naimjeg.combatnexus.damage.core.rule.DatapackDamageRuleStore
                .resetForTesting();
    }

    static synchronized void failBootstrap(
            DamageModuleRegistrationAccess access
    ) {
        if (access == null) {
            throw new IllegalStateException(
                    "DamageModule bootstrap failure requires its registration "
                            + "capability. state="
                            + state
            );
        }

        if (state != DamageModuleLifecycleState.REGISTERING
                && state != DamageModuleLifecycleState.FROZEN) {
            throw invalidTransition(DamageModuleLifecycleState.FAILED);
        }

        access.close();
        state = DamageModuleLifecycleState.FAILED;
    }

    private static void transitionFrom(
            DamageModuleLifecycleState expected,
            DamageModuleLifecycleState next
    ) {
        if (state != expected) {
            throw invalidTransition(next);
        }

        state = next;
    }

    private static IllegalStateException invalidTransition(
            DamageModuleLifecycleState next
    ) {
        return new IllegalStateException(
                "Invalid DamageModule lifecycle transition: "
                        + state
                        + " -> "
                        + next
        );
    }
}
