package io.github.naimjeg.combatnexus.combat.api.action;
/** Counter scope is explicit. ACTION requires real adapter evidence. */
public record TriggerLimit(Scope scope, int maximum) {
    public enum Scope { OPERATION, TARGET, ACTION, ROOT, SOURCE }
    public static final TriggerLimit ONCE = new TriggerLimit(Scope.OPERATION, 1);
    public TriggerLimit { if (scope == null || maximum < 1 || maximum > 64) throw new IllegalArgumentException("Invalid trigger limit"); }
}
