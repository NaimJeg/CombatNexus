package io.github.naimjeg.combatnexus.effect.api;

import java.util.Objects;
import java.util.OptionalLong;
import com.google.gson.JsonObject;
import io.github.naimjeg.combatnexus.config.SettingsJson;

/** Duration is independent of removal policy, stacking and persistence. */
public record EffectLifetime(Kind kind, int ticks) {
    public enum Kind { TIMED, INSTANT, PERMANENT }
    public EffectLifetime {
        Objects.requireNonNull(kind);
        if (kind == Kind.TIMED ? ticks < 1 : ticks != 0)
            throw new IllegalArgumentException("Timed effects require positive ticks; other lifetimes require zero ticks");
    }
    public static EffectLifetime timed(int ticks) { return new EffectLifetime(Kind.TIMED, ticks); }
    public static EffectLifetime instant() { return new EffectLifetime(Kind.INSTANT, 0); }
    public static EffectLifetime permanent() { return new EffectLifetime(Kind.PERMANENT, 0); }
    public OptionalLong deadline(long now) {
        if (now < 0) throw new IllegalArgumentException("Negative world time");
        return kind == Kind.TIMED ? OptionalLong.of(Math.addExact(now, ticks)) : OptionalLong.empty();
    }
    public static EffectLifetime decode(JsonObject value) {
        var kind = SettingsJson.enumeration(value, "kind", Kind.class);
        SettingsJson.fields(value, kind == Kind.TIMED ? new String[]{"kind", "ticks"} : new String[]{"kind"});
        return new EffectLifetime(kind, kind == Kind.TIMED ? SettingsJson.integer(value, "ticks") : 0);
    }
}
