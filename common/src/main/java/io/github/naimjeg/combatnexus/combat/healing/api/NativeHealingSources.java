package io.github.naimjeg.combatnexus.combat.healing.api;

import net.minecraft.world.entity.LivingEntity;
import java.util.Objects;

/** One-call attribution scopes. The top scope never falls through to an outer target. */
public final class NativeHealingSources {
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private NativeHealingSources() {}

    public static Scope open(LivingEntity target, NativeHealingSource source) {
        var scope = new Scope(Objects.requireNonNull(target), Objects.requireNonNull(source), CURRENT.get());
        CURRENT.set(scope);
        return scope;
    }

    public static NativeHealingSource consume(LivingEntity target) {
        var scope = CURRENT.get();
        if (scope == null || scope.consumed || scope.target != target) return NativeHealingSource.UNKNOWN;
        scope.consumed = true;
        return scope.source;
    }

    public static final class Scope implements AutoCloseable {
        private final Thread thread = Thread.currentThread();
        private final LivingEntity target;
        private final NativeHealingSource source;
        private final Scope previous;
        private boolean consumed, closed;
        private Scope(LivingEntity target, NativeHealingSource source, Scope previous) {
            this.target = target; this.source = source; this.previous = previous;
        }
        @Override public void close() {
            if (closed) return;
            if (Thread.currentThread() != thread || CURRENT.get() != this)
                throw new IllegalStateException("Healing source scope closed out of order or on another thread");
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
            closed = true;
        }
    }
}
