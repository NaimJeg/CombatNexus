package io.github.naimjeg.combatnexus.combat.api.reaction;
import net.minecraft.resources.Identifier;
import java.util.Optional;
/** Bounded descriptive fact, never a retained callback or execution capability. */
public record ReactionFact(long operation, java.util.UUID target, Identifier reaction, Optional<Identifier> source, String outcome, Optional<String> childRequest) {}
