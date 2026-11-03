package io.github.naimjeg.combatnexus.combat.damage.bridge.vanilla;

import net.minecraft.world.damagesource.DamageSource;
import java.util.Objects;
import java.util.function.Predicate;

/** Target-owned classification of platform tags; no guessed cross-platform tags. */
public final class DamageSourceClassification {
    private static Predicate<DamageSource> magic;
    private static Predicate<DamageSource> technical;
    private DamageSourceClassification() {}
    public static synchronized void install(Predicate<DamageSource> magicTag, Predicate<DamageSource> technicalTag) {
        if (magic != null) throw new IllegalStateException("Damage classification already installed");
        magic = Objects.requireNonNull(magicTag);
        technical = Objects.requireNonNull(technicalTag);
    }
    public static boolean isMagic(DamageSource source) { return Objects.requireNonNull(magic, "Target classification not installed").test(source); }
    public static boolean isTechnical(DamageSource source) { return Objects.requireNonNull(technical, "Target classification not installed").test(source); }
}
