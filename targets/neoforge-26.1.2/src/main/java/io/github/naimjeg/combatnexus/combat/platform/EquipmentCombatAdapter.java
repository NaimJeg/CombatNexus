package io.github.naimjeg.combatnexus.combat.platform;

import io.github.naimjeg.combatnexus.combat.api.platform.EquipmentDamageAccess;
import io.github.naimjeg.combatnexus.combat.platform.api.CombatRegisterEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "combatnexus")
public final class EquipmentCombatAdapter {
    private EquipmentCombatAdapter() {}
    private static EquipmentDamageAccess access;
    private static io.github.naimjeg.combatnexus.combat.api.platform.NativeEffectsAccess effects;
    public static io.github.naimjeg.combatnexus.combat.api.platform.NativeEffectsAccess effects() { return java.util.Objects.requireNonNull(effects, "Effect adapter not registered"); }
    private static io.github.naimjeg.combatnexus.combat.api.platform.EffectOriginAccess origins;
    public static io.github.naimjeg.combatnexus.combat.api.platform.EffectOriginAccess effectOrigins() { return java.util.Objects.requireNonNull(origins); }
    private static io.github.naimjeg.combatnexus.combat.api.platform.NativeDamageAccess nativeAccess;
    public static io.github.naimjeg.combatnexus.combat.api.platform.NativeDamageAccess nativeDamage() { return java.util.Objects.requireNonNull(nativeAccess, "Native adapter not registered"); }
    @SubscribeEvent public static void register(CombatRegisterEvent event) { access = event.registrar().equipmentDamage(); nativeAccess = event.registrar().nativeDamage(); origins = event.registrar().effectOrigins(); effects = event.registrar().nativeEffects(); }
    public static EquipmentDamageAccess access() { return java.util.Objects.requireNonNull(access, "Equipment adapter not registered"); }
}
