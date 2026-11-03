package io.github.naimjeg.combatnexus.combat.state.internal;
import java.util.*;
import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.state.api.*;
/** Entity-owned values. Native entity lifecycle invokes the declared boundaries. */
public final class RuntimeStateStore {
    private record Key(Identifier type,Optional<UUID> scope) {}
    private final Map<Key,String> values=new LinkedHashMap<>();
    public <T> Optional<T> read(StateAddress<T> address) {
        RuntimeStateTypes.require(address.key());
        try(var read=io.github.naimjeg.combatnexus.combat.api.transaction.CombatReadScope.open()) {
            return Optional.ofNullable(values.get(new Key(address.key().id(),address.scope()))).map(json->address.key().codec().parse(JsonOps.INSTANCE,JsonParser.parseString(json)).getOrThrow());
        }
    }
    public <T> T write(StateAddress<T> address,T value) {
        RuntimeStateTypes.require(address.key());var key=new Key(address.key().id(),address.scope());
        if(!values.containsKey(key)&&values.size()>=64)throw new IllegalStateException("Runtime state capacity");
        String encoded;T detached;
        try(var read=io.github.naimjeg.combatnexus.combat.api.transaction.CombatReadScope.open()) {
            encoded=address.key().codec().encodeStart(JsonOps.INSTANCE,value).getOrThrow().toString();
            if(encoded.length()>16384)throw new IllegalArgumentException("Runtime state value capacity");
            detached=address.key().codec().parse(JsonOps.INSTANCE,JsonParser.parseString(encoded)).getOrThrow();
        }
        values.put(key,encoded);return detached;
    }
    public boolean remove(StateAddress<?> address) { RuntimeStateTypes.require(address.key());return values.remove(new Key(address.key().id(),address.scope()))!=null; }
    public void clear(StateKey.Boundary boundary,Optional<UUID> scope) {
        values.keySet().removeIf(k->RuntimeStateTypes.get(k.type()).clearOn().contains(boundary)&&(scope.isEmpty()||k.scope().equals(scope)));
    }
    public String encode(boolean persistentOnly) {
        var json=new JsonArray();values.forEach((key,value)->{
            if(persistentOnly&&!RuntimeStateTypes.get(key.type()).persistent())return;
            var entry=new JsonObject();entry.addProperty("type",key.type().toString());key.scope().ifPresent(id->entry.addProperty("scope",id.toString()));entry.addProperty("value",value);json.add(entry);
        });return json.toString();
    }
    public void restore(String encoded) {
        if(encoded.length()>1048576)throw new IllegalArgumentException("Runtime state save capacity");
        var restored=new LinkedHashMap<Key,String>();
        for(var element:JsonParser.parseString(encoded).getAsJsonArray()) {
            var json=element.getAsJsonObject();var id=Identifier.parse(json.get("type").getAsString());var type=RuntimeStateTypes.get(id);
            var scope=json.has("scope")?Optional.of(UUID.fromString(json.get("scope").getAsString())):Optional.<UUID>empty();
            if((type.scope()==StateKey.Scope.ENTITY)!=scope.isEmpty())throw new IllegalArgumentException("Restored state scope mismatch");
            String value=json.get("value").getAsString();
            try(var read=io.github.naimjeg.combatnexus.combat.api.transaction.CombatReadScope.open()) { type.codec().parse(JsonOps.INSTANCE,JsonParser.parseString(value)).getOrThrow(); }
            if(value.length()>16384||restored.putIfAbsent(new Key(id,scope),value)!=null||restored.size()>64)throw new IllegalArgumentException("Restored runtime state capacity/duplicate");
        }
        values.clear();values.putAll(restored);
    }
}
