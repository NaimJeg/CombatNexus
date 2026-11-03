package io.github.naimjeg.combatnexus.damage.core.lifecycle;

import io.github.naimjeg.combatnexus.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.event.DamageModuleRegistrar;
import io.github.naimjeg.combatnexus.damage.api.item.template.DamageModuleTemplates;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageModuleOperations;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleDefinition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleRole;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleStacking;
import io.github.naimjeg.combatnexus.damage.api.rule.affix.*;
import io.github.naimjeg.combatnexus.damage.api.rule.builder.DamageRuleBuilder;
import io.github.naimjeg.combatnexus.damage.api.rule.entry.*;
import io.github.naimjeg.combatnexus.damage.core.template.DamageTemplateRegistry;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Phase10TemplateRegistrationTest {
    private DamageModuleRegistrationAccess access;

    @AfterEach
    void reset() {
        if (access != null) access.close();
        DamageModuleLifecycle.resetForTesting();
    }

    @Test
    void registersTypedExternalTemplatesAndFreezesImmutableLookup() {
        DamageModuleRegistrationSession session = session();
        DamageEntryDefinition z = entry("zmod", "entry");
        DamageEntryDefinition a = entry("amod", "entry");
        DamageAffixDefinition affix = affix("contentmod", "affix");
        session.registerEntryTemplate(z.id(), z);
        session.registerEntryTemplate(a.id(), a);
        session.registerAffixTemplate(affix.id(), affix);
        DamageTemplateRegistry.freeze(access);

        assertEquals(Optional.of(a), DamageModuleTemplates.entry(a.id()));
        assertEquals(Optional.of(affix), DamageModuleTemplates.affix(affix.id()));
        assertTrue(DamageModuleTemplates.entry(affix.id()).isEmpty());
        assertTrue(DamageModuleTemplates.affix(a.id()).isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> DamageTemplateRegistry.snapshot().entries()
                        .put(id("test", "x"), a));
        assertThrows(IllegalStateException.class,
                () -> session.registerEntryTemplate(
                        id("contentmod", "late"),
                        entry("contentmod", "late")));
    }

    @Test
    void mismatchDuplicatesAndExpiredRegistrarAreRejected() {
        DamageModuleRegistrationSession session = session();
        DamageEntryDefinition entry = entry("contentmod", "entry");
        session.registerEntryTemplate(entry.id(), entry);
        assertThrows(IllegalArgumentException.class,
                () -> session.registerEntryTemplate(entry.id(), entry));
        assertThrows(IllegalArgumentException.class,
                () -> session.registerEntryTemplate(
                        id("contentmod", "other"), entry));
        session.close();
        assertThrows(IllegalStateException.class,
                () -> session.registerAffixTemplate(
                        id("contentmod", "expired"),
                        affix("contentmod", "expired")));
    }

    @Test
    void entryAndAffixNamespacesAreTypedEvenForTheSameIdentifier() {
        DamageModuleRegistrationSession session = session();
        DamageEntryDefinition entry = entry("contentmod", "shared_id");
        DamageAffixDefinition affix = affix("contentmod", "shared_id");
        session.registerEntryTemplate(entry.id(), entry);
        session.registerAffixTemplate(affix.id(), affix);
        DamageTemplateRegistry.freeze(access);

        assertEquals(Optional.of(entry), DamageModuleTemplates.entry(entry.id()));
        assertEquals(Optional.of(affix), DamageModuleTemplates.affix(affix.id()));
    }

    @Test
    void invalidRulesAreRejectedWithoutPartialRegistration() {
        DamageModuleRegistrationSession session = session();
        Identifier invalidId = id("contentmod", "invalid_rule");
        DamageRuleDefinition wrongPhase = new DamageRuleDefinition(
                id("contentmod", "wrong_phase"),
                DamageRuleRole.OFFENSIVE,
                DamagePhase.BASE_MODIFICATION,
                500,
                List.of(),
                List.of(DamageModuleOperations.overrideFinalDamage(3.0f)),
                DamageRuleStacking.STACK,
                Optional.empty(),
                Optional.empty());
        DamageEntryDefinition invalid = new DamageEntryDefinition(
                invalidId, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(wrongPhase), DamageEntryStacking.STACK,
                Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> session.registerEntryTemplate(invalidId, invalid));

        DamageTemplateRegistry.freeze(access);
        assertTrue(DamageModuleTemplates.entry(invalidId).isEmpty());
    }

    private DamageModuleRegistrationSession session() {
        access = DamageModuleLifecycle.beginRegistering();
        return new DamageModuleRegistrationSession(access);
    }

    static DamageEntryDefinition entry(String namespace, String path) {
        Identifier id = id(namespace, path);
        return new DamageEntryDefinition(
                id, DamageEntryDisplay.EMPTY, DamageEntrySlot.ITEM,
                List.of(DamageRuleBuilder.offensive(
                                id(namespace, path + "_rule"))
                        .addBaseDamage(DamageChannel.UNTYPED_ID, 1.0f)
                        .build()),
                DamageEntryStacking.STACK, Optional.empty());
    }

    static DamageAffixDefinition affix(String namespace, String path) {
        Identifier id = id(namespace, path);
        return new DamageAffixDefinition(
                id, DamageAffixDisplay.EMPTY, DamageAffixSlot.ITEM,
                DamageAffixRarity.COMMON,
                List.of(entry(namespace, path + "_nested")),
                DamageAffixStacking.STACK, Optional.empty());
    }

    static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
