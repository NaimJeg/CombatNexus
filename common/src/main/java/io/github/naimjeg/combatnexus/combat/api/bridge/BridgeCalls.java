package io.github.naimjeg.combatnexus.combat.api.bridge;
import java.util.*;
import net.minecraft.resources.Identifier;
public interface BridgeCalls {
    BridgeCall open(UUID invocation, UUID action, UUID target, Identifier capability, Identifier position, Identifier executor);
    List<BridgeInvocation> history();
}
