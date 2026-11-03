package io.github.naimjeg.combatnexus.combat.api.source;

import java.util.UUID;

/** Opaque identity; only the exact object held by its world service can revoke an installation. Never serialized. */
public final class InstallationHandle {
    private final UUID session, target, instance;
    @org.jetbrains.annotations.ApiStatus.Internal
    public InstallationHandle(UUID session, UUID target, UUID instance) {
        this.session = session; this.target = target; this.instance = instance;
    }
    public UUID session() { return session; }
    public UUID target() { return target; }
    public UUID instance() { return instance; }
}
