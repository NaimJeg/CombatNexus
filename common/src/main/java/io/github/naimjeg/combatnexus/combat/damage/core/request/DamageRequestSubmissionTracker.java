package io.github.naimjeg.combatnexus.combat.damage.core.request;
import io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration;

import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageFailureReason;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageRequest;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageResult;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageOrigin;
import io.github.naimjeg.combatnexus.combat.damage.core.pipeline.DamageApplicationPlan;
import io.github.naimjeg.combatnexus.combat.damage.core.settlement.DamageSettlementCompletion;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Short-lived server-thread association between a public request and the
 * vanilla Incoming event created by its hurtServer call.
 */
public final class DamageRequestSubmissionTracker {

    private static final ThreadLocal<Deque<Submission>> ACTIVE =
            new ThreadLocal<>();

    private DamageRequestSubmissionTracker() {
    }

    static Submission open(
            DamageRequest request,
            DamageSource source,
            DamageAdmissionResult admission,
            DamageOrigin origin, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions
    ) {
        if (!Objects.requireNonNull(admission, "admission").admitted()) {
            throw new IllegalArgumentException(
                    "A rejected admission cannot open a submission"
            );
        }
        Submission submission = new Submission(
                request, source, admission, origin, definitions
        );
        Deque<Submission> stack = ACTIVE.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            ACTIVE.set(stack);
        }

