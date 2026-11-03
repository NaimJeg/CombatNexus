package io.github.naimjeg.combatnexus.damage.registry;

import com.mojang.serialization.Codec;
import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.item.DamageModuleItemEntries;
import io.github.naimjeg.combatnexus.damage.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.damage.api.item.template.DamageItemTemplateReferences;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, DamageModule.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<DamageAffixDefinition>>> DAMAGE_AFFIXES =
            COMPONENTS.register("damage_affixes", () ->
                    DataComponentType.<List<DamageAffixDefinition>>builder()
                            .persistent(
                                    DamageModuleItemEntries.AFFIX_STORAGE_CODEC
                            )
                            .networkSynchronized(
                                    DamageModuleItemEntries.AFFIX_NETWORK_CODEC
                            )
                            .cacheEncoding()
                            .build()
            );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<List<DamageEntryDefinition>>
            > DAMAGE_ENTRIES =
            COMPONENTS.register(
                    "damage_entries",
                    () -> DataComponentType
                            .<List<DamageEntryDefinition>>builder()
                            .persistent(
                                    DamageModuleItemEntries.ENTRY_STORAGE_CODEC
                            )
                            .networkSynchronized(
                                    DamageModuleItemEntries.ENTRY_NETWORK_CODEC
                            )
                            .build()
            );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<DamageItemTemplateReferences>
            > DAMAGE_TEMPLATE_REFERENCES =
            COMPONENTS.register(
                    "damage_template_references",
                    () -> DataComponentType
                            .<DamageItemTemplateReferences>builder()
                            .persistent(DamageItemTemplateReferences.CODEC)
                            .networkSynchronized(
                                    DamageItemTemplateReferences.NETWORK_CODEC)
                            .cacheEncoding()
                            .build()
            );

    public static final DeferredHolder<
            DataComponentType<?>,
            DataComponentType<Boolean>
            > TEST_ITEM =
            COMPONENTS.register(
                    "test_item",
                    () -> DataComponentType
                            .<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .build()
            );

    private ModDataComponents() {
    }

    public static void register(IEventBus eventBus) {
        COMPONENTS.register(eventBus);
    }
}
