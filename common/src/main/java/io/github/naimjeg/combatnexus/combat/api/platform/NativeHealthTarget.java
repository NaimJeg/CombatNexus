package io.github.naimjeg.combatnexus.combat.api.platform;

import net.minecraft.world.damagesource.DamageSource;

/** Native health projection contract implemented by the version adapter. Calls do not create application authority. */
public interface NativeHealthTarget {
    boolean combatnexus$nativeProtection(DamageSource source);
    java.util.List<io.github.naimjeg.combatnexus.combat.api.ApplicationJournal.Entry> combatnexus$protectionEvidence();
    void combatnexus$deathSound();
    boolean combatnexus$deathConfirmed();
}
