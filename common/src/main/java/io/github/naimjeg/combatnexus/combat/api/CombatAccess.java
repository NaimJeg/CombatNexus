package io.github.naimjeg.combatnexus.combat.api;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
/** Lookup never constructs, advances or resurrects a world runtime. */
public final class CombatAccess {
    private CombatAccess() {}
    public static java.util.List<io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability> bridgeCoverage() {
        return io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.bridgeCoverage();
    }
    public static Optional<CombatWorld> find(ServerLevel level) {
        return io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.existing(level.getServer())
            .flatMap(server -> server.world(level)).map(world -> new io.github.naimjeg.combatnexus.combat.internal.WorldServices(world));
    }
}
