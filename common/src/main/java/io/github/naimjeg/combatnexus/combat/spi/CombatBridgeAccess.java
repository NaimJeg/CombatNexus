package io.github.naimjeg.combatnexus.combat.spi;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
public final class CombatBridgeAccess {
    private CombatBridgeAccess() {}
    public static void require(CombatOperation operation) { io.github.naimjeg.combatnexus.combat.internal.coordination.BridgeExecutions.require(operation); }
    public static void healing(io.github.naimjeg.combatnexus.combat.healing.api.HealingSettlement result,CombatLineage lineage) {
        var context=new CommandContext(result.request().requestId(),result.request().target(),CombatOperation.HEALING,java.util.Optional.of(result.request().source()),
                java.util.Optional.of(new CommandContext.Identity(lineage.id(),lineage.rootId(),lineage.parentId(),lineage.depth())));
        io.github.naimjeg.combatnexus.combat.internal.coordination.BridgeExecutions.evidence(new io.github.naimjeg.combatnexus.combat.api.bridge.BridgeInvocation.CommandEvidence(java.util.Optional.of(context),result.journal(),result.rejection()));
    }
}
