package io.github.naimjeg.combatnexus.effect.api;

import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.LivingEntity;

/** Frozen native effects, observed without installing a second contribution. */
public final class EffectFacts {
    private final List<MobEffectInstance> effects;
    private final List<EffectView> views;
    private EffectFacts(LivingEntity entity, java.util.function.IntConsumer budget) {
        effects = entity.getActiveEffects().stream().map(effect -> { budget.accept(1); return new MobEffectInstance(effect); }).toList();
        var merged = new ArrayList<EffectView>();
        var managed = entity.level() instanceof net.minecraft.server.level.ServerLevel server
            ? io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions.managedEffects(server, entity.getUUID(), budget) : List.<EffectInstance>of();
        for (var effect : effects) {
            var id = effect.getEffect().unwrapKey().orElseThrow().identifier();
            if (managed.stream().anyMatch(e -> id.equals(e.display().nativeId()))) continue;
            var category = effect.getEffect().value().getCategory().name().toLowerCase(Locale.ROOT);
            merged.add(new EffectView(entity.getUUID(), id, Optional.empty(), Optional.empty(), EffectInstance.Ownership.VANILLA_OBSERVED,
                Set.of(net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus", category)), effect.getAmplifier(),
                effect.isInfiniteDuration() ? OptionalLong.empty() : OptionalLong.of(Math.max(0, effect.getDuration())),
                Optional.empty(), EffectView.Completeness.UNKNOWN_SOURCE, Set.of(EffectView.Operation.DISPEL), io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance.UNKNOWN));
        }
        if (entity.level() instanceof net.minecraft.server.level.ServerLevel level)
            for (var effect : managed) {
                var state = effect.snapshot();
                var tags = new HashSet<>(effect.policy().tags());
                tags.add(net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus", effect.policy().category().name().toLowerCase(Locale.ROOT)));
                var identity = io.github.naimjeg.combatnexus.combat.api.CombatWorldSessions.source(level, entity.getUUID(), state.source());
                merged.add(new EffectView(entity.getUUID(), state.effect(), Optional.of(state.source()), identity.map(s -> s.instance()),
                    effect.ownership(), tags, state.level(), state.expiresAtTick().isEmpty() ? OptionalLong.empty()
                        : OptionalLong.of(Math.max(0, state.expiresAtTick().getAsLong() - level.getGameTime())),
                    Optional.ofNullable(effect.display().owner()).or(() -> identity.flatMap(s -> s.owner())),
                    identity.isPresent() || !effect.display().provenance().equals(io.github.naimjeg.combatnexus.combat.api.source.SourceProvenance.UNKNOWN) ? EffectView.Completeness.COMPLETE : EffectView.Completeness.UNKNOWN_SOURCE,
                    effect.policy().dispellable() ? Set.of(EffectView.Operation.DISPEL) : Set.of(), effect.display().provenance()));
            }
        views = List.copyOf(merged);
    }
    public static EffectFacts capture(LivingEntity entity) { return new EffectFacts(entity, ignored -> {}); }
    public static EffectFacts capture(LivingEntity entity, java.util.function.IntConsumer budget) { return new EffectFacts(entity, budget); }
    public List<EffectView> views() { return views; }
    public boolean hasEffect(net.minecraft.resources.Identifier definition) { return views.stream().anyMatch(e -> e.definition().equals(definition)); }
    public List<MobEffectInstance> values() { return effects.stream().map(MobEffectInstance::new).toList(); }
    public boolean hasEffect(Holder<MobEffect> effect) { return amplifier(effect).isPresent(); }
    public OptionalInt amplifier(Holder<MobEffect> effect) {
        return effects.stream().filter(value -> value.getEffect().equals(effect))
                .mapToInt(MobEffectInstance::getAmplifier).findFirst();
    }
}
