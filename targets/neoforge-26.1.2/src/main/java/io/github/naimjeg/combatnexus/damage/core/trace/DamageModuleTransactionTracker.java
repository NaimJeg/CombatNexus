package io.github.naimjeg.combatnexus.damage.core.trace;

import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.DamageModuleLogKind;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.TransactionDiagnosticsLog;
import io.github.naimjeg.combatnexus.damage.registry.ModAttachments;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.*;
import java.util.function.Function;

public final class DamageModuleTransactionTracker {

    private static final Map<DamageContainer, DamageModuleTransaction>
            INCOMING_CANDIDATES =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final int MAX_QUEUE_SIZE_PER_VICTIM = 64;

    private static final long MAX_TRANSACTION_AGE_TICKS = 40L;
    private static final long SAME_POST_MAX_AGE_TICKS = 2L;

    private static final float ABSOLUTE_AMOUNT_EPSILON = 0.001f;
    private static final float RELATIVE_AMOUNT_EPSILON = 0.0001f;

    private DamageModuleTransactionTracker() {
    }

    public static void recordIncomingCandidate(
            DamageContainer container,
            DamageModuleTransaction tx
    ) {
        if (!enabled() || container == null || tx == null) {
            return;
        }

        if (!isTrackableAmount(tx.finalEventAmount())) {
            return;
        }

        pruneIncomingCandidates(tx.gameTime());

        synchronized (INCOMING_CANDIDATES) {
            INCOMING_CANDIDATES.put(container, tx);
        }

        logCandidateRecorded(tx);
    }

    public static DamageModuleTransaction pollMatchingPostTrackable(
            LivingEntity victim,
            DamageSource source,
            float eventInflictedDamage
    ) {
        if (!enabled()) {
            return null;
        }

        DamageTransactionQueue txQueue =
                victim.getData(ModAttachments.DAMAGE_TRANSACTIONS);

        Deque<DamageModuleTransaction> queue =
                txQueue.entries();

        if (queue.isEmpty()) {
            return null;
        }

        Identifier wantedSourceId = sourceId(source);
        long now = victim.level().getGameTime();

        return pollMatchingPostTrackable(
                queue,
                wantedSourceId,
                now,
                tx -> sourceId(tx.source()),
                source,
                eventInflictedDamage
        );
    }

    static DamageModuleTransaction pollMatchingPostTrackable(
            Deque<DamageModuleTransaction> queue,
            Identifier wantedSourceId,
            long now,
            Function<DamageModuleTransaction, Identifier> transactionSourceId,
            DamageSource source,
            float eventInflictedDamage
    ) {
        dropExpired(queue, now, source, eventInflictedDamage);

        if (queue.isEmpty()) {
            return null;
        }

        DamageModuleTransaction exact =
                findLifoPreAmountCandidate(
                        queue,
                        wantedSourceId,
                        transactionSourceId,
                        eventInflictedDamage
                );

        if (exact != null) {
            removeCandidate(queue, exact);
            if (DamageModuleSettings.fullTraceEnabled()) {
                TransactionDiagnosticsLog.postCorrelated(
                        exact,
                        "LIFO_PRE_AMOUNT_HEURISTIC",
                        queue.size()
                );
            }
            return exact;
        }

        DamageModuleTransaction lateAmount =
                findRecentSameSourceCandidate(
                        queue,
                        wantedSourceId,
                        now,
                        transactionSourceId
                );

        if (lateAmount != null) {
            LateMatchKind lateMatchKind =
                    classifyLateAmountMatch(lateAmount, eventInflictedDamage);

            removeCandidate(queue, lateAmount);

            logLateAmountMatch(
                    lateAmount,
                    source,
                    eventInflictedDamage,
                    lateMatchKind,
                    0
            );

            return lateAmount;

        }

        return null;
    }

    /*
     * NeoForge Post does not expose its DamageContainer. When nested entries
     * coexist, damage processing unwinds LIFO; source and Pre amount validate
     * that ordering but are not an identity key.
     */
    private static DamageModuleTransaction findLifoPreAmountCandidate(
            Deque<DamageModuleTransaction> queue,
            Identifier wantedSourceId,
            Function<DamageModuleTransaction, Identifier> transactionSourceId,
            float eventInflictedDamage
    ) {
        DamageModuleTransaction last = null;

        for (DamageModuleTransaction tx : queue) {
            if (!transactionSourceId.apply(tx).equals(wantedSourceId)) {
                continue;
            }

            if (amountClose(tx.preNewDamage(), eventInflictedDamage)) {
                last = tx;
            }
        }

        return last;
    }

    private static DamageModuleTransaction findRecentSameSourceCandidate(
            Deque<DamageModuleTransaction> queue,
            Identifier wantedSourceId,
            long now,
            Function<DamageModuleTransaction, Identifier> transactionSourceId
    ) {
        DamageModuleTransaction candidate = null;

        for (DamageModuleTransaction tx : queue) {
            if (!transactionSourceId.apply(tx).equals(wantedSourceId)) {
                continue;
            }

            if (now - tx.gameTime() > SAME_POST_MAX_AGE_TICKS) {
                continue;
            }

            if (candidate != null) {
                logAmbiguousLateAmountMatch(
                        candidate,
                        tx,
                        wantedSourceId
                );

                return null;
            }

            candidate = tx;
        }

        return candidate;
    }

