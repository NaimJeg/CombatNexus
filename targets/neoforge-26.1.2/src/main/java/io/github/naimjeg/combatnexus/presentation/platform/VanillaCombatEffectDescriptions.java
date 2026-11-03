package io.github.naimjeg.combatnexus.presentation.platform;

import net.minecraft.network.chat.Component;
import io.github.naimjeg.combatnexus.combat.damage.api.display.DamageText;
import net.minecraft.resources.Identifier;
import java.util.List;

/** Level-aware descriptions of native combat effects. */
public final class VanillaCombatEffectDescriptions {
    private VanillaCombatEffectDescriptions() {}
    private static Component damage(int amount, String type) {
        return DamageText.amount(amount, DamageText.type(Identifier.parse(type)));
    }
    public static List<Component> describe(Identifier id, int amplifier) {
        var base = Component.translatable("tooltip.combatnexus.native_effect." + id.getNamespace() + "." + id.getPath());
        Component value = switch (id.getPath()) {
            case "poison" -> Component.translatable("tooltip.combatnexus.vanilla_combat.periodic_damage", damage(1, "neoforge:poison"), Math.max(1, 25 >> amplifier), 1);
            case "wither" -> Component.translatable("tooltip.combatnexus.vanilla_combat.periodic_damage", damage(1, "minecraft:wither"), Math.max(1, 40 >> amplifier), 0);
            case "regeneration" -> Component.translatable("tooltip.combatnexus.vanilla_combat.periodic_healing", 1, Math.max(1, 50 >> amplifier));
            case "instant_health" -> Component.translatable("tooltip.combatnexus.vanilla_combat.instant_health", 4 << amplifier, damage(6 << amplifier, "minecraft:magic"));
            case "instant_damage" -> Component.translatable("tooltip.combatnexus.vanilla_combat.instant_damage", damage(6 << amplifier, "minecraft:magic"), 4 << amplifier);
            default -> null;
        };
        return value == null ? List.of(base) : List.of(base, value);
    }
}
