package io.github.naimjeg.combatnexus.combat.api.transaction;

/** Shared admission ceilings; world sessions never get separate copies of the server tick allowance. */
public record CombatLimits(int maximumDepth, int maximumDerived, int maximumPerTick) {
    public static final int HARD_MAX_DEPTH = 64;
    public static final int HARD_MAX_DERIVED = 65_536;
    public static final int HARD_MAX_PER_TICK = 1_000_000;
    public static final CombatLimits DEFAULT = new CombatLimits(5, 64, 2048);
    public CombatLimits {
        if (maximumDepth < 1 || maximumDepth > HARD_MAX_DEPTH
                || maximumDerived < 1 || maximumDerived > HARD_MAX_DERIVED
                || maximumPerTick < 1 || maximumPerTick > HARD_MAX_PER_TICK) {
            throw new IllegalArgumentException("Invalid combat admission limits");
        }
    }
}
