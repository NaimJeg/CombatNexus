package io.github.naimjeg.combatnexus.combat.healing.api;
import com.google.gson.JsonObject;
import io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame;
import io.github.naimjeg.combatnexus.config.*;
import net.minecraft.resources.Identifier;
import java.util.*;
/** Frozen-fact predicates and bounded arithmetic only. No resource writes or child operations. */
public record HealingRuleDefinition(int priority, HealingPhase phase, Set<HealingRequest.Reason> reasons,
        Set<Identifier> sources, double minimumHealthRatio, double maximumHealthRatio, double additive, double multiplier) {
    public HealingRuleDefinition {
        DefinitionJson.range(priority, -10000, 10000, "priority");
        if (phase != HealingPhase.CONTRIBUTIONS && phase != HealingPhase.LIMITS) throw new IllegalArgumentException("Healing rules require CONTRIBUTIONS or LIMITS phase");
        reasons = Set.copyOf(reasons); sources = Set.copyOf(sources);
        if (sources.size() > 32) throw new IllegalArgumentException("Source selector capacity exceeded");
        DefinitionJson.range(minimumHealthRatio, 0, 1, "minimum_health_ratio");
        DefinitionJson.range(maximumHealthRatio, minimumHealthRatio, 1, "maximum_health_ratio");
        DefinitionJson.range(additive, -1000000, 1000000, "additive"); DefinitionJson.range(multiplier, 0, 10, "multiplier");
    }
    public boolean matches(HealingRequest request, CombatInputFrame input) {
        double ratio = input.facts().maxHealth() <= 0 ? 0 : input.facts().health() / input.facts().maxHealth();
        return (reasons.isEmpty() || reasons.contains(request.reason())) && (sources.isEmpty() || sources.contains(request.source().id()))
                && ratio >= minimumHealthRatio && ratio <= maximumHealthRatio;
    }
    public static HealingRuleDefinition decode(JsonObject json) {
        SettingsJson.fields(json, "priority", "phase", "reasons", "sources", "minimum_health_ratio", "maximum_health_ratio", "additive", "multiplier");
        var reasons = EnumSet.noneOf(HealingRequest.Reason.class);
        for (var value : DefinitionJson.array(json, "reasons", HealingRequest.Reason.values().length)) {
            if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || !reasons.add(HealingRequest.Reason.valueOf(value.getAsString())))
                throw new IllegalArgumentException("Invalid/duplicate healing reason");
        }
        return new HealingRuleDefinition(SettingsJson.integer(json, "priority"), SettingsJson.enumeration(json, "phase", HealingPhase.class),
                reasons, DefinitionJson.ids(json, "sources"), DefinitionJson.number(json, "minimum_health_ratio", 0, 1),
                DefinitionJson.number(json, "maximum_health_ratio", 0, 1), DefinitionJson.number(json, "additive", -1000000, 1000000), DefinitionJson.number(json, "multiplier", 0, 10));
    }
}
