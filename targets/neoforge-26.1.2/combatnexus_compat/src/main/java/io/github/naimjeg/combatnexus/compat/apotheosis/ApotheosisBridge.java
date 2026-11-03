package io.github.naimjeg.combatnexus.compat.apotheosis;

import java.util.Optional;
import java.util.Set;
import dev.shadowsoffire.apotheosis.Apoth;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import io.github.naimjeg.combatnexus.attribute.api.EffectiveAttributePolicy;
import io.github.naimjeg.combatnexus.combat.api.CombatRegistrar;
import io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability;

/** Only loaded after NeoForge has confirmed the locked optional mod is present. */
public final class ApotheosisBridge {
    private ApotheosisBridge() {}
    public static void register(CombatRegistrar registrar) {
        EquipmentFacts.register(registrar);
        registrar.effectiveAttribute(id("attribute_toggle"), 100, (entity, attribute, raw, effective) -> {
            if (entity instanceof Player player && player.getData(Apoth.Attachments.ATTRIBUTE_TOGGLES).isSuppressed(attribute))
                return Optional.of(new EffectiveAttributePolicy.Reading(effective, id("attribute_toggle")));
            return Optional.empty();
        });
        registrar.bridges().register(new BridgeCapability(id("attribute_toggle"), id("attribute_read"),
            Identifier.fromNamespaceAndPath("minecraft", "living_entity/get_attribute_value"),
            Identifier.fromNamespaceAndPath("combatnexus_compat", "apotheosis"), BridgeCapability.Mode.OBSERVE,
            Set.of(), "Native effective value captured with Apotheosis toggle attribution"));
        registrar.bridges().register(new BridgeCapability(id("executing"), id("post_attack"), id("executing/do_post_attack"),
                Identifier.fromNamespaceAndPath("combatnexus_compat", "apotheosis"), BridgeCapability.Mode.REPLACE,
                Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.LETHAL), "Execution routes through the shared lethal coordinator"));
        registrar.bridges().register(new BridgeCapability(id("mageslayer"), id("on_hurt"), id("mageslayer/on_hurt"),
                Identifier.fromNamespaceAndPath("combatnexus_compat", "apotheosis"), BridgeCapability.Mode.REPLACE,
                Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.HEALING), "Frozen child healing before the reduced parent hit"));
        registrar.bridges().register(new BridgeCapability(id("bloody_arrow"),id("projectile_launch"),id("bloody_arrow/on_projectile_fired"),
                Identifier.fromNamespaceAndPath("combatnexus_compat","apotheosis"),BridgeCapability.Mode.REPLACE,
                Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.DAMAGE,
                        io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.HEALING,
                        io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.PROJECTILE,
                        io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.STATE),"Launch snapshot, damage cost, arrow multiplier and persistent cooldown"));
    }
    static Identifier id(String path) { return Identifier.fromNamespaceAndPath("apotheosis", path); }
    static void nativeCapability(CombatRegistrar registrar, String namespace, String name) {
        var id = Identifier.fromNamespaceAndPath(namespace, name);
        registrar.bridges().register(new BridgeCapability(id, id, id,
            Identifier.fromNamespaceAndPath(namespace, "native"), BridgeCapability.Mode.NATIVE,
            Set.of(), "Native execution retained; replacement not implemented"));
    }
}
