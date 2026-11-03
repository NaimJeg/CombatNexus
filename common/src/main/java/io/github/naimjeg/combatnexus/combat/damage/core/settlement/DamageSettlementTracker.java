package io.github.naimjeg.combatnexus.combat.damage.core.settlement;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageFailureReason;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageApplicationPlan;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;

import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Objects;

/** Always-on, server-thread settlement correlation independent of diagnostics. */
public final class DamageSettlementTracker {
    /** Live incoming context: cancellation must be read again when hurt completes. */
    public interface Incoming {
        LivingEntity getEntity();
        DamageSource getSource();
        Object getContainer();
        boolean isCanceled();
        void setCanceled(boolean canceled);
    }

    private static final int MAX_ACTIVE_HURT_SCOPES = io.github.naimjeg.combatnexus.combat.api.transaction.CombatLimits.HARD_MAX_DEPTH + 1;
    private static final ThreadLocal<Deque<HurtScope>> ACTIVE_HURTS =
            new ThreadLocal<>();

    private DamageSettlementTracker() {
    }

    public static void applyEquipment(LivingEntity target, DamageSource source,
            java.util.function.Consumer<java.util.function.Consumer<io.github.naimjeg.combatnexus.combat.damage.api.damage.EquipmentDamageReceipt>> nativeCall) {
        var scope = matchingScope(target, source);
        if (scope == null || scope.state == null) {
            var thread = Thread.currentThread();
            boolean[] active = { true };
            try { nativeCall.accept(receipt -> {
                if (!active[0] || Thread.currentThread() != thread)
                    throw new IllegalStateException("Equipment receipt callback expired or wrong thread");
            }); } finally { active[0] = false; }
            return;
        }
        var state = scope.state;
        if (state.phase() != DamageSettlementPhase.CALCULATED)
            throw new IllegalStateException("Equipment application requires calculated native damage");
        state.capturePre(target.getHealth(), target.getAbsorptionAmount());
        var owner = Thread.currentThread();
        boolean[] active = { true };
        try {
            nativeCall.accept(receipt -> {
                if (!active[0] || Thread.currentThread() != owner || matchingScope(target, source) != scope
                        || target.level() != scope.level || state.phase() != DamageSettlementPhase.PRE_APPLY_CAPTURED)
                    throw new IllegalStateException("Equipment receipt callback expired or wrong session");
                state.recordEquipmentReceipt(receipt);
            });
            state.capturePost(0, target.getHealth(), target.getAbsorptionAmount());
        } finally { active[0] = false; }
    }

    public static float penetratedArmor(LivingEntity target, DamageSource source, float vanillaArmor) {
        var scope = matchingScope(target, source);
        var plan = scope == null || scope.state == null ? null : scope.state.calculation();
        return plan != null && Float.isFinite(plan.penetratedArmor()) ? plan.penetratedArmor() : vanillaArmor;
    }

    public static java.util.Map<net.minecraft.resources.Identifier, Float> channelDamage(LivingEntity target, DamageSource source) {
        var scope = matchingScope(target, source);
        var plan = scope == null || scope.state == null ? null : scope.state.calculation();
        return plan == null ? java.util.Map.of() : plan.resolvedChannelDamage();
    }

    public static HurtScope openHurt(
            LivingEntity target,
            ServerLevel level,
            DamageSource source
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(source, "source");

        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            ACTIVE_HURTS.set(stack);
        }
        if (stack.size() >= MAX_ACTIVE_HURT_SCOPES) {
            throw new IllegalStateException(
                    "Damage settlement hurt-scope capacity exceeded: "
                            + MAX_ACTIVE_HURT_SCOPES
            );
        }

