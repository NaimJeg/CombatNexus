package io.github.naimjeg.combatnexus.combat.healing.api;

import io.github.naimjeg.combatnexus.combat.api.CombatSource;
import io.github.naimjeg.combatnexus.formula.Amounts;
import java.util.Objects;
import java.util.UUID;

/** Independent healing command; never interpreted as negative damage. */
public record HealingRequest(UUID requestId, UUID target, CombatSource source, Reason reason, double amount,
                             java.util.Optional<UUID> healer) {
    public HealingRequest(UUID requestId, UUID target, CombatSource source, Reason reason, double amount) {
        this(requestId, target, source, reason, amount, source.owner());
    }
    public HealingRequest {
        Objects.requireNonNull(healer, "healer");
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(reason, "reason");
        Amounts.nonNegative(amount, "amount");
    }

    public enum Reason { NATURAL_REGENERATION, EFFECT, ITEM, LIFESTEAL, SKILL, CRYSTAL, UNKNOWN_EXTERNAL }
}
