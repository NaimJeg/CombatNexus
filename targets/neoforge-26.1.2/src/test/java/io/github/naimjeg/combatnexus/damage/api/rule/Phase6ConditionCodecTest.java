package io.github.naimjeg.combatnexus.damage.api.rule;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.damage.*;
import io.github.naimjeg.combatnexus.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.damage.api.item.DamageModuleItemEntries;
import io.github.naimjeg.combatnexus.damage.api.rule.affix.*;
import io.github.naimjeg.combatnexus.damage.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.*;
import io.github.naimjeg.combatnexus.damage.builtin.rule.condition.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class Phase6ConditionCodecTest {

    private static final Identifier ACTION = id("contentmod", "example_action");
    private static final Identifier SOURCE_TAG = id("contentmod", "example_damage");

    @Test
    void effectTagConditionsRoundTripAndKeepExternalNamespace() {
        TagKey<MobEffect> tag = TagKey.create(
                Registries.MOB_EFFECT,
                id("contentmod", "example_effects")
        );
        DamageRuleCondition attacker =
                DamageModuleConditions.attackerEffectTag(tag);
        DamageRuleCondition target =
                DamageModuleConditions.targetEffectTag(tag);

        assertEquals(attacker, roundTrip(DamageRuleCondition.CODEC, attacker));
        assertEquals(target, roundTrip(DamageRuleCondition.CODEC, target));
        assertEquals(
                id("contentmod", "example_effects"),
                ((AttackerEffectTagCondition) attacker).tag().location()
        );
        assertFalse(attacker.test(context(
                DamageRequestKind.ENVIRONMENTAL,
                DamageLineage.newRoot(),
                Optional.empty(),
                Set.of(),
                DamageTriggerPolicy.ALL_ALLOWED
        )));
        assertThrows(
                NullPointerException.class,
                () -> new AttackerEffectTagCondition(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> DamageModuleConditions.targetEffectTag(null)
        );
    }

    @Test
    void sourceActionAndTagMatchOnlyFinalOriginMetadata() {
        DamageRuleContext matching = context(
                DamageRequestKind.PRIMARY,
                DamageLineage.newRoot(),
                Optional.of(ACTION),
                Set.of(SOURCE_TAG),
                DamageTriggerPolicy.ALL_ALLOWED
        );
        DamageRuleContext absent = context(
                DamageRequestKind.PRIMARY,
                DamageLineage.newRoot(),
                Optional.empty(),
                Set.of(),
                DamageTriggerPolicy.ALL_ALLOWED
        );

        DamageRuleCondition action = DamageModuleConditions.sourceActionIs(ACTION);
        DamageRuleCondition tag = DamageModuleConditions.sourceTag(SOURCE_TAG);
        assertTrue(action.test(matching));
        assertFalse(action.test(absent));
        assertFalse(DamageModuleConditions.sourceActionIs(
                id("othermod", "action")
        ).test(matching));
        assertTrue(tag.test(matching));
        assertFalse(tag.test(absent));
        assertFalse(DamageModuleConditions.sourceTag(
                id("othermod", "tag")
        ).test(matching));
        assertEquals(action, roundTrip(DamageRuleCondition.CODEC, action));
        assertEquals(tag, roundTrip(DamageRuleCondition.CODEC, tag));
    }

    @ParameterizedTest
    @EnumSource(DamageRequestKind.class)
    void requestKindCodecAndConditionRoundTripEveryStableName(
            DamageRequestKind kind
    ) {
        assertEquals(
                kind,
                DamageRequestKind.CODEC.parse(
                        JsonOps.INSTANCE,
                        new com.google.gson.JsonPrimitive(kind.serializedName())
                ).getOrThrow()
        );
        DamageRuleCondition condition = DamageModuleConditions.requestKindIs(kind);
        assertEquals(condition, roundTrip(DamageRuleCondition.CODEC, condition));
        assertTrue(condition.test(context(
                kind,
                DamageLineage.newRoot(),
                Optional.empty(),
                Set.of(),
                DamageTriggerPolicy.defaultsFor(kind)
        )));
    }

    @Test
    void unknownRequestKindAndMalformedRequiredFieldsFailClearly() {
        assertTrue(DamageRequestKind.CODEC.parse(
                JsonOps.INSTANCE,
                new com.google.gson.JsonPrimitive("future_kind")
        ).error().orElseThrow().message().contains("future_kind"));

        JsonObject missing = new JsonObject();
        missing.addProperty("type", "combatnexus:request_kind_is");
        assertTrue(DamageRuleCondition.CODEC.parse(
                JsonOps.INSTANCE, missing
        ).error().isPresent());

        JsonObject wrongType = new JsonObject();
        wrongType.addProperty("type", "combatnexus:source_action_is");
        wrongType.addProperty("action", 42);
        assertTrue(DamageRuleCondition.CODEC.parse(
                JsonOps.INSTANCE, wrongType
        ).error().isPresent());
    }

    @Test
    void lineageAndPolicyLeavesHaveIndependentPreciseSemantics() {
        DamageLineage root = DamageLineage.newRoot();
        DamageRuleContext primary = context(
                DamageRequestKind.PRIMARY, root, Optional.empty(), Set.of(),
                DamageTriggerPolicy.ALL_ALLOWED
        );
        DamageRuleContext proc = context(
                DamageRequestKind.PROC, root.newChild(), Optional.empty(), Set.of(),
                DamageTriggerPolicy.PROC_SUPPRESSED
        );
        DamageRuleContext nonProcSuppressed = context(
                DamageRequestKind.CUSTOM, root.newChild(), Optional.empty(), Set.of(),
                DamageTriggerPolicy.PROC_SUPPRESSED
        );

        assertTrue(DamageModuleConditions.isPrimaryDamage().test(primary));
        assertFalse(DamageModuleConditions.isPrimaryDamage().test(proc));
        assertTrue(DamageModuleConditions.isProcDamage().test(proc));
        assertFalse(DamageModuleConditions.hasParentDamage().test(primary));
        assertTrue(DamageModuleConditions.hasParentDamage().test(proc));
        assertTrue(DamageModuleConditions.procAllowed().test(primary));
        assertFalse(DamageModuleConditions.procAllowed().test(proc));
        assertFalse(DamageModuleConditions.isProcDamage().test(nonProcSuppressed));
        assertFalse(DamageModuleConditions.procAllowed().test(nonProcSuppressed));
    }

    @Test
    void emptyLeafCodecsRetainTheirOwnTypes() {
        List<DamageRuleCondition> leaves = List.of(
                DamageModuleConditions.isPrimaryDamage(),
                DamageModuleConditions.isProcDamage(),
                DamageModuleConditions.hasParentDamage(),
                DamageModuleConditions.procAllowed()
        );
        for (DamageRuleCondition leaf : leaves) {
            DamageRuleCondition decoded = roundTrip(
                    DamageRuleCondition.CODEC,
                    leaf
            );
            assertEquals(leaf.type(), decoded.type());
            assertEquals(leaf.getClass(), decoded.getClass());
        }
    }

    @Test
    void newLeavesRoundTripInsideAllAnyAndNot() {
        DamageRuleCondition nested = DamageModuleConditions.allOf(
                DamageModuleConditions.sourceActionIs(ACTION),
                DamageModuleConditions.anyOf(
                        DamageModuleConditions.sourceTag(SOURCE_TAG),
                        DamageModuleConditions.not(
                                DamageModuleConditions.isProcDamage()
                        )
                ),
                DamageModuleConditions.procAllowed()
        );
        assertEquals(nested, roundTrip(DamageRuleCondition.CODEC, nested));
        assertEquals(
                7,
                DamageRuleLimits.measureRuleCost(rule(nested))
                        .orElseThrow().conditionNodes()
        );
    }

    @Test
    void entryAffixStorageAndActualNetworkCodecsCarryNewConditions() {
        DamageRuleCondition nested = DamageModuleConditions.allOf(
                DamageModuleConditions.sourceActionIs(ACTION),
                DamageModuleConditions.sourceTag(SOURCE_TAG),
                DamageModuleConditions.requestKindIs(DamageRequestKind.PROC),
                DamageModuleConditions.hasParentDamage()
        );
        DamageEntryDefinition entry = entry(nested);
        DamageAffixDefinition affix = affix(entry);

        assertEquals(entry, roundTrip(DamageEntryDefinition.CODEC, entry));
        assertEquals(entry, roundTrip(DamageEntryDefinition.STORAGE_CODEC, entry));
        assertEquals(affix, roundTrip(DamageAffixDefinition.CODEC, affix));
        assertEquals(affix, roundTrip(DamageAffixDefinition.STORAGE_CODEC, affix));
        assertEquals(
                List.of(entry),
                networkRoundTrip(
                        DamageModuleItemEntries.ENTRY_NETWORK_CODEC,
                        List.of(entry)
                )
        );
        assertEquals(
                List.of(affix),
                networkRoundTrip(
                        DamageModuleItemEntries.AFFIX_NETWORK_CODEC,
                        List.of(affix)
                )
        );
    }

    @Test
    void builderShortcutsDelegateToCanonicalPhase6ConditionsAndRoundTrip() {
        TagKey<MobEffect> effectTag = TagKey.create(
                Registries.MOB_EFFECT, id("contentmod", "builder_effects"));
        List<DamageRuleDefinition> rules = List.of(
                builder("attacker_tag").attackerEffectTag(effectTag).build(),
                builder("target_tag").targetEffectTag(effectTag).build(),
                builder("action").sourceActionIs(ACTION).build(),
                builder("source_tag").sourceTag(SOURCE_TAG).build(),
                builder("kind").requestKindIs(DamageRequestKind.PROC).build(),
                builder("primary").isPrimaryDamage().build(),
                builder("proc").isProcDamage().build(),
                builder("parent").hasParentDamage().build(),
                builder("proc_allowed").procAllowed().build()
        );
        List<Class<?>> expected = List.of(
                AttackerEffectTagCondition.class,
                TargetEffectTagCondition.class,
                SourceActionIsCondition.class,
                SourceTagCondition.class,
                RequestKindIsCondition.class,
                IsPrimaryDamageCondition.class,
                IsProcDamageCondition.class,
                HasParentDamageCondition.class,
                ProcAllowedCondition.class
        );
        for (int index = 0; index < rules.size(); index++) {
            DamageRuleCondition condition = rules.get(index).conditions().getFirst();
            assertEquals(expected.get(index), condition.getClass());
            assertEquals(condition, roundTrip(DamageRuleCondition.CODEC, condition));
        }
        assertThrows(NullPointerException.class,
                () -> builder("null_tag").attackerEffectTag(null));
    }

    private static DamageRuleBuilder builder(String path) {
        return DamageRuleBuilder.offensive(id("test", path))
                .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f);
    }

    private static DamageRuleDefinition rule(DamageRuleCondition condition) {
        return DamageRuleBuilder.offensive(id("test", "phase6_rule"))
                .when(condition)
                .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f)
                .build();
    }

    private static DamageEntryDefinition entry(DamageRuleCondition condition) {
        return new DamageEntryDefinition(
                id("test", "phase6_entry"),
                DamageEntryDisplay.EMPTY,
                DamageEntrySlot.ITEM,
                List.of(rule(condition)),
                DamageEntryStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageAffixDefinition affix(DamageEntryDefinition entry) {
        return new DamageAffixDefinition(
                id("test", "phase6_affix"),
                DamageAffixDisplay.EMPTY,
                DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(entry),
                DamageAffixStacking.STACK,
                Optional.empty()
        );
    }

    private static DamageRuleContext context(
            DamageRequestKind kind,
            DamageLineage lineage,
            Optional<Identifier> action,
            Set<Identifier> tags,
            DamageTriggerPolicy policy
    ) {
        DamageOrigin origin = new DamageOrigin(
                lineage,
                kind,
                DamageAttribution.ENVIRONMENT,
                DamageSourceDescriptor.of(DamageTypes.GENERIC),
                1.0f,
                action,
                tags,
                policy,
                DamageMetadata.empty()
        );
        return (DamageRuleContext) Proxy.newProxyInstance(
                DamageRuleContext.class.getClassLoader(),
                new Class<?>[]{DamageRuleContext.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "origin" -> origin;
                    case "requestKind" -> kind;
                    case "lineage" -> lineage;
                    case "actionId" -> action;
                    case "sourceTags" -> tags;
                    case "logicalAttacker", "attacker" -> null;
                    default -> defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        throw new IllegalStateException("Unsupported primitive: " + type);
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    private static <T> T roundTrip(Codec<T> codec, T value) {
        return codec.parse(
                JsonOps.INSTANCE,
                codec.encodeStart(JsonOps.INSTANCE, value).getOrThrow()
        ).getOrThrow();
    }

    private static <T> T networkRoundTrip(
            StreamCodec<ByteBuf, T> codec,
            T value
    ) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            codec.encode(buffer, value);
            return codec.decode(buffer);
        } finally {
            buffer.release();
        }
    }
}
