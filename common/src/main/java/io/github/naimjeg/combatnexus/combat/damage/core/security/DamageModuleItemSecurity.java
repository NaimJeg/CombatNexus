package io.github.naimjeg.combatnexus.combat.damage.core.security;
import io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration;

import io.github.naimjeg.combatnexus.combat.damage.api.item.DamageModuleItemEntries;
import io.github.naimjeg.combatnexus.combat.damage.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleLimits;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleReferenceValidator;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleValidator;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixValidator;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryValidator;
import io.github.naimjeg.combatnexus.combat.damage.builtin.rule.operation.AddTrueDamageOperation;
import io.github.naimjeg.combatnexus.combat.damage.builtin.rule.operation.CancelDamageOperation;
import io.github.naimjeg.combatnexus.combat.damage.builtin.rule.operation.OverrideFinalDamageOperation;
import io.github.naimjeg.combatnexus.combat.damage.core.template.DamageTemplateSnapshot;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.naimjeg.combatnexus.combat.damage.api.item.DamageComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.ItemStack;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Server authority boundary for executable ItemStack rule components.
 *
 * <p>No data component is treated as a trust marker. Trust comes from the
 * server-side creation path or the submitting player's real permission set,
 * and every execution path still passes through structural validation.</p>
 */
public final class DamageModuleItemSecurity {
    private static final int EXECUTION_CACHE_CAPACITY = 256;
    private static final CacheEntry[] EXECUTION_CACHE =
            new CacheEntry[EXECUTION_CACHE_CAPACITY];
    private static int nextCacheIndex;

    private DamageModuleItemSecurity() {
    }

    public static void sanitizeCreativeInbound(
            ServerPlayer player,
            ItemStack submittedStack
    ) {
        if (player == null) {
            return;
        }

        if (player.level().getServer() == null
                || !player.level().getServer().isSameThread()) {
            throw new IllegalStateException(
                    "Creative ItemStack security must run on the "
                            + "Minecraft server thread"
            );
        }

        if (submittedStack == null
                || submittedStack.isEmpty()
                || !hasExecutableComponents(submittedStack)) {
            return;
        }

        DamageModuleItemEntries submitted = readRaw(submittedStack);
        DamageItemTemplateReferences references = readReferences(submittedStack);
        boolean administrator = player.permissions().hasPermission(
                Permissions.COMMANDS_GAMEMASTER
        );
        var definitions = io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(player.level().getServer());
        InboundDecision decision = DamageChannelRegistry.withCandidate(definitions.damage().channels(),
                () -> evaluateCreativeInbound(administrator, submitted, references, definitions));

        if (!stripRejectedComponents(submittedStack, decision)) {
            return;
        }

        DamageModuleItemEntries resolved = resolveDefinitions(
                submitted, references,
                definitions.damage().templates(),
                "creative_item_ingress");
        String reason = containsDangerousOperation(resolved)
                ? "dangerous_operation"
                : "executable_component";
    }

    public static InboundDecision evaluateCreativeInbound(
            boolean administrator,
            DamageModuleItemEntries submitted,
            DamageItemTemplateReferences references, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions
    ) {
        DamageItemTemplateReferences safeReferences = references == null
                ? DamageItemTemplateReferences.EMPTY
                : references;
        if ((submitted == null || submitted.isEmpty())
                && safeReferences.isEmpty()) {
            return InboundDecision.NO_EXECUTABLE_COMPONENTS;
        }

        if (!administrator) {
            return InboundDecision.STRIP_UNTRUSTED;
        }

        DamageModuleItemEntries safeSubmitted = submitted == null
                ? DamageModuleItemEntries.EMPTY
                : submitted;
        DamageModuleItemEntries resolved = resolveDefinitions(
                safeSubmitted,
                safeReferences,
                definitions.damage().templates(),
                "creative_item_ingress");
        ValidatedItemRules validated = validateExecutionDefinitions(
                resolved.entries(),
                resolved.affixes(),
                "creative_item_ingress",
                definitions.damage().channels().contentRevision()
        );

        if (!validated.authoritative()) {
            return InboundDecision.STRIP_INVALID;
        }

        if (!shapeOf(validated.entries(), validated.affixes()).equals(
                shapeOf(resolved.entries(), resolved.affixes())
        )) {
            return InboundDecision.STRIP_INVALID;
        }

        return InboundDecision.ALLOW;
    }

