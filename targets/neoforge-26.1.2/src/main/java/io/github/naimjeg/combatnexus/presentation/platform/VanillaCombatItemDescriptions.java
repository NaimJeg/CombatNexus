package io.github.naimjeg.combatnexus.presentation.platform;

import io.github.naimjeg.combatnexus.combat.platform.api.CombatRegisterEvent;
import io.github.naimjeg.combatnexus.presentation.api.TooltipBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Version-specific combat explanations; generic document rendering contains no enchantment IDs. */
@EventBusSubscriber(modid = "combatnexus")
public final class VanillaCombatItemDescriptions {
    private VanillaCombatItemDescriptions() {}
    private static final Set<String> ENCHANTMENTS = Set.of("fire_aspect", "flame", "thorns", "bane_of_arthropods",
            "channeling", "wind_burst", "lunge", "riptide", "loyalty", "multishot", "piercing", "quick_charge",
            "infinity", "frost_walker", "soul_speed", "unbreaking");
    @SubscribeEvent public static void register(CombatRegisterEvent event) {
        event.registrar().itemDescriptor(Identifier.parse("combatnexus:vanilla_combat_enchantments"), DataComponents.ENCHANTMENTS, (stack, enchantments) -> {
            var result = new ArrayList<TooltipBlock>();
            for (var entry : enchantments.entrySet()) {
                var id = entry.getKey().unwrapKey().map(key -> key.identifier()).orElse(null);
                if (id == null || !id.getNamespace().equals("minecraft") || !ENCHANTMENTS.contains(id.getPath())) continue;
                if (id.getPath().equals("unbreaking") && !stack.has(DataComponents.WEAPON)
                        && !stack.has(DataComponents.EQUIPPABLE) && !stack.has(DataComponents.BLOCKS_ATTACKS)
                        && !stack.is(Items.BOW) && !stack.is(Items.CROSSBOW) && !stack.is(Items.TRIDENT)) continue;
                result.add(block(Enchantment.getFullname(entry.getKey(), entry.getIntValue()), id.getPath()));
            }
            return result;
        });
        event.registrar().itemDescriptor(Identifier.parse("combatnexus:wolf_armor"), DataComponents.EQUIPPABLE, (stack, equippable) ->
            stack.is(Items.WOLF_ARMOR) ? List.of(block(stack.getHoverName(), "wolf_armor")) : List.of());
        event.registrar().itemDescriptor(Identifier.parse("combatnexus:turtle_helmet"), DataComponents.EQUIPPABLE, (stack, equippable) ->
            stack.is(Items.TURTLE_HELMET) ? List.of(block(stack.getHoverName(), "turtle_helmet")) : List.of());
        event.registrar().itemDescriptor(Identifier.parse("combatnexus:item_blocking"), DataComponents.BLOCKS_ATTACKS, (stack, blocking) ->
            List.of(block(stack.getHoverName(), "blocking")));
        event.registrar().itemDescriptor(Identifier.parse("combatnexus:death_protection"), DataComponents.DEATH_PROTECTION, (stack, protection) ->
            List.of(block(stack.getHoverName(), "death_protection")));
    }
    private static TooltipBlock block(Component title, String key) {
        return new TooltipBlock(title, List.of(Component.translatable("tooltip.combatnexus.vanilla_combat." + key)), List.of());
    }
}
