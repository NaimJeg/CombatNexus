package io.github.naimjeg.combatnexus.combat.damage.core.config;
import io.github.naimjeg.combatnexus.combat.config.CombatServerSettings;
import io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions;
import net.minecraft.server.MinecraftServer;
/** Derived view of the single adopted combat generation; no independent publication. */
public final class DamageSettingsStore {
    public record Snapshot(long revision, CombatServerSettings values) {}
    private DamageSettingsStore() {}
    public static Snapshot current(MinecraftServer server) {
        var generation = CombatDefinitions.forServer(server);
        return new Snapshot(generation.generation(), generation.settings());
    }
}
