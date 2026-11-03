package io.github.naimjeg.combatnexus.presentation.api;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.mojang.datafixers.util.Pair;
import java.util.stream.Stream;

/** Enforces presentation work budgets while codecs create nodes, not after a full JSON tree exists. */
public final class BudgetJsonOps extends JsonOps {
    private final PresentationBudget budget;
    public BudgetJsonOps(PresentationBudget budget) { super(false); this.budget = budget; }
    @Override public JsonElement createString(String value) {
        budget.entry(1); budget.encoded(value); return super.createString(value);
    }
    @Override public JsonElement createNumeric(Number value) {
        budget.entry(1); budget.encoded(value.toString()); return super.createNumeric(value);
    }
    @Override public JsonElement createBoolean(boolean value) {
        budget.entry(1); return super.createBoolean(value);
    }
    @Override public JsonElement createList(Stream<JsonElement> input) {
        var array = new JsonArray(); input.forEach(value -> { budget.entry(1); array.add(value); }); return array;
    }
    @Override public JsonElement createMap(Stream<Pair<JsonElement, JsonElement>> input) {
        var object = new JsonObject();
        input.forEach(pair -> { budget.entry(1); object.add(pair.getFirst().getAsString(), pair.getSecond()); });
        return object;
    }
}
