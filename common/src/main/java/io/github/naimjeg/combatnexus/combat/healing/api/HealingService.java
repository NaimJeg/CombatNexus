package io.github.naimjeg.combatnexus.combat.healing.api;

/**
 * World-session healing service with native application evidence and post-commit delivery.
 * Implementations must correlate API -> vanilla heal -> event once, confirm actual
 * application, and reject ordinary healing of dead entities.
 */
public interface HealingService {
    HealingSettlement submit(HealingRequest request);
    HealingSettlement submit(HealingRequest request, io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent parent);
}
