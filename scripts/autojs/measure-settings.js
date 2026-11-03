(function () {
    var root = "io.github.naimjeg.combatnexus.combat.damage.";
    var Store = Java.loadClass(root + "core.config.DamageSettingsStore");
    var Api = Java.loadClass(root + "api.DamageModuleApi");
    var Request = Java.loadClass(root + "api.damage.DamageRequest");
    var Source = Java.loadClass(root + "api.damage.DamageSourceDescriptor");
    var EntityType = Java.loadClass("net.minecraft.world.entity.EntityType");
    var Reason = Java.loadClass("net.minecraft.world.entity.EntitySpawnReason");
    var Attributes = Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");
    var server = AutoJS.server();
    var level = server.overworld();
    var settings = Store.current(server);
    var victim = EntityType.COW.create(level, Reason.COMMAND);
    victim.setPos(0, 100, 0);
    victim.getAttribute(Attributes.ARMOR).setBaseValue(10);
    level.addFreshEntity(victim);
    try {
        var source = Source.from(victim.damageSources().mobAttack(victim));
        var result = Api.submitDamage(Request.builder(level, victim, source, 8).build());
        if (String(result.status()) !== "APPLIED") throw new Error("Expected applied damage");
        if (Store.current(server) !== settings) throw new Error("Unexpected settings replacement during damage");
        return JSON.stringify({revision:settings.revision(), armorK:settings.values().formulas().asymptoticKValue(),
            resistanceK:settings.values().formulas().resistanceKValue(), rating:settings.values().formulas().ratingPerProtScore(),
            damage:result.resolvedDamage(), depth:settings.values().safety().maxRecursionDepth(),
            runtimeDepth:Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime").of(server).limits().maximumDepth()});
    } finally { victim.discard(); }
})()
