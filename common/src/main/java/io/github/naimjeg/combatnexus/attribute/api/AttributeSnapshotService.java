package io.github.naimjeg.combatnexus.attribute.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;

/** Server-session revision allocator; captures authoritative values once for downstream consumers. */
public final class AttributeSnapshotService {
    private final Map<UUID, AttributeSnapshot> latest = new HashMap<>();
    private long revision;

    public AttributeSnapshot capture(LivingEntity entity, Iterable<Holder<Attribute>> requested) {
        return capture(entity, requested, count -> {});
    }
    public AttributeSnapshot capture(LivingEntity entity, Iterable<Holder<Attribute>> requested,
                                     java.util.function.IntConsumer budget) {
        long next = Math.incrementExact(revision);
        AttributeSnapshot snapshot = AttributeReader.capture(entity, next, requested, budget);
        revision = next;
        latest.put(entity.getUUID(), snapshot);
        return snapshot;
    }

    public java.util.Optional<AttributeSnapshot> latest(UUID entity) {
        return java.util.Optional.ofNullable(latest.get(entity));
    }
    public void clear(UUID entity) { latest.remove(entity); }
    /** Revisions remain monotonic for the lifetime of this server service. */
    public void clear() { latest.clear(); }
}
