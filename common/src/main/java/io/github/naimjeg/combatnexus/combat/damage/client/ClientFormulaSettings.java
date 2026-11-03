package io.github.naimjeg.combatnexus.combat.damage.client;

import io.github.naimjeg.combatnexus.combat.damage.network.payload.FormulaSettingsPayload;
import java.util.Optional;

/** Display-only cache for the current connection. No local formula defaults impersonate server settings. */
public final class ClientFormulaSettings {
    private static FormulaSettingsPayload current;
    private ClientFormulaSettings() {}
    public static void accept(FormulaSettingsPayload payload) {
        if (current == null || payload.revision() >= current.revision()) current = payload;
    }
    public static Optional<FormulaSettingsPayload> current() { return Optional.ofNullable(current); }
    public static void clear() { current = null; }
}
