package io.github.naimjeg.combatnexus.combat.damage.core.rule;

import io.github.naimjeg.combatnexus.combat.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class DamageRuleStackingResolver {
    private static final Identifier FALLBACK_RULE_ID =
            DamageModuleIds.id("invalid_rule");

    private static final Identifier FALLBACK_STACKING_GROUP =
            DamageModuleIds.id("invalid_stacking_group");

    private DamageRuleStackingResolver() {
    }

    public static DamageRuleStackingResult resolve(List<RuntimeDamageRule> input) {
        if (input == null || input.isEmpty()) {
            return new DamageRuleStackingResult(List.of());
        }

        List<RuntimeDamageRule> sanitized = sanitizeInput(input);

        if (sanitized.isEmpty()) {
            return new DamageRuleStackingResult(List.of());
        }

        if (sanitized.size() == 1) {
            return new DamageRuleStackingResult(sanitized);
        }

        if (allStack(sanitized)) {
            return new DamageRuleStackingResult(sanitized);
        }

        List<RuntimeDamageRule> stacked = new ArrayList<>(sanitized.size());

        Map<StackingKey, RuntimeDamageRule> uniqueSource = null;
        Map<StackingKey, RuntimeDamageRule> highestValue = null;
        Map<StackingKey, RuntimeDamageRule> lowestValue = null;
        Map<StackingKey, RuntimeDamageRule> replace = null;

        for (RuntimeDamageRule runtimeRule : sanitized) {
            DamageRuleDefinition rule = runtimeRule.definition();

            DamageRuleStacking stacking = safeStacking(rule);

            switch (stacking) {
                case STACK -> stacked.add(runtimeRule);

                case UNIQUE_SOURCE -> {
                    if (uniqueSource == null) {
                        uniqueSource = new LinkedHashMap<>();
                    }

                    merge(
                            uniqueSource,
                            keyOf(runtimeRule, true),
                            runtimeRule,
                            DamageRuleStackingResolver::chooseHigherPriority
                    );
                }

                case HIGHEST_VALUE -> {
                    if (highestValue == null) {
                        highestValue = new LinkedHashMap<>();
                    }

                    merge(
                            highestValue,
                            keyOf(runtimeRule, false),
                            runtimeRule,
                            DamageRuleStackingResolver::chooseHigherValue
                    );
                }

                case LOWEST_VALUE -> {
                    if (lowestValue == null) {
                        lowestValue = new LinkedHashMap<>();
                    }

                    merge(
                            lowestValue,
                            keyOf(runtimeRule, false),
                            runtimeRule,
                            DamageRuleStackingResolver::chooseLowestValue
                    );
                }

                case REPLACE -> {
                    if (replace == null) {
                        replace = new LinkedHashMap<>();
                    }

                    merge(
                            replace,
                            keyOf(runtimeRule, false),
                            runtimeRule,
                            DamageRuleStackingResolver::chooseReplacement
                    );
                }
            }
        }

        if (uniqueSource != null) {
            stacked.addAll(uniqueSource.values());
        }

        if (highestValue != null) {
            stacked.addAll(highestValue.values());
        }

        if (lowestValue != null) {
            stacked.addAll(lowestValue.values());
        }

        if (replace != null) {
            stacked.addAll(replace.values());
        }

        return new DamageRuleStackingResult(
                List.copyOf(stacked)
        );
    }

    private static List<RuntimeDamageRule> sanitizeInput(
            List<RuntimeDamageRule> input
    ) {
        List<RuntimeDamageRule> result = new ArrayList<>(input.size());

        for (RuntimeDamageRule runtimeRule : input) {
            if (runtimeRule == null) {
                continue;
            }

            DamageRuleDefinition rule = runtimeRule.definition();

            if (rule == null) {
                continue;
            }

            if (rule.id() == null) {
                continue;
            }

            if (rule.phase() == null) {
                continue;
            }

            if (rule.role() == null) {
                continue;
            }

            if (rule.stacking() == null) {
                continue;
            }

            if (rule.stacking() != DamageRuleStacking.STACK
                    && rule.stackingKey() == null) {
                continue;
            }

            result.add(runtimeRule);
        }

        return result;
    }

    private static boolean allStack(List<RuntimeDamageRule> input) {
        for (RuntimeDamageRule runtimeRule : input) {
            if (safeStacking(runtimeRule.definition()) != DamageRuleStacking.STACK) {
                return false;
            }
        }

        return true;
    }

    private static void merge(Map<StackingKey, RuntimeDamageRule> map,
            StackingKey key, RuntimeDamageRule candidate, RuleChooser chooser) {
        RuntimeDamageRule existing = map.get(key);
        if (existing == null) {
            map.put(key, candidate);
            return;
        }
        RuntimeDamageRule chosen;
        try {
            chosen = chooser.choose(existing, candidate);
        } catch (Exception exception) {
            chosen = existing;
        }
        map.put(key, chosen);
    }

    private static RuntimeDamageRule chooseHigherPriority(
            RuntimeDamageRule existing,
            RuntimeDamageRule candidate
    ) {
        int existingPriority = safePriority(existing.definition());
        int candidatePriority = safePriority(candidate.definition());

        if (candidatePriority > existingPriority) {
            return candidate;
        }

        return existing;
    }

    private static RuntimeDamageRule chooseReplacement(
            RuntimeDamageRule existing,
            RuntimeDamageRule candidate
    ) {
        int existingPriority = safePriority(existing.definition());
        int candidatePriority = safePriority(candidate.definition());

        if (candidatePriority >= existingPriority) {
            return candidate;
        }

        return existing;
    }

    private static RuntimeDamageRule chooseHigherValue(
            RuntimeDamageRule existing,
            RuntimeDamageRule candidate
    ) {
        float existingValue = safeStackingValue(existing.definition());
        float candidateValue = safeStackingValue(candidate.definition());

        int valueCompare = Float.compare(candidateValue, existingValue);

        if (valueCompare > 0) {
            return candidate;
        }

        if (valueCompare == 0
                && safePriority(candidate.definition()) > safePriority(existing.definition())) {
            return candidate;
        }

        return existing;
    }

    private static RuntimeDamageRule chooseLowestValue(
            RuntimeDamageRule existing,
            RuntimeDamageRule candidate
    ) {
        float existingValue = safeStackingValue(existing.definition());
        float candidateValue = safeStackingValue(candidate.definition());

        int valueCompare = Float.compare(candidateValue, existingValue);

        if (valueCompare < 0) {
            return candidate;
        }

        if (valueCompare == 0
                && safePriority(candidate.definition()) > safePriority(existing.definition())) {
            return candidate;
        }

        return existing;
    }

    private static StackingKey keyOf(
            RuntimeDamageRule runtimeRule,
            boolean includeSource
    ) {
        DamageRuleDefinition rule = runtimeRule.definition();

        return new StackingKey(
                safeStackingKey(rule),
                safePhase(rule),
                safeRole(rule),
                includeSource
                        ? RuleSourceSignature.from(runtimeRule.executionContext())
                        : RuleSourceSignature.GROUP_ONLY
        );
    }

    private static float safeStackingValue(DamageRuleDefinition rule) {
        if (rule == null || rule.operations() == null || rule.operations().isEmpty()) {
            return 0.0f;
        }

        float result = 0.0f;
        boolean hasValue = false;

        for (DamageRuleOperation operation : rule.operations()) {
            if (operation == null) {
                continue;
            }

            float value;

            try {
                value = operation.stackingValue();
            } catch (Exception exception) {
                continue;
            }

            if (!Float.isFinite(value)) {
                continue;
            }

            if (!hasValue || value > result) {
                result = value;
                hasValue = true;
            }
        }

        return hasValue ? result : 0.0f;
    }

    private static int safePriority(DamageRuleDefinition rule) {
        return rule == null ? 0 : rule.priority();
    }

    private static Identifier safeStackingKey(DamageRuleDefinition rule) {
        if (rule == null) {
            return FALLBACK_STACKING_GROUP;
        }

        try {
            Identifier key = rule.stackingKey();
            return key != null ? key : FALLBACK_STACKING_GROUP;
        } catch (Exception exception) {
            return FALLBACK_STACKING_GROUP;
        }
    }

    private static DamagePhase safePhase(DamageRuleDefinition rule) {
        if (rule == null || rule.phase() == null) {
            return DamagePhase.BASE_MODIFICATION;
        }

        return rule.phase();
    }

    private static DamageRuleRole safeRole(DamageRuleDefinition rule) {
        if (rule == null || rule.role() == null) {
            return DamageRuleRole.ANY;
        }

        return rule.role();
    }

    private static DamageRuleStacking safeStacking(DamageRuleDefinition rule) {
        if (rule == null || rule.stacking() == null) {
            return DamageRuleStacking.STACK;
        }

        return rule.stacking();
    }

    @FunctionalInterface
    private interface RuleChooser {
        RuntimeDamageRule choose(RuntimeDamageRule existing, RuntimeDamageRule candidate);
    }

    private record RuleSourceSignature(
            DamageRuleProviderType providerType,
            RuleSourceLocation sourceLocation,
            DamageRuleRole runtimeRole,
            Identifier itemId,
            String equipmentSlot,
            int ownerId,
            int sourceEntityId,
            Identifier externalProviderId,
            Identifier externalSourceKey,
            Identifier slotSemantic
    ) {
        private static final Identifier EMPTY_ITEM_ID =
                DamageModuleIds.id("empty_item");

        private static final RuleSourceSignature GROUP_ONLY =
                new RuleSourceSignature(
                        null,
                        null,
                        null,
                        null,
                        null,
                        Integer.MIN_VALUE,
                        Integer.MIN_VALUE, null, null, null
                );

        private static RuleSourceSignature from(
                RuleExecutionContext executionContext
        ) {
            if (executionContext == null) {
                return GROUP_ONLY;
            }

            return new RuleSourceSignature(
                    executionContext.providerType(),
                    executionContext.sourceLocation(),
                    executionContext.role(),
                    itemId(executionContext.sourceStack()),
                    executionContext.equipmentSlot() == null
                            ? null
                            : executionContext.equipmentSlot().name(),
                    executionContext.owner() == null
                            ? Integer.MIN_VALUE
                            : executionContext.owner().getId(),
                    executionContext.sourceEntity() == null
                            ? Integer.MIN_VALUE
                            : executionContext.sourceEntity().getId(),
                    executionContext.externalProviderId(),
                    executionContext.externalSourceKey(),
                    executionContext.slotSemantic()
            );
        }

        private static Identifier itemId(ItemStack stack) {
            if (stack == null || stack.isEmpty()) {
                return EMPTY_ITEM_ID;
            }

            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());

            return id != null ? id : EMPTY_ITEM_ID;
        }
    }

    private record StackingKey(
            Identifier group,
            DamagePhase phase,
            DamageRuleRole role,
            RuleSourceSignature source
    ) {
    }
}
