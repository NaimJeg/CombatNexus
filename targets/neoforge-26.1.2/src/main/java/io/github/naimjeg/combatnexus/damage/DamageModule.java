package io.github.naimjeg.combatnexus.damage;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.combatnexus.damage.config.DamageModuleConfig;
import io.github.naimjeg.combatnexus.damage.network.DamageModuleNetwork;
import io.github.naimjeg.combatnexus.damage.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import org.slf4j.Logger;

public class DamageModule {
    public static final String MODID = io.github.naimjeg.combatnexus.CombatNexusCommon.MOD_ID;
    public static final Logger LOGGER = LogUtils.getLogger();
    
    public DamageModule(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(DamageModuleConfig::onLoad);
        modEventBus.addListener(DamageModuleConfig::onReload);
        modEventBus.addListener(DamageModuleNetwork::registerPayloads);


        ModAttributes.register(modEventBus);
        ModDamageProcessors.register(modEventBus);
        ModAttachments.ATTACHMENTS.register(modEventBus);
        ModDataComponents.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntityTypes.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        // Creative inventory integration: append the pedestal BlockItem to
        // the vanilla Functional Blocks tab. BuildCreativeModeTabContentsEvent
        // is a mod-bus event, so it is registered here rather than on the
        // game (NeoForge.EVENT_BUS) or inside the physical-client handler.
        modEventBus.addListener(ModCreativeTabContents::buildContents);

        // Damage dummy attribute lifecycle: a baseline supplier first, then
        // generic attachment of the full registered ATTRIBUTE registry.
        modEventBus.addListener(ModEntityAttributes::onCreateAttributes);
        modEventBus.addListener(ModEntityAttributes::onModifyAttributes);

        modContainer.registerConfig(
                net.neoforged.fml.config.ModConfig.Type.COMMON,
                DamageModuleConfig.SPEC
        );
    }

}

