package io.github.naimjeg.combatnexus.damage.registry;

import io.github.naimjeg.combatnexus.damage.DamageModule;
import io.github.naimjeg.combatnexus.damage.block.entity.DamageDummyBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DamageModule block entity type registry.
 *
 * <p>{@code DamageDummyBlockEntity} is the lifecycle controller for the
 * anchored dummy; it persists only the linked entity's UUID and never
 * duplicates attribute/health state.</p>
 */
public final class ModBlockEntityTypes {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(
                    Registries.BLOCK_ENTITY_TYPE,
                    DamageModule.MODID
            );

    public static final DeferredHolder<
            BlockEntityType<?>,
            BlockEntityType<DamageDummyBlockEntity>
            > DAMAGE_DUMMY =
            BLOCK_ENTITY_TYPES.register(
                    "damage_dummy",
                    () -> new BlockEntityType<>(
                            DamageDummyBlockEntity::new,
                            ModBlocks.DAMAGE_DUMMY.get()
                    )
            );

    private ModBlockEntityTypes() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITY_TYPES.register(eventBus);
    }
}
