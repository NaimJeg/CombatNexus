package io.github.naimjeg.combatnexus.damage.core.pipeline;

import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;

import java.util.Objects;

/**
 * Creates API-safe views for calls into third-party extension code.
 */
public final class DamageRuleContextViews {

    private DamageRuleContextViews() {
    }

    public static DamageRuleContext restricted(DamageRuleContext context) {
        Objects.requireNonNull(context, "context");

        if (context instanceof RestrictedDamageRuleContext) {
            return context;
        }

        if (context instanceof DamageModuleContext internal) {
            return internal.restrictedView();
        }

        return new RestrictedDamageRuleContext(context);
    }
}
