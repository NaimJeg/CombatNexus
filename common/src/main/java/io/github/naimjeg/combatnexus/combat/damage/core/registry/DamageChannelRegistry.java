package io.github.naimjeg.combatnexus.combat.damage.core.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.naimjeg.combatnexus.combat.damage.api.enums.DamageChannel;
import io.github.naimjeg.combatnexus.combat.damage.core.lifecycle.DamageModuleReloadAccess;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.*;

public class DamageChannelRegistry extends SimpleJsonResourceReloadListener<DamageChannelRegistry.ChannelDefinition> {
    public static final int MAX_CUSTOM_CHANNELS = 128;
    public static final int MAX_TRIGGER_TAGS_PER_CHANNEL = 32;

    private static final ChannelData UNTYPED_DATA = new ChannelData(
            new DamageChannel(DamageChannel.UNTYPED_ID, 0),
            List.of(),
            null,
            true,
            Integer.MIN_VALUE
    );
    private static volatile RegistryState STATE = RegistryState.initial();

    public DamageChannelRegistry(DamageModuleReloadAccess access) {
        super(ChannelDefinition.CODEC, FileToIdConverter.json("combatnexus_channels"));
        Objects.requireNonNull(access, "access")
                .requireFrameworkOwner("DamageChannelRegistry");
    }

    private static final ThreadLocal<RegistryState> CANDIDATE = new ThreadLocal<>();

    private static RegistryState state() {
        RegistryState candidate = CANDIDATE.get();
        return candidate != null ? candidate : STATE;
    }

    public static RegistryState buildCandidate(Map<Identifier, ChannelDefinition> prepared, long generation) {
        var ids = new java.util.HashSet<Identifier>();
        for (var entry : prepared.entrySet()) {
            var value = entry.getValue();
            if (value.channel().equals(DamageChannel.UNTYPED_ID) || !ids.add(value.channel())) {
                throw new IllegalArgumentException("Duplicate or reserved channel: " + entry.getKey());
            }
            if (value.resistanceAttribute().isPresent() && BuiltInRegistries.ATTRIBUTE.get(value.resistanceAttribute().get()).isEmpty()) {
                throw new IllegalArgumentException("Unknown resistance attribute: " + entry.getKey());
            }
        }
        return buildState(prepared).withRevision(generation);
    }

    /** Candidate validation is thread confined and never changes executable definitions. */
    public static <T> T withCandidate(RegistryState candidate, java.util.function.Supplier<T> action) {
        if (CANDIDATE.get() != null) throw new IllegalStateException("Nested definition compilation");
        CANDIDATE.set(candidate);
        try { return action.get(); } finally { CANDIDATE.remove(); }
    }

    public static DamageChannel getPhysical() {
        return getChannelOrUntyped(DamageChannel.PHYSICAL_ID);
    }

    public static DamageChannel getFire() {
        return getChannelOrUntyped(DamageChannel.FIRE_ID);
    }

    public static DamageChannel getMagic() {
        return getChannelOrUntyped(DamageChannel.MAGIC_ID);
    }

    public static int channelCount() {
        return state().byIndex.length;
    }

    /** Definition generation binding this catalog to its rules and templates. */
    public static long contentRevision() {
        return state().contentRevision;
    }

    public static DamageChannel getUntyped() {
        return state().byIndex[0].channel;
    }

    public static DamageChannel getChannelOrUntyped(Identifier id) {
        RegistryState state = state();

        if (id == null) {
            return state.byIndex[0].channel;
        }

        ChannelData data = state.byId.get(id);
        return data != null ? data.channel : state.byIndex[0].channel;
    }

    public static boolean containsChannel(Identifier id) {
        return id != null && state().byId.containsKey(id);
    }

    public static DamageChannel resolve(DamageChannel channel) {
        return resolveData(channel, state()).channel;
    }

    public static DamageChannel determineInitialChannel(DamageSource source) {
        RegistryState state = state();

        if (source == null) {
            return state.byIndex[0].channel;
        }

        for (ChannelData data : state.matchOrder) {
            for (TagKey<DamageType> tag : data.triggerTags) {
                if (source.is(tag)) {
                    return data.channel;
                }
            }
        }

        return state.byIndex[0].channel;
    }

    public static Holder<Attribute> getResistanceAttribute(DamageChannel rawChannel) {
        return resolveData(rawChannel, state()).resistanceAttribute;
    }

    public static ChannelData getData(DamageChannel rawChannel) {
        return resolveData(rawChannel, state());
    }

