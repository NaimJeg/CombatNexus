package io.github.naimjeg.combatnexus.presentation.api;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Presentation rounding never turns a nonzero contribution into zero. */
public final class TooltipNumbers {
    private TooltipNumbers() {}
    public static String number(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite display value");
        if (value == 0) return "0";
        if (Math.abs(value) < 0.01) return value < 0 ? ">-0.01" : "<0.01";
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
    public static String percent(double ratio) { return number(ratio * 100) + "%"; }
}
