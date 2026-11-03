package io.github.naimjeg.combatnexus.combat.spi;

import net.minecraft.world.entity.LivingEntity;
import io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope;

public final class NativeResourceWrites {
    private static final ThreadLocal<Token> CURRENT = new ThreadLocal<>();
    private NativeResourceWrites() {}
    public static Token health(LivingEntity target) {
        CombatExecutionScope.requireNativeWritePhase();
        var previous = CURRENT.get();
        for (var active = previous; active != null; active = active.previous)
            if (active.target == target) active.uncontended = false;
        var token = new Token(target, previous); CURRENT.set(token); return token;
    }
    public static void requireWritePhase() { CombatExecutionScope.requireNativeWritePhase(); }
    public static final class Token implements AutoCloseable {
        private final LivingEntity target;
        private final Token previous;
        private final Thread thread = Thread.currentThread();
        private boolean uncontended = true, closed;
        private Token(LivingEntity target, Token previous) { this.target = target; this.previous = previous; }
        public boolean uncontended() { return uncontended; }
        @Override public void close() {
            if (closed) return;
            if (Thread.currentThread() != thread || CURRENT.get() != this) throw new IllegalStateException("Native write scope order");
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
            closed = true;
        }
    }
}
