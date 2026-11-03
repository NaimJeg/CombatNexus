package io.github.naimjeg.combatnexus.combat.state.internal;
import java.util.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.state.api.StateKey;
public final class RuntimeStateTypes {
    private RuntimeStateTypes() {}
    private static Map<Identifier,StateKey<?>> types=Map.of();
    public static void install(Map<Identifier,StateKey<?>> values) { types=Map.copyOf(values); }
    public static StateKey<?> get(Identifier id) { return Objects.requireNonNull(types.get(id),"Unregistered runtime state: "+id); }
    public static void require(StateKey<?> key) { if(get(key.id())!=key)throw new IllegalArgumentException("State definition identity mismatch"); }
}
