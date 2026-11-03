package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView;

/**
 * Internal adapter for built-in systems that still need the concrete pipeline
 * context while public APIs receive DamageContextView.
 */
public final class DamageInternalContexts {

    private DamageInternalContexts() {
    }

    public static DamageModuleContext require(
            DamageContextView ctx,
            String usage
    ) {
        if (ctx instanceof DamageModuleContext internal) {
            return internal;
        }

        throw new IllegalArgumentException(
                "DamageModule internal context required for "
                        + usage
                        + ": "
                        + (ctx == null ? "<null>" : ctx.getClass().getName())
        );
    }
}
