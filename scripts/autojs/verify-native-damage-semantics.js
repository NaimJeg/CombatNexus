// The runner inserts the committed classification matrix; actual runtime tags/catalog are the subject under test.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var matrix = [];
    // MATRIX
    var prefix = "io.github.naimjeg.combatnexus.";
    function cls(n) { return Java.loadClass(prefix + n); }
    var Id = Java.loadClass("net.minecraft.resources.Identifier"), Key = Java.loadClass("net.minecraft.resources.ResourceKey");
    var Registries = Java.loadClass("net.minecraft.core.registries.Registries");
    var DamageSource = Java.loadClass("net.minecraft.world.damagesource.DamageSource");
    var level = AutoJS.server().overworld(), registry = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
    var definitions = cls("combat.internal.definition.CombatDefinitions").forServer(AutoJS.server());
    var channels = definitions.damage().channels(), checks = [], entities = [];
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    function source(id) { return new DamageSource(registry.getOrThrow(Key.create(Registries.DAMAGE_TYPE, Id.parse(id)))); }
    function spawn(type, world) { var e = type.create(world, Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        e.setPos(0,100,0);world.addFreshEntity(e);entities.push(e);return e; }
    check(matrix.length === 51, "runner supplies complete versioned matrix");
    for (var i = 0; i < matrix.length; i++) {
        var row = matrix[i], s = source(row.id);
        check(String(channels.initialChannel(s).id()) === row.channel && cls("combat.damage.core.pipeline.DamageSourcePolicy").shouldManage(s) === row.managed,
            row.id + " channel and ownership");
        var Tag = Java.loadClass("net.minecraft.tags.TagKey");
        for (var j = 0; j < row.categories.length; j++) check(s.is(Tag.create(Registries.DAMAGE_TYPE, Id.parse("combatnexus:is_" + row.categories[j]))), row.id + " category " + row.categories[j]);
    }
    try {
        var Types = Java.loadClass("net.minecraft.world.entity.EntityType"), cow = spawn(Types.COW, level), arrow = spawn(Types.ARROW, level);
        var holder = Java.loadClass("net.minecraft.core.Holder").direct(new (Java.loadClass("net.minecraft.world.damagesource.DamageType"))("player",0.1));
        var unknown = new DamageSource(holder, cow, cow);
        var profile = cls("combat.damage.bridge.vanilla.VanillaDamageSourceProfile").create(unknown,cow,cow,false);
        check(!profile.playerAttack() && !profile.mobAttack(), "same msgId cannot impersonate vanilla player attack");
        check(!cls("combat.damage.core.pipeline.DamageSourcePolicy").shouldManage(unknown), "unregistered damage source remains outside managed requests");
        var Crit = cls("combat.damage.bridge.vanilla.VanillaCriticalCapture");
        Crit.capture(cow.getId(), cow.getId(), level.getGameTime(), 1.5);
        check(Crit.consumePendingVanillaCritical(cow,cow,unknown) === null, "same msgId cannot consume captured vanilla critical");
        var Capture = cls("combat.damage.bridge.vanilla.VanillaDamageCapture");
        var ItemStack = Java.loadClass("net.minecraft.world.item.ItemStack");
        Capture.captureModifyDamage(level,ItemStack.EMPTY,cow,unknown,4,5);
        var captured = Capture.consumeOffensiveSnapshot(unknown,cow,7);
        check(String(captured.preEventDelta().kind()) === "UNKNOWN", "unknown native delta remains opaque despite vanilla-looking msgId");
        var plan = cls("combat.damage.bridge.vanilla.VanillaBridgePlan").from(7,profile,null,3,3);
        check(plan.initialBaseAmount() === 7 && !plan.rebuildOffensiveMobEffects() && !plan.rebuildOffensiveEnchantment(), "missing capture preserves opaque damage despite observed effects");
        var Input = cls("combat.damage.core.pipeline.DamageInput"), Factory = cls("combat.damage.core.pipeline.DamageModuleContextFactory");
        var attributed = new DamageSource(registry.getOrThrow(Key.create(Registries.DAMAGE_TYPE,Id.parse("minecraft:arrow"))),arrow,cow);
        var origin = Factory.nativeOrigin(new Input(cow,attributed,2));
        check(origin.attribution().directEntity() === arrow && origin.attribution().logicalAttacker() === cow && origin.attribution().equipmentOwner() === cow,
            "projectile preserves direct entity and native shooter roles");
        var ownerless = new DamageSource(registry.getOrThrow(Key.create(Registries.DAMAGE_TYPE,Id.parse("minecraft:arrow"))),arrow);
        var noOwner = Factory.nativeOrigin(new Input(cow,ownerless,2));
        check(noOwner.attribution().logicalAttacker() === null && noOwner.attribution().equipmentOwner() === null, "ownerless projectile cannot borrow equipment");
        var nonliving = new DamageSource(registry.getOrThrow(Key.create(Registries.DAMAGE_TYPE,Id.parse("minecraft:generic"))),arrow,arrow);
        var nonlivingOrigin = Factory.nativeOrigin(new Input(cow,nonliving,2));
        check(nonlivingOrigin.attribution().effectOwner() === arrow && nonlivingOrigin.attribution().equipmentOwner() === null, "nonliving native effect owner does not imply equipment authority");
        arrow.discard();
        var stale = Factory.nativeOrigin(new Input(cow,attributed,2));
        check(stale.attribution().directEntity() === null && stale.attribution().logicalAttacker() === cow, "removed direct entity normalized without discarding valid shooter");
        var foreign = Types.COW.create(AutoJS.server().getLevel(Java.loadClass("net.minecraft.world.level.Level").NETHER),Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
        var foreignOrigin = Factory.nativeOrigin(new Input(cow,new DamageSource(registry.getOrThrow(Key.create(Registries.DAMAGE_TYPE,Id.parse("minecraft:mob_attack"))),foreign,foreign),2));
        check(foreignOrigin.attribution().logicalAttacker() === null && foreignOrigin.attribution().effectOwner() === null, "cross-world identities cannot supply combat attribution");
        foreign.discard();
    } finally { for (var k=0;k<entities.length;k++) entities[k].discard(); }
    return JSON.stringify({passed:checks.length,checks:checks});
})()
