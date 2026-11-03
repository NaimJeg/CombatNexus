// Explicit immutable handles survive adoption; no ambient definition scope exists.
(function () {
    Java.loadClass("io.github.naimjeg.combatnexus.combat.spi.CombatWorldLifecycleAccess").beginTick(Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.platform.PlatformWorldLifecycle").existing(AutoJS.server().overworld()));
    var Definitions=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.definition.CombatDefinitions");
    var Snapshot=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.definition.DefinitionGeneration");
    var Settings=Java.loadClass("io.github.naimjeg.combatnexus.combat.config.CombatServerSettings");
    var Formula=Java.loadClass("io.github.naimjeg.combatnexus.formula.CombatFormulaSettings");
    var server=AutoJS.server(),original=Definitions.forServer(server),checks=[];
    function check(ok,label){if(!ok)throw new Error(label);checks.push(label);}
    try {
        var changed=new Snapshot(Definitions.nextGeneration(),new Settings(new Formula(30,75,7),
            original.settings().vanillaCompatibility(),new (Java.loadClass("io.github.naimjeg.combatnexus.combat.config.CombatSafetySettings"))(4,8,128)),original.damage(),original.behaviors(),original.domains());
        Definitions.publish(server,changed);
        var kernel=Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime").of(server);
        check(kernel.limits().equals(changed.settings().safety().combatLimits()),"budget matches adopted generation");
        var denied=false;
        try {Java.loadClass("io.github.naimjeg.combatnexus.combat.internal.transaction.CombatServerRuntime").configure(server,original.settings().safety().combatLimits());} catch(error){denied=true;}
        check(denied&&kernel.limits().equals(changed.settings().safety().combatLimits()),"no independent budget configuration entry");
        check(Definitions.forServer(server)===changed,"new requests observe adopted generation");
        check(original.settings().formulas().asymptoticKValue()===15 && original.damage()===changed.damage(),
            "in-flight explicit handle retains its old settings and shared immutable catalog");
        check(changed.generation()>original.generation(),"new request generation is independent of retained old input");
        var stale=false;try{Definitions.publish(server,original);}catch(error){stale=true;}
        check(stale && Definitions.forServer(server)===changed,"stale generation cannot replace adopted definitions");
    } finally {Definitions.publish(server,new Snapshot(Definitions.nextGeneration(),original.settings(),
        original.damage(),original.behaviors(),original.domains()));}
    return JSON.stringify({passed:checks.length,checks:checks});
})()
