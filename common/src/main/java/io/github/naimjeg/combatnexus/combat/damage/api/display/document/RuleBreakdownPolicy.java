package io.github.naimjeg.combatnexus.combat.damage.api.display.document;

public enum RuleBreakdownPolicy {
    NONE,
    DETAIL_ONLY,
    SUMMARY_AND_DETAIL;

    public static RuleBreakdownPolicy fromDisplay(
            boolean showRuleBreakdown,
            boolean hasAuthoredSummary
    ) {
        if (!showRuleBreakdown) {
            return NONE;
        }
        return hasAuthoredSummary ? DETAIL_ONLY : SUMMARY_AND_DETAIL;
    }

    public boolean visibleInCompact() {
        return this == SUMMARY_AND_DETAIL;
    }

    public boolean visibleInDetail() {
        return this != NONE;
    }
}
