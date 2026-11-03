package io.github.naimjeg.combatnexus.combat.health.api;

public record LethalReceipt(Outcome outcome, float healthBefore, float healthAfter) {
    public enum Outcome { MANAGED_PROTECTED, NATIVE_PROTECTED, DEATH_CONFIRMED, DEATH_REFUSED }
}
