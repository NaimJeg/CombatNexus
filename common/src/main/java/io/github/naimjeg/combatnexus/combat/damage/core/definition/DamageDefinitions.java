package io.github.naimjeg.combatnexus.combat.damage.core.definition;
import io.github.naimjeg.combatnexus.combat.damage.core.registry.DamageChannelRegistry;
import io.github.naimjeg.combatnexus.combat.damage.core.rule.DatapackDamageRuleStore;
import io.github.naimjeg.combatnexus.combat.damage.core.template.DamageTemplateSnapshot;
/** Immutable damage-domain catalogs in a complete combat generation. */
public record DamageDefinitions(DamageChannelRegistry.RegistryState channels,
        DatapackDamageRuleStore.Snapshot rules, DamageTemplateSnapshot templates) {
    public DamageDefinitions { java.util.Objects.requireNonNull(channels); java.util.Objects.requireNonNull(rules); java.util.Objects.requireNonNull(templates); }
}
