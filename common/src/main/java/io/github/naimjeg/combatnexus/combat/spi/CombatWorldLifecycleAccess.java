package io.github.naimjeg.combatnexus.combat.spi;

import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import io.github.naimjeg.combatnexus.effect.api.EffectService;
import java.util.UUID;

/** Trusted platform lifecycle SPI; snapshots and business callbacks cannot construct its authority. */
public final class CombatWorldLifecycleAccess {
    private CombatWorldLifecycleAccess() {}
    public static final class Permit {
        private final CombatRuntime world;
        private final Thread owner = Thread.currentThread();
        private boolean active = true;
        private Permit(CombatRuntime world) { this.world = world; }
        public void require(CombatRuntime world) {
            if (!active || this.world != world || owner != Thread.currentThread())
                throw new IllegalStateException("Lifecycle authority expired or belongs to another session");
        }
    }
    public static String save(CombatRuntime world, UUID target) {
        var permit = new Permit(world);
        try { return world.saveLifecycle(target, permit); } finally { permit.active = false; io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue.drainIfSafe(); }
    }
    public static void restore(CombatRuntime world, UUID target, String value) {
        var permit = new Permit(world);
        try { world.restoreLifecycle(target, value, permit); } finally { permit.active = false; io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue.drainIfSafe(); }
    }
    public static void beginTick(CombatRuntime world) {
        var permit = new Permit(world);
        try { world.advanceLifecycle(permit); } finally { permit.active = false; io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue.drainIfSafe(); }
    }
    public static void tickRecovery(CombatRuntime world) {
        var permit = new Permit(world);
        try { world.tickRecoveryLifecycle(permit); } finally { permit.active = false; io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue.drainIfSafe(); }
    }
    public static void close(CombatRuntime world) {
        var permit = new Permit(world);
        try { world.closeLifecycle(permit); } finally { permit.active = false; io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue.drainIfSafe(); }
    }
    public static void clear(CombatRuntime world, UUID entity, EffectService.Removal reason) {
        var permit = new Permit(world);
        try { world.clearLifecycle(entity, reason, permit); } finally { permit.active = false; io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue.drainIfSafe(); }
    }
}
