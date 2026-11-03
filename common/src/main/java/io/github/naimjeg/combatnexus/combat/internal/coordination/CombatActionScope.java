package io.github.naimjeg.combatnexus.combat.internal.coordination;
import io.github.naimjeg.combatnexus.combat.api.action.CombatAction;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;
import java.util.*;
/** Native adapter scope; never inferred from a tick or reused after attack returns. */
public final class CombatActionScope implements AutoCloseable {
    private static final ThreadLocal<Deque<CombatActionScope>> STACK = ThreadLocal.withInitial(ArrayDeque::new);
    private final CombatAction action;
    private final io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame input;
    private final CombatRuntime world;
    private final Thread owner = Thread.currentThread();
    private boolean closed;
    private CombatActionScope(CombatRuntime world, UUID actor) {
        this.world=world;
        action = new CombatAction(UUID.randomUUID(), world.sessionId(), actor);
        if (STACK.get().size() >= 64) throw new IllegalStateException("Action nesting capacity exceeded");
        input = world.captureInput((net.minecraft.world.entity.LivingEntity)world.level().getEntity(actor),
                io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions.forServer(world.level().getServer()).generation());
        world.decisionSamples().open(action.id());
        STACK.get().addLast(this);
    }
    public static CombatActionScope open(CombatRuntime world, UUID actor) { world.level(); return new CombatActionScope(world, actor); }
    public static Optional<CombatAction> current(UUID session, UUID actor) {
        var current = STACK.get().peekLast();
        return current != null && current.action.session().equals(session) && current.action.actor().equals(actor)
                ? Optional.of(current.action) : Optional.empty();
    }
    public static Optional<io.github.naimjeg.combatnexus.combat.api.input.CombatInputFrame> input(UUID session, UUID actor) {
        var current = STACK.get().peekLast();
        return current != null && current.action.session().equals(session) && current.action.actor().equals(actor)
                ? Optional.of(current.input) : Optional.empty();
    }
    public void close() {
        if (closed) return;
        if (Thread.currentThread() != owner || STACK.get().peekLast() != this) throw new IllegalStateException("Action scope close order");
        STACK.get().removeLast(); if (STACK.get().isEmpty()) STACK.remove(); closed = true;
        world.decisionSamples().close(action.id());
        if(world.level().getEntity(action.actor()) instanceof io.github.naimjeg.combatnexus.combat.state.spi.NativeRuntimeStates states)
            states.combatnexus$states().clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.ACTION_FINISHED,Optional.of(action.id()));
    }
}
