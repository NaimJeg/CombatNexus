package io.github.naimjeg.combatnexus.combat.platform;
import io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class CombatDataComponents {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, "combatnexus");
    static {
        COMPONENTS.register("equipment_bindings", () -> DataComponentType.<EquipmentBindings>builder()
                .persistent(EquipmentBindings.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(EquipmentBindings.CODEC)).build());
    }
    private CombatDataComponents() {}
    public static void register(IEventBus bus) { COMPONENTS.register(bus); }
}
