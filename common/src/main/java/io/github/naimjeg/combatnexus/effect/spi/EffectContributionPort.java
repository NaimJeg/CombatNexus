package io.github.naimjeg.combatnexus.effect.spi;
import io.github.naimjeg.combatnexus.effect.api.*;

import java.util.List;

/** Prepare is read-only; apply reports every confirmed contribution write, including failures. */
public interface EffectContributionPort {
    Prepared prepare(EffectService.Change change);
    /** Notification after authoritative state commits; never used to install contributions. */
    default void committed(EffectService.Change change) {}
    default boolean mayExpire(EffectInstance effect) { return true; }
    /** Called after active state is committed; zero-delay behaviors may now capture the installed effect. */
    default void activated(EffectInstance effect) {}
    default io.github.naimjeg.combatnexus.combat.api.CommandResult<Boolean> instant(EffectInstance effect) {
        return io.github.naimjeg.combatnexus.combat.api.CommandResult.rejected(
                io.github.naimjeg.combatnexus.combat.api.CommandRejection.Code.INVALID_INPUT, "This contribution port cannot execute instant effects");
    }
    interface Prepared {
        Receipt apply();
        /** Restore the pre-change installation. Safe to retry after a partial compensation. */
        Receipt reconcile();
    }
    record Write(String source, boolean installed) {}
    record Receipt(List<Write> writes, boolean completed, RuntimeException failure) {
        public Receipt { writes = List.copyOf(writes); }
    }
}
