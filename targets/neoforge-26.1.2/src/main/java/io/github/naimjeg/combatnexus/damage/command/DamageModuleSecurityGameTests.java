package io.github.naimjeg.combatnexus.damage.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.core.gametest.GameTestCodecVerifier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Launch-only security checks. RegisterGameTestsEvent is only fired when
 * NeoForge's GameTest facility is enabled, so no test is added in production.
 */
@EventBusSubscriber(modid = DamageModule.MODID)
final class DamageModuleSecurityGameTests {

    private static final ResourceKey<Consumer<GameTestHelper>>
            SECURITY_BOUNDARIES_FUNCTION = ResourceKey.create(
            Registries.TEST_FUNCTION,
            id("security_boundaries")
    );

    private DamageModuleSecurityGameTests() {
    }

    @SubscribeEvent
    public static void registerTestFunction(RegisterEvent event) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        event.register(
                Registries.TEST_FUNCTION,
                SECURITY_BOUNDARIES_FUNCTION.identifier(),
                () -> DamageModuleSecurityGameTests::securityBoundaries
        );
    }

    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> environment =
                event.registerEnvironment(
                        id("security_environment"),
                        new TestEnvironmentDefinition.AllOf(List.of())
                );
        TestData<Holder<TestEnvironmentDefinition<?>>> data =
                new TestData<>(
                        environment,
                        Identifier.fromNamespaceAndPath(DamageModule.MODID, "test_arena"),
                        20,
                        0,
                        true,
                        Rotation.NONE
                );

        event.registerTest(
                id("security_boundaries"),
                new FunctionGameTestInstance(
                        SECURITY_BOUNDARIES_FUNCTION,
                        data
                )
        );
    }

    private static void securityBoundaries(GameTestHelper helper) {
        GameTestCodecVerifier.verifyFunctionInstance(helper);
        DamageModule.LOGGER.info(
                "[DamageModule] Executing GameTest {}",
                SECURITY_BOUNDARIES_FUNCTION.identifier()
        );
        verifyCommandPermissions();
        helper.succeed();
    }

    private static void verifyCommandPermissions() {
        var root = Commands.literal("combatnexus");

        DamageTestCommands.register(root);
        DamageItemCommands.register(root);
        DamageDamageCommands.register(root);
        DamageBypassCommands.register(root);
        DamageMobCommands.register(root);
        DamageEffectCommands.register(root);
        DamageAttributeCommands.register(root);
        DamageCleanupCommands.register(root);

        CommandDispatcher<CommandSourceStack> dispatcher =
                new CommandDispatcher<>();
        CommandNode<CommandSourceStack> built =
                dispatcher.register(root);
        DamageCommandSecurity.verifyTree(built);

        CommandSourceStack ordinary =
                Commands.createCompilationContext(
                        PermissionSet.NO_PERMISSIONS
                );
        CommandSourceStack administrator =
                Commands.createCompilationContext(
                        PermissionSet.ALL_PERMISSIONS
                );

        for (CommandNode<CommandSourceStack> child
                : built.getChildren()) {
            String branch = child.getName();

            if (child.getRequirement().test(ordinary)
                    || !child.getRequirement().test(administrator)) {
                throw new AssertionError(
                        "Invalid admin permission on branch " + branch
                );
            }

            verifyParse(
                    dispatcher,
                    ordinary,
                    branch,
                    false
            );
            verifyParse(
                    dispatcher,
                    administrator,
                    branch,
                    true
            );
        }

        for (String command : List.of(
                "combatnexus mob baseline",
                "combatnexus mob baseline at 120.5 65 200.5",
                "combatnexus mob baseline at ~ ~1 ~ immortal",
                "combatnexus mob baseline at ^ ^1 ^ immortal",
                "combatnexus mob zombie at ~ ~ ~ immortal",
                "combatnexus mob cow at ~ ~ ~ immortal",
                "combatnexus mob spider at ~ ~ ~ immortal",
                "combatnexus mob iron at ~ ~ ~ immortal",
                "combatnexus mob diamond at ~ ~ ~ immortal",
                "combatnexus mob netherite_prot at ~ ~ ~ immortal",
                "combatnexus mob low_hp at ~ ~ ~ immortal",
                "combatnexus mob invul at ~ ~ ~ immortal",
                "combatnexus mob zombie mutation resistance fire 50",
                "combatnexus mob zombie at ~ ~ ~ mutation resistance physical 100",
                "combatnexus mob zombie at ~ ~ ~ immortal "
                        + "mutation resistance magic 75",
                "combatnexus mob mortalize @e[tag="
                        + "combatnexus_test_immortal]",
                "combatnexus item conditional target_health_above_80",
                "combatnexus item conditional kit",
                "combatnexus test targets defense at ~ ~ ~",
                "combatnexus test targets defense at ~ ~ ~ immortal"
        )) {
            var parse = dispatcher.parse(command, administrator);
            if (parse.getReader().canRead()
                    || !parse.getExceptions().isEmpty()) {
                throw new AssertionError(
                        "Required command syntax did not parse: " + command
                );
            }
        }

        var unclassified = Commands.literal("root")
                .then(Commands.literal("future_branch"));
        boolean rejected = false;

        try {
            DamageCommandSecurity.verifyTree(unclassified.build());
        } catch (IllegalStateException expected) {
            rejected = true;
        }

        if (!rejected) {
            throw new AssertionError(
                    "An unprotected command branch was accepted"
            );
        }

        var protectedFuture = Commands.literal("root")
                .then(Commands.literal("future_branch")
                        .requires(DamageCommandSecurity
                                .adminPermission()));
        DamageCommandSecurity.verifyTree(protectedFuture.build());
    }

    private static void verifyParse(
            CommandDispatcher<CommandSourceStack> dispatcher,
            CommandSourceStack source,
            String path,
            boolean expectedToConsume
    ) {
        var result = dispatcher.parse(
                "combatnexus " + path,
                source
        );
        boolean consumed = !result.getReader().canRead();

        if (consumed != expectedToConsume) {
            throw new AssertionError(
                    "Unexpected command visibility for /combatnexus "
                            + path
                            + ": consumed="
                            + consumed
                            + " expected="
                            + expectedToConsume
            );
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(DamageModule.MODID, path);
    }
}
