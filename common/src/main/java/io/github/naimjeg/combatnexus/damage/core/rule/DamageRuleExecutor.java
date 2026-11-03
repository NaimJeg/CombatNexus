package io.github.naimjeg.combatnexus.damage.core.rule;

import io.github.naimjeg.combatnexus.damage.api.context.DamageMutationResult;
import io.github.naimjeg.combatnexus.damage.api.context.DamageRuleContext;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleCondition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleDefinition;
import io.github.naimjeg.combatnexus.damage.api.rule.DamageRuleOperation;
import io.github.naimjeg.combatnexus.damage.api.rule.RuleExecutionContext;
import io.github.naimjeg.combatnexus.damage.api.rule.RuntimeDamageRule;
import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import io.github.naimjeg.combatnexus.damage.core.contribution.DamageRuleContributionRecorder;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageModuleContext;
import io.github.naimjeg.combatnexus.damage.core.pipeline.DamageRuleContextViews;
import io.github.naimjeg.combatnexus.damage.core.trace.RuleSkipReason;
import io.github.naimjeg.combatnexus.damage.core.util.StrictCallbackFailure;
import io.github.naimjeg.combatnexus.damage.diagnostics.logging.RuleExecutionDiagnosticsLog;

public final class DamageRuleExecutor {

    private DamageRuleExecutor() {
    }

    public static void execute(
            DamageModuleContext ctx,
            DamagePhase runningPhase,
            RuntimeDamageRule runtimeRule
    ) {
        DamageRuleDefinition rule = runtimeRule.definition();

        if (rule.phase() != runningPhase) {
            ctx.trace().rules().phaseMismatch(
                    runningPhase,
                    rule
            );
            return;
        }

        if (!rule.role().canRunAs(runtimeRule.executionContext().role())) {
            ctx.trace().rules().roleMismatch(
                    runningPhase,
                    rule,
                    runtimeRule.executionContext()
            );
            return;
        }

        for (DamageRuleCondition condition : rule.conditions()) {
            Boolean conditionResult =
                    safeTestCondition(
                            ctx,
                            runningPhase,
                            rule,
                            condition,
                            runtimeRule
                    );

            if (conditionResult == null) {
                /*
                 * Tolerant mode:
                 * A throwing condition invalidates this rule only.
                 */
                return;
            }

            if (!conditionResult) {
                ctx.trace().rules().conditionFailed(
                        runningPhase,
                        rule,
                        condition
                );
                return;
            }
        }

        if (rule.operations().isEmpty()) {
            ctx.trace().rules().skipped(
                    runningPhase,
                    rule,
                    RuleSkipReason.EMPTY_OPERATIONS
            );
            return;
        }

        ctx.trace().rules().executed(
                runningPhase,
                rule
        );

        int appliedOperations = 0;
        int rejectedOperations = 0;
        int noOpOperations = 0;
        int operationIndex = 0;

        for (DamageRuleOperation operation : rule.operations()) {
            if (!isOperationAllowedInPhase(operation, runningPhase)) {
                ctx.trace().rules().skipped(
                        runningPhase,
                        rule,
                        RuleSkipReason.PHASE_OPERATION_MISMATCH
                );

                DamageRuleContributionRecorder.recordOperation(
                        ctx,
                        runningPhase,
                        runtimeRule,
                        operation,
                        DamageMutationResult.REJECTED_WRONG_PHASE,
                        operationIndex
                );

                rejectedOperations++;
                operationIndex++;
                continue;
            }

            DamageMutationResult result =
                    safeApplyOperation(
                            ctx,
                            runningPhase,
                            rule,
                            operation
                    );

            DamageRuleContributionRecorder.recordOperation(
                    ctx,
                    runningPhase,
                    runtimeRule,
                    operation,
                    result,
                    operationIndex
            );

            if (result.applied()) {
                appliedOperations++;
            } else if (result.rejected()) {
                rejectedOperations++;
            } else {
                noOpOperations++;
            }

            operationIndex++;

            if (ctx.isDamageCancelled()) {
                break;
            }
        }

        ctx.trace().rules().result(
                runningPhase,
                rule,
                appliedOperations,
                rejectedOperations,
                noOpOperations
        );
    }

    public static boolean isOperationAllowedInPhase(
            DamageRuleOperation operation,
            DamagePhase phase
    ) {
        return operation.supportsPhase(phase);
    }

    private static Boolean safeTestCondition(
            DamageModuleContext ctx,
            DamagePhase runningPhase,
            DamageRuleDefinition rule,
            DamageRuleCondition condition,
            RuntimeDamageRule runtimeRule
    ) {
        DamageModuleContext.MutableStateCheckpoint checkpoint =
                ctx.checkpointMutableState();
        try {
            return invokeCondition(
                    ctx,
                    condition,
                    runtimeRule.executionContext()
            );
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);
            handleRuleFailure(
                    ctx,
                    runningPhase,
                    rule,
                    "condition/" + safeConditionType(condition),
                    exception
            );

            return null;
        }
    }

    private static DamageMutationResult safeApplyOperation(
            DamageModuleContext ctx,
            DamagePhase runningPhase,
            DamageRuleDefinition rule,
            DamageRuleOperation operation
    ) {
        DamageModuleContext.MutableStateCheckpoint checkpoint =
                ctx.checkpointMutableState();
        try {
            return invokeOperation(ctx, runningPhase, operation);
        } catch (Exception exception) {
            ctx.rollbackMutableState(checkpoint);
            handleRuleFailure(
                    ctx,
                    runningPhase,
                    rule,
                    "operation/" + safeOperationType(operation),
                    exception
            );

            return DamageMutationResult.REJECTED_RULE_EXCEPTION;
        }
    }

    static boolean invokeCondition(
            DamageRuleContext context,
            DamageRuleCondition condition,
            RuleExecutionContext executionContext
    ) {
        return condition.test(
                DamageRuleContextViews.restricted(context),
                executionContext
        );
    }

    static DamageMutationResult invokeOperation(
            DamageRuleContext context,
            DamagePhase phase,
            DamageRuleOperation operation
    ) {
        if (!isOperationAllowedInPhase(operation, phase)) {
            return DamageMutationResult.REJECTED_WRONG_PHASE;
        }

        return operation.apply(DamageRuleContextViews.restricted(context));
    }

    private static void handleRuleFailure(
            DamageModuleContext ctx,
            DamagePhase runningPhase,
            DamageRuleDefinition rule,
            String stage,
            Exception exception
    ) {
        RuleExecutionDiagnosticsLog.error(
                rule.id(),
                runningPhase,
                stage,
                exception
        );

        if (DamageModuleSettings.strictRuleErrors()) {
            throw new StrictCallbackFailure(
                    "[DamageModule] Rule failure at "
                            + stage
                            + ": rule="
                            + rule.id()
                            + " phase="
                            + runningPhase,
                    exception
            );
        }

        ctx.trace().mutations().rejected(
                "rule_" + stage,
                runningPhase,
                "rule=" + rule.id()
                        + " threw "
                        + exception.getClass().getSimpleName()
                        + ": "
                        + exception.getMessage()
        );
    }

    private static String safeConditionType(
            DamageRuleCondition condition
    ) {
        try {
            return condition.type().toString();
        } catch (Exception ignored) {
            return "<unknown_condition>";
        }
    }

    private static String safeOperationType(
            DamageRuleOperation operation
    ) {
        try {
            return operation.type().toString();
        } catch (Exception ignored) {
            return "<unknown_operation>";
        }
    }
}

