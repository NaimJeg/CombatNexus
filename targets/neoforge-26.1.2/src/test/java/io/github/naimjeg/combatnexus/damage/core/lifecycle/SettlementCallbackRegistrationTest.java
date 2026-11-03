package io.github.naimjeg.combatnexus.damage.core.lifecycle;

import io.github.naimjeg.combatnexus.damage.core.settlement.DamageSettlementCallbacks;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SettlementCallbackRegistrationTest {

    private DamageModuleRegistrationAccess access;

    @AfterEach
    void reset() {
        if (access != null) {
            access.close();
        }
        DamageModuleLifecycle.resetForTesting();
    }

    @Test
    void identifiersAndPriorityOrderingAreDeterministic() {
        DamageModuleRegistrationSession session = session();
        session.registerSettlementListener(id("zmod", "same"), 5,
                callback -> { });
        session.registerSettlementListener(id("amod", "same"), 5,
                callback -> { });
        session.registerSettlementListener(id("midmod", "high"), 9,
                callback -> { });

        DamageSettlementCallbacks.freeze(access);

        assertEquals(List.of(
                        id("midmod", "high"),
                        id("amod", "same"),
                        id("zmod", "same")),
                DamageSettlementCallbacks.orderedIds());
    }

    @Test
    void duplicateBoundsFreezeAndRegistrarExpiryAreEnforced() {
        DamageModuleRegistrationSession session = session();
        Identifier id = id("contentmod", "settlement");
        session.registerSettlementListener(id, 0, callback -> { });

        assertThrows(IllegalArgumentException.class,
                () -> session.registerSettlementListener(
                        id, 1, callback -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> session.registerSettlementListener(
                        id("contentmod", "too_high"),
                        DamageSettlementCallbacks.MAX_PRIORITY + 1,
                        callback -> { }));
        assertThrows(IllegalArgumentException.class,
                () -> session.registerSettlementListener(
                        id("contentmod", "too_low"),
                        DamageSettlementCallbacks.MIN_PRIORITY - 1,
                        callback -> { }));

        DamageSettlementCallbacks.freeze(access);
        assertThrows(IllegalStateException.class,
                () -> session.registerSettlementListener(
                        id("contentmod", "late"), 0, callback -> { }));

        session.close();
        assertThrows(IllegalStateException.class,
                () -> session.registerSettlementListener(
                        id("contentmod", "expired"), 0, callback -> { }));
    }

    private DamageModuleRegistrationSession session() {
        access = DamageModuleLifecycle.beginRegistering();
        return new DamageModuleRegistrationSession(access);
    }

    private static Identifier id(String namespace, String path) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }
}
