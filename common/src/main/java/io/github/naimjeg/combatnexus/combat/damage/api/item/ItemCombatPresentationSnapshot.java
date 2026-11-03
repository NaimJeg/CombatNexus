package io.github.naimjeg.combatnexus.combat.damage.api.item;

import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixDefinition;
import java.util.List;

/** Resolved server display projection; not an execution capability or an item mutation. */
public record ItemCombatPresentationSnapshot(long generation, List<DamageEntryDefinition> entries,
                                             List<DamageAffixDefinition> affixes) {
    public ItemCombatPresentationSnapshot { entries = List.copyOf(entries); affixes = List.copyOf(affixes); }
}