    private static boolean removeCandidate(
            Deque<DamageModuleTransaction> queue,
            DamageModuleTransaction candidate
    ) {
        Iterator<DamageModuleTransaction> iterator =
                queue.iterator();

        while (iterator.hasNext()) {
            DamageModuleTransaction tx = iterator.next();
            if (tx == candidate) {
                iterator.remove();
                return true;
            }
        }

        return false;
    }

    private static void dropExpired(
            Deque<DamageModuleTransaction> queue,
            long now,
            DamageSource wantedSource,
            float eventInflictedDamage
    ) {
        Iterator<DamageModuleTransaction> iterator =
                queue.iterator();

        while (iterator.hasNext()) {
            DamageModuleTransaction tx = iterator.next();

            if (now - tx.gameTime() <= MAX_TRANSACTION_AGE_TICKS) {
                continue;
            }

            iterator.remove();

            logDrop(
                    "expired",
                    tx,
                    wantedSource,
                    eventInflictedDamage
            );
        }
    }

    private static boolean amountClose(float a, float b) {
        float diff = Math.abs(a - b);

        if (diff <= ABSOLUTE_AMOUNT_EPSILON) {
            return true;
        }

        float scale = Math.max(Math.abs(a), Math.abs(b));

        return diff <= scale * RELATIVE_AMOUNT_EPSILON;
    }

    private static Identifier sourceId(DamageSource source) {
        return source.typeHolder()
                .unwrapKey()
                .map(key -> key.identifier())
                .orElseGet(() -> Identifier.fromNamespaceAndPath(
                        "unknown",
                        sanitizePath(source.type().msgId())
                ));
    }

