package io.github.naimjeg.combatnexus.damage.diagnostics.logging;

import io.github.naimjeg.combatnexus.damage.api.DamagePhaseProcessor;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;

import java.util.List;

public interface CombatPipelineLog {

    void layout(DamagePhase phase, List<DamagePhaseProcessor> processors);

    void phase(DamagePhase phase);

    void processorRun(
            DamagePhase phase,
            DamagePhaseProcessor processor
    );

    void processorSkip(
            DamagePhase phase,
            DamagePhaseProcessor processor
    );
}

