package io.github.naimjeg.combatnexus.combat.api.source;
import java.util.List;
import io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame;
/** Return the provider's current slots/casts. Unchanged offers retain their installation and consumed state. */
@FunctionalInterface
public interface SourceProvider {
    record Offer(SourceInstance source, SourcePlan plan) {}
    List<Offer> snapshot(CombatInputFrame target);
}
