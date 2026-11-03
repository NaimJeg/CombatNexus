package io.github.naimjeg.combatnexus.presentation.platform.client;
import io.github.naimjeg.combatnexus.presentation.network.CombatControlPayload;
import io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = "combatnexus", value = Dist.CLIENT)
public final class CombatControlClient {
    private static CombatControlPayload state;
    private static Object level, player;
    private static void reconcile() {
        var mc = Minecraft.getInstance();
        if (level != mc.level || player != mc.player) { state = null; level = mc.level; player = mc.player; }
    }
    public static void accept(CombatControlPayload value) {
        reconcile(); var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.player.getId() != value.entityId()
                || !mc.level.dimension().identifier().equals(value.dimension())) return;
        if (state != null && (!state.session().equals(value.session()) || value.tick() < state.tick())) return;
        state = value;
    }
    public static boolean allowed(RestrictedAction action) { reconcile(); return state == null || (state.mask() & (1 << action.ordinal())) == 0; }
    public static void register() {
        io.github.naimjeg.combatnexus.combat.api.action.CombatActionGate.clientView((entity, action) -> entity != Minecraft.getInstance().player || allowed(action));
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { state = null; level = null; player = null; }
    @SubscribeEvent public static void input(MovementInputUpdateEvent event) {
        var input = event.getInput(); var old = input.keyPresses;
        boolean walk = allowed(RestrictedAction.WALK);
        input.keyPresses = new net.minecraft.world.entity.player.Input(walk && old.forward(), walk && old.backward(), walk && old.left(), walk && old.right(),
                allowed(RestrictedAction.JUMP) && old.jump(), old.shift(), walk && old.sprint());
    }
    @SubscribeEvent public static void action(InputEvent.InteractionKeyMappingTriggered event) {
        if (event.isAttack() && Minecraft.getInstance().hitResult instanceof net.minecraft.world.phys.EntityHitResult
                && !allowed(RestrictedAction.ATTACK)) {
            event.setCanceled(true); event.setSwingHand(false);
        }
    }
}
