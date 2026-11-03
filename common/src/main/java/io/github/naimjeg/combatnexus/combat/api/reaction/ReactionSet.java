package io.github.naimjeg.combatnexus.combat.api.reaction;
import java.util.*;
import java.util.function.Consumer;
/** Fixed code definitions. Source instances are supplied by the domain capture, not registered here. */
public final class ReactionSet<C> {
    private final List<SettlementReaction<C>> reactions;
    public ReactionSet(List<SettlementReaction<C>> definitions) {
        var ids = new HashSet<net.minecraft.resources.Identifier>();
        for (var r : definitions) if (!ids.add(r.id())) throw new IllegalArgumentException("Duplicate reaction " + r.id());
        reactions = definitions.stream().sorted(Comparator.<SettlementReaction<C>>comparingInt(SettlementReaction::priority)
                .reversed().thenComparing(r -> r.id().toString())).toList();
    }
    public void dispatch(C context, long operation, UUID target, Optional<net.minecraft.resources.Identifier> source, Consumer<ReactionFact> journal) {
        for (var r : reactions) {
            String result;
            Optional<String> child = Optional.empty();
            try { var rejected = r.rejection().apply(context); result = rejected.orElse("executed");
                if (rejected.isEmpty()) { var applied = r.action().apply(context); result = applied.outcome(); child = applied.childRequest(); }
            } catch (Throwable failure) {
                io.github.naimjeg.combatnexus.combat.api.transaction.CombatFailures.rethrowFatal(failure);
                result = "callback_failed";
            }
            try { journal.accept(new ReactionFact(operation, target, r.id(), source, result, child)); }
            catch (Throwable failure) {
                io.github.naimjeg.combatnexus.combat.api.transaction.CombatFailures.rethrowFatal(failure);
            }
        }
    }
}
