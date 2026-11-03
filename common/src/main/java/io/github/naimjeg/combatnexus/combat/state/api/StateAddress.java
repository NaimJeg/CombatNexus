package io.github.naimjeg.combatnexus.combat.state.api;
import java.util.*;
public record StateAddress<T>(UUID target,StateKey<T> key,Optional<UUID> scope) {
    public StateAddress { Objects.requireNonNull(target);Objects.requireNonNull(key);Objects.requireNonNull(scope);if((key.scope()==StateKey.Scope.ENTITY)!=scope.isEmpty())throw new IllegalArgumentException("State scope identity mismatch"); }
}
