package io.github.naimjeg.combatnexus.combat.damage.core.request;

import io.github.naimjeg.combatnexus.combat.api.transaction.CombatLineage;
import io.github.naimjeg.combatnexus.combat.damage.api.damage.*;
import net.minecraft.resources.Identifier;
import java.util.HashSet;

/** Damage trigger restrictions survive healing/shield hops in the common causal lineage. */
public final class DamageExecutionPolicy {
    private static final Identifier PROC = Identifier.fromNamespaceAndPath("combatnexus", "proc");
    private static final Identifier REFLECTION = Identifier.fromNamespaceAndPath("combatnexus", "reflection");
    private static final Identifier THORNS = Identifier.fromNamespaceAndPath("combatnexus", "thorns");
    private DamageExecutionPolicy() {}
    public static DamageTriggerPolicy inherited(CombatLineage lineage) {
        var blocked = lineage.suppressedTriggers();
        return new DamageTriggerPolicy(!blocked.contains(PROC), !blocked.contains(REFLECTION), !blocked.contains(THORNS));
    }
    public static DamageTriggerPolicy constrain(CombatLineage lineage, DamageTriggerPolicy policy) {
        var result = policy.intersect(inherited(lineage));
        var blocked = new HashSet<Identifier>();
        if (!result.procAllowed()) blocked.add(PROC);
        if (!result.reflectionAllowed()) blocked.add(REFLECTION);
        if (!result.thornsAllowed()) blocked.add(THORNS);
        lineage.suppressTriggers(blocked);
        return result;
    }
    public static DamageFailureReason failure(CombatLineage lineage, DamageRequestKind kind) {
        return DamageAdmissionController.triggerFailure(kind, inherited(lineage));
    }
}
