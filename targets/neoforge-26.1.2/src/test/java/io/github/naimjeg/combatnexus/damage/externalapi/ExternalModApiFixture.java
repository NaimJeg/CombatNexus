package io.github.naimjeg.combatnexus.damage.externalapi;

import io.github.naimjeg.combatnexus.damage.api.DamageModuleAttributes;
import io.github.naimjeg.combatnexus.damage.api.DamageModulePreMultiplierBuckets;
import io.github.naimjeg.combatnexus.damage.api.critical.CriticalDecision;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageMetadataKey;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageRequest;
import io.github.naimjeg.combatnexus.damage.api.damage.DamageSourceDescriptor;
import io.github.naimjeg.combatnexus.damage.api.enums.DamageApplicationBucket;
import io.github.naimjeg.combatnexus.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.damage.api.event.DamageModuleRegistrar;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditionIds;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleConditions;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleOperationIds;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleOperations;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.damage.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.DamageEntryDefinition;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.DamageEntryDisplay;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.DamageEntrySlot;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.DamageEntryStacking;
import io.github.naimjeg.combatnexus.damage.api.rule.affix.DamageAffixDefinition;
import io.github.naimjeg.combatnexus.damage.api.rule.affix.DamageAffixDisplay;
import io.github.naimjeg.combatnexus.damage.api.rule.affix.DamageAffixSlot;
import io.github.naimjeg.combatnexus.damage.api.rule.affix.DamageAffixRarity;
import io.github.naimjeg.combatnexus.damage.api.rule.affix.DamageAffixStacking;
import io.github.naimjeg.combatnexus.damage.api.item.template.DamageEntryTemplateReference;
import io.github.naimjeg.combatnexus.damage.api.item.template.DamageAffixTemplateReference;
import io.github.naimjeg.combatnexus.damage.api.item.template.DamageModuleTemplates;
import io.github.naimjeg.combatnexus.damage.api.item.template.DamageItemTemplateReferences;
import io.github.naimjeg.combatnexus.damage.api.item.DamageModuleItemApi;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/** Compilation-only fixture representing a third-party mod using API imports. */
public final class ExternalModApiFixture {
    private static final Identifier ID =
            Identifier.fromNamespaceAndPath("contentmod", "example");

    private ExternalModApiFixture() {}

    public static int frozenRuntimeBucketIndex() {
        return DamageModulePreMultiplierBuckets.runtimeIndex(
                DamageModulePreMultiplierBuckets.GENERIC_DAMAGE);
    }

    /** Stores a payload-free ID; the server decides readiness and execution. */
    public static boolean attachTemplateReference(ItemStack stack) {
        DamageModuleTemplates.serverExecutionReady();
        return DamageModuleItemApi.addEntryTemplateReference(
                stack, new DamageEntryTemplateReference(ID));
    }

    public static DamageRequest exercise(
            DamageModuleRegistrar registrar,
            ServerLevel level,
            LivingEntity target,
            ResourceKey<DamageType> damageType
    ) {
        registrar.registerCondition(ID,
                DamageRuleCondition.CODEC.fieldOf("delegate"));
        registrar.registerOperation(ID,
                DamageRuleOperation.CODEC.fieldOf("delegate"));
        registrar.registerPreMultiplierBucket(ID);
        registrar.registerAttributionResolver(ID, 0, query -> Optional.empty());
        registrar.registerEquippedItemRuleSource(ID, 0, query -> List.of());
        registrar.registerCriticalDecisionProvider(ID, 0,
                (context, collector) -> collector.contribute(
                        CriticalDecision.FORCE_CRITICAL));
        registrar.registerSettlementListener(ID, 0, callback ->
                callback.childAuthority());

        var templateRule = DamageRuleBuilder.offensive(ID)
                .operation(DamageModuleOperations.addBaseDamage(
                        DamageChannel.UNTYPED_ID, 1.0f))
                .build();
        var entryTemplate = new DamageEntryDefinition(
                ID, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(templateRule), DamageEntryStacking.STACK,
                Optional.empty());
        var affixId = Identifier.fromNamespaceAndPath(
                "contentmod", "example_affix");
        var affixTemplate = new DamageAffixDefinition(
                affixId, DamageAffixDisplay.EMPTY, DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON, List.of(entryTemplate),
                DamageAffixStacking.STACK, Optional.empty());
        registrar.registerEntryTemplate(ID, entryTemplate);
        registrar.registerAffixTemplate(affixId, affixTemplate);
        new DamageItemTemplateReferences(
                List.of(new DamageEntryTemplateReference(ID)),
                List.of(new DamageAffixTemplateReference(affixId)));
        DamageModuleTemplates.entry(ID);
        DamageModuleTemplates.affix(affixId);

        DamageRuleBuilder.offensive(ID)
                .when(DamageModuleConditions.sourceTag(ID))
                .operation(DamageModuleOperations.addGlobalPreMultiplier(ID, 0.25f))
                .build();
        DamageModuleConditionIds.ALWAYS.toString();
        DamageModuleOperationIds.ADD_BASE_DAMAGE.toString();
        DamageModulePreMultiplierBuckets.GENERIC_DAMAGE.toString();
        DamageModuleAttributes.CRIT_CHANCE.identifier();
        DamageChannel.FIRE_ID.toString();
        DamageApplicationBucket.DN_RULE_BASE.serializedName();

        return DamageRequest.builder(level, target,
                        DamageSourceDescriptor.of(damageType), 1.0f)
                .actionId(ID)
                .sourceTag(ID)
                .metadata(DamageMetadataKey.stringKey(ID), "value")
                .build();
    }
}
