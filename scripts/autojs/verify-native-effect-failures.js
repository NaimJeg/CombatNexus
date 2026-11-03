// Real AttributeInstance writes followed by controlled exceptions in the common writer port.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var prefix = "io.github.naimjeg.combatnexus.";
    function cls(name) { return Java.loadClass(prefix + name); }
    var Id = Java.loadClass("net.minecraft.resources.Identifier"), List = Java.loadClass("java.util.List");
    var Set = Java.loadClass("java.util.Set"), OptionalLong = Java.loadClass("java.util.OptionalLong");
    var Attributes = Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes");
    var Operation = Java.loadClass("net.minecraft.world.entity.ai.attributes.AttributeModifier$Operation");
    var level = AutoJS.server().overworld();
    var cow = Java.loadClass("net.minecraft.world.entity.EntityType").COW.create(level,
        Java.loadClass("net.minecraft.world.entity.EntitySpawnReason").COMMAND);
    cow.setPos(0, 100, 0); level.addFreshEntity(cow);
    var installs = 0, failSecond = false, failRemove = false;
    var Writer = cls("attribute.internal.EffectAttributeContributions$ModifierWriter");
    var writer = new Writer({
        install: function (attribute, modifier) {
            attribute.addTransientModifier(modifier); installs++;
            if (failSecond && installs === 2) throw new (Java.loadClass("java.lang.IllegalStateException"))("after second native installation");
        },
        remove: function (attribute, id) {
            attribute["removeModifier(net.minecraft.resources.Identifier)"](id);
            if (failRemove) { failRemove = false; throw new (Java.loadClass("java.lang.IllegalStateException"))("after native removal"); }
        }
    });
    var rawPort = new (cls("attribute.internal.EffectAttributeContributions"))(level, writer);
    var failures = [];
    var port = new (cls("effect.spi.EffectContributionPort"))({prepare:function(change) {
        var prepared = rawPort.prepare(change);
        function capture(receipt) { if (receipt.failure() !== null) failures.push(String(receipt.failure())); return receipt; }
        return new (cls("effect.spi.EffectContributionPort$Prepared"))({
            apply:function() { return capture(prepared.apply()); },
            reconcile:function() { return capture(prepared.reconcile()); }
        });
    }});
    var service = new (cls("effect.internal.DefaultEffectService"))(function () { return false; }, port);
    var now = level.getGameTime(); service.tick(now);
    var effectId = Id.parse("combatnexus:native_failure"), source = Id.parse("combatnexus:cast_a");
    function effect(amount) {
        var Contribution = cls("attribute.api.AttributeSnapshot$Contribution");
        var Attribute = cls("effect.api.EffectInstance$AttributeContribution");
        return new (cls("effect.api.EffectInstance"))(
            new (cls("effect.api.EffectSnapshot"))(cow.getUUID(), effectId, source, 0, OptionalLong.of(now + 1000), Set.of()),
            List.of(new Attribute(Id.parse("minecraft:movement_speed"), new Contribution(effectId, amount, Operation.ADD_VALUE)),
                    new Attribute(Id.parse("minecraft:armor"), new Contribution(effectId, amount, Operation.ADD_VALUE))),
            cls("effect.api.EffectInstance").Ownership.MANAGED);
    }
    var speed = cow.getAttribute(Attributes.MOVEMENT_SPEED), armor = cow.getAttribute(Attributes.ARMOR);
    var speedBefore = speed.getValue(), armorBefore = armor.getValue(), checks = [];
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    try {
        failSecond = true;
        var rejected = false;
        try { service.apply(effect(1), cls("effect.api.EffectService").Stacking.REPLACE); } catch (error) { rejected = true; }
        check(rejected && service.snapshot(cow.getUUID()).isEmpty(), "second native install failure leaves no committed active effect");
        check(Math.abs(speed.getValue()-speedBefore)<0.0001 && Math.abs(armor.getValue()-armorBefore)<0.0001,
            "all partial native installations compensated");
        failSecond = false; installs = 0;
        service.apply(effect(1), cls("effect.api.EffectService").Stacking.REPLACE);
        failSecond = true; installs = 0; rejected = false;
        try { service.apply(effect(2), cls("effect.api.EffectService").Stacking.REPLACE); } catch (error) { rejected = true; }
        check(rejected && service.snapshot(cow.getUUID()).getFirst().attributes().getFirst().modifier().amount() === 1,
            "failed refresh preserves previous active definition");
        check(Math.abs(speed.getValue()-speedBefore-1)<0.0001 && Math.abs(armor.getValue()-armorBefore-1)<0.0001,
            "failed refresh restores both previous native modifiers");
        failSecond = false; failRemove = true; rejected = false;
        try { service.remove(cow.getUUID(), effectId, source, cls("effect.api.EffectService").Removal.REMOVED); } catch (error) { rejected = true; }
        check(rejected && service.snapshot(cow.getUUID()).size() === 1, "native revocation failure keeps old active source");
        check(Math.abs(speed.getValue()-speedBefore-1)<0.0001 && Math.abs(armor.getValue()-armorBefore-1)<0.0001,
            "native revocation failure reconciles original contributions");
        service.remove(cow.getUUID(), effectId, source, cls("effect.api.EffectService").Removal.REMOVED);
        check(service.pendingSources(cow.getUUID()).isEmpty() && service.snapshot(cow.getUUID()).isEmpty(),
            "reconciled source can be removed without residual installation");
    } finally { failSecond = false; failRemove = false; try { service.close(); } catch(error) { throw new Error(JSON.stringify(failures)); } finally { cow.discard(); } }
    return JSON.stringify({passed:checks.length, checks:checks});
})()
