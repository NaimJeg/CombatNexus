package io.github.naimjeg.combatnexus.presentation.api;

import com.google.gson.*;
import io.github.naimjeg.combatnexus.attribute.api.AttributeUnits;
import io.github.naimjeg.combatnexus.attribute.api.AttributeCatalog;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;

/** Common projection of an already frozen authoritative panel. */
public final class CombatPanelProjector {
    private CombatPanelProjector() {}
    public static JsonObject refused(String reason) {
        var object = new JsonObject(); object.addProperty("status", "refused"); object.addProperty("reason", reason);
        var rows = new JsonArray(); var row = new JsonObject(); row.addProperty("message", "gui.combatnexus.panel.refused");
        rows.add(row); object.add("rows", rows); return object;
    }
    public static JsonObject project(CombatPanelSnapshot value, long now) {
        var json = new JsonObject();
        var budget = new PresentationBudget(256, 24000);
            var rows = new JsonArray();
            var health = new JsonObject(); health.addProperty("health", value.health());
            health.addProperty("absorption", value.vanillaAbsorption()); add(budget, rows, health);
            value.attributes().values().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                var attribute = entry.getValue();
                var row = new JsonObject();
                row.addProperty("attribute", entry.getKey().toString());
                row.addProperty("name", BuiltInRegistries.ATTRIBUTE.get(entry.getKey()).orElseThrow().value().getDescriptionId());
                row.addProperty("value", attribute.effective()); row.addProperty("base", attribute.base());
                var captured=value.attributes().valueFrame().values().get(entry.getKey());
                if(captured.raw()!=captured.effective()){row.addProperty("raw",captured.raw());row.addProperty("effective_reason",captured.effectiveReason().toString());}
                row.addProperty("unit", AttributeUnits.unit(entry.getKey()).map(Enum::name).orElse(""));
                // Only our catalog can assert whether a framework consumer exists.
                AttributeCatalog.definition(entry.getKey()).ifPresent(definition ->
                        row.addProperty("reserved", definition.reserved()));
                add(budget, rows, row);
                for (var source : attribute.contributions()) add(budget, rows, "  " + source.source() + " : " + source.amount() + " (" + source.operation() + ")");
            });
            for (var shield : value.shields()) add(budget, rows, "Shield " + shield.source().id() + ": " + shield.balance() + " / " + shield.definition().capacity());
            for (var effect : value.effects()) {
                add(budget, rows, "Effect " + effect.definition() + " [" + effect.source().map(Object::toString).orElse("unknown") + "] level " + effect.level()
                        + " " + effect.ownership() + " " + effect.tags() + " dispellable=" + effect.operations().contains(io.github.naimjeg.combatnexus.effect.api.EffectView.Operation.DISPEL)
                        + (effect.remainingTicks().isPresent() ? " ticks " + effect.remainingTicks().getAsLong() : ""));
            }
            for (var behavior : value.behaviors()) {
                var source = behavior.source(); var plan = source.plan();
                add(budget, rows, "Behavior " + source.effect() + " [" + source.source() + "] protection=" + behavior.protectionAvailable()
                    + " types=" + plan.components().stream().map(c -> c.type().toString()).toList());
            }
            for (var reaction : value.reactions()) add(budget, rows, "Reaction " + reaction.reaction() + " #" + reaction.operation()
                    + " " + reaction.source().map(Object::toString).orElse("builtin") + " : " + reaction.outcome() + reaction.childRequest().map(child -> " -> " + child).orElse(""));
            json.add("rows", rows);
            for(var source:value.sourceDescriptions()) {
                var row=new JsonObject();row.addProperty("source",source.source().toString());
                row.add("display",DisplayText.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,source.description()).getOrThrow());add(budget,rows,row);
            }
        return json;
    }
    private static void add(PresentationBudget budget, JsonArray rows, JsonElement row) {
        budget.entry(1);
        budget.encoded(row.toString());
        rows.add(row);
    }
    private static void add(PresentationBudget budget, JsonArray rows, String row) {
        if (row.length() > 4096) throw new PresentationBudget.Exceeded();
        add(budget, rows, new JsonPrimitive(row));
    }
}
