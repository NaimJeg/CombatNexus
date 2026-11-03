package io.github.naimjeg.combatnexus.combat.api.source;

import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.effect.api.EffectDefinition;
import io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationDefinition;
import io.github.naimjeg.combatnexus.combat.shield.api.ShieldDefinition;

/** One installation with independently owned contributions. Definitions are copied at installation. */
public record SourcePlan(Identifier definition, Map<Identifier, EffectDefinition> effects,
        Map<Identifier, RegenerationDefinition> regeneration, Map<Identifier, Shield> shields, boolean persistent) {
    public enum ShieldRemoval { REVOKE, KEEP_UNTIL_EXPIRY }
    public record Shield(ShieldDefinition definition, double amount, int durationTicks, ShieldRemoval removal) {
        public Shield {
            Objects.requireNonNull(definition); Objects.requireNonNull(removal);
            if (!Double.isFinite(amount) || amount < 0 || amount > definition.capacity() || durationTicks < 1 || durationTicks > 1200000)
                throw new IllegalArgumentException("Invalid source shield");
        }
    }
    public SourcePlan {
        Objects.requireNonNull(definition); effects = Map.copyOf(effects); regeneration = Map.copyOf(regeneration); shields = Map.copyOf(shields);
        if (effects.size() + regeneration.size() + shields.size() > 64) throw new IllegalArgumentException("Source contribution capacity exceeded");
    }
}
