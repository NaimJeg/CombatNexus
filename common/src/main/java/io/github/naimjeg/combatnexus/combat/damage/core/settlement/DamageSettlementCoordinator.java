package io.github.naimjeg.combatnexus.combat.damage.core.settlement;

import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatCompletionQueue;
import io.github.naimjeg.combatnexus.combat.damage.core.request.DamageRequestSubmissionTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/** Damage correlation remains local; completed deliveries share the server-wide combat FIFO. */
public final class DamageSettlementCoordinator {
    private DamageSettlementCoordinator() {}
    public static void handoff(DamageSettlementCompletion completion, LivingEntity target, DamageSource source) {
        if (completion == null) return;
        DamageRequestSubmissionTracker.attachSettlement(target, source, completion);
        completion.enqueue(() -> DamageSettlementEventPublisher.publish(completion));
        drainIfSafe();
    }
    public static void drainIfSafe() { CombatCompletionQueue.drainIfSafe(); }
    public static int pendingCountForTests() { return CombatCompletionQueue.pendingCount(); }
    public static boolean drainingForTests() { return CombatCompletionQueue.draining(); }
}
