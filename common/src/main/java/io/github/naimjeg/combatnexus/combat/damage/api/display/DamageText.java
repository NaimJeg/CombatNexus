package io.github.naimjeg.combatnexus.combat.damage.api.display;

import io.github.naimjeg.combatnexus.presentation.api.TooltipNumbers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Composable names and quantities. Native types and damage channels remain distinct identities. */
public final class DamageText {
    private DamageText() {}

    public static Component type(Identifier id) {
        return named("damage_type.", id);
    }

    public static Component channel(Identifier id) {
        return named("damage_channel.", id);
    }

    public static Component kind(Component name) {
        return Component.translatable("tooltip.combatnexus.damage.kind", name);
    }

    public static Component amount(double amount, Component name) {
        return Component.translatable("tooltip.combatnexus.damage.amount", TooltipNumbers.number(amount), kind(name));
    }

    private static Component named(String prefix, Identifier id) {
        return Component.translatableWithFallback(prefix + id.getNamespace() + "." + id.getPath().replace('/', '.'), id.toString());
    }
}
