package io.github.naimjeg.combatnexus.damage.diagnostics.logging;

import com.mojang.logging.LogUtils;
import io.github.naimjeg.combatnexus.damage.api.enums.DamagePhase;
import io.github.naimjeg.combatnexus.damage.core.DamageOperation;
import io.github.naimjeg.combatnexus.damage.core.registry.PreMultiplierBucketRegistry;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class CombatTraceState {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final long damageId;
    private List<DamageOperation> operations = null;
    private Boolean acceptsSummary = null;
    private Boolean acceptsDetail = null;

    CombatTraceState(
            long damageId
    ) {
        this.damageId = damageId;
    }

    static String fmt(float value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    static String pct(float value) {
        return String.format(Locale.ROOT, "%.3f%%", value * 100.0f);
    }

    static boolean isOffensiveOperation(DamageOperation op) {
        return op.phase() != DamagePhase.MITIGATION_SETUP
                && op.phase() != DamagePhase.FINAL_OVERRIDE;
    }

    static boolean isDefensiveOperation(DamageOperation op) {
        return op.phase() == DamagePhase.MITIGATION_SETUP
                || op.phase() == DamagePhase.FINAL_OVERRIDE;
    }

    static String formatMutationMetadata(DamageOperation op) {
        StringBuilder builder = new StringBuilder();

        if (op.applicationBucket() != null) {
            builder
                    .append(" application_bucket=")
                    .append(op.applicationBucket().name().toLowerCase(Locale.ROOT));
        }

        if (op.preMultiplierBucket()
                != DamageOperation.NO_PRE_MULTIPLIER_BUCKET) {
            builder
                    .append(" pre_bucket=")
                    .append(PreMultiplierBucketRegistry.describePreMultiplierBucket(
                            op.preMultiplierBucket()
                    ));
        }

        return builder.toString();
    }

    String prefix() {
        return "[DN#" + damageId + "]";
    }

    void info(String template, Object... args) {
        info(DamageModuleLogKind.TRACE_DETAIL, template, args);
    }

    void info(
            DamageModuleLogKind kind,
            String template,
            Object... args
    ) {
        DamageModuleLogSink.info(
                kind,
                LOGGER,
                template,
                args
        );
    }

    void addOperation(DamageOperation operation) {
        if (operations == null) {
            operations = new ArrayList<>(4);
        }

        operations.add(operation);
    }

    boolean hasOperations() {
        return operations != null && !operations.isEmpty();
    }

    Iterable<DamageOperation> operations() {
        return operations != null ? operations : List.of();
    }

    int operationCount() {
        return operations == null ? 0 : operations.size();
    }

    void rollbackOperations(int checkpoint) {
        int size = operationCount();
        if (checkpoint < 0 || checkpoint > size) {
            throw new IllegalArgumentException(
                    "Invalid combat trace checkpoint: " + checkpoint
            );
        }
        if (operations != null) {
            operations.subList(checkpoint, operations.size()).clear();
        }
    }

    boolean accepts(DamageModuleLogKind kind) {
        DamageModuleLogKind effectiveKind =
                kind == null ? DamageModuleLogKind.TRACE_DETAIL : kind;

        if (effectiveKind == DamageModuleLogKind.TRACE_SUMMARY) {
            if (acceptsSummary == null) {
                acceptsSummary = DamageModuleLogSink.shouldAccept(
                        effectiveKind
                );
            }

            return acceptsSummary;
        }

        if (effectiveKind == DamageModuleLogKind.TRACE_DETAIL) {
            if (acceptsDetail == null) {
                acceptsDetail = DamageModuleLogSink.shouldAccept(
                        effectiveKind
                );
            }

            return acceptsDetail;
        }

        return DamageModuleLogSink.shouldAccept(effectiveKind);
    }
}

