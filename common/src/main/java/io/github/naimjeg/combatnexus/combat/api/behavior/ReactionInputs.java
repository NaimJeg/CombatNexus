package io.github.naimjeg.combatnexus.combat.api.behavior;
import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame;
/** Request-scoped frozen participants and source classification, never re-sampled at settlement. */
public record ReactionInputs(Map<UUID,CombatInputFrame> participants, Optional<Identifier> damageType, Set<Identifier> damageTags) {
    public static final ReactionInputs EMPTY=new ReactionInputs(Map.of(),Optional.empty(),Set.of());
    public ReactionInputs { participants=Map.copyOf(participants);Objects.requireNonNull(damageType);damageTags=Set.copyOf(damageTags); }
}
