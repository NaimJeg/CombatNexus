package io.github.naimjeg.combatnexus.damage.bridge.vanilla;

import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.VanillaBridgeDiagnosticsLog;

public final class VanillaBridgeLogger {

    private VanillaBridgeLogger() {
    }

    public static void logSnapshot(
            VanillaDamageCapture.OffensiveSnapshot snapshot
    ) {
        if (!DamageModuleSettings.fullTraceEnabled() || snapshot == null) {
            return;
        }

        VanillaDamageCapture.PreEventDelta delta = snapshot.preEventDelta();

        if (snapshot.hasEnchantDelta()) {
            VanillaBridgeDiagnosticsLog.offensiveEnchantSnapshot(snapshot);
        }

        if (snapshot.preEventDelta().kind() != PreEventDeltaKind.NONE) {
            VanillaBridgeDiagnosticsLog.preEventDelta(snapshot, delta);
        }

        if (delta.kind() == PreEventDeltaKind.UNKNOWN) {
            VanillaBridgeDiagnosticsLog.unknownPreEventDelta(snapshot, delta);
        }

        if (snapshot.hasProjectileCriticalBonus()) {
            VanillaBridgeDiagnosticsLog.projectileCriticalBonus(snapshot);
        }
    }
}

