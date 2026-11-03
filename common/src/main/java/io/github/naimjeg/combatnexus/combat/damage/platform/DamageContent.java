package io.github.naimjeg.combatnexus.combat.damage.platform;

import io.github.naimjeg.combatnexus.combat.damage.block.DamageDummyBlock;
import io.github.naimjeg.combatnexus.combat.damage.block.entity.DamageDummyBlockEntity;
import io.github.naimjeg.combatnexus.combat.damage.entity.DamageDummyEntity;
import io.github.naimjeg.combatnexus.combat.damage.menu.DamageDummyMenu;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import java.util.Objects;
import java.util.function.Supplier;

/** Loader-installed registry handles. Values are resolved only when content is used. */
public final class DamageContent {
    public record Handles(Supplier<DamageDummyBlock> block,
                          Supplier<BlockEntityType<DamageDummyBlockEntity>> blockEntity,
                          Supplier<EntityType<DamageDummyEntity>> entity,
                          Supplier<MenuType<DamageDummyMenu>> menu) {
        public Handles {
            Objects.requireNonNull(block); Objects.requireNonNull(blockEntity);
            Objects.requireNonNull(entity); Objects.requireNonNull(menu);
        }
    }
    private static volatile Handles handles;
    private DamageContent() {}
    public static synchronized void install(Handles value) {
        if (handles != null) throw new IllegalStateException("Damage content already installed");
        handles = Objects.requireNonNull(value);
    }
    private static Handles handles() {
        return Objects.requireNonNull(handles, "Damage content has not been installed");
    }
    public static DamageDummyBlock block() { return handles().block().get(); }
    public static BlockEntityType<DamageDummyBlockEntity> blockEntity() { return handles().blockEntity().get(); }
    public static EntityType<DamageDummyEntity> entity() { return handles().entity().get(); }
    public static MenuType<DamageDummyMenu> menu() { return handles().menu().get(); }
}