    private static String sanitizePath(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9_./-]", "_");
    }

    private static void logDrop(
            String reason,
            DamageModuleTransaction tx,
            DamageSource wantedSource,
            float eventInflictedDamage
    ) {
        if (!DamageModuleSettings.shouldEmitServer(dropKind(reason))) {
            return;
        }

        TransactionDiagnosticsLog.drop(
                reason,
                tx,
                wantedSource,
                eventInflictedDamage
        );
    }

    private static void logLateAmountMatch(
            DamageModuleTransaction tx,
            DamageSource wantedSource,
            float eventInflictedDamage,
            LateMatchKind kind,
            int staleDropped
    ) {
        if (!DamageModuleSettings.compatibilityDiagnosticsEnabled()) {
            return;
        }

        String label =
                kind == LateMatchKind.PRE_TO_POST_ADJUSTED
                        ? "PRE_TO_POST_ADJUSTMENT"
                        : "AMOUNT_CHANGED";

        TransactionDiagnosticsLog.lateAmountMatch(
                label,
                tx,
                wantedSource,
                eventInflictedDamage,
                Math.abs(tx.eventAmountAfterSet() - eventInflictedDamage),
                staleDropped
        );
    }

    public static DamageModuleTransaction promoteIncomingCandidate(
            DamageContainer container,
            LivingDamageEvent.Pre event
    ) {
        if (!enabled() || container == null || event == null) {
            return null;
        }

        LivingEntity victim = event.getEntity();
        long now = victim.level().getGameTime();

        pruneIncomingCandidates(now);

        DamageModuleTransaction incoming;

        synchronized (INCOMING_CANDIDATES) {
            incoming = INCOMING_CANDIDATES.remove(container);
        }

        if (incoming == null) {
            logMissingCandidateAtPre(event);
            return null;
        }

        if (!isTrackableAmount(event.getNewDamage())) {
            logDrop(
                    "pre_zero_or_invalid_damage",
                    incoming,
                    event.getSource(),
                    event.getNewDamage()
            );
            return null;
        }

        DamageModuleTransaction promoted =
                withPreSnapshot(incoming, event);

        logCandidatePromoted(promoted);

        recordPostTrackable(promoted);

        return promoted;
    }

    private static void logAmbiguousLateAmountMatch(
            DamageModuleTransaction first,
            DamageModuleTransaction second,
            Identifier wantedSourceId
    ) {
        TransactionDiagnosticsLog.ambiguousLateAmountMatch(
                wantedSourceId.toString(),
                first,
                second
        );
    }

    private static LateMatchKind classifyLateAmountMatch(
            DamageModuleTransaction tx,
            float eventInflictedDamage
    ) {
        if (tx.victimInvulnerableTimeBefore() > 0
                && eventInflictedDamage + ABSOLUTE_AMOUNT_EPSILON
                < tx.preNewDamage()) {
            return LateMatchKind.PRE_TO_POST_ADJUSTED;
        }

        return LateMatchKind.AMOUNT_CHANGED;
    }

    private static DamageModuleTransaction withPreSnapshot(
            DamageModuleTransaction incoming,
            LivingDamageEvent.Pre event
    ) {
        LivingEntity victim = event.getEntity();

        return new DamageModuleTransaction(
                incoming.damageId(),
                incoming.attacker(),
                incoming.victim(),
                incoming.source(),

                incoming.eventOriginalAmount(),
                incoming.initialBaseAmount(),
                incoming.offensiveTotal(),
                incoming.finalEventAmount(),

                incoming.eventAmountBeforeSet(),
                incoming.eventAmountAfterSet(),

                event.getNewDamage(),
                event.getContainer().getBlockedDamage(),
                event.getContainer().getReduction(DamageContainer.Reduction.INVULNERABILITY),
                event.getContainer().getReduction(DamageContainer.Reduction.ARMOR),
                event.getContainer().getReduction(DamageContainer.Reduction.ENCHANTMENTS),
                event.getContainer().getReduction(DamageContainer.Reduction.MOB_EFFECTS),
                event.getContainer().getReduction(DamageContainer.Reduction.INNATE_RESISTANCE),
                hasKnownPreStageAdjustment(event),

                victim.getHealth(),
                victim.getAbsorptionAmount(),
                victim.invulnerableTime,
                victim.level().getGameTime(),

                incoming.vanillaReductionMode(),
                incoming.suppressArmor(),
                incoming.suppressEnchantments(),
                incoming.suppressMobEffects(),
                incoming.suppressInnateResistance()
        );
    }

    private static void recordPostTrackable(
            DamageModuleTransaction tx
    ) {
        DamageTransactionQueue txQueue =
                tx.victim().getData(ModAttachments.DAMAGE_TRANSACTIONS);

        Deque<DamageModuleTransaction> queue =
                txQueue.entries();

        queue.addLast(tx);

        for (DamageModuleTransaction dropped : trimToCapacity(queue)) {
            logDrop(
                    "post_queue_overflow",
                    dropped,
                    null,
                    Float.NaN
            );
        }
    }

    private static boolean hasKnownPreStageAdjustment(
            LivingDamageEvent.Pre event
    ) {
        LivingEntity victim = event.getEntity();
        DamageSource source = event.getSource();
        return source.is(DamageTypeTags.DAMAGES_HELMET)
                && !victim.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || source.is(DamageTypeTags.IS_FREEZING)
                && victim.is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES);
    }

    static List<DamageModuleTransaction> trimToCapacity(
            Deque<DamageModuleTransaction> queue
    ) {
        List<DamageModuleTransaction> dropped = new ArrayList<>();
        while (queue.size() > MAX_QUEUE_SIZE_PER_VICTIM) {
            dropped.add(queue.removeFirst());
        }
        return List.copyOf(dropped);
    }

    private static boolean isTrackableAmount(float amount) {
        return Float.isFinite(amount) && amount > ABSOLUTE_AMOUNT_EPSILON;
    }

    private static void logCandidateRecorded(DamageModuleTransaction tx) {
        if (!DamageModuleSettings.fullTraceEnabled()) {
            return;
        }

        TransactionDiagnosticsLog.candidateRecord(tx);
    }

    private static void logCandidatePromoted(DamageModuleTransaction promoted) {
        if (!DamageModuleSettings.summaryTraceEnabled()) {
            return;
        }

        TransactionDiagnosticsLog.candidatePromote(promoted);
    }

    private static void logMissingCandidateAtPre(LivingDamageEvent.Pre event) {
        TransactionDiagnosticsLog.preWithoutCandidate(
                event.getEntity(),
                event.getSource(),
                event.getNewDamage(),
                event.getEntity().level().getGameTime()
        );
    }

    private static void pruneIncomingCandidates(long now) {
        int removed = 0;
        int remaining;

        synchronized (INCOMING_CANDIDATES) {
            var iterator = INCOMING_CANDIDATES.entrySet().iterator();

            while (iterator.hasNext()) {
                DamageModuleTransaction tx =
                        iterator.next().getValue();

                if (tx == null
                        || tx.victim() == null
                        || tx.victim().isRemoved()
                        || now - tx.gameTime() > MAX_TRANSACTION_AGE_TICKS) {
                    iterator.remove();
                    removed++;
                }
            }

            remaining = INCOMING_CANDIDATES.size();
        }

        if (DamageModuleSettings.fullTraceEnabled()) {
            TransactionDiagnosticsLog.candidatePrune(
                    removed,
                    remaining,
                    now
            );
        }
    }

    public static boolean enabled() {
        return DamageModuleSettings.transactionTrackingEnabled();
    }

    private static DamageModuleLogKind dropKind(String reason) {
        if (reason != null && reason.startsWith("stale_before")) {
            return DamageModuleLogKind.COMPATIBILITY;
        }

        return DamageModuleLogKind.TRACE_DETAIL;
    }

    private enum LateMatchKind {
        AMOUNT_CHANGED,
        PRE_TO_POST_ADJUSTED
    }
}

