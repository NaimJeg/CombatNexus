package io.github.naimjeg.combatnexus.damage.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.config.DamageModuleConfig;
import io.github.naimjeg.combatnexus.damage.config.DiagnosticMode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = DamageModule.MODID)
public final class DamageModuleCommands {

    private DamageModuleCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        var config = DamageModuleConfig.current();

        boolean diagnosticsEnabled = config.diagnostics().diagnosticMode()
                != DiagnosticMode.OFF;
        boolean testCommands = config.developer().testCommandsEnabled();

        if (!diagnosticsEnabled && !testCommands) {
            return;
        }

        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        var root = Commands.literal("combatnexus");

        if (testCommands) {
            DamageTestCommands.register(root);
            DamageItemCommands.register(root);
            DamageDamageCommands.register(root);
            DamageBypassCommands.register(root);
            DamageMobCommands.register(root);
        }

        if (diagnosticsEnabled) {
            DamageEffectCommands.register(root);
            DamageAttributeCommands.register(root);
        }

        if (testCommands || diagnosticsEnabled) {
            DamageCleanupCommands.register(root);
        }

        DamageCommandSecurity.verifyTree(root.build());
        dispatcher.register(root);
    }
}
