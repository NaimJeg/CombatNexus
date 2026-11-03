package io.github.naimjeg.combatnexus.combat.internal.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.combat.damage.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.combatnexus.combat.config.CombatServerSettings;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.DatapackDamageRuleReloadListener;
import io.github.naimjeg.combatnexus.combat.damage.core.template.DatapackDamageTemplateReloadListener;
import io.github.naimjeg.combatnexus.config.SettingsJson;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Strict decode and candidate compilation; apply does not publish executable state. */
public final class CombatDefinitionReloadListener
        extends SimplePreparableReloadListener<CombatDefinitionReloadListener.Prepared> {
    public record Prepared(CombatServerSettings settings,
            Map<Identifier, DamageChannelRegistry.ChannelDefinition> channels,
            Map<Identifier, DamageRuleDefinition> rules,
            Map<Identifier, DamageEntryDefinition> entries,
            Map<Identifier, DamageAffixDefinition> affixes, Map<Identifier, io.github.naimjeg.combatnexus.combat.api.behavior.BehaviorDefinition> behaviors,
            io.github.naimjeg.combatnexus.combat.api.DomainDefinitions domains) {}
    private io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration candidate;
    @Override protected Prepared prepare(ResourceManager manager, ProfilerFiller profiler) {
        var behaviors = io.github.naimjeg.combatnexus.combat.internal.coordination.BehaviorDefinitionResources.read(manager);
        return new Prepared(CombatServerSettings.decode(settings(manager, "formulas"), settings(manager, "damage")),
                read(manager, "combatnexus_channels", DamageChannelRegistry.ChannelDefinition.CODEC, DamageChannelRegistry.MAX_CUSTOM_CHANNELS),
                read(manager, "combatnexus_rules", DamageRuleDefinition.CODEC, DatapackDamageRuleReloadListener.MAX_DATAPACK_RULES),
                read(manager, DatapackDamageTemplateReloadListener.ENTRY_DIRECTORY, DamageEntryDefinition.CODEC, DatapackDamageTemplateReloadListener.MAX_ENTRY_TEMPLATES),
                read(manager, DatapackDamageTemplateReloadListener.AFFIX_DIRECTORY, DamageAffixDefinition.CODEC, DatapackDamageTemplateReloadListener.MAX_AFFIX_TEMPLATES),
                behaviors, DomainDefinitionResources.read(manager, behaviors));
    }
    private static com.google.gson.JsonObject settings(ResourceManager manager, String name) {
        var id = Identifier.fromNamespaceAndPath("combatnexus", "settings/" + name + ".json");
        try (var reader = manager.getResourceOrThrow(id).openAsReader()) {
            return SettingsJson.read(reader);
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Cannot read " + id, error); }
    }
    private static <T> Map<Identifier, T> read(ResourceManager manager, String directory, Codec<T> codec, int limit) {
        var converter = FileToIdConverter.json(directory);
        var resources = converter.listMatchingResources(manager);
        if (resources.size() > limit) throw new IllegalArgumentException("Resource budget exceeded: " + directory);
        Map<Identifier, T> result = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (var reader = entry.getValue().openAsReader()) {
                var id = converter.fileToId(entry.getKey());
                if (id.toString().length() > 256) throw new IllegalArgumentException("Definition ID too long");
                var json = SettingsJson.read(reader);
                io.github.naimjeg.combatnexus.combat.damage.core.definition.DamageDefinitionFields.validate(directory, json);
                T value = codec.parse(JsonOps.INSTANCE, json).getOrThrow();
                if (result.putIfAbsent(id, value) != null) throw new IllegalArgumentException("Duplicate definition: " + id);
            } catch (java.io.IOException | RuntimeException error) {
                throw new IllegalArgumentException("Invalid combat definition: " + entry.getKey(), error);
            }
        });
        return Map.copyOf(result);
    }
    @Override protected void apply(Prepared prepared, ResourceManager manager, ProfilerFiller profiler) {
        var channels = DamageChannelRegistry.buildCandidate(prepared.channels(), CombatDefinitions.nextGeneration());
        candidate = DamageChannelRegistry.withCandidate(channels, () -> {
            prepared.domains().shields().values().forEach(shield -> shield.channels().forEach(id -> {
                if (!DamageChannelRegistry.containsChannel(id)) throw new IllegalArgumentException("Unknown shield channel: " + id);
            }));
            var rules = DatapackDamageRuleReloadListener.buildCandidate(prepared.rules());
            var templates = DatapackDamageTemplateReloadListener.buildCandidate(prepared.entries(), prepared.affixes());
            return new io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration(DamageChannelRegistry.contentRevision(),
                    prepared.settings(), new io.github.naimjeg.combatnexus.combat.damage.core.definition.DamageDefinitions(channels, rules, templates), prepared.behaviors(), prepared.domains());
        });
    }
    public io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration candidate() { return Objects.requireNonNull(candidate, "Definitions have not compiled"); }
}