        stack.addLast(submission);
        return submission;
    }

    public static boolean hasActiveSubmission() {
        Deque<Submission> stack = ACTIVE.get();
        return stack != null && !stack.isEmpty();
    }
    public static boolean independentCooldown(LivingEntity target, DamageSource source) {
        var stack = ACTIVE.get(); var submission = stack == null ? null : stack.peekLast();
        return submission != null && submission.request.target() == target && submission.source == source
            && submission.request.cooldownPolicy() == io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageCooldownPolicy.INDEPENDENT;
    }

    /**
     * Returns whether the current innermost public submission owns this exact
     * target/source pair. This is an identity check used by the hurtServer
     * boundary; it is deliberately narrower than {@link #hasActiveSubmission()}.
     */
    @ApiStatus.Internal
    public static boolean matchesActiveSubmission(
            LivingEntity target,
            DamageSource source
    ) {
        if (target == null || source == null) {
            return false;
        }
        Deque<Submission> stack = ACTIVE.get();
        Submission submission = stack == null ? null : stack.peekLast();
        return submission != null
                && submission.request.target() == target
                && submission.source == source && !submission.nativeEntered;
    }

    /** The exact public continuation may enter once; nested native calls cannot steal it. */
    public static @Nullable Claim enterNative(LivingEntity target, DamageSource source) {
        if (!matchesActiveSubmission(target, source)) return null;
        var submission = ACTIVE.get().peekLast();
        submission.nativeEntered = true;
        return new Claim(submission.request, submission.origin, submission.admission, submission.definitions);
    }
    public static boolean attachSettlement(
            net.minecraft.world.entity.LivingEntity target,
            DamageSource source,
            DamageSettlementCompletion completion
    ) {
        Deque<Submission> stack = ACTIVE.get();
        if (stack == null) {
            return false;
        }
        Submission submission = stack.peekLast();
        if (submission == null
                || submission.source != source
                || submission.request.target() != target) {
            return false;
        }
        if (submission.settlement != null) {
            throw new IllegalStateException(
                    "Damage request settlement was attached more than once"
            );
        }
        if (!completion.snapshot().lineage().equals(
                submission.origin.lineage()
        )) {
            return false; // A nested native call can legally reuse the same target/source objects.
        }
        if (completion.snapshot().origin() != submission.origin) {
            throw new IllegalStateException(
                    "Damage request settlement did not retain the authoritative origin"
            );
        }
        submission.settlement = Objects.requireNonNull(
                completion,
                "completion"
        );
        return true;
    }

    public static @Nullable Claim claimIncoming(
            LivingEntity target, DamageSource source
    ) {
        Submission submission = matching(target, source);
        if (submission == null) {
            return null;
        }

        if (submission.claimed || !CombatExecutionScope.currentLineage(target.level().getServer())
                .map(id -> id.equals(submission.origin.lineage().transaction())).orElse(false)) return null;
        submission.claimed = true;
        return new Claim(
                submission.request,
                submission.origin,
                submission.admission, submission.definitions
        );
    }

    public static void recordPipelineResult(
            LivingEntity target, DamageSource source,
            DamageApplicationPlan summary
    ) {
        Submission submission = matching(target, source);
        if (submission == null) {
            return;
        }

        if (!CombatExecutionScope.currentLineage(target.level().getServer())
                .map(id -> id.equals(submission.origin.lineage().transaction())).orElse(false)) return;
        if (submission.summary != null) {
            throw new IllegalStateException(
                    "Damage request pipeline result was recorded more than once"
            );
        }

        submission.summary = Objects.requireNonNull(
                summary,
                "Damage execution summary must not be null"
        );
    }

    static int activeDepthForTests() {
        Deque<Submission> stack = ACTIVE.get();
        return stack == null ? 0 : stack.size();
    }

    private static @Nullable Submission matching(
            LivingEntity target, DamageSource source
    ) {
        if (target == null || source == null) {
            return null;
        }

        Deque<Submission> stack = ACTIVE.get();
        if (stack == null) {
            return null;
        }
        Submission submission = stack.peekLast();

        if (submission == null
                || submission.source != source
                || submission.request.target() != target) {
            return null;
        }

        return submission;
    }

    static final class Submission implements AutoCloseable {

        private final CombatExecutionScope execution;
        private final DamageRequest request;
        private final DamageSource source;
        private final DamageAdmissionResult admission;
        private final DamageOrigin origin;
        private final io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions;
        private @Nullable DamageApplicationPlan summary;
        private @Nullable DamageSettlementCompletion settlement;
        private boolean claimed;
        private boolean nativeEntered;
        private boolean closed;

        private Submission(
                DamageRequest request,
                DamageSource source,
                DamageAdmissionResult admission,
                DamageOrigin origin, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions
        ) {
            this.request = Objects.requireNonNull(request, "request");
            this.source = Objects.requireNonNull(source, "source");
            this.admission = Objects.requireNonNull(admission, "admission");
            this.origin = Objects.requireNonNull(origin, "origin");
            this.definitions = Objects.requireNonNull(definitions);
            this.execution = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(request.level().getServer())
                    .ticket(origin.lineage().transaction()).enter(CombatExecutionScope.Phase.SUBMISSION);
        }

        DamageResult finish(boolean vanillaAccepted) {
            if (settlement != null) {
                return DamageResult.fromSettlement(
                        request,
                        settlement.snapshot(),
                        vanillaAccepted
                );
            }

            return DamageResult.failed(
                    request,
                    DamageFailureReason.PIPELINE_NOT_OBSERVED,
                    claimed && summary != null
                            ? "DamageModule calculated the request but no "
                                    + "completed settlement was observed"
                            : "The vanilla damage call did not enter the "
                                    + "DamageModule Incoming pipeline",
                    claimed && summary != null,
                    vanillaAccepted,
                    summary == null ? 0.0f : summary.resolvedDamage(),
                    summary != null && summary.critical(),
                    summary != null && summary.cancelled()
            );
        }

        @Nullable DamageSettlementCompletion settlement() {
            return settlement;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }

            Deque<Submission> stack = ACTIVE.get();
            if (stack == null) {
                throw new IllegalStateException(
                        "Damage request submission stack is missing"
                );
            }
            Submission active = stack.peekLast();

            if (active != this) {
                throw new IllegalStateException(
                        "Damage request submission stack closed out of order"
                );
            }

            execution.close();
            io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime.of(request.level().getServer()).finish(origin.lineage().transaction());
            stack.removeLast();
            closed = true;

            if (stack.isEmpty()) {
                ACTIVE.remove();
            }
        }
    }

    /** Public-request association claimed exactly once by its Incoming event. */
    public record Claim(
            DamageRequest request,
            DamageOrigin origin,
            DamageAdmissionResult admission, io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration definitions
    ) {
        public Claim {
            Objects.requireNonNull(request, "request");
            Objects.requireNonNull(origin, "origin");
            if (!Objects.requireNonNull(admission, "admission").admitted()) {
                throw new IllegalArgumentException(
                        "Incoming claim requires an admitted request"
                );
            }
        }
    }
}
