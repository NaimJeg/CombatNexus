// Requires a prepared disposable server. Exercises final health writes through real damage requests.
(function () {
    function cls(name) { return Java.loadClass("io.github.naimjeg.combatnexus." + name); }
    var level = AutoJS.server().overworld(), checks = [];
    cls("combat.spi.CombatWorldLifecycleAccess").beginTick(cls("combat.internal.platform.PlatformWorldLifecycle").existing(level));
    var cow = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0, 100, 0); cow.setNoAi(true); level.addFreshEntity(cow);
    var api = cls("combat.api.CombatAccess").find(level).orElseThrow();
    var Request = cls("combat.damage.api.damage.DamageRequest"), Descriptor = cls("combat.damage.api.damage.DamageSourceDescriptor");
    var descriptor = Descriptor.of(Java.loadClass("net.minecraft.world.damagesource.DamageTypes").MAGIC);
    function check(value, label) { if (!value) throw new Error(label); checks.push(label); }
    function hit(floor) { cow.invulnerableTime = 0; return api.damage(Request.builder(level, cow, descriptor, 50).minimumHealth(floor).build()); }
    try {
        check(level.getEntity(cow.getUUID()) !== null, "target is available");
        cow.setHealth(3);
        hit(1);
        check(cow.getHealth() === 1 && cow.isAlive(), "large resolved damage respects final health floor");
        cow.setHealth(0.5);
        hit(1);
        check(cow.getHealth() === 0.5, "health floor cannot heal an already lower-health target");
        cow.setHealth(3);
        hit(10);
        check(cow.getHealth() === 3, "floor above current health prevents loss without increasing health");
        var rejected = false;
        try { Request.builder(level, cow, descriptor, 1).minimumHealth(-1); } catch (e) { rejected = true; }
        check(rejected, "negative health floor is rejected");
        rejected = false;
        try { Request.builder(level, cow, descriptor, 1).minimumHealth(NaN); } catch (e) { rejected = true; }
        check(rejected, "nonfinite health floor is rejected");
        hit(0);
        check(cow.getHealth() === 0, "a later unrestricted request receives no leftover protection");
        return JSON.stringify({passed:checks.length,checks:checks,scope:"generic request constraint; native poison and starvation entrypoints are not yet replaced"});
    } finally { cow.discard(); }
})()
