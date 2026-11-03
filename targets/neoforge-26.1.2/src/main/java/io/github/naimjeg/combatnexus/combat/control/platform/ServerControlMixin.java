package io.github.naimjeg.combatnexus.combat.control.platform;

import io.github.naimjeg.combatnexus.combat.api.action.CombatActionGate;
import static io.github.naimjeg.combatnexus.effect.api.EffectSnapshot.RestrictedAction.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerControlMixin {
    @Shadow public ServerPlayer player;
    @ModifyVariable(method = "handleMovePlayer", at = @At("HEAD"), argsOnly = true)
    private ServerboundMovePlayerPacket combatnexus$movement(ServerboundMovePlayerPacket packet) {
        if (!player.level().getServer().isSameThread()) return packet;
        boolean walk = CombatActionGate.allows(player, WALK), jump = CombatActionGate.allows(player, JUMP), turn = CombatActionGate.allows(player, TURN);
        if (walk && jump && turn) return packet;
        double x = packet.getX(player.getX()), y = packet.getY(player.getY()), z = packet.getZ(player.getZ());
        float yaw = packet.getYRot(player.getYRot()), pitch = packet.getXRot(player.getXRot());
        // Leave invalid values to vanilla's validation instead of sanitizing them away.
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch)) return packet;
        return new ServerboundMovePlayerPacket.PosRot(walk ? x : player.getX(), jump ? y : Math.min(y, player.getY()),
                walk ? z : player.getZ(), turn ? yaw : player.getYRot(), turn ? pitch : player.getXRot(),
                packet.isOnGround(), packet.horizontalCollision());
    }
}
