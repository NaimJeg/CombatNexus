package io.github.naimjeg.combatnexus.combat.api.reaction;
import java.util.*;
/** Read-only reaction diagnostics. Dispatch and allowance accounting are internal responsibilities. */
public interface CombatReactionService {
    List<ReactionFact> facts();
}
