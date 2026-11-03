package io.github.naimjeg.combatnexus.combat.state.api;

import java.util.*;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;
public record CooldownCommand(UUID requestId, UUID target, CombatSource source, Operation operation, CooldownStamp.DeathPolicy deathPolicy) {
    public enum Operation { START, CLEAR }
    public CooldownCommand { Objects.requireNonNull(requestId); Objects.requireNonNull(target); Objects.requireNonNull(source); Objects.requireNonNull(operation); Objects.requireNonNull(deathPolicy); }
}
