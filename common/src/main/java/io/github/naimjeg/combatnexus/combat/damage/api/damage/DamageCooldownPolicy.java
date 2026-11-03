package io.github.naimjeg.combatnexus.combat.damage.api.damage;

/** Independent scheduled damage neither reads nor writes the shared native hurt cooldown. Immunity still applies. */
public enum DamageCooldownPolicy { VANILLA, INDEPENDENT }
