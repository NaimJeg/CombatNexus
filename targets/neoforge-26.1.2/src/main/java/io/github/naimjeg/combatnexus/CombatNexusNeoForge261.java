package io.github.naimjeg.combatnexus;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Loader composition root. Damage is one module of CombatNexus. */
@Mod(CombatNexusCommon.MOD_ID)
public final class CombatNexusNeoForge261 {
    public CombatNexusNeoForge261(IEventBus bus, ModContainer container) {
        new DamageModule(bus, container);
    }
}
