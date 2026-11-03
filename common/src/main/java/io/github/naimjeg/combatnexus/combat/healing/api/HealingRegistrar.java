package io.github.naimjeg.combatnexus.combat.healing.api;

import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;

/** Valid only during the startup registration event. All handlers receive frozen participant facts. */
public interface HealingRegistrar {
    void processor(Identifier id, HealingPhase phase, int priority, Consumer<HealingCalculation> processor);
    void observe(Identifier id, int priority, Consumer<HealingSettlement> observer);
    void react(Identifier id, int priority, Set<CombatOperation> permissions, Consumer<CombatCallback<HealingSettlement>> reaction);
}
