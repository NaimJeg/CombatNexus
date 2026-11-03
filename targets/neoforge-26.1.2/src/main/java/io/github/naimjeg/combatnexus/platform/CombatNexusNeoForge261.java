package io.github.naimjeg.combatnexus.platform;

import io.github.naimjeg.combatnexus.CombatNexusCommon;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/** Loader composition root. Damage is one module of CombatNexus. */
@Mod(CombatNexusCommon.MOD_ID)
public final class CombatNexusNeoForge261 {
    public CombatNexusNeoForge261(IEventBus bus, ModContainer container) {
        new DamageModule(bus, container);
        io.github.naimjeg.combatnexus.combat.platform.CombatDataComponents.register(bus);
        io.github.naimjeg.combatnexus.effect.platform.EnvironmentEffectAdapters.register(bus);
        bus.addListener(io.github.naimjeg.combatnexus.presentation.platform.CombatPresentationNetwork::register);
    }
}
