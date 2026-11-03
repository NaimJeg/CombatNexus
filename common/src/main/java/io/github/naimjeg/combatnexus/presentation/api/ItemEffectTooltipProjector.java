package io.github.naimjeg.combatnexus.presentation.api;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import java.util.function.BiFunction;
import io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.*;
import java.util.*;

/** Reads adopted definitions only; never executes effects, predicates or random rolls. */
public final class ItemEffectTooltipProjector {
    private ItemEffectTooltipProjector() {}
    public static List<TooltipBlock> project(HolderLookup.Provider registries, BiFunction<Identifier, EquipmentBindings.Kind, Optional<TooltipBlock>> definitions, ItemStack stack, boolean managedProtection, double ratingPerScore) {
        var result = new ArrayList<TooltipBlock>();
        var budget = new PresentationBudget(4096, 24000);
        var ops = RegistryOps.create(new BudgetJsonOps(budget), registries);
        for (var binding : stack.getOrDefault(EquipmentBindings.component(), EquipmentBindings.EMPTY).bindings()) {
            var block = definitions.apply(binding.definition(), binding.kind());
            var lines = new ArrayList<Component>();
            lines.add(text("equipped", join(binding.slots().stream().map(slot -> text("slot." + slot.getName())).toList(), ", ")));
            if (block.isPresent()) {
                lines.addAll(block.get().summary());
                var details = new ArrayList<>(block.get().details());
                details.add(text("equipment_lifetime"));
                result.add(new TooltipBlock(block.get().title(), lines, details));
            } else {
                throw new IllegalArgumentException("Missing equipment description: " + binding.definition());
            }
        }
        var enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            budget.entry(1);
            var enchantment = entry.getKey().value();
            int enchantmentLevel = entry.getIntValue();
            var encoded = Enchantment.DIRECT_CODEC.encodeStart(ops, enchantment).getOrThrow().getAsJsonObject();
            var lines = new ArrayList<Component>();
            var details = new ArrayList<Component>();
            var effects = encoded.getAsJsonObject("effects");
            if (effects == null) continue;
            for (var field : effects.entrySet()) {
                String kind = switch (field.getKey()) {
                    case "minecraft:damage" -> "damage";
                    case "minecraft:damage_protection" -> "protection";
                    case "minecraft:armor_effectiveness" -> "armor";
                    case "minecraft:smash_damage_per_fallen_block" -> "smash";
                    case "minecraft:knockback" -> "knockback";
                    default -> null;
                };
                // Vanilla already presents attribute components. Unsupported effects retain their native tooltip.
                if (field.getKey().equals("minecraft:attributes")) continue;
                if (kind == null) continue;
                if (!field.getValue().isJsonArray()) throw new IllegalArgumentException("Invalid enchantment effects");
                for (var raw : field.getValue().getAsJsonArray()) {
                    budget.entry(1);
                    var effect = raw.getAsJsonObject();
                    var action = valueEffect(effect.getAsJsonObject("effect"), kind, enchantmentLevel, 0);
                    var operation = effect.getAsJsonObject("effect");
                    if (kind.equals("protection") && managedProtection && operation.get("type").getAsString().equals("minecraft:add")) {
                        double score = LevelBasedValue.CODEC.parse(JsonOps.INSTANCE, operation.get("value")).getOrThrow().calculate(enchantmentLevel);
                        action = action.copy().append(" ").append(text("converted_resistance", TooltipNumbers.number(score * ratingPerScore)));
                    }
                    lines.add(effect.has("requirements") ? text("conditional", condition(effect.get("requirements"), 0), action) : action);
                }
                if (kind.equals("protection")) details.add(text(managedProtection ? "protection_managed" : "protection_native"));
                if (kind.equals("armor")) details.add(text("armor_detail"));
            }
            if (lines.isEmpty()) continue;
            if (encoded.has("slots")) lines.addFirst(text("equipped", join(encoded.getAsJsonArray("slots").asList().stream()
                    .map(value -> text("slot." + value.getAsString())).toList(), ", ")));
            result.add(new TooltipBlock(Enchantment.getFullname(entry.getKey(), enchantmentLevel), lines, details));
        }
        result.addAll(io.github.naimjeg.combatnexus.presentation.internal.ItemComponentDescriptions.project(stack));
        return List.copyOf(result);
    }
    private static Component valueEffect(JsonObject effect, String kind, int level, int depth) {
        if (depth > 16 || effect == null || !effect.has("type")) throw new IllegalArgumentException("Unsupported enchantment value effect");
        String type = effect.get("type").getAsString();
        if (type.equals("minecraft:all_of")) return join(effect.getAsJsonArray("effects").asList().stream()
                .map(child -> valueEffect(child.getAsJsonObject(), kind, level, depth + 1)).toList(), "; ");
        String operation = switch (type) {
            case "minecraft:add" -> "add";
            case "minecraft:multiply" -> "multiply";
            case "minecraft:set" -> "set";
            default -> null;
        };
        String field = type.equals("minecraft:multiply") ? "factor" : "value";
        if (operation == null || !effect.has(field)) throw new IllegalArgumentException("Unsupported enchantment value effect");
        float value = LevelBasedValue.CODEC.parse(JsonOps.INSTANCE, effect.get(field)).getOrThrow().calculate(level);
        if (!Float.isFinite(value)) throw new IllegalArgumentException("Non-finite enchantment value");
        return text("value." + operation, text("quantity." + kind), TooltipNumbers.number(value));
    }
    private static Component condition(JsonElement raw, int depth) {
        if (depth > 16 || !raw.isJsonObject()) throw new IllegalArgumentException("Unsupported enchantment condition");
        var object = raw.getAsJsonObject();
        if (!object.has("condition")) throw new IllegalArgumentException("Unsupported enchantment condition");
        return switch (object.get("condition").getAsString()) {
            case "minecraft:all_of", "minecraft:any_of" -> text(object.get("condition").getAsString().endsWith("all_of") ? "all" : "any",
                    join(object.getAsJsonArray("terms").asList().stream().map(term -> condition(term, depth + 1)).toList(), "; "));
            case "minecraft:inverted" -> text("not", condition(object.get("term"), depth + 1));
            case "minecraft:entity_properties" -> {
                var predicate = object.getAsJsonObject("predicate");
                if (predicate == null || predicate.size() != 1 || !predicate.has("type") || !predicate.get("type").isJsonPrimitive()) throw new IllegalArgumentException("Unsupported enchantment condition");
                yield text("entity_condition", text("entity." + object.get("entity").getAsString()), identifier(predicate.get("type").getAsString(), "entity"));
            }
            case "minecraft:damage_source_properties" -> {
                var predicate = object.getAsJsonObject("predicate");
                if (predicate == null) throw new IllegalArgumentException("Unsupported enchantment condition");
                var parts = new ArrayList<Component>();
                for (var field : predicate.entrySet()) {
                    if (field.getKey().equals("tags")) for (var tag : field.getValue().getAsJsonArray()) {
                        var value = tag.getAsJsonObject();
                        parts.add(text(value.get("expected").getAsBoolean() ? "damage_tag" : "not_damage_tag", identifier("#" + value.get("id").getAsString(), "damage_type")));
                    } else if (field.getKey().equals("is_direct")) parts.add(text(field.getValue().getAsBoolean() ? "direct" : "indirect"));
                    else throw new IllegalArgumentException("Unsupported damage source predicate: " + field.getKey());
                }
                if (parts.isEmpty()) throw new IllegalArgumentException("Empty damage source predicate");
                yield text("all", join(parts, "; "));
            }
            default -> throw new IllegalArgumentException("Unsupported enchantment condition");
        };
    }
    private static Component identifier(String value, String kind) {
        boolean tag = value.startsWith("#");
        String id = tag ? value.substring(1) : value;
        return Component.translatableWithFallback((tag ? "tag." + (kind.equals("entity") ? "entity_type" : kind) : kind) + "." + id.replace(':', '.').replace('/', '.'), value);
    }
    private static Component join(List<Component> values, String separator) {
        var result = Component.empty();
        for (int i = 0; i < values.size(); i++) { if (i > 0) result.append(separator); result.append(values.get(i)); }
        return result;
    }
    private static Component text(String key, Object... args) { return Component.translatable("tooltip.combatnexus.item_effect." + key, args); }
}
