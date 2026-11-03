package io.github.naimjeg.combatnexus.combat.damage.api.event;

/**
 * Server-side completed-damage callback registered during
 * the platform registration event.
 *
 * <p>Unlike the observational NeoForge settlement event, this
 * callback runs inside a framework-controlled dynamic scope and may obtain
 * temporary child-request authority from its argument. Ordinary callback
 * failures are isolated per registered listener after the parent damage has
 * committed.</p>
 */
@FunctionalInterface
public interface DamageSettlementListener {

    void onDamageSettled(DamageSettlementCallback callback);
}
