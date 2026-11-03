package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

/** Dynamic decision for the exact DamageSource captured by the player's current attack. */
public final class VanillaAttackTakeover implements AutoCloseable {
    private static final ThreadLocal<Boolean> ACTIVE = new ThreadLocal<>();
    private final Boolean previous;
    private VanillaAttackTakeover(boolean managed) { previous = ACTIVE.get(); ACTIVE.set(managed); }
    public static VanillaAttackTakeover enter(boolean managed) { return new VanillaAttackTakeover(managed); }
    public static boolean managed() { return Boolean.TRUE.equals(ACTIVE.get()); }
    @Override public void close() { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
}
