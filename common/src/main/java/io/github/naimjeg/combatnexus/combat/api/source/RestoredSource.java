package io.github.naimjeg.combatnexus.combat.api.source;

import java.util.Objects;

/** Delivered only to the registered provider owner after its installation has been restored. */
public record RestoredSource(SourceView source, InstallationHandle handle) {
    public RestoredSource { Objects.requireNonNull(source); Objects.requireNonNull(handle); }
}
