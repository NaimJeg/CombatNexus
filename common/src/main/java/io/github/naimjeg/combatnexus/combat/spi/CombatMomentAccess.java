package io.github.naimjeg.combatnexus.combat.spi;
import net.minecraft.server.level.ServerLevel;
import io.github.naimjeg.combatnexus.combat.api.action.CombatMoment;
/** Trusted platform publisher; observers receive no publication capability. */
public final class CombatMomentAccess {
    private CombatMomentAccess() {}
    public static void publish(ServerLevel level,CombatMoment moment) { io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.moment(level,moment); }
}
