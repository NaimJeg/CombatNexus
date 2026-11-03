package io.github.naimjeg.combatnexus.compat;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import io.github.naimjeg.combatnexus.combat.platform.api.CombatRegisterEvent;

@Mod("combatnexus_compat")
public final class CombatNexusCompat {
    public CombatNexusCompat() {
        NeoForge.EVENT_BUS.addListener(CombatNexusCompat::register);
    }
    private static void register(CombatRegisterEvent event) {
        if (ModList.get().isLoaded("apotheosis"))
            io.github.naimjeg.combatnexus.compat.apotheosis.ApotheosisBridge.register(event.registrar());
        if (ModList.get().isLoaded("apothic_attributes"))
            io.github.naimjeg.combatnexus.compat.apotheosis.AttributesBridge.register(event.registrar());
    }
}
