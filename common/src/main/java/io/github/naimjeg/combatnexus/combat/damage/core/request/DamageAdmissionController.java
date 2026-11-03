package io.github.naimjeg.combatnexus.combat.damage.core.request;
import io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration;

import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageFailureReason;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageTriggerPolicy;
import io.github.naimjeg.combatnexus.combat.config.CombatSafetySettings;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/** Atomic Phase 4 trigger, lineage, root, and server-tick admission. */
public final class DamageAdmissionController {

    private DamageAdmissionController() {
    }

    public static DamageAdmissionResult admitNative(
            DamageOrigin origin,
            MinecraftServer server, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions
    ) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(server, "server");
        CombatSafetySettings settings = definitions.settings().safety();

        DamageFailureReason depthFailure = depthFailure(
                origin.lineage().recursionDepth(),
                settings.maxRecursionDepth()
        );
        if (depthFailure != null) {
            return rejectedWithoutConsumption(
                    origin,
                    server,
                    depthFailure
            );
        }

        var kernel = CombatServerRuntime.of(server);
        if (kernel.hasTicket(origin.lineage().transaction()))
            return DamageAdmissionResult.admitted(origin.lineage().derivedRequestCountInternal(), kernel.currentTickCount());
        throw new IllegalStateException("Native damage requires admission before entering hurtServer");
    }

    static @Nullable DamageFailureReason triggerFailure(
            DamageRequestKind kind,
            @Nullable DamageTriggerPolicy parentPolicy
    ) {
        if (parentPolicy == null) {
            return null;
        }
        return switch (kind) {
            case PROC -> parentPolicy.procAllowed()
                    ? null
                    : DamageFailureReason.PROC_SUPPRESSED;
            case REFLECTED -> parentPolicy.reflectionAllowed()
                    ? null
                    : DamageFailureReason.REFLECTION_SUPPRESSED;
            case THORNS -> parentPolicy.thornsAllowed()
                    ? null
                    : DamageFailureReason.THORNS_SUPPRESSED;
            case PRIMARY, SWEEP, DOT, ENVIRONMENTAL, CUSTOM -> null;
        };
    }

    static @Nullable DamageFailureReason depthFailure(
            int depth,
            int maximum
    ) {
        if (depth < 0 || maximum < 1) {
            throw new IllegalArgumentException(
                    "Depth must be non-negative and maximum must be positive"
            );
        }
        return depth > maximum
                ? DamageFailureReason.MAX_RECURSION_DEPTH
                : null;
    }

    public static void clearServer(MinecraftServer server) {
        CombatServerRuntime.stop(server);
    }

    public static DamageAdmissionResult rejectEventReentrancy(
            DamageOrigin origin,
            MinecraftServer server
    ) {
        return rejectedWithoutConsumption(
                Objects.requireNonNull(origin, "origin"),
                Objects.requireNonNull(server, "server"),
                DamageFailureReason.EVENT_REENTRANCY_LIMIT
        );
    }

    static int currentTickCount(MinecraftServer server) {
        return CombatServerRuntime.of(server).currentTickCount();
    }

    private static DamageAdmissionResult rejectedWithoutConsumption(
            DamageOrigin origin,
            MinecraftServer server,
            DamageFailureReason reason
    ) {
        return DamageAdmissionResult.rejected(
                reason,
                origin.lineage().derivedRequestCountInternal(),
                currentTickCount(server)
        );
    }

}
