package io.github.naimjeg.combatnexus.damage.core.lifecycle;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.api.DamageModuleIds;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModulePipeline;
import io.github.naimjeg.combatnexus.damage.core.registry.DamageChannelRegistry;
import io.github.naimjeg.combatnexus.damage.core.rule.DatapackDamageRuleReloadListener;
import io.github.naimjeg.combatnexus.damage.core.template.DatapackDamageTemplateReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.List;

@EventBusSubscriber(modid = DamageModule.MODID)
public final class DamageModuleResourceReloadHandler {

    private static final net.minecraft.resources.Identifier CHANNELS =
            DamageModuleIds.id("channel_registry");
    private static final net.minecraft.resources.Identifier GLOBAL_RULES =
            DamageModuleIds.id("global_damage_rules");
    private static final net.minecraft.resources.Identifier TEMPLATES =
            DamageModuleIds.id("static_damage_templates");
    private static final List<ReloadDependency> REQUIRED_DEPENDENCIES =
            List.of(
                    new ReloadDependency(CHANNELS, GLOBAL_RULES),
                    new ReloadDependency(CHANNELS, TEMPLATES)
            );

    private static final DamageModuleReloadAccess RELOAD_ACCESS =
            new DamageModuleReloadAccess();

    private DamageModuleResourceReloadHandler() {
    }

    @SubscribeEvent
    public static void onAddServerReloadListeners(
            AddServerReloadListenersEvent event
    ) {
        event.addListener(
                CHANNELS,
                new DamageChannelRegistry(RELOAD_ACCESS)
        );

        event.addListener(
                GLOBAL_RULES,
                new DatapackDamageRuleReloadListener(RELOAD_ACCESS)
        );

        event.addListener(
                TEMPLATES,
                new DatapackDamageTemplateReloadListener(RELOAD_ACCESS)
        );

        // SortedReloadListenerEvent graph edges: channel content is validated
        // before either dependent registry can publish its bound revision.
        for (ReloadDependency dependency : REQUIRED_DEPENDENCIES) {
            event.addDependency(dependency.first(), dependency.second());
        }

        DamageModulePipeline.clearCache();
    }

    static List<ReloadDependency> requiredDependenciesForTesting() {
        return REQUIRED_DEPENDENCIES;
    }

    record ReloadDependency(
            net.minecraft.resources.Identifier first,
            net.minecraft.resources.Identifier second
    ) {}
}
