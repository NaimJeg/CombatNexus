package io.github.naimjeg.combatnexus.combat.state.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Duration is supplied from the frozen action facts; changing reduction does not reset the last-use tick. */
public record CooldownStamp(long started, DeathPolicy deathPolicy) {
    public enum DeathPolicy { RETAIN, CLEAR }
    public static final Codec<CooldownStamp> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("started").forGetter(CooldownStamp::started),
            Codec.STRING.xmap(DeathPolicy::valueOf, DeathPolicy::name).fieldOf("death").forGetter(CooldownStamp::deathPolicy)
    ).apply(i, CooldownStamp::new));
    public long remaining(long now, int duration) { return Math.max(0, (long)duration - Math.max(0, now-started)); }
}
