// Run on a disposable server. Callback failures retain their original isolation semantics.
(function () {
    function cls(name) { return Java.loadClass("io.github.naimjeg.combatnexus." + name); }
    var List = Java.loadClass("java.util.List"), Optional = Java.loadClass("java.util.Optional");
    var UUID = Java.loadClass("java.util.UUID"), Id = Java.loadClass("net.minecraft.resources.Identifier");
    var Failure = Java.loadClass("java.lang.IllegalStateException"), checks = [];
    function check(ok, label) { if (!ok) throw new Error(label); checks.push(label); }
    var Reaction = cls("combat.api.reaction.SettlementReaction"), Set = cls("combat.api.reaction.ReactionSet");
    var Result = cls("combat.api.reaction.ReactionResult"), actions = 0, facts = [];
    var reactions = new Set(List.of(
        new Reaction(Id.parse("probe:failed"), 10, function (c) { return Optional.empty(); },
            function (c) { throw new Failure("reaction probe"); }),
        new Reaction(Id.parse("probe:next"), 0, function (c) { return Optional.empty(); },
            function (c) { actions++; return Result.completed(); })));
    reactions.dispatch("context", 1, UUID.randomUUID(), Optional.empty(), function (fact) { facts.push(String(fact.outcome())); });
    check(actions === 1 && facts.join(",") === "callback_failed,executed", "failed reaction retains result and later reaction executes once");
    reactions.dispatch("context", 2, UUID.randomUUID(), Optional.empty(), function (fact) { throw new Failure("observer probe"); });
    check(actions === 2, "failed observer does not interrupt or replay reactions");
    var candidates = new (Java.loadClass("java.util.ArrayList"))();
    candidates.add(null);
    var stacked = cls("combat.damage.core.rule.DamageRuleStackingResolver").resolve(candidates);
    check(stacked.rules().isEmpty(), "null rule is rejected without dereference");
    return JSON.stringify({passed: checks.length, checks: checks});
})()