    private static ChannelData resolveData(
            DamageChannel channel,
            RegistryState state
    ) {
        if (channel == null) {
            return state.byIndex[0];
        }

        int index = channel.index();

        if (index >= 0 && index < state.byIndex.length) {
            ChannelData data = state.byIndex[index];

            if (data.channel.id().equals(channel.id())) {
                return data;
            }
        }

        ChannelData byId = state.byId.get(channel.id());
        return byId != null ? byId : state.byIndex[0];
    }

    public static boolean isKnownRuntimeChannel(DamageChannel channel) {
        if (channel == null) {
            return false;
        }

        RegistryState state = state();

        int index = channel.index();

        if (index >= 0 && index < state.byIndex.length) {
            ChannelData data = state.byIndex[index];

            if (data.channel.id().equals(channel.id())) {
                return true;
            }
        }

        return state.byId.containsKey(channel.id());
    }

    public static boolean isCurrentRuntimeChannel(DamageChannel channel) {
        if (channel == null) {
            return false;
        }

        RegistryState state = state();
        int index = channel.index();

        if (index < 0 || index >= state.byIndex.length) {
            return false;
        }

        return state.byIndex[index].channel.id().equals(channel.id());
    }

    @Override
    protected void apply(
            Map<Identifier, ChannelDefinition> prepared,
            ResourceManager manager,
            ProfilerFiller profiler
    ) {
        RegistryState nextState = buildState(prepared);
        publish(nextState);
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    static void replaceStateForTesting(
            Map<Identifier, ChannelDefinition> prepared
    ) {
        publish(buildState(prepared));
    }

    @org.jetbrains.annotations.ApiStatus.Internal
    static void resetStateForTesting() {
        STATE = RegistryState.initial();
    }

    public static void resetServerState() {
        publish(RegistryState.initial());
    }

    static List<Identifier> matchOrderIdsForTesting() {
        return Arrays.stream(state().matchOrder)
                .map(data -> data.channel().id())
                .toList();
    }

    private static RegistryState buildState(
            Map<Identifier, ChannelDefinition> prepared
    ) {
        if (prepared == null) {
            throw new IllegalArgumentException(
                    "Damage channel definitions must not be null"
            );
        }

        if (prepared.size() > MAX_CUSTOM_CHANNELS) {
            throw new IllegalArgumentException(
                    "Damage channel reload exceeds maximum definition count: "
                            + prepared.size()
                            + " > "
                            + MAX_CUSTOM_CHANNELS
            );
        }

        for (Map.Entry<Identifier, ChannelDefinition> entry
                : prepared.entrySet()) {
            ChannelDefinition definition = Objects.requireNonNull(
                    entry.getValue(),
                    "Null channel definition from " + entry.getKey()
            );

            if (definition.triggerTags().size()
                    > MAX_TRIGGER_TAGS_PER_CHANNEL) {
                throw new IllegalArgumentException(
                        "Damage channel "
                                + definition.channel()
                                + " from "
                                + entry.getKey()
                                + " has "
                                + definition.triggerTags().size()
                                + " trigger tags; maximum="
                                + MAX_TRIGGER_TAGS_PER_CHANNEL
                );
            }
        }

        Map<Identifier, ChannelData> nextById = new HashMap<>();
        List<ChannelData> nextByIndex = new ArrayList<>();

        nextById.put(DamageChannel.UNTYPED_ID, UNTYPED_DATA);
        nextByIndex.add(UNTYPED_DATA);

        List<Map.Entry<Identifier, ChannelDefinition>> ordered =
                new ArrayList<>(prepared.entrySet());
        ordered.sort(
                Comparator
                        .comparing((Map.Entry<Identifier, ChannelDefinition> entry) ->
                                entry.getValue().channel().toString())
                        .thenComparing(entry -> entry.getKey().toString())
        );

        Map<Identifier, Identifier> sourceFileByChannel = new HashMap<>();

        for (Map.Entry<Identifier, ChannelDefinition> entry : ordered) {
            Identifier fileId = entry.getKey();
            ChannelDefinition def = entry.getValue();
            Identifier channelId = def.channel();

            if (channelId.equals(DamageChannel.UNTYPED_ID)) {
                continue;
            }

            if (nextById.containsKey(channelId)) {
                continue;
            }

            Holder<Attribute> resistanceAttr = null;

            if (def.resistanceAttribute().isPresent()) {
                Identifier attrId = def.resistanceAttribute().get();

                resistanceAttr = BuiltInRegistries.ATTRIBUTE
                        .get(attrId)
                        .orElse(null);

            }

            int denseIndex = nextByIndex.size();
            DamageChannel channel = new DamageChannel(channelId, denseIndex);
            ChannelData data = new ChannelData(
                    channel,
                    List.copyOf(def.triggerTags()),
                    resistanceAttr,
                    def.affectedByArmor(),
                    def.priority()
            );

            nextById.put(channelId, data);
            sourceFileByChannel.put(channelId, fileId);
            nextByIndex.add(data);

        }

        ChannelData[] indexedArray = nextByIndex.toArray(ChannelData[]::new);

        ChannelData[] matchArray = nextByIndex.stream()
                .filter(data -> !data.channel.id().equals(DamageChannel.UNTYPED_ID))
                .sorted(
                        Comparator
                                .comparingInt(ChannelData::priority)
                                .reversed()
                                .thenComparing(data -> data.channel.id().toString())
                )
                .toArray(ChannelData[]::new);

        RegistryState nextState = new RegistryState(
                0L,
                Map.copyOf(nextById),
                indexedArray,
                matchArray
        );

        return nextState;
    }

    private static synchronized void publish(RegistryState candidate) {
        RegistryState previous = state();
        long revision = previous.sameContent(candidate)
                ? previous.contentRevision
                : Math.addExact(previous.contentRevision, 1L);
        STATE = candidate.withRevision(revision);
    }

    public static final class RegistryState {
        public long contentRevision() { return contentRevision; }
        public int channelCount() { return byIndex.length; }
        public DamageChannel channel(Identifier id) {
            var data = byId.get(id); return data == null ? byIndex[0].channel : data.channel;
        }
        public DamageChannel resolve(DamageChannel channel) { return resolveData(channel, this).channel; }
        public ChannelData data(DamageChannel channel) { return resolveData(channel, this); }
        public DamageChannel initialChannel(DamageSource source) {
            if (source != null) for (var data : matchOrder) {
                for (var tag : data.triggerTags) if (source.is(tag)) return data.channel;
            }
            return byIndex[0].channel;
        }
        private final long contentRevision;
        private final Map<Identifier, ChannelData> byId;
        private final ChannelData[] byIndex;
        private final ChannelData[] matchOrder;
        private RegistryState(long contentRevision, Map<Identifier, ChannelData> byId,
                              ChannelData[] byIndex, ChannelData[] matchOrder) {
            this.contentRevision = contentRevision;
            this.byId = Map.copyOf(byId);
            this.byIndex = byIndex.clone();
            this.matchOrder = matchOrder.clone();
        }

        static RegistryState initial() {
            return new RegistryState(
                    0L,
                    Map.of(DamageChannel.UNTYPED_ID, UNTYPED_DATA),
                    new ChannelData[]{UNTYPED_DATA},
                    new ChannelData[0]
            );
        }

        RegistryState withRevision(long revision) {
            return new RegistryState(revision, byId, byIndex, matchOrder);
        }

        boolean sameContent(RegistryState other) {
            return byId.equals(other.byId)
                    && Arrays.equals(matchOrder, other.matchOrder);
        }
    }

    public record ChannelDefinition(
            Identifier channel,
            List<TagKey<DamageType>> triggerTags,
            Optional<Identifier> resistanceAttribute,
            boolean affectedByArmor,
            int priority
    ) {
        public static final Codec<ChannelDefinition> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Identifier.CODEC
                                .fieldOf("channel")
                                .forGetter(ChannelDefinition::channel),

                        io.github.naimjeg.combatnexus.combat.damage.api.rule.DamageRuleLimits
                                .boundedList(
                                        TagKey.codec(
                                                Registries.DAMAGE_TYPE
                                        ),
                                        MAX_TRIGGER_TAGS_PER_CHANNEL,
                                        "damage channel trigger_tags"
                                )
                                .optionalFieldOf("trigger_tags", List.of())
                                .forGetter(ChannelDefinition::triggerTags),

                        Identifier.CODEC
                                .optionalFieldOf("resistance_attribute")
                                .forGetter(ChannelDefinition::resistanceAttribute),

                        Codec.BOOL
                                .optionalFieldOf("affected_by_armor", true)
                                .forGetter(ChannelDefinition::affectedByArmor),

                        Codec.INT
                                .optionalFieldOf("priority", 0)
                                .forGetter(ChannelDefinition::priority)
                ).apply(instance, ChannelDefinition::new));
    }

    public record ChannelData(
            DamageChannel channel,
            List<TagKey<DamageType>> triggerTags,
            Holder<Attribute> resistanceAttribute,
            boolean affectedByArmor,
            int priority
    ) {
    }
}

