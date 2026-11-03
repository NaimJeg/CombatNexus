package io.github.naimjeg.combatnexus.combat.api.behavior;
import java.util.*;
import java.util.function.*;
import io.github.naimjeg.combatnexus.combat.api.*;
import io.github.naimjeg.combatnexus.combat.api.action.TriggerLimit;
import io.github.naimjeg.combatnexus.combat.api.transaction.CombatParent;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.DamageParentRef;
/** Authorities are queried from the current callback, never reconstructed from the envelope. */
public record SettlementContext(CombatWorld world, Optional<BehaviorSource> source, SettlementEnvelope settlement,
        Function<TriggerLimit,Optional<String>> claim, Supplier<Optional<CombatParent>> authority,
        Supplier<Optional<DamageParentRef>> damageAuthority) {}
