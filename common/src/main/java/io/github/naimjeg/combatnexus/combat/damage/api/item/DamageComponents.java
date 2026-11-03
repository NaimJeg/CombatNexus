package io.github.naimjeg.combatnexus.combat.damage.api.item;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.item.template.DamageItemTemplateReferences;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Typed lookups after registration. Does not expose platform registry holders. */
public final class DamageComponents {
    private DamageComponents() {}
    public static DataComponentType<List<DamageEntryDefinition>> entries() { return lookup("damage_entries"); }
    public static DataComponentType<List<DamageAffixDefinition>> affixes() { return lookup("damage_affixes"); }
    public static DataComponentType<DamageItemTemplateReferences> templates() { return lookup("damage_template_references"); }
    public static DataComponentType<Boolean> testItem() { return lookup("test_item"); }
    @SuppressWarnings("unchecked")
    private static <T> DataComponentType<T> lookup(String path) {
        return (DataComponentType<T>) BuiltInRegistries.DATA_COMPONENT_TYPE.get(Identifier.fromNamespaceAndPath("combatnexus", path)).orElseThrow().value();
    }
}
