package io.github.naimjeg.combatnexus.combat.shield.api;

import com.google.gson.JsonObject;
import io.github.naimjeg.combatnexus.config.*;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Immutable pool policy. Type selectors and channel selectors must both match. */
public record ShieldDefinition(double capacity, int priority, Set<Identifier> damageTypes,
        Set<Identifier> damageTypeTags, Set<Identifier> channels, long recoveryDelayTicks, double recoveryPerSecond, Stacking stacking) {
    public ShieldDefinition(double capacity, int priority, Set<Identifier> damageTypes,
            Set<Identifier> damageTypeTags, long recoveryDelayTicks, double recoveryPerSecond, Stacking stacking) {
        this(capacity, priority, damageTypes, damageTypeTags, Set.of(), recoveryDelayTicks, recoveryPerSecond, stacking);
    }
    public enum Stacking { REPLACE_SAME_SOURCE, REFRESH_SAME_SOURCE }
    public ShieldDefinition {
        DefinitionJson.range(capacity, 0, 1000000, "capacity");
        DefinitionJson.range(priority, -10000, 10000, "priority");
        DefinitionJson.range(recoveryDelayTicks, 0, 1200000, "recovery_delay_ticks");
        DefinitionJson.range(recoveryPerSecond, 0, 1000000, "recovery_per_second");
        channels = Set.copyOf(channels);
        if (channels.size() > 32) throw new IllegalArgumentException("Shield channel budget exceeded");
        damageTypes = Set.copyOf(damageTypes); damageTypeTags = Set.copyOf(damageTypeTags); Objects.requireNonNull(stacking);
        if (damageTypes.size() > 32 || damageTypeTags.size() > 32) throw new IllegalArgumentException("Shield filter budget exceeded");
    }
    public boolean unrestricted() { return damageTypes.isEmpty() && damageTypeTags.isEmpty(); }
    public static ShieldDefinition decode(JsonObject json) {
        SettingsJson.fields(json, "capacity", "priority", "damage_types", "damage_type_tags", "channels", "recovery_delay_ticks", "recovery_per_second", "stacking");
        return new ShieldDefinition(DefinitionJson.number(json, "capacity", 0, 1000000), SettingsJson.integer(json, "priority"),
                DefinitionJson.ids(json, "damage_types"), DefinitionJson.ids(json, "damage_type_tags"), DefinitionJson.ids(json, "channels"), SettingsJson.integer(json, "recovery_delay_ticks"),
                DefinitionJson.number(json, "recovery_per_second", 0, 1000000), SettingsJson.enumeration(json, "stacking", Stacking.class));
    }
}
