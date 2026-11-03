package io.github.naimjeg.combatnexus.combat.api.input;

import java.util.*;

/** Per-request role assignments. Missing facts mean unknown, never an inferred owner. */
public record ParticipantInputs(Map<Role, CombatInputFrame> roles) {
    public enum Role { TARGET, HEALER, EFFECT_OWNER, DIRECT_ENTITY, ATTACKER, EQUIPMENT_OWNER }
    public ParticipantInputs { roles = Map.copyOf(roles); }
    public Optional<CombatInputFrame> find(Role role) { return Optional.ofNullable(roles.get(role)); }
    public CombatInputFrame target() { return Objects.requireNonNull(roles.get(Role.TARGET), "Target input required"); }
}
