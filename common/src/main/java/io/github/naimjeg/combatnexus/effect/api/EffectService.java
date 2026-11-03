package io.github.naimjeg.combatnexus.effect.api;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.resources.Identifier;

/** World-owned business operations; managed changes never reapply observed vanilla modifiers. */
public interface EffectService {
    boolean apply(EffectInstance effect, Identifier stacking);
    default boolean apply(EffectInstance effect, Stacking stacking) { return apply(effect, stacking.id()); }
    io.github.naimjeg.combatnexus.combat.api.CommandResult<Boolean> applyInstant(EffectInstance effect);
    default boolean bind(EffectBinding binding) {
        if (binding.satisfied()) return apply(binding.effect(), binding.stacking());
        var state = binding.effect().snapshot();
        remove(state.target(), state.effect(), state.source(), Removal.CONDITION_ENDED);
        return true;
    }
    void remove(UUID target, Identifier effect, Identifier source, Removal reason);
    List<EffectInstance> snapshot(UUID target);
    List<EffectInstance> snapshot(UUID target, java.util.function.IntConsumer budget);
    Set<EffectSnapshot.RestrictedAction> restrictions(UUID target);
    Set<Identifier> pendingSources(UUID target);

    enum Stacking {
        REPLACE, REFRESH_KEEP_STRONGER, STRONGER_WITH_HIDDEN;
        public Identifier id() { return Identifier.fromNamespaceAndPath("combatnexus", name().toLowerCase(java.util.Locale.ROOT)); }
    }
    enum Removal { DISPELLED, REMOVED, EXPIRED, CONDITION_ENDED, DEATH, RESPAWN, DIMENSION_CHANGE, WORLD_EXIT }
    /** before/after lists have zero or one element; empty after means revoke only the before source. */
    record Change(List<EffectInstance> before, List<EffectInstance> after, java.util.Optional<Removal> removal, boolean preservePeriodicPhase) {
        public Change(List<EffectInstance> before, List<EffectInstance> after, java.util.Optional<Removal> removal) { this(before, after, removal, false); }
        public Change {
            before = List.copyOf(before);
            after = List.copyOf(after);
            java.util.Objects.requireNonNull(removal, "removal");
        }
    }
}
