package io.github.naimjeg.combatnexus.combat.damage.platform.registry;

import io.github.naimjeg.combatnexus.combat.damage.platform.bootstrap.DamageModule;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * DamageModule item registry.
 *
 * <p>The pedestal has a normal {@link BlockItem} so {@code /give} and block
 * placement work. There is no creative tab in this phase; the item is still
 * fully obtainable through commands.</p>
 */
public final class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(DamageModule.MODID);

    public static final DeferredItem<BlockItem> DAMAGE_DUMMY =
            ITEMS.registerSimpleBlockItem(
                    "damage_dummy",
                    ModBlocks.DAMAGE_DUMMY
            );

    private ModItems() {
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
