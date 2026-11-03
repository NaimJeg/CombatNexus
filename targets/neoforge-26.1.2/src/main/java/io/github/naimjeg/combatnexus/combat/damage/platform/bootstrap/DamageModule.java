package io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap;

import net.neoforged.neoforge.common.NeoForge;

import io.github.naimjeg.combatnexus.combat.damage.platform.api.event.DamageSettledEvent;

import io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementEventPublisher;

import io.github.naimjeg.combatnexus.combat.damage.platform.DamageContent;

import io.github.naimjeg.combatnexus.combat.damage.platform.network.DamageModuleNetwork;
import io.github.naimjeg.combatnexus.combat.damage.registry.*;
import io.github.naimjeg.combatnexus.combat.damage.platform.registry.*;
import io.github.naimjeg.combatnexus.attribute.platform.registry.ModAttributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

public class DamageModule {
    public static final String MODID = io.github.naimjeg.combatnexus.CombatNexusCommon.MOD_ID;

    public DamageModule(IEventBus modEventBus, ModContainer modContainer) {
        io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.DamageSourceClassification.install(
                source -> source.is(net.neoforged.neoforge.common.Tags.DamageTypes.IS_MAGIC),
                source -> source.is(net.neoforged.neoforge.common.Tags.DamageTypes.IS_TECHNICAL));
        modEventBus.addListener(DamageModuleNetwork::registerPayloads);

        DamageContent.install(
                new DamageContent.Handles(
                        ModBlocks.DAMAGE_DUMMY, ModBlockEntityTypes.DAMAGE_DUMMY,
                        ModEntityTypes.DAMAGE_DUMMY, ModMenuTypes.DAMAGE_DUMMY));
        DamageSettlementEventPublisher.install(
                snapshot -> {
                    if(snapshot.successfulHit()) io.github.naimjeg.combatnexus.combat.spi.CombatMomentAccess.publish(snapshot.level(),
                            new io.github.naimjeg.combatnexus.combat.api.action.CombatMoment.Hit(snapshot.lineage().damageId(),snapshot.target().getUUID(),snapshot.evidence().healthLoss(),snapshot.absorptionDamage(),snapshot.evidence().shieldLoss(),snapshot.evidence().action(),snapshot.evidence().equipment()));
                    NeoForge.EVENT_BUS.post(new DamageSettledEvent(snapshot));
                });
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
    }

}

