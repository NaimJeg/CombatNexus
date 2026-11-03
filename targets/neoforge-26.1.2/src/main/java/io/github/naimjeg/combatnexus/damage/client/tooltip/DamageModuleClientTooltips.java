package io.github.naimjeg.combatnexus.damage.client.tooltip;

import io.github.naimjeg.combatnexus.damage.api.client.phrase.RegisterRulePhrasesEvent;
import io.github.naimjeg.combatnexus.damage.api.client.phrase.RulePhraseRegistry;
import net.neoforged.neoforge.common.NeoForge;

public final class DamageModuleClientTooltips {

    private static volatile RulePhraseRegistry registry;

    private DamageModuleClientTooltips() {
    }

    public static synchronized void register() {
        if (registry != null) {
            return;
        }

        RulePhraseRegistry building = new RulePhraseRegistry();
        DamageModuleRulePhraseBootstrap.register(building);
        NeoForge.EVENT_BUS.post(new RegisterRulePhrasesEvent(building));
        building.freeze();
        registry = building;
    }

    public static RulePhraseRegistry registry() {
        register();
        return registry;
    }
}
