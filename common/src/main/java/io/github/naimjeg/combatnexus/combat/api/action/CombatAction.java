package io.github.naimjeg.combatnexus.combat.api.action;
import java.util.UUID;
/** Actual action identity supplied by an adapter; descriptive, not a parent capability. */
public record CombatAction(UUID id, UUID session, UUID actor) {}
