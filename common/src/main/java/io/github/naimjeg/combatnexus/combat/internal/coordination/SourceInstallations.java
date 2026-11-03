package io.github.naimjeg.combatnexus.combat.internal.coordination;
import io.github.naimjeg.combatnexus.combat.internal.CombatRuntime;

import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.source.*;
import io.github.naimjeg.combatnexus.combat.api.transaction.*;
import io.github.naimjeg.combatnexus.combat.internal.transaction.StateCommands;
import io.github.naimjeg.combatnexus.combat.regeneration.api.RegenerationPlan;
import io.github.naimjeg.combatnexus.combat.shield.api.*;
import io.github.naimjeg.combatnexus.effect.api.EffectService;
import java.util.*;
import net.minecraft.resources.Identifier;

/** Shared installation ledger. Failed compensation remains owned and blocks replacement of that instance only. */
public final class SourceInstallations implements SourceService {
    private record Key(UUID target, UUID instance) {}
    private static final class Installed {
        final InstallationHandle handle;
        final SourceInstance source;
        final SourcePlan plan;
        boolean pending;
        Installed(InstallationHandle handle, SourceInstance source, SourcePlan plan) { this.handle = handle; this.source = source; this.plan = plan; }
    }
    private final CombatRuntime world;
    private final Map<Key, Installed> installed = new LinkedHashMap<>();
    private final Set<UUID> watched = new LinkedHashSet<>();
    public SourceInstallations(CombatRuntime world) { this.world = world; }
    public SourceService businessView() { return new SourceService() {
        public SourceResult install(SourceInstallation request) { return SourceInstallations.this.install(request); }
        public SourceResult install(SourceInstallation request, CombatParent parent) { return SourceInstallations.this.install(request, parent); }
        public CommandResult<Boolean> revoke(UUID request, InstallationHandle handle) { return SourceInstallations.this.revoke(request, handle); }
        public CommandResult<Boolean> revoke(UUID request, InstallationHandle handle, CombatParent parent) { return SourceInstallations.this.revoke(request, handle, parent); }
        public List<SourceView> snapshot(UUID target) { return SourceInstallations.this.snapshot(target); }
        public CommandResult<Boolean> refresh(UUID request, UUID target) { return SourceInstallations.this.refresh(request, target); }
    }; }
    public void track(UUID target) { CombatMutationGuard.requireWorldWritePhase(CombatOperation.SOURCE); if (watched.size() < 4096) watched.add(target); }
    public void tick() {
        if (io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.sourceProviders().isEmpty()) return;
        for (var target : List.copyOf(watched)) {
            if (!(world.level().getEntity(target) instanceof net.minecraft.world.entity.LivingEntity living) || !living.isAlive()) { watched.remove(target); continue; }
            refresh(UUID.randomUUID(), target);
        }
    }
    public CommandResult<Boolean> refresh(UUID request, UUID target) {
        return StateCommands.submit(world, request, target, CombatOperation.SOURCE, null, () -> {
            track(target);
            boolean complete = true;
            for (var provider : io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.sourceProviders()) {
                Map<UUID, SourceProvider.Offer> offers = new LinkedHashMap<>();
                try (var compute = io.github.naimjeg.combatnexus.combat.internal.transaction.CombatExecutionScope.computation()) {
                    var frame = new io.github.naimjeg.combatnexus.combat.internal.WorldServices(world).facts(target).orElseThrow();
                    var values = List.copyOf(provider.listener().snapshot(frame));
                    if (values.size() > 64) throw new IllegalArgumentException("Source provider capacity exceeded");
                    for (var offer : values) {
                        if (!offer.source().provider().equals(provider.id()) || offers.putIfAbsent(offer.source().instance(), offer) != null)
                            throw new IllegalArgumentException("Foreign or duplicate source offer");
                    }
                } catch (RuntimeException failure) {
                    complete = false;
                    continue;
                }
                for (var value : List.copyOf(installed.values())) if (value.handle.target().equals(target) && value.source.provider().equals(provider.id())) {
                    var desired = offers.get(value.source.instance());
                    if (desired == null || !desired.plan().equals(value.plan) || !desired.source().equals(value.source) || value.pending)
                        complete &= revoke(UUID.randomUUID(), value.handle).applied();
                }
                for (var offer : offers.values()) if (!installed.containsKey(new Key(target, offer.source().instance())))
                    complete &= install(new SourceInstallation(UUID.randomUUID(), target, offer.source(), offer.plan())).applied();
            }
            return complete ? CommandResult.applied(true) : CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Source provider reconciliation incomplete; inspect source views");
        });
    }
    public static Identifier key(SourceInstance source) { return Identifier.fromNamespaceAndPath("combatnexus", "source/" + source.instance()); }
    private static Identifier part(SourceInstance source, Identifier definition) {
        return Identifier.fromNamespaceAndPath("combatnexus", "source/" + source.instance() + "/" + definition.getNamespace() + "/" + definition.getPath());
    }
    public Optional<InstallationHandle> handle(UUID target, UUID instance) { world.level(); return Optional.ofNullable(installed.get(new Key(target, instance))).map(i -> i.handle); }
    public Optional<SourceInstance> identity(UUID target, Identifier key) {
        world.level();
        return installed.values().stream().filter(i -> i.handle.target().equals(target) &&
            (key(i.source).equals(key) || i.plan.regeneration().keySet().stream().anyMatch(id -> part(i.source, id).equals(key))
                || i.plan.shields().keySet().stream().anyMatch(id -> part(i.source, id).equals(key)))).map(i -> i.source).findFirst();
    }
    public List<SourceView> snapshot(UUID target) {
        world.level(); return installed.values().stream().filter(i -> i.handle.target().equals(target))
            .map(i -> new SourceView(target, i.source, i.plan.definition(), i.plan.persistent(), i.pending)).toList();
    }
    public SourceResult install(SourceInstallation request) { return install(request, null); }
    public SourceResult install(SourceInstallation request, CombatParent parent) {
        Objects.requireNonNull(request);
        var attempted = new InstallationHandle[1];
        CommandResult<InstallationHandle> result = StateCommands.submit(world, request.requestId(), request.target(), CombatOperation.SOURCE, parent, () -> {
            var key = new Key(request.target(), request.source().instance());
            if (installed.containsKey(key)) return CommandResult.rejected(CommandRejection.Code.DUPLICATE, "Source instance already installed");
            if (snapshot(request.target()).size() >= 64) return CommandResult.rejected(CommandRejection.Code.INVALID_INPUT, "Source capacity exceeded");
            var value = new Installed(new InstallationHandle(world.sessionId(), request.target(), request.source().instance()), request.source(), request.plan());
            installed.put(key, value); attempted[0] = value.handle;
            try {
                long now = world.level().getGameTime();
                for (var entry : value.plan.effects().entrySet()) {
                    var definition = entry.getValue();
                    if (!world.effects().apply(definition.instantiate(request.target(), entry.getKey(), key(value.source), now), definition.stacking()))
                        throw new IllegalStateException("Source effect rejected: " + entry.getKey());
                }
                for (var entry : value.plan.regeneration().entrySet()) {
                    var source = new CombatSource(part(value.source, entry.getKey()), value.source.owner(), Optional.empty());
                    world.regeneration().replace(new RegenerationPlan(request.target(), source, entry.getValue(), OptionalLong.of(Math.addExact(now, entry.getValue().durationTicks()))));
                }
                for (var entry : value.plan.shields().entrySet()) {
                    var grant = entry.getValue();
                    world.shields().replace(request.target(), new ShieldGrant(new CombatSource(part(value.source, entry.getKey()), value.source.owner(), Optional.empty()),
                        grant.definition(), grant.amount(), OptionalLong.of(Math.addExact(now, grant.durationTicks()))));
                }
                return CommandResult.applied(value.handle);
            } catch (RuntimeException failure) {
                value.pending = true;
                try { cleanup(value, true); installed.remove(key); }
                catch (RuntimeException compensation) { failure.addSuppressed(compensation); }
                return CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Source installation failed; compensation pending=" + installed.containsKey(key));
            }
        });
        var retained = result.value();
        if (retained.isEmpty() && attempted[0] != null && installed.containsKey(new Key(attempted[0].target(), attempted[0].instance()))) retained = Optional.of(attempted[0]);
        return new SourceResult(retained, result.rejection(), result.journal(), result.context());
    }
    public CommandResult<Boolean> revoke(UUID request, InstallationHandle handle) { return revoke(request, handle, null); }
    public CommandResult<Boolean> revoke(UUID request, InstallationHandle handle, CombatParent parent) {
        Objects.requireNonNull(handle);
        return StateCommands.submit(world, request, handle.target(), CombatOperation.SOURCE, parent, () -> {
            var key = new Key(handle.target(), handle.instance()); var value = installed.get(key);
            if (value == null || value.handle != handle || !handle.session().equals(world.sessionId()))
                return CommandResult.rejected(CommandRejection.Code.STALE_HANDLE, "Installation handle is not current");
            try { cleanup(value, false); installed.remove(key); return CommandResult.applied(true); }
            catch (RuntimeException failure) {
                value.pending = true;
                return CommandResult.rejected(CommandRejection.Code.INCOMPLETE_APPLICATION, "Source cleanup pending");
            }
        });
    }
    private void cleanup(Installed value, boolean rollback) {
        var target = value.handle.target();
        if(world.level().getEntity(target) instanceof io.github.naimjeg.combatnexus.combat.state.spi.NativeSchedules schedules)
            schedules.combatnexus$schedules().clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.SOURCE_REMOVED,Optional.of(value.source.instance()));
        if(world.level().getEntity(target) instanceof io.github.naimjeg.combatnexus.combat.state.spi.NativeRuntimeStates state)
            state.combatnexus$states().clear(io.github.naimjeg.combatnexus.combat.state.api.StateKey.Boundary.SOURCE_REMOVED,Optional.of(value.source.instance()));
        for (var id : value.plan.regeneration().keySet()) world.regeneration().remove(target, part(value.source, id));
        for (var id : value.plan.effects().keySet()) world.effects().remove(target, id, key(value.source), EffectService.Removal.REMOVED);
        for (var entry : value.plan.shields().entrySet()) if (rollback || entry.getValue().removal() == SourcePlan.ShieldRemoval.REVOKE)
            world.shields().remove(target, part(value.source, entry.getKey()), ShieldPoolSnapshot.RemovalReason.REMOVED);
    }
    public void clear(UUID target) { installed.keySet().removeIf(key -> key.target().equals(target)); watched.remove(target); }
    public void close() { installed.clear(); watched.clear(); }
    public record Saved(SourceInstance source, SourcePlan plan) {}
    public List<Saved> save(UUID target) {
        if (installed.values().stream().anyMatch(i -> i.handle.target().equals(target) && i.pending)) throw new IllegalStateException("Source compensation pending");
        return installed.values().stream().filter(i -> i.handle.target().equals(target) && i.plan.persistent()).map(i -> new Saved(i.source, i.plan)).toList();
    }
    public Set<Identifier> persistentKeys(UUID target) {
        var keys = new HashSet<Identifier>();
        for (var saved : save(target)) {
            keys.add(key(saved.source()));
            saved.plan().regeneration().keySet().forEach(id -> keys.add(part(saved.source(), id)));
            saved.plan().shields().keySet().forEach(id -> keys.add(part(saved.source(), id)));
        }
        return Set.copyOf(keys);
    }
    public void restore(UUID target, List<Saved> saved) {
        for (var value : saved) {
            var key = new Key(target, value.source().instance());
            if (installed.putIfAbsent(key, new Installed(new InstallationHandle(world.sessionId(), target, value.source().instance()), value.source(), value.plan())) != null)
                throw new IllegalArgumentException("Duplicate saved source");
        }
    }
    public void notifyRestored(UUID target) {
        for (var value : List.copyOf(installed.values())) if (value.handle.target().equals(target)) {
            var view = new SourceView(target, value.source, value.plan.definition(), value.plan.persistent(), value.pending);
            io.github.naimjeg.combatnexus.combat.internal.registration.CombatExtensions.restoredSource(world.level().getServer(),
                    new RestoredSource(view, value.handle));
        }
    }
}
