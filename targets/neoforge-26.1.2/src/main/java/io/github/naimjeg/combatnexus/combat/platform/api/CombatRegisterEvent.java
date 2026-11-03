package io.github.naimjeg.combatnexus.combat.platform.api;

import io.github.naimjeg.combatnexus.combat.api.CombatRegistrar;
import net.neoforged.bus.api.Event;

/** Posted once on the NeoForge game bus during common setup. Registrar expires when dispatch returns. */
public final class CombatRegisterEvent extends Event {
    private final CombatRegistrar registrar;
    public CombatRegisterEvent(CombatRegistrar registrar) { this.registrar = java.util.Objects.requireNonNull(registrar); }
    public CombatRegistrar registrar() { return registrar; }
}
