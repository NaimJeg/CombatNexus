package io.github.naimjeg.combatnexus.combat.api.behavior;
import java.util.OptionalDouble;
/** A decision before the health write. Only an effective decision consumes a use. */
public interface ProtectionBehavior extends BehaviorComponent {
    OptionalDouble protect(ProtectionContext context);
    default int maximumUses() { return 1; }
}
