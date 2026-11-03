package io.github.naimjeg.combatnexus.combat.damage.core.pipeline;

import io.github.naimjeg.combatnexus.combat.damage.api.context.DamageRuleContext;

import java.util.Objects;

/**
 * Creates API-safe views for calls into third-party extension code.
 */
public final class DamageRuleContextViews {

    private DamageRuleContextViews() {
    }

    public static io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView readOnly(
            io.github.naimjeg.combatnexus.combat.damage.api.context.DamageContextView context) {
        return new ReadOnlyDamageContext(context);
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
