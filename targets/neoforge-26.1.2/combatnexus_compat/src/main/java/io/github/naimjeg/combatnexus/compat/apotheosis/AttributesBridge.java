package io.github.naimjeg.combatnexus.compat.apotheosis;

import io.github.naimjeg.combatnexus.combat.api.CombatRegistrar;

public final class AttributesBridge {
    private AttributesBridge() {}
    public static void register(CombatRegistrar registrar) {
        var critical = net.minecraft.resources.Identifier.fromNamespaceAndPath("apothic_attributes", "multi_critical");
        registrar.damageFormula(critical, (input, samples) -> {
            var attacker = input.attacker();
            if (attacker.isEmpty()) return new io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaPolicy.Output(input.amount(), java.util.List.of());
            var values = attacker.orElseThrow().attributes();
            var chanceValue = values.valueFrame().values().get(net.minecraft.resources.Identifier.fromNamespaceAndPath("apothic_attributes", "crit_chance"));
            double chance = chanceValue == null ? 0 : chanceValue.effective();
            float multiplier = values.valueOrZero(net.minecraft.resources.Identifier.fromNamespaceAndPath("apothic_attributes", "crit_damage"));
            float base = (float)input.amount(), damage = base;
            var multipliers = new java.util.ArrayList<Double>();
            // Preserve native sample order, float arithmetic, additive overcrits and 15% decay.
            while (samples.nextFloat() <= chance && multiplier > 1.0F) {
                chance--; damage += base * (multiplier - 1); multipliers.add((double)multiplier); multiplier *= 0.85F;
            }
            return new io.github.naimjeg.combatnexus.combat.api.bridge.DamageFormulaPolicy.Output(damage, multipliers);
        });
        registrar.bridges().register(new io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability(critical, critical, critical,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus_compat", "apothic_attributes"),
                io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability.Mode.REPLACE, java.util.Set.of(),
                "Frozen multi-critical formula with host-recorded samples and multiplier sequence"));
        var overheal = net.minecraft.resources.Identifier.fromNamespaceAndPath("apothic_attributes", "overheal");
        registrar.bridges().register(new io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability(overheal, overheal, overheal,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("combatnexus_compat", "apothic_attributes"),
                io.github.naimjeg.combatnexus.combat.api.bridge.BridgeCapability.Mode.REPLACE,
                java.util.Set.of(io.github.naimjeg.combatnexus.combat.api.transaction.CombatOperation.ABSORPTION),
                "Native absorption grant with source-owned capacity and native-first consumption"));
    }
}
