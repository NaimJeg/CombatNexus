package io.github.naimjeg.combatnexus.effect.api;

import java.util.*;

public record NativeEffectReceipt(Optional<State> before, Optional<State> after, boolean nativeAccepted) {
    public NativeEffectReceipt { Objects.requireNonNull(before); Objects.requireNonNull(after); }
    public record State(int durationTicks, int amplifier, boolean ambient, boolean visible, boolean icon) {}
}