    public static ValidatedItemRules validateDefinitions(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            String source
    ) {
        List<DamageEntryDefinition> safeEntries =
                entries == null ? List.of() : entries;
        List<DamageAffixDefinition> safeAffixes =
                affixes == null ? List.of() : affixes;
        Optional<String> problem = DamageRuleLimits.findItemProblem(
                safeEntries,
                safeAffixes
        );

        if (problem.isPresent()) {
            return ValidatedItemRules.REJECTED;
        }

        List<DamageEntryDefinition> validEntries =
                DamageEntryValidator.filterValid(
                        safeEntries,
                        stableSource(source, "entries")
                );
        List<DamageAffixDefinition> validAffixes =
                DamageAffixValidator.filterValid(
                        safeAffixes,
                        stableSource(source, "affixes")
                );

        return new ValidatedItemRules(
                validEntries,
                validAffixes,
                true
        );
    }

    public static synchronized ValidatedItemRules validateForExecution(
            ItemStack stack, String source, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions) {
        return DamageChannelRegistry.withCandidate(definitions.damage().channels(),
                () -> validateCaptured(stack, source, definitions.damage().templates(), definitions.generation()));
    }
    private static ValidatedItemRules validateCaptured(
            ItemStack stack, String source, DamageTemplateSnapshot templateSnapshot, long generation) {
        if (stack == null || stack.isEmpty()) {
            return ValidatedItemRules.EMPTY;
        }

        List<DamageEntryDefinition> entries = stack.getOrDefault(
                DamageComponents.entries(),
                List.of()
        );
        List<DamageAffixDefinition> affixes = stack.getOrDefault(
                DamageComponents.affixes(),
                List.of()
        );
        DamageItemTemplateReferences references = stack.getOrDefault(
                DamageComponents.templates(),
                DamageItemTemplateReferences.EMPTY
        );
        DamageTemplateSnapshot safeSnapshot = Objects.requireNonNull(
                templateSnapshot, "templateSnapshot");
        long currentChannelRevision = DamageChannelRegistry.contentRevision();

        for (CacheEntry cached : EXECUTION_CACHE) {
            if (cached != null
                    && cached.matches(
                    stack, entries, affixes, references,
                    safeSnapshot.revision(),
                    safeSnapshot.validatedChannelRevision(),
                    safeSnapshot.serverAuthoritative(),
                    currentChannelRevision, generation)) {
                return cached.validated();
            }
        }

        DamageModuleItemEntries resolved = resolveDefinitions(
                new DamageModuleItemEntries(entries, affixes),
                references,
                safeSnapshot,
                source);
        ValidatedItemRules validated = validateExecutionDefinitions(
                resolved.entries(),
                resolved.affixes(),
                source,
                currentChannelRevision
        );
        EXECUTION_CACHE[nextCacheIndex] = new CacheEntry(
                new WeakReference<>(stack),
                entries,
                affixes,
                references,
                safeSnapshot.revision(),
                safeSnapshot.validatedChannelRevision(),
                safeSnapshot.serverAuthoritative(),
                currentChannelRevision, generation,
                validated
        );
        nextCacheIndex = (nextCacheIndex + 1)
                % EXECUTION_CACHE_CAPACITY;

        return validated;
    }