        try {
            HurtScope scope = new HurtScope(target, level, source);
            stack.addLast(scope);
            return scope;
        } catch (RuntimeException | Error failure) {
            if (stack.isEmpty()) ACTIVE_HURTS.remove();
            throw failure;
        }
    }

    public static boolean beginIncoming(
            Incoming event,
            DamageOrigin origin
    ) {
        HurtScope scope = matchingOpenScope(
                event.getEntity(),
                event.getSource()
        );
        if (scope == null
                || scope.state != null
                || event.getEntity().level() != scope.level) {
            return false;
        }

        var kernel = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(scope.level.getServer());
        if (!origin.lineage().transaction().equals(scope.lineage))
            throw new IllegalStateException("Incoming belongs to another native invocation");
        scope.incomingEvent = event;
        scope.state = new DamageSettlementState(
                origin,
                event.getEntity(),
                scope.level
        );
        return true;
    }

    public static void recordCalculated(
            Incoming event,
            DamageApplicationPlan summary
    ) {
        stateFor(event.getContainer()).calculated(summary);
    }

    public static boolean rejectAdmission(
            Incoming event,
            DamageOrigin origin,
            DamageFailureReason reason
    ) {
        if (!beginIncoming(event, origin)) {
            return false;
        }
        stateFor(event.getContainer()).rejectAdmission(reason);
        return true;
    }

    public static boolean enforceAdmissionRejection(
            Incoming event
    ) {
        DamageSettlementState state = findState(event.getContainer());
        if (state == null || state.phase() != DamageSettlementPhase.ADMISSION_REJECTED
                && (state.calculation() == null || !state.calculation().cancelled())) {
            return false;
        }
        event.setCanceled(true);
        return true;
    }

    public static void markIncomingDispatchComplete(
            Incoming event
    ) {
        DamageSettlementState state = findState(event.getContainer());
        if (state != null && !event.isCanceled() && !state.applyBeforePlan()) event.setCanceled(true);
        if (state != null && event.isCanceled()) {
            state.markLateIncomingCancellation();
        }
    }

    public static void capturePre(Object container, LivingEntity target) {
        DamageSettlementState state = findState(container);
        if (state != null
                && state.phase() == DamageSettlementPhase.CALCULATED) {
            state.capturePre(target.getHealth(), target.getAbsorptionAmount());
        }
    }

    public static void capturePost(LivingEntity target, DamageSource source, float inflictedDamage) {
        HurtScope scope = matchingScope(
                target,
                source
        );
        if (scope == null
                || scope.state == null
                || scope.state.phase()
                != DamageSettlementPhase.PRE_APPLY_CAPTURED) {
            return;
        }
        scope.state.capturePost(inflictedDamage, target.getHealth(), target.getAbsorptionAmount());
    }

    public static boolean permitsNativeHitEffects(LivingEntity target, DamageSource source) {
        var scope = matchingScope(target, source);
        return scope == null || scope.state == null || scope.state.permitsNativeHitEffects();
    }
    public static java.util.OptionalLong currentOperation(LivingEntity target, DamageSource source) {
        var scope = matchingScope(target, source);
        if (scope != null) return java.util.OptionalLong.of(scope.lineage.id());
        if (target.level() instanceof ServerLevel level) {
            var lineage = CombatExecutionScope.currentLineage(level.getServer());
            if (lineage.isPresent()) return java.util.OptionalLong.of(lineage.get().id());
        }
        return java.util.OptionalLong.empty();
    }
    public static void recordProtection(LivingEntity target, DamageSource source,
            java.util.List<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry> entries) {
        var scope = matchingScope(target, source);
        if (scope != null && scope.state != null) scope.state.recordProtection(entries);
    }

    public static float protectHealth(LivingEntity target, DamageSource source, float before, float proposed) {
        var scope = matchingScope(target, source);
        return scope != null && scope.state != null ? scope.state.protectHealth(before, proposed) : proposed;
    }
    public static HealthWrite beginHealthWrite(LivingEntity target, DamageSource source, float before) {
        var scope = matchingScope(target, source);
        var state = scope == null ? null : scope.state;
        if (state != null) state.beginHealthWrite(before);
        return new HealthWrite(state, before, io.github.naimjeg.combatnexus.combat.spi.NativeResourceWrites.health(target));
    }
    public static final class HealthWrite implements AutoCloseable {
        private final DamageSettlementState state;
        private final float before;
        private final io.github.naimjeg.combatnexus.combat.spi.NativeResourceWrites.Token token;
        private HealthWrite(DamageSettlementState state, float before, io.github.naimjeg.combatnexus.combat.spi.NativeResourceWrites.Token token) {
            this.state = state; this.before = before; this.token = token;
        }
        public void finish(float after, boolean completed) {
            if (state != null) state.recordHealthWrite(before, after, completed, token.uncontended());
        }
        @Override public void close() { token.close(); }
    }
    public static void recordShieldReceipt(LivingEntity target, DamageSource source, io.github.naimjeg.combatnexus.combat.shield.api.ShieldCommitReceipt receipt) {
        HurtScope scope = matchingScope(target, source);
        if (scope != null && scope.state != null) scope.state.recordShieldReceipt(receipt);
    }

    public static boolean hasActiveHurt() {
        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        return stack != null && !stack.isEmpty();
    }
    public static boolean acceptsBeforePlan(java.util.UUID target) {
        var stack = ACTIVE_HURTS.get();
        return stack != null && !stack.isEmpty() && stack.peekLast().target.getUUID().equals(target)
                && stack.peekLast().state != null && stack.peekLast().state.phase() == DamageSettlementPhase.CALCULATED;
    }
    public static void recordBeforePlan(java.util.UUID target, io.github.naimjeg.combatnexus.combat.api.BeforeApplicationPlan.Result result) {
        if (!acceptsBeforePlan(target)) throw new IllegalStateException("Native before-plan parent is no longer current");
        ACTIVE_HURTS.get().peekLast().state.appendBeforeResult(result);
    }

    public static int activeDepthForTests() {
        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        return stack == null ? 0 : stack.size();
    }

    private static DamageSettlementState stateFor(Object container) {
        DamageSettlementState state = findState(container);
        if (state == null) {
            throw new IllegalStateException(
                    "DamageContainer is not associated with a managed hurt"
            );
        }
        return state;
    }

    private static @Nullable DamageSettlementState findState(
            Object container
    ) {
        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        if (stack == null) {
            return null;
        }

        Iterator<HurtScope> iterator = stack.descendingIterator();
        while (iterator.hasNext()) {
            HurtScope scope = iterator.next();
            DamageSettlementState state = scope.state;
            if (state != null && scope.incomingEvent.getContainer() == container) {
                return state;
            }
        }
        return null;
    }

    private static @Nullable HurtScope matchingOpenScope(
            LivingEntity target,
            DamageSource source
    ) {
        HurtScope scope = matchingScope(target, source);
        return scope != null && scope.state == null ? scope : null;
    }

    private static @Nullable HurtScope matchingScope(
            LivingEntity target,
            DamageSource source
    ) {
        Deque<HurtScope> stack = ACTIVE_HURTS.get();
        if (stack == null) {
            return null;
        }

        Iterator<HurtScope> iterator = stack.descendingIterator();
        while (iterator.hasNext()) {
            HurtScope scope = iterator.next();
            if (scope.target == target && scope.source == source) {
                return scope;
            }
        }
        return null;
    }

    public static final class NativeRejected extends RuntimeException {
        public NativeRejected(String reason) { super(reason, null, false, false); }
    }
    public static final class HurtScope implements AutoCloseable {
        private final CombatExecutionScope execution;
        private final io.github.naimjeg.combatnexus.combat.api.transaction.CombatLineage lineage;
        private final LivingEntity target;
        private final ServerLevel level;
        private final DamageSource source;
        private DamageSettlementState state;
        private Incoming incomingEvent;
        private boolean completed;
        private boolean closed;

        private HurtScope(
                LivingEntity target,
                ServerLevel level,
                DamageSource source
        ) {
            this.target = target;
            this.level = level;
            this.source = source;
            var kernel = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(level.getServer());
            var continuation = io.github.naimjeg.combatnexus.combat.damage.core.request.DamageRequestSubmissionTracker.enterNative(target, source);
            if (continuation != null) {
                if (CombatExecutionScope.isActive(CombatExecutionScope.Phase.COMPUTE)) throw new NativeRejected("COMPUTE");
                this.lineage = continuation.origin().lineage().transaction();
            } else {
                try { this.lineage = kernel.requestLineage(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.DAMAGE, null); }
                catch (IllegalStateException forbidden) { throw new NativeRejected(forbidden.getMessage()); }
                var kind = io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageSourcePolicy.isVanillaThorns(source)
                        ? io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind.THORNS
                        : io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla.NativeSweepScope.matches(target, source)
                        ? io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind.SWEEP
                        : io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequestKind.PRIMARY;
                var failure = io.github.naimjeg.combatnexus.combat.damage.core.request.DamageExecutionPolicy.failure(lineage, kind);
                if (failure != null) throw new NativeRejected(failure.name());
                io.github.naimjeg.combatnexus.combat.damage.core.request.DamageExecutionPolicy.constrain(lineage,
                        io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageTriggerPolicy.defaultsFor(kind));
                var admission = kernel.admit(lineage);
                if (!admission.admitted()) throw new NativeRejected(admission.rejection().name());
            }
            try { this.execution = kernel.nativeApplication(level, target, lineage); }
            catch (RuntimeException failure) { kernel.finish(lineage); throw failure; }
        }

        public @Nullable DamageSettlementCompletion complete(
                boolean vanillaAccepted
        ) {
            if (completed) {
                throw new IllegalStateException(
                        "Damage hurt scope completed more than once"
                );
            }
            completed = true;
            return state == null
                    ? null
                    : state.completeWithoutPost(vanillaAccepted, incomingEvent.isCanceled());
        }

        public @Nullable DamageSettlementCompletion abort() {
            return state == null ? null : state.abort();
        }

        public LivingEntity target() {
            return target;
        }

        public DamageSource source() {
            return source;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            Deque<HurtScope> stack = ACTIVE_HURTS.get();
            if (stack == null || stack.peekLast() != this) {
                throw new IllegalStateException(
                        "Damage hurt scope closed out of order"
                );
            }
            execution.close();
            io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(level.getServer()).finish(lineage);
            closed = true;
            stack.removeLast();
            if (stack.isEmpty()) {
                ACTIVE_HURTS.remove();
            }
        }
    }
}
