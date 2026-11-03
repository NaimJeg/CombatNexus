package io.github.naimjeg.combatnexus.combat.damage.core.template;

import com.mojang.serialization.JsonOps;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.core.lifecycle.DamageModuleReloadAccess;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Atomically reloads the two typed static-template datapack directories. */
public final class DatapackDamageTemplateReloadListener
        extends SimplePreparableReloadListener<
        DatapackDamageTemplateReloadListener.Prepared> {
    public static final String ENTRY_DIRECTORY =
            "combatnexus_entry_templates";
    public static final String AFFIX_DIRECTORY =
            "combatnexus_affix_templates";
    public static final int MAX_ENTRY_TEMPLATES =
            DamageTemplateLimits.MAX_ENTRY_TEMPLATES;
    public static final int MAX_AFFIX_TEMPLATES =
            DamageTemplateLimits.MAX_AFFIX_TEMPLATES;
    public static final int MAX_TEMPLATE_RULES =
            DamageTemplateLimits.MAX_TEMPLATE_RULES;
    public static final int MAX_TEMPLATE_CONDITION_NODES =
            DamageTemplateLimits.MAX_TEMPLATE_CONDITION_NODES;
    public static final int MAX_TEMPLATE_OPERATIONS =
            DamageTemplateLimits.MAX_TEMPLATE_OPERATIONS;
    private static final FileToIdConverter ENTRY_FILES =
            FileToIdConverter.json(ENTRY_DIRECTORY);
    private static final FileToIdConverter AFFIX_FILES =
            FileToIdConverter.json(AFFIX_DIRECTORY);

    public DatapackDamageTemplateReloadListener(
            DamageModuleReloadAccess access
    ) {
        Objects.requireNonNull(access, "access")
                .requireFrameworkOwner("DatapackDamageTemplateReloadListener");
    }

    @Override
    protected Prepared prepare(
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        int entryResources = ENTRY_FILES.listMatchingResources(manager).size();
        int affixResources = AFFIX_FILES.listMatchingResources(manager).size();
        if (entryResources > MAX_ENTRY_TEMPLATES
                || affixResources > MAX_AFFIX_TEMPLATES) {
            throw new IllegalArgumentException(
                    "template_resource_budget_exceeded category="
                            + (entryResources > MAX_ENTRY_TEMPLATES
                            ? "entry_templates" : "affix_templates")
                            + " actual=" + (entryResources
                            > MAX_ENTRY_TEMPLATES
                            ? entryResources : affixResources)
                            + " maximum=" + (entryResources
                            > MAX_ENTRY_TEMPLATES
                            ? MAX_ENTRY_TEMPLATES : MAX_AFFIX_TEMPLATES)
                            + " reason=predecode_resource_limit");
        }

        Map<Identifier, DamageEntryDefinition> entries =
                new LinkedHashMap<>();
        Map<Identifier, DamageAffixDefinition> affixes =
                new LinkedHashMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(
                manager,
                ENTRY_FILES,
                JsonOps.INSTANCE,
                DamageEntryDefinition.CODEC,
                entries
        );
        SimpleJsonResourceReloadListener.scanDirectory(
                manager,
                AFFIX_FILES,
                JsonOps.INSTANCE,
                DamageAffixDefinition.CODEC,
                affixes
        );
        return new Prepared(entries, affixes);
    }

    @Override
    protected void apply(
            Prepared prepared,
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        publishPrepared(prepared);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    public static boolean applyPreparedForTesting(
            Map<Identifier, DamageEntryDefinition> entries,
            Map<Identifier, DamageAffixDefinition> affixes
    ) {
        return publishPrepared(new Prepared(entries, affixes));
    }

    public static DamageTemplateSnapshot buildCandidate(
            Map<Identifier, DamageEntryDefinition> entries,
            Map<Identifier, DamageAffixDefinition> affixes) {
        Validated validated = validatePrepared(new Prepared(entries, affixes));
        var combinedEntries = new LinkedHashMap<>(DamageTemplateRegistry.javaEntries());
        combinedEntries.putAll(validated.entries());
        var combinedAffixes = new LinkedHashMap<>(DamageTemplateRegistry.javaAffixes());
        combinedAffixes.putAll(validated.affixes());
        return new DamageTemplateSnapshot(DamageChannelRegistry.contentRevision(),
                validated.channelRevision(), true, combinedEntries, combinedAffixes);
    }

    private static boolean publishPrepared(Prepared prepared) {
        try {
            Validated validated = validatePrepared(prepared);
            DamageTemplateRegistry.publishDatapack(
                    validated.entries(), validated.affixes(),
                    validated.channelRevision());
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static Validated validatePrepared(Prepared prepared) {
        if (prepared == null || prepared.entries() == null
                || prepared.affixes() == null) {
            throw new IllegalArgumentException("prepared_templates_are_null");
        }
        if (prepared.entries().size() > MAX_ENTRY_TEMPLATES) {
            throw new IllegalArgumentException(
                    "template_aggregate_budget_exceeded category=entry_templates"
                            + " actual=" + prepared.entries().size()
                            + " maximum=" + MAX_ENTRY_TEMPLATES
                            + " reason=aggregate_limit");
        }
        if (prepared.affixes().size() > MAX_AFFIX_TEMPLATES) {
            throw new IllegalArgumentException(
                    "template_aggregate_budget_exceeded category=affix_templates"
                            + " actual=" + prepared.affixes().size()
                            + " maximum=" + MAX_AFFIX_TEMPLATES
                            + " reason=aggregate_limit");
        }

        Map<Identifier, DamageEntryDefinition> entries =
                sorted(prepared.entries());
        Map<Identifier, DamageAffixDefinition> affixes =
                sorted(prepared.affixes());
        Map<Identifier, Identifier> entryDefinitionFiles = new HashMap<>();
        Map<Identifier, Identifier> affixDefinitionFiles = new HashMap<>();
        long channelRevision = DamageChannelRegistry.contentRevision();
        validateJavaTemplates();

        for (var item : entries.entrySet()) {
            Identifier fileId = Objects.requireNonNull(item.getKey(), "fileId");
            DamageEntryDefinition definition = Objects.requireNonNull(
                    item.getValue(), "entry definition from " + fileId);
            requireFileId(fileId, definition.id(), "entry");
            requireUnique(entryDefinitionFiles, definition.id(), fileId, "entry");
            if (DamageTemplateRegistry.javaEntries().containsKey(definition.id())) {
                throw new IllegalArgumentException(
                        "entry_java_datapack_conflict id=" + definition.id());
            }
            DamageTemplateDefinitionValidator.requireEntry(
                    definition, "datapack_template/entry/" + fileId, true);
        }

        for (var item : affixes.entrySet()) {
            Identifier fileId = Objects.requireNonNull(item.getKey(), "fileId");
            DamageAffixDefinition definition = Objects.requireNonNull(
                    item.getValue(), "affix definition from " + fileId);
            requireFileId(fileId, definition.id(), "affix");
            requireUnique(affixDefinitionFiles, definition.id(), fileId, "affix");
            if (DamageTemplateRegistry.javaAffixes().containsKey(definition.id())) {
                throw new IllegalArgumentException(
                        "affix_java_datapack_conflict id=" + definition.id());
            }
            DamageTemplateDefinitionValidator.requireAffix(
                    definition, "datapack_template/affix/" + fileId, true);
        }

        Map<Identifier, DamageEntryDefinition> combinedEntries =
                new LinkedHashMap<>(DamageTemplateRegistry.javaEntries());
        combinedEntries.putAll(entries);
        Map<Identifier, DamageAffixDefinition> combinedAffixes =
                new LinkedHashMap<>(DamageTemplateRegistry.javaAffixes());
        combinedAffixes.putAll(affixes);
        DamageTemplateLimits.requireWithinLimits(
                combinedEntries, combinedAffixes);
        return new Validated(entries, affixes, channelRevision);
    }

    private static void validateJavaTemplates() {
        for (DamageEntryDefinition definition
                : DamageTemplateRegistry.javaEntries().values()) {
            DamageTemplateDefinitionValidator.requireEntry(
                    definition, "java_template/reload/entry/" + definition.id(), true);
        }
        for (DamageAffixDefinition definition
                : DamageTemplateRegistry.javaAffixes().values()) {
            DamageTemplateDefinitionValidator.requireAffix(
                    definition, "java_template/reload/affix/" + definition.id(), true);
        }
    }

    private static void requireFileId(
            Identifier fileId,
            Identifier definitionId,
            String kind
    ) {
        if (!fileId.equals(definitionId)) {
            throw new IllegalArgumentException(
                    kind + "_template_id_mismatch file=" + fileId
                            + " definition=" + definitionId);
        }
    }

    private static void requireUnique(
            Map<Identifier, Identifier> firstFiles,
            Identifier definitionId,
            Identifier fileId,
            String kind
    ) {
        Identifier previous = firstFiles.putIfAbsent(definitionId, fileId);
        if (previous != null) {
            throw new IllegalArgumentException(
                    "duplicate_" + kind + "_template_definition id="
                            + definitionId + " first=" + previous
                            + " duplicate=" + fileId);
        }
    }

    private static <T> Map<Identifier, T> sorted(Map<Identifier, T> input) {
        List<Map.Entry<Identifier, T>> values =
                new ArrayList<>(input.entrySet());
        values.sort(Comparator.comparing(entry -> entry.getKey().toString()));
        LinkedHashMap<Identifier, T> result = new LinkedHashMap<>();
        for (var value : values) {
            result.put(value.getKey(), value.getValue());
        }
        return Map.copyOf(result);
    }

    public record Prepared(
            Map<Identifier, DamageEntryDefinition> entries,
            Map<Identifier, DamageAffixDefinition> affixes
    ) {}

    private record Validated(
            Map<Identifier, DamageEntryDefinition> entries,
            Map<Identifier, DamageAffixDefinition> affixes,
            long channelRevision
    ) {}
}
