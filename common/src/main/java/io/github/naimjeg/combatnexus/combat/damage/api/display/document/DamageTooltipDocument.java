package io.github.naimjeg.combatnexus.combat.damage.api.display.document;

import java.util.List;

public record DamageTooltipDocument(
        List<AffixTooltipView> affixes,
        List<EntryTooltipView> standaloneEntries,
        List<VanillaTooltipAugmentation> vanillaAugmentations,
        List<TemplateReferenceTooltipView> templateReferences
) {
    public DamageTooltipDocument {
        affixes = affixes == null ? List.of() : List.copyOf(affixes);
        standaloneEntries = standaloneEntries == null ? List.of() : List.copyOf(standaloneEntries);
        vanillaAugmentations = vanillaAugmentations == null ? List.of() : List.copyOf(vanillaAugmentations);
        templateReferences = templateReferences == null ? List.of() : List.copyOf(templateReferences);
    }

    public boolean isEmpty() {
        return affixes.isEmpty()
                && standaloneEntries.isEmpty()
                && vanillaAugmentations.isEmpty()
                && templateReferences.isEmpty();
    }
}
