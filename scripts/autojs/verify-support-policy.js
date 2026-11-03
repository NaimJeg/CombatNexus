// Isolated server only: temporarily publishes definition generations, then restores values.
// Requires built-in definitions and no third-party combat modifiers. Does not prove mod-pack compatibility.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix = "io.github.naimjeg.combatnexus.";
    function cls(name) { return Java.loadClass(prefix + name); }
    var server = AutoJS.server(), level = server.overworld(), checks = [], entities = [];
    var Definitions = cls("combat.internal.definition.CombatDefinitions"), original = Definitions.forServer(server);
    var Generation = cls("combat.internal.definition.DefinitionGeneration"), Settings = cls("combat.config.CombatServerSettings");
    var Policy = cls("combat.damage.config.VanillaCompatibilitySettings");
    var Reduction = cls("combat.damage.config.VanillaCompatibilitySettings$Reduction");
    var Mode = cls("combat.damage.config.VanillaReductionCompatibilityMode");
    var Attributes = Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");
    var Id = Java.loadClass("net.minecraft.resources.Identifier"), Catalog = cls("attribute.api.AttributeCatalog");
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    function near(actual, expected, label) { check(Math.abs(actual - expected) < 0.0001, label + ": " + actual + " / " + expected); }
    function rejects(action) { try { action(); return false; } catch (failure) { return true; } }
    function adopt(policy) {
        Definitions.publish(server, new Generation(Definitions.nextGeneration(),
            new Settings(original.settings().formulas(), policy, original.settings().safety()), original.damage(), original.behaviors(),original.domains()));
    }
    function spawn() {
        var entity = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,
            Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        entity.setPos(0, 100, 0); level.addFreshEntity(entity); entities.push(entity);
        check(level.getEntity(entity.getUUID()) === entity, "probe entity has live world identity");
        return entity;
    }
    function hit(entity, source, expectedIncoming, expectedLoss, label) {
        entity.setHealth(entity.getMaxHealth()); entity.invulnerableTime = 0;
        var before = entity.getHealth();
        var result = cls("combat.damage.api.DamageModuleApi").submitDamage(
            cls("combat.damage.api.damage.DamageRequest").builder(level, entity,
                cls("combat.damage.api.damage.DamageSourceDescriptor").from(source), 8).build());
        check(result.pipelineExecuted(), label + " executes pipeline");
        near(result.resolvedDamage(), expectedIncoming, label + " incoming");
        near(before - entity.getHealth(), expectedLoss, label + " native health write");
        check(result.settlement().isPresent(), label + " completed settlement");
    }
    check(original.settings().formulas().asymptoticKValue() === 15 && original.settings().formulas().ratingPerProtScore() === 3.5
        && original.settings().formulas().resistanceKValue() === 50, "requires default formula fixture");
    check(String(cls("attribute.api.AttributeUnits").unit(Catalog.LIFESTEAL.id()).get()) === "CONVERSION_RATIO", "lifesteal is conversion, not probability");
    check(String(Catalog.CRIT_CHANCE.unit()) === "PROBABILITY", "critical chance remains probability");
    check(!Catalog.DODGE_CHANCE.reserved() && !Catalog.VULNERABLE_DAMAGE_ADDITIVE.reserved(), "consumed attributes no longer reserved");
    check(!cls("attribute.api.AttributeUnits").unit(Id.parse("example:unknown")).isPresent(), "unknown units are not guessed");
    var Definition = cls("attribute.api.AttributeCatalog$Definition");
    check(rejects(function () { new Definition(Id.parse("combatnexus:invalid"), 2, 0, 1, true, false, Catalog.LIFESTEAL.unit()); }), "invalid attribute bounds rejected");
    check(rejects(function () { new Definition(Id.parse("combatnexus:invalid"), NaN, 0, 1, true, false, Catalog.LIFESTEAL.unit()); }), "nonfinite attribute default rejected");
    check(rejects(function () { new Policy(null, true, true, true, true); }), "missing mode rejected");
    var reductions = [Reduction.ARMOR, Reduction.ENCHANTMENTS, Reduction.MOB_EFFECTS, Reduction.INNATE_RESISTANCE];
    try {
        var armor = spawn(), resistance = spawn(), protection = spawn(), plain = spawn();
        armor.getAttribute(Attributes.ARMOR).setBaseValue(10);
        var Effects = Java.loadClass("net.minecraft.world.effect.MobEffects");
        resistance.addEffect(new (Java.loadClass("net.minecraft.world.effect.MobEffectInstance"))(Effects.RESISTANCE, 200, 0));
        // An enchanted stick in an armor slot contributes protection without adding native armor.
        var stack = new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").STICK);
        var ItemEnchantments = Java.loadClass("net.minecraft.world.item.enchantment.ItemEnchantments");
        var mutable = new (Java.loadClass("net.minecraft.world.item.enchantment.ItemEnchantments$Mutable"))(ItemEnchantments.EMPTY);
        var protectionHolder = level.registryAccess().lookupOrThrow(Java.loadClass("net.minecraft.core.registries.Registries").ENCHANTMENT)
            .getOrThrow(Java.loadClass("net.minecraft.world.item.enchantment.Enchantments").PROTECTION);
        mutable.set(protectionHolder, 4);
        stack.set(Java.loadClass("net.minecraft.core.component.DataComponents").ENCHANTMENTS, mutable.toImmutable());
        protection.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").HEAD, stack);
        var modes = [Mode.FULL_REPLACEMENT, Mode.COOPERATIVE, Mode.CONFIGURABLE];
        for (var m = 0; m < modes.length; m++) for (var bits = 0; bits < 16; bits++) {
            var flags = [!!(bits & 1), !!(bits & 2), !!(bits & 4), !!(bits & 8)];
            var policy = new Policy(modes[m], flags[0], flags[1], flags[2], flags[3]);
            var label = String(modes[m]) + "/" + bits, owned = [];
            for (var r = 0; r < reductions.length; r++) {
                owned[r] = m === 0 || (m === 2 && flags[r]);
                var expected = !owned[r] ? "VANILLA" : "COMBATNEXUS";
                check(String(policy.executor(reductions[r])) === expected, label + " ownership " + reductions[r]);
            }
            adopt(policy);
            hit(armor, armor.damageSources().mobAttack(plain), owned[0] ? 40 / 7 : 8, owned[0] ? 40 / 7 : (typeof COMBATNEXUS_COMPATIBILITY !== "undefined" && COMBATNEXUS_COMPATIBILITY ? 4 : 6.08), label + " armor once");
            // Apothic Attributes 3.1.0 ALCombatRules uses 2.5% per protection point.
            hit(protection, protection.damageSources().mobAttack(plain), owned[1] ? 6.25 : 8, owned[1] ? 6.25 : (typeof COMBATNEXUS_COMPATIBILITY !== "undefined" && COMBATNEXUS_COMPATIBILITY ? 7.2 : 6.72), label + " protection once");
            hit(resistance, resistance.damageSources().mobAttack(plain), owned[2] ? 6.4 : 8, 6.4, label + " resistance once");
        }
        adopt(new Policy(Mode.COOPERATIVE, true, true, true, true));
        plain.getAttribute(cls("attribute.api.CombatAttributes").resistancePhysical()).setBaseValue(50);
        hit(plain, plain.damageSources().mobAttack(plain), 4, 4, "cooperative keeps independent channel resistance");
        adopt(Policy.defaults());
        hit(armor, armor.damageSources().fall(), 8, 8, "native armor bypass retained");
        resistance.removeAllEffects();
        resistance.addEffect(new (Java.loadClass("net.minecraft.world.effect.MobEffectInstance"))(Effects.RESISTANCE, 200, 4));
        hit(resistance, resistance.damageSources().mobAttack(plain), 0, 0, "full resistance boundary");
        var runtime = cls("combat.internal.platform.PlatformWorldLifecycle").existing(level);
        var holders = new (Java.loadClass("java.util.ArrayList"))();
        var CombatAttributes = cls("attribute.api.CombatAttributes");
        holders.add(CombatAttributes.lifesteal()); holders.add(CombatAttributes.dodgeChance());
        var snapshot = runtime.captureAttributes(plain, holders);
        var empty = Java.loadClass("java.util.List").of();
        var panel = new (cls("presentation.api.CombatPanelSnapshot"))(snapshot, plain.getHealth(), 0, empty, empty);
        var rows = cls("presentation.api.CombatPanelProjector").project(panel, level.getGameTime()).getAsJsonArray("rows");
        var found = 0;
        for (var i = 0; i < rows.size(); i++) {
            var row = rows.get(i);
            if (!row.isJsonObject() || !row.getAsJsonObject().has("attribute")) continue;
            var object = row.getAsJsonObject(), id = String(object.get("attribute").getAsString());
            if (id === "combatnexus:lifesteal") { check(String(object.get("unit").getAsString()) === "CONVERSION_RATIO" && !object.get("reserved").getAsBoolean(), "server projects lifesteal semantics"); found++; }
            if (id === "combatnexus:dodge_chance") { check(!object.get("reserved").getAsBoolean(), "server projects consumed dodge status"); found++; }
        }
        check(found === 2, "both requested panel attributes captured");
    } finally {
        for (var e = 0; e < entities.length; e++) entities[e].discard();
        Definitions.publish(server, new Generation(Definitions.nextGeneration(), original.settings(), original.damage(), original.behaviors(),original.domains()));
    }
    return JSON.stringify({passed: checks.length, checks: checks, scope: "isolated server, all modes/flags; no real reload or client transport"});
})()
