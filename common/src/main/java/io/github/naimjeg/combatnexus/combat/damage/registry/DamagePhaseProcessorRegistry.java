package io.github.naimjeg.combatnexus.combat.damage.registry;

import io.github.naimjeg.combatnexus.combat.api.pipeline.*;
import io.github.naimjeg.combatnexus.combat.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.combat.damage.core.lifecycle.*;
import io.github.naimjeg.combatnexus.combat.damage.builtin.BuiltinDamageProcessors;
import net.minecraft.resources.Identifier;

/** Registration metadata is captured once, before freeze. */
public final class DamagePhaseProcessorRegistry {
    private static final DomainPipeline.Builder<DamagePhase, DamagePhaseProcessor> BUILDER = new DomainPipeline.Builder<>(DamagePhase.class);
    private static DomainPipeline<DamagePhase, DamagePhaseProcessor> frozen;
    private DamagePhaseProcessorRegistry() {}
    public static synchronized PipelineRegistration registerExternal(DamageModuleRegistrationAccess access, Identifier id, DamagePhaseProcessor processor) {
        DamageModuleLifecycle.requireRegistering(access, "registerPhaseProcessor");
        return BUILDER.register(id, processor.phase(), processor.getPriority(), true, processor);
    }
    public static synchronized void freeze(DamageModuleRegistrationAccess access) {
        DamageModuleLifecycle.requireRegistering(access, "freezePhaseProcessors");
        for (var processor : BuiltinDamageProcessors.builtInProcessors())
            BUILDER.register(BuiltinDamageProcessors.builtInProcessorId(processor), processor.phase(), processor.getPriority(), false, processor);
        frozen = BUILDER.freeze();
    }
    public static DomainPipeline<DamagePhase, DamagePhaseProcessor> snapshot() {
        if (frozen == null) throw new IllegalStateException("Damage pipeline is not frozen");
        return frozen;
    }
}
