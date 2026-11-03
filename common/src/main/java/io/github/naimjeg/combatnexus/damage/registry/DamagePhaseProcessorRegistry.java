package io.github.naimjeg.combatnexus.damage.registry;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleLifecycle;
import io.github.naimjeg.combatnexus.damage.core.lifecycle.DamageModuleRegistrationAccess;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleLifecycleLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DamagePhaseProcessorRegistry {

    private static final List<DamagePhaseProcessor> EXTERNAL_PROCESSORS =
            new ArrayList<>();

    private static int version = 0;

    private DamagePhaseProcessorRegistry() {
    }

    public static synchronized void registerExternal(
            DamageModuleRegistrationAccess access,
            DamagePhaseProcessor processor
    ) {
        DamageModuleLifecycle.requireRegistering(
                access,
                "DamagePhaseProcessorRegistry.registerExternal"
        );
        Objects.requireNonNull(
                processor,
                "DamagePhaseProcessor cannot be null"
        );

        if (EXTERNAL_PROCESSORS.contains(processor)) {
            DamageModule.LOGGER.warn(
                    "[DamageModule] Ignoring duplicate external damage phase processor instance: {}",
                    processor.getClass().getName()
            );
            return;
        }

        EXTERNAL_PROCESSORS.add(processor);
        version++;

        DamageModuleLifecycleLog.externalProcessorRegistered(processor);
    }

    public static synchronized List<DamagePhaseProcessor> externalProcessors() {
        return List.copyOf(EXTERNAL_PROCESSORS);
    }

    public static synchronized int version() {
        return version;
    }
}

