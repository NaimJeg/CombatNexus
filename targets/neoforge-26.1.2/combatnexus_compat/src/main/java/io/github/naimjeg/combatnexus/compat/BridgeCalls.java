package io.github.naimjeg.combatnexus.compat;

import java.util.UUID;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.bridge.*;

public final class BridgeCalls {
    private BridgeCalls() {}
    public static BridgeCall replacing(CombatWorld world, String id, UUID target, UUID action) {
        var capability=CombatAccess.bridgeCoverage().stream().filter(c->c.id().equals(Identifier.parse(id))&&c.mode()==BridgeCapability.Mode.REPLACE).findFirst().orElseThrow();
        var call=world.bridges().open(UUID.randomUUID(),action,target,capability.id(),capability.position(),capability.executor());
        call.nativeContribution(BridgeInvocation.Contribution.SUPPRESSED);
        return call;
    }
}
