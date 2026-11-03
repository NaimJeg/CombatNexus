package io.github.naimjeg.combatnexus.damage.core.lifecycle;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.event.DamageModuleRegisterEvent;
import io.github.naimjeg.combatnexus.damage.api.event.DamageModuleRegistrar;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleProvider;
import io.github.naimjeg.combatnexus.damage.builtin.rule.condition.AlwaysCondition;
import io.github.naimjeg.combatnexus.damage.builtin.rule.operation.CancelDamageOperation;
import io.github.naimjeg.combatnexus.damage.core.registry.DamageChannelRegistry;
import io.github.naimjeg.combatnexus.damage.core.registry.PreMultiplierBucketRegistry;
import io.github.naimjeg.combatnexus.damage.core.rule.DatapackDamageRuleStore;
import io.github.naimjeg.combatnexus.damage.core.rule.DatapackDamageRuleReloadListener;
import io.github.naimjeg.combatnexus.damage.registry.DamagePhaseProcessorRegistry;
import io.github.naimjeg.combatnexus.damage.registry.PreMultiplierBuckets;
import io.github.naimjeg.combatnexus.damage.registry.rule.DamageRuleConditionTypes;
import io.github.naimjeg.combatnexus.damage.registry.rule.DamageRuleOperationTypes;
import io.github.naimjeg.combatnexus.damage.registry.rule.DamageRuleProviders;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.EventBusSubscriber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageModuleLifecycleTest {

    private DamageModuleRegistrationAccess access;

    @AfterEach
    void resetLifecycle() {
        if (access != null) {
            access.close();
        }
        DamageModuleLifecycle.resetForTesting();
    }

    @Test
    void legalLifecycleIsForwardOnly() {
        assertEquals(
                DamageModuleLifecycleState.CONSTRUCTING,
                DamageModuleLifecycle.state()
        );

        access = DamageModuleLifecycle.beginRegistering();
        assertEquals(
                DamageModuleLifecycleState.REGISTERING,
                DamageModuleLifecycle.state()
        );

        DamageModuleLifecycle.freezeRegistration(access);
        assertEquals(
                DamageModuleLifecycleState.FROZEN,
                DamageModuleLifecycle.state()
        );

        DamageModuleLifecycle.running();
        assertEquals(
                DamageModuleLifecycleState.RUNNING,
                DamageModuleLifecycle.state()
        );
    }

    @Test
    void allIllegalTransitionsAreRejected() {
        assertThrows(
                IllegalStateException.class,
                () -> DamageModuleLifecycle.freezeRegistration(null)
        );
        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::running
        );

        access = DamageModuleLifecycle.beginRegistering();

        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::beginRegistering
        );
        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::running
        );

        DamageModuleLifecycle.freezeRegistration(access);

        assertThrows(
                IllegalStateException.class,
                () -> DamageModuleLifecycle.freezeRegistration(access)
        );

        DamageModuleLifecycle.running();

        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::beginRegistering
        );
        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::running
        );
    }

    @Test
    void registrationFailureIsTerminalAndClosesCapability() {
        access = DamageModuleLifecycle.beginRegistering();
        DamageModuleRegistrationSession session =
                new DamageModuleRegistrationSession(access);
        DamageModuleRegistrar saved =
                new DamageModuleRegisterEvent(session).registrar();

        DamageModuleLifecycle.failBootstrap(access);

        assertEquals(
                DamageModuleLifecycleState.FAILED,
                DamageModuleLifecycle.state()
        );
        assertFalse(access.isActive());
        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::beginRegistering
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamageModuleLifecycle.freezeRegistration(access)
        );
        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::running
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleProviders.register(
                        access,
                        (ctx, phase, out) -> {
                        }
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> saved.registerRuleProvider(
                        (ctx, phase, out) -> {
                        }
                )
        );
    }

    @Test
    void postFreezeBootstrapFailureAlsoBecomesTerminal() {
        access = DamageModuleLifecycle.beginRegistering();
        DamageModuleLifecycle.freezeRegistration(access);

        DamageModuleLifecycle.failBootstrap(access);

        assertEquals(
                DamageModuleLifecycleState.FAILED,
                DamageModuleLifecycle.state()
        );
        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::running
        );
        assertThrows(
                IllegalStateException.class,
                DamageModuleLifecycle::beginRegistering
        );
    }

    @Test
    void failureTransitionCannotBeForgedOutsideBootstrapWindow() {
        assertThrows(
                IllegalStateException.class,
                () -> DamageModuleLifecycle.failBootstrap(null)
        );

        access = DamageModuleLifecycle.beginRegistering();
        DamageModuleLifecycle.freezeRegistration(access);
        DamageModuleLifecycle.running();

        assertThrows(
                IllegalStateException.class,
                () -> DamageModuleLifecycle.failBootstrap(access)
        );
        assertEquals(
                DamageModuleLifecycleState.RUNNING,
                DamageModuleLifecycle.state()
        );
    }

    @Test
    void registrarWorksOnlyDuringEventCallbackWindow() {
        access = DamageModuleLifecycle.beginRegistering();
        DamageModuleRegistrationSession session =
                new DamageModuleRegistrationSession(access);
        DamageModuleRegisterEvent event =
                new DamageModuleRegisterEvent(session);
        DamageModuleRegistrar saved = event.registrar();
        DamageRuleProvider provider = (ctx, phase, out) -> {
        };
        DamagePhaseProcessor processor = new TestProcessor();
        Identifier conditionId =
                Identifier.fromNamespaceAndPath(
                        "test",
                        "session_condition"
                );
        Identifier operationId =
                Identifier.fromNamespaceAndPath(
                        "test",
                        "session_operation"
                );
        Identifier bucketId =
                Identifier.fromNamespaceAndPath(
                        "test",
                        "session_bucket"
                );

        event.registerCondition(conditionId, AlwaysCondition.CODEC);
        event.registerOperation(operationId, CancelDamageOperation.CODEC);
        event.registerRuleProvider(provider);
        event.registerPhaseProcessor(processor);
        event.registerPreMultiplierBucket(bucketId);

        assertTrue(DamageRuleConditionTypes.registeredTypes()
                .contains(conditionId));
        assertTrue(DamageRuleOperationTypes.registeredTypes()
                .contains(operationId));
        assertTrue(DamageRuleProviders.all().contains(provider));
        assertTrue(DamagePhaseProcessorRegistry.externalProcessors()
                .contains(processor));
        assertTrue(PreMultiplierBucketRegistry.registeredBuckets()
                .contains(bucketId));

        session.close();

        assertThrows(
                IllegalStateException.class,
                () -> saved.registerRuleProvider(
                        (ctx, phase, out) -> {
                        }
                )
        );

        DamageModuleLifecycle.freezeRegistration(access);
        DamageModuleLifecycle.running();

        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleProviders.register(
                        access,
                        (ctx, phase, out) -> {
                        }
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamagePhaseProcessorRegistry.registerExternal(
                        access,
                        new TestProcessor()
                )
        );
    }

    @Test
    void registryWritesBeforeEventRequireUnforgeableCapability() {
        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleConditionTypes.register(
                        null,
                        Identifier.fromNamespaceAndPath(
                                "test",
                                "outside_event_condition"
                        ),
                        AlwaysCondition.CODEC
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleOperationTypes.register(
                        null,
                        Identifier.fromNamespaceAndPath(
                                "test",
                                "outside_event_operation"
                        ),
                        CancelDamageOperation.CODEC
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamageRuleProviders.register(
                        null,
                        (ctx, phase, out) -> {
                        }
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> DamagePhaseProcessorRegistry.registerExternal(
                        null,
                        new TestProcessor()
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> PreMultiplierBucketRegistry
                        .registerPreMultiplierBucket(
                                null,
                                Identifier.fromNamespaceAndPath(
                                        "test",
                                        "outside_event"
                                )
                        )
        );
    }

    @Test
    void externalCodeCannotFreezeRegistryEarly() {
        assertFalse(PreMultiplierBucketRegistry.isFrozen());

        assertThrows(
                IllegalStateException.class,
                () -> PreMultiplierBucketRegistry.freeze(null)
        );

        assertFalse(PreMultiplierBucketRegistry.isFrozen());
    }

    @Test
    void registryReadSnapshotsAreImmutable() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamageRuleProviders.all().clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamagePhaseProcessorRegistry
                        .externalProcessors()
                        .clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamageRuleConditionTypes
                        .registeredTypes()
                        .clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> DamageRuleOperationTypes
                        .registeredTypes()
                        .clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> PreMultiplierBucketRegistry
                        .registeredBuckets()
                        .clear()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> DatapackDamageRuleStore.rules().clear()
        );
    }

    @Test
    void lifecycleProgressionAndCapabilityConstructionAreNotPublic()
            throws Exception {
        assertNotPublic(DamageModuleLifecycle.class.getDeclaredMethod(
                "beginRegistering"
        ));
        assertNotPublic(DamageModuleLifecycle.class.getDeclaredMethod(
                "freezeRegistration",
                DamageModuleRegistrationAccess.class
        ));
        assertNotPublic(DamageModuleLifecycle.class.getDeclaredMethod(
                "running"
        ));
        assertNotPublic(DamageModuleLifecycle.class.getDeclaredMethod(
                "failBootstrap",
                DamageModuleRegistrationAccess.class
        ));
        assertThrows(
                NoSuchMethodException.class,
                () -> DamageModuleLifecycle.class.getDeclaredMethod(
                        "beginReloading"
                )
        );

        assertTrue(Arrays.stream(
                        DamageModuleRegistrationAccess.class
                                .getDeclaredConstructors()
                )
                .noneMatch(constructor -> Modifier.isPublic(
                        constructor.getModifiers()
                )));
    }

    @Test
    void bootstrapIsAFrameworkOwnedModBusSubscriber() {
        assertFalse(Modifier.isPublic(
                DamageModuleBootstrap.class.getModifiers()
        ));
        assertTrue(Arrays.stream(
                        DamageModuleBootstrap.class.getDeclaredMethods()
                )
                .noneMatch(method -> method.getName().equals("install")));

        EventBusSubscriber annotation =
                DamageModuleBootstrap.class.getAnnotation(
                        EventBusSubscriber.class
                );

        assertTrue(annotation != null);
        assertEquals(DamageModule.MODID, annotation.modid());
    }

    @Test
    void commonSetupCanOnlyBeClaimedOncePerProcess() {
        assertTrue(DamageModuleBootstrap.claimCommonSetup());
        assertFalse(DamageModuleBootstrap.claimCommonSetup());
    }

    @Test
    void publicRegistryMutationsAllRequireRegistrationCapability() {
        assertCapabilityProtectedMutation(
                DamageRuleProviders.class,
                "register"
        );
        assertCapabilityProtectedMutation(
                DamagePhaseProcessorRegistry.class,
                "registerExternal"
        );
        assertCapabilityProtectedMutation(
                DamageRuleConditionTypes.class,
                "register"
        );
        assertCapabilityProtectedMutation(
                DamageRuleOperationTypes.class,
                "register"
        );
        assertCapabilityProtectedMutation(
                PreMultiplierBucketRegistry.class,
                "registerPreMultiplierBucket"
        );
        assertCapabilityProtectedMutation(
                PreMultiplierBucketRegistry.class,
                "freeze"
        );
        assertCapabilityProtectedMutation(
                PreMultiplierBuckets.class,
                "register"
        );
    }

    @Test
    void datapackRegistryMutationMethodsAreNotPublic() {
        assertTrue(Arrays.stream(DamageChannelRegistry.class
                        .getDeclaredMethods())
                .filter(method -> method.getName().equals("apply")
                        || method.getName().equals(
                        "replaceStateForTesting"
                )
                        || method.getName().equals(
                        "resetStateForTesting"
                ))
                .noneMatch(method -> Modifier.isPublic(
                        method.getModifiers()
                )));
        assertTrue(Arrays.stream(DatapackDamageRuleStore.class
                        .getDeclaredMethods())
                .filter(method -> method.getName().equals("replace"))
                .noneMatch(method -> Modifier.isPublic(
                        method.getModifiers()
                )));
    }

    @Test
    void reloadListenersRequireFrameworkOwnedCapability() {
        assertTrue(Arrays.stream(
                        DamageModuleReloadAccess.class
                                .getDeclaredConstructors()
                )
                .noneMatch(constructor -> Modifier.isPublic(
                        constructor.getModifiers()
                )));
        assertTrue(Arrays.stream(
                        DamageChannelRegistry.class
                                .getDeclaredConstructors()
                )
                .allMatch(constructor ->
                        constructor.getParameterCount() == 1
                                && constructor.getParameterTypes()[0]
                                == DamageModuleReloadAccess.class));
        assertTrue(Arrays.stream(
                        DatapackDamageRuleReloadListener.class
                                .getDeclaredConstructors()
                )
                .allMatch(constructor ->
                        constructor.getParameterCount() == 1
                                && constructor.getParameterTypes()[0]
                                == DamageModuleReloadAccess.class));
    }

    @Test
    void registerEventExposesTheExactExpiringRegistrar() {
        access = DamageModuleLifecycle.beginRegistering();
        DamageModuleRegistrationSession session =
                new DamageModuleRegistrationSession(access);
        DamageModuleRegisterEvent event =
                new DamageModuleRegisterEvent(session);

        assertSame(session, event.registrar());
        assertTrue(session.isOpen());

        session.close();

        assertFalse(session.isOpen());
    }

    private static void assertNotPublic(Method method) {
        assertFalse(Modifier.isPublic(method.getModifiers()));
    }

    private static void assertCapabilityProtectedMutation(
            Class<?> owner,
            String methodName
    ) {
        assertTrue(Arrays.stream(owner.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .allMatch(method -> method.getParameterCount() > 0
                        && method.getParameterTypes()[0]
                        == DamageModuleRegistrationAccess.class));
    }

    private static final class TestProcessor
            implements DamagePhaseProcessor {

        @Override
        public void apply(DamageRuleContext ctx) {
        }

        @Override
        public DamagePhase phase() {
            return DamagePhase.BASE_MODIFICATION;
        }
    }
}
