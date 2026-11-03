package io.github.naimjeg.combatnexus.combat.api.input;

import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;

/** Serialized at capture; decoding returns a detached value on each read. No live objects survive. */
public final class FrozenFacts {
    private final Map<FactKey<?>, String> values;
    private final Set<Identifier> incomplete;
    public FrozenFacts(Map<FactKey<?>, String> values, Set<Identifier> incomplete) {
        this.values = Map.copyOf(values); this.incomplete = Set.copyOf(incomplete);
    }
    public Set<Identifier> incomplete() { return incomplete; }
    public <T> Optional<T> find(FactKey<T> key) {
        var json = values.get(key);
        return json == null ? Optional.empty() : Optional.of(key.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(json)).getOrThrow());
    }
}
