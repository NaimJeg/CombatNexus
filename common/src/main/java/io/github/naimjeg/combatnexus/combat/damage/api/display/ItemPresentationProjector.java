package io.github.naimjeg.combatnexus.combat.damage.api.display;
import io.github.naimjeg.combatnexus.presentation.api.PresentationBudget;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.combatnexus.combat.damage.api.item.ItemCombatPresentationSnapshot;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixDefinition;

/** Streaming-by-entry construction; refuses before accumulating an unbounded presentation tree. */
public final class ItemPresentationProjector {
    private ItemPresentationProjector() {}
    public static JsonObject project(ItemCombatPresentationSnapshot snapshot) {
        var budget = new PresentationBudget(4096, 24000);
        var ops = new io.github.naimjeg.combatnexus.presentation.api.BudgetJsonOps(budget);
        var documents = new JsonArray();
        for (var document : DamageDescriptions.project(snapshot.entries(), snapshot.affixes())) {
            budget.entry(1);
            documents.add(io.github.naimjeg.combatnexus.presentation.api.DescriptionDocument.CODEC.encodeStart(ops, document).getOrThrow());
        }
        var result = new JsonObject(); result.add("documents", documents); return result;
    }
}
