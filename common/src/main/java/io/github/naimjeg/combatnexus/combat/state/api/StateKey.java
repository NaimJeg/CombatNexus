package io.github.naimjeg.combatnexus.combat.state.api;
import java.util.*;
import com.mojang.serialization.Codec;
import net.minecraft.resources.Identifier;
/** A startup definition explicitly owns state lifetime; queries never advance or clear it. */
public record StateKey<T>(Identifier id,Codec<T> codec,Scope scope,Set<Boundary> clearOn,boolean persistent) {
    public enum Scope { ENTITY, SOURCE_INSTALLATION, ACTION }
    public enum Boundary { DEATH, RESPAWN, SOURCE_REMOVED, DIMENSION_CHANGE, ACTION_FINISHED }
    public StateKey { Objects.requireNonNull(id);Objects.requireNonNull(codec);Objects.requireNonNull(scope);clearOn=Set.copyOf(clearOn);if(scope==Scope.ACTION&&persistent)throw new IllegalArgumentException("Action state is session scoped"); }
}
