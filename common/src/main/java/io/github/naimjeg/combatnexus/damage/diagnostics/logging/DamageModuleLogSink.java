package io.github.naimjeg.combatnexus.damage.diagnostics.logging;

import io.github.naimjeg.combatnexus.damage.core.config.DamageModuleSettings;
import org.slf4j.Logger;

/** Mode-gated server diagnostic output. Operational warnings/errors bypass this sink. */
public final class DamageModuleLogSink {

    private DamageModuleLogSink() {
    }

    public static void info(Logger logger, String template, Object... args) {
        info(DamageModuleLogKind.TRACE_DETAIL, logger, template, args);
    }

    public static void info(
            DamageModuleLogKind kind,
            Logger logger,
            String template,
            Object... args
    ) {
        if (!shouldAccept(kind)) {
            return;
        }

        logger.info(
                DiagnosticTextSanitizer.sanitizeLine(template),
                DiagnosticTextSanitizer.sanitizeArguments(args)
        );
    }

    public static boolean shouldAccept(DamageModuleLogKind kind) {
        DamageModuleLogKind effectiveKind = kind == null
                ? DamageModuleLogKind.TRACE_DETAIL
                : kind;
        return DamageModuleSettings.shouldEmitServer(effectiveKind);
    }
}
