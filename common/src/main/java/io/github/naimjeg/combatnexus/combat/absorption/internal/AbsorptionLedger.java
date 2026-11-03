package io.github.naimjeg.combatnexus.combat.absorption.internal;

import java.util.*;
import com.google.gson.*;
import net.minecraft.resources.Identifier;
import io.github.naimjeg.combatnexus.combat.absorption.api.*;
import io.github.naimjeg.combatnexus.combat.api.CombatSource;

/** Entity-owned native resource ledger. Native ticking and ordinary absorption remain vanilla-owned. */
public final class AbsorptionLedger {
    private record Key(Identifier id, Optional<UUID> owner) {}
    private final Map<Key, AbsorptionSource> sources = new LinkedHashMap<>();
    private static Key key(CombatSource source) { return new Key(source.id(), source.owner()); }
    public List<AbsorptionSource> snapshot() { return List.copyOf(sources.values()); }
    public double balance() { return sources.values().stream().mapToDouble(AbsorptionSource::balance).sum(); }
    public AbsorptionSource change(AbsorptionCommand command, double current) {
        var key = key(command.source());
        var old = sources.getOrDefault(key, new AbsorptionSource(command.source(), command.capacity(), 0, 0, 0));
        if (!sources.containsKey(key) && sources.size() >= 64) throw new IllegalStateException("Absorption source capacity exceeded");
        double limit = command.operation() == AbsorptionCommand.Operation.REMOVE ? 0 : command.capacity();
        double next = Math.min(old.balance(), limit);
        double added = command.operation() == AbsorptionCommand.Operation.GRANT
                ? Math.min(command.amount(), Math.max(0, Math.min(limit - next, limit - current))) : 0;
        var updated = new AbsorptionSource(command.source(), limit, next + added, old.granted() + added,
                old.consumed() + old.balance() - next);
        sources.put(key, updated);
        return updated;
    }
    /** Native absorption is consumed before source-owned absorption; owned sources use stable installation order. */
    public void reconcile(double actual) {
        double loss = Math.max(0, balance() - Math.max(0, actual));
        for (var entry : sources.entrySet()) {
            if (loss <= 0) break;
            var source = entry.getValue(); double taken = Math.min(loss, source.balance()); loss -= taken;
            entry.setValue(new AbsorptionSource(source.source(), source.capacity(), source.balance() - taken, source.granted(), source.consumed() + taken));
        }
    }
    public String encode() {
        var json = new JsonArray();
        for (var source : sources.values()) {
            var value = new JsonObject(); value.addProperty("id", source.source().id().toString());
            source.source().owner().ifPresent(owner -> value.addProperty("owner", owner.toString()));
            value.addProperty("capacity", source.capacity()); value.addProperty("balance", source.balance());
            value.addProperty("granted", source.granted()); value.addProperty("consumed", source.consumed()); json.add(value);
        }
        return json.toString();
    }
    public void restore(String encoded) {
        var restored = new LinkedHashMap<Key, AbsorptionSource>();
        var array = JsonParser.parseString(encoded).getAsJsonArray();
        if (array.size() > 64) throw new IllegalArgumentException("Absorption source capacity exceeded");
        for (var element : array) {
            var value = element.getAsJsonObject();
            if(value.has("owner"))io.github.naimjeg.combatnexus.config.SettingsJson.fields(value, "id", "owner", "capacity", "balance", "granted", "consumed");
            else io.github.naimjeg.combatnexus.config.SettingsJson.fields(value,"id","capacity","balance","granted","consumed");
            var source = new CombatSource(Identifier.parse(value.get("id").getAsString()),
                    value.has("owner") ? Optional.of(UUID.fromString(value.get("owner").getAsString())) : Optional.empty(), Optional.empty());
            double capacity = number(value, "capacity"), balance = number(value, "balance"), granted = number(value, "granted"), consumed = number(value, "consumed");
            if (balance > capacity || restored.putIfAbsent(key(source), new AbsorptionSource(source, capacity, balance, granted, consumed)) != null)
                throw new IllegalArgumentException("Invalid absorption source state");
        }
        sources.clear(); sources.putAll(restored);
    }
    private static double number(JsonObject value, String key) {
        double number = value.get(key).getAsDouble();
        if (!Double.isFinite(number) || number < 0) throw new IllegalArgumentException("Invalid absorption " + key);
        return number;
    }
}
