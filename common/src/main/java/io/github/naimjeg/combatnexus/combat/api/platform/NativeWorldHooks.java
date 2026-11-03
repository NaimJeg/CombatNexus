package io.github.naimjeg.combatnexus.combat.api.platform;

import io.github.naimjeg.combatnexus.effect.api.*;

/** Version-specific callbacks supplied once when loading a world. */
public interface NativeWorldHooks {
    NativeHealingApplication healing();
    boolean mayExpire(EffectInstance effect);
    void effectCommitted(EffectService.Change change);
}
