package io.github.naimjeg.combatnexus.combat.spi;

import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.combat.healing.api.*;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatDispatchScope;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;

/** Trusted native entry adapter; no nested admission authority is exposed. */
public final class NativeHealingAccess {
    private NativeHealingAccess() {}
    public enum Route { MANAGED, REJECTED_PHASE }
    public static Route route() {
        try { CombatExecutionScope.requireSubmissionPhase(); return Route.MANAGED; }
        catch (IllegalStateException invalid) { return Route.REJECTED_PHASE; }
    }
    public static HealingSettlement submit(CombatRuntime world, LivingEntity target, float amount, NativeHealingSource source) {
        if (route() != Route.MANAGED) throw new IllegalStateException("Native healing is not at a permitted entry");
        if (target.level() != world.level()) throw new IllegalArgumentException("Wrong healing world");
        return world.healing().submit(new HealingRequest(UUID.randomUUID(), target.getUUID(), source.source(), source.reason(), amount));
    }
}
