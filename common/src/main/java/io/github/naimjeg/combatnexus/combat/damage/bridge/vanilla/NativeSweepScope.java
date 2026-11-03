package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;

/** Exact native sweep call correlation; never applies to another target or nested source. */
public final class NativeSweepScope implements AutoCloseable {
    private static final ThreadLocal<NativeSweepScope> CURRENT = new ThreadLocal<>();
    private final NativeSweepScope previous;
    private final LivingEntity target;
    private final DamageSource source;
    public NativeSweepScope(LivingEntity target, DamageSource source) {
        this.target = target; this.source = source; previous = CURRENT.get(); CURRENT.set(this);
    }
    public static boolean matches(LivingEntity target, DamageSource source) {
        var scope = CURRENT.get(); return scope != null && scope.target == target && scope.source == source;
    }
    @Override public void close() { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
}