    static ValidatedItemRules validateExecutionDefinitions(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            String source,
            long currentChannelRevision
    ) {
        ValidatedItemRules structural = validateDefinitions(
                entries,
                affixes,
                source
        );
        if (!structural.authoritative()) {
            return ValidatedItemRules.REJECTED;
        }

        for (DamageEntryDefinition entry : structural.entries()) {
            if (!validateExecutionRules(
                    entry.rules(),
                    stableSource(source, "entry/" + entry.id()),
                    currentChannelRevision
            )) {
                return ValidatedItemRules.REJECTED;
            }
        }
        for (DamageAffixDefinition affix : structural.affixes()) {
            for (DamageEntryDefinition entry : affix.entries()) {
                if (!validateExecutionRules(
                        entry.rules(),
                        stableSource(
                                source,
                                "affix/" + affix.id() + "/entry/" + entry.id()
                        ),
                        currentChannelRevision
                )) {
                    return ValidatedItemRules.REJECTED;
                }
            }
        }
        return structural;
    }

    private static boolean validateExecutionRules(
            List<DamageRuleDefinition> rules,
            String source,
            long currentChannelRevision
    ) {
        for (DamageRuleDefinition rule : rules) {
            if (!DamageRuleReferenceValidator.validateDatapackReferences(
                    rule,
                    source + "/channel_revision/" + currentChannelRevision,
                    DamageRuleValidator.Policy.IGNORE
            )) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsDangerousOperation(
            DamageModuleItemEntries entries
    ) {
        if (entries == null) {
            return false;
        }

        for (DamageEntryDefinition entry : entries.entries()) {
            if (entry != null && containsDangerousOperation(entry.rules())) {
                return true;
            }
        }

        for (DamageAffixDefinition affix : entries.affixes()) {
            if (affix == null) {
                continue;
            }

            for (DamageEntryDefinition entry : affix.entries()) {
                if (entry != null
                        && containsDangerousOperation(entry.rules())) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void resetServerState() {
        clearCaches();
    }

    static synchronized void clearCaches() {
        java.util.Arrays.fill(EXECUTION_CACHE, null);
        nextCacheIndex = 0;
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static synchronized void invalidateExecutionCache() {
        clearCaches();
    }

    private static boolean containsDangerousOperation(
            List<DamageRuleDefinition> rules
    ) {
        for (DamageRuleDefinition rule : rules) {
            if (rule == null) {
                continue;
            }

            for (DamageRuleOperation operation : rule.operations()) {
                if (operation instanceof CancelDamageOperation
                        || operation
                        instanceof OverrideFinalDamageOperation
                        || operation instanceof AddTrueDamageOperation) {
                    return true;
                }
            }
        }

        return false;
    }

    private static DefinitionShape shapeOf(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes
    ) {
        int entryCount = entries == null ? 0 : entries.size();
        int affixCount = affixes == null ? 0 : affixes.size();
        int nestedEntryCount = 0;
        int ruleCount = 0;

        if (entries != null) {
            for (DamageEntryDefinition entry : entries) {
                if (entry != null) {
                    ruleCount += entry.rules().size();
                }
            }
        }

        if (affixes != null) {
            for (DamageAffixDefinition affix : affixes) {
                if (affix == null) {
                    continue;
                }

                nestedEntryCount += affix.entries().size();

                for (DamageEntryDefinition entry : affix.entries()) {
                    if (entry != null) {
                        ruleCount += entry.rules().size();
                    }
                }
            }
        }

        return new DefinitionShape(
                entryCount,
                affixCount,
                nestedEntryCount,
                ruleCount
        );
    }

    private static boolean hasExecutableComponents(ItemStack stack) {
        return stack.has(DamageComponents.entries())
                || stack.has(DamageComponents.affixes())
                || stack.has(DamageComponents.templates());
    }

    private static boolean stripRejectedComponents(
            ItemStack stack,
            InboundDecision decision
    ) {
        if (decision == InboundDecision.ALLOW
                || decision
                == InboundDecision.NO_EXECUTABLE_COMPONENTS) {
            return false;
        }

        stack.remove(DamageComponents.entries());
        stack.remove(DamageComponents.affixes());
        stack.remove(DamageComponents.templates());
        return true;
    }

    private static DamageModuleItemEntries readRaw(ItemStack stack) {
        return new DamageModuleItemEntries(
                stack.getOrDefault(
                        DamageComponents.entries(),
                        List.of()
                ),
                stack.getOrDefault(
                        DamageComponents.affixes(),
                        List.of()
                )
        );
    }

    private static DamageItemTemplateReferences readReferences(
            ItemStack stack
    ) {
        return stack.getOrDefault(
                DamageComponents.templates(),
                DamageItemTemplateReferences.EMPTY
        );
    }

    private static DamageModuleItemEntries resolveDefinitions(
            DamageModuleItemEntries materialized,
            DamageItemTemplateReferences references,
            DamageTemplateSnapshot snapshot,
            String source) {
        java.util.ArrayList<DamageEntryDefinition> entries =
                new java.util.ArrayList<>(materialized.entries());
        java.util.ArrayList<DamageAffixDefinition> affixes =
                new java.util.ArrayList<>(materialized.affixes());
        if (!references.isEmpty() && !snapshot.serverAuthoritative()) {
            return new DamageModuleItemEntries(entries, affixes);
        }
        for (var reference : references.entries()) {
            var resolved = snapshot.entry(reference.id());
            if (resolved.isPresent()) {
                entries.add(resolved.get());
            }
        }
        for (var reference : references.affixes()) {
            var resolved = snapshot.affix(reference.id());
            if (resolved.isPresent()) {
                affixes.add(resolved.get());
            }
        }
        return new DamageModuleItemEntries(entries, affixes);
    }

    private static String stableSource(
            String source,
            String category
    ) {
        if (source == null || source.isBlank()) {
            return "item_security/" + category;
        }

        int separator = source.indexOf('/');
        String stableCategory = separator < 0
                ? source
                : source.substring(0, separator);

        return "item_security/"
                + stableCategory
                + "/"
                + category;
    }

    public enum InboundDecision {
        NO_EXECUTABLE_COMPONENTS,
        ALLOW,
        STRIP_UNTRUSTED,
        STRIP_INVALID
    }

    public record ValidatedItemRules(
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            boolean authoritative
    ) {
        private static final ValidatedItemRules EMPTY =
                new ValidatedItemRules(List.of(), List.of(), true);
        private static final ValidatedItemRules REJECTED =
                new ValidatedItemRules(List.of(), List.of(), false);

        public ValidatedItemRules {
            entries = entries == null ? List.of() : List.copyOf(entries);
            affixes = affixes == null ? List.of() : List.copyOf(affixes);
        }

        public boolean isEmpty() {
            return entries.isEmpty() && affixes.isEmpty();
        }
    }

    private record CacheEntry(
            WeakReference<ItemStack> stack,
            List<DamageEntryDefinition> entries,
            List<DamageAffixDefinition> affixes,
            DamageItemTemplateReferences references,
            long templateRevision,
            long validatedChannelRevision,
            boolean serverAuthoritative,
            long currentChannelRevision,
            long definitionGeneration,
            ValidatedItemRules validated
    ) {
        boolean matches(
                ItemStack candidate,
                List<DamageEntryDefinition> candidateEntries,
                List<DamageAffixDefinition> candidateAffixes,
                DamageItemTemplateReferences candidateReferences,
                long candidateTemplateRevision,
                long candidateValidatedChannelRevision,
                boolean candidateServerAuthoritative,
                long candidateCurrentChannelRevision, long candidateGeneration
        ) {
            return definitionGeneration == candidateGeneration && stack.get() == candidate
                    && entries == candidateEntries
                    && affixes == candidateAffixes
                    && references == candidateReferences
                    && templateRevision == candidateTemplateRevision
                    && validatedChannelRevision
                    == candidateValidatedChannelRevision
                    && serverAuthoritative == candidateServerAuthoritative
                    && currentChannelRevision
                    == candidateCurrentChannelRevision;
        }
    }

    private record DefinitionShape(
            int entryCount,
            int affixCount,
            int nestedEntryCount,
            int ruleCount
    ) {
    }

}
