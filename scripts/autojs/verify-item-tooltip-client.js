// Semantic rendering probes, executed inside the actual isolated client via AutoJS.
(function () {
    var root = "io.github.naimjeg.combatnexus.", checks = [];
    function cls(name) { return Java.loadClass(root + name); }
    function list(values) { var out = new (Java.loadClass("java.util.ArrayList"))(); for (var value of values) out.add(value); return out; }
    function check(value, name) { if (!value) throw new Error(name); checks.push(name); }
    var Id = Java.loadClass("net.minecraft.resources.Identifier"), Optional = Java.loadClass("java.util.Optional");
    var Component = Java.loadClass("net.minecraft.network.chat.Component"), Text = cls("presentation.api.DisplayText");
    var unknown = new (cls("combat.damage.api.rule.DamageRuleCondition"))({type:function(){return Id.parse("tooltip_probe:unknown");},test:function(){return false;}});
    var operation = cls("combat.damage.builtin.rule.operation.AddBaseDamageOperation").CODEC.codec().parse(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,
        Java.loadClass("com.google.gson.JsonParser").parseString('{"channel":"combatnexus:physical","value":7}')).getOrThrow();
    var rule = new (cls("combat.damage.api.rule.DamageRuleDefinition"))(Id.parse("tooltip_probe:rule"), cls("combat.damage.api.rule.DamageRuleRole").OFFENSIVE,
        cls("combat.damage.api.enums.DamagePhase").BASE_MODIFICATION, 500, list([unknown]), list([operation]), cls("combat.damage.api.rule.DamageRuleStacking").STACK, Optional.empty());
    function entry(authored) {
        var display = new (cls("combat.damage.api.rule.entry.DamageEntryDisplay"))(Optional.of(Text.literal("Entry")), authored ? list([Text.literal("AUTHORED_SUMMARY")]) : list([]), Optional.empty(), true);
        return new (cls("combat.damage.api.rule.entry.DamageEntryDefinition"))(Id.parse("tooltip_probe:entry"),display,cls("combat.damage.api.rule.entry.DamageEntrySlot").WEAPON,list([rule]),cls("combat.damage.api.rule.entry.DamageEntryStacking").STACK,Optional.empty());
    }
    function render(entries, affixes, expanded) {
        var docs = cls("combat.damage.api.display.DamageDescriptions").project(list(entries),list(affixes));
        var lines = list([]);
        for(var doc of docs) lines.addAll(cls("presentation.client.DescriptionRenderer").render(doc, expanded));
        var text=[]; for(var line of lines)text.push(String(line.getString())); return text.join("\n");
    }
    var unknownPhrase = String(Component.translatable("rule_phrase.combatnexus.unknown_condition.default.compact").getString());
    var compact = render([entry(false)],[],false);
    check(compact.indexOf(unknownPhrase)>=0,"compact unknown condition remains visible");
    check(compact.indexOf("7")>=0,"condition preserves its associated numeric effect");
    check(render([entry(true)],[],true).indexOf("AUTHORED_SUMMARY")>=0,"Shift keeps authored entry summary");
    var display = new (cls("combat.damage.api.rule.affix.DamageAffixDisplay"))(Optional.of(Text.literal("Affix")),list([Text.literal("AFFIX_SUMMARY")]),Optional.empty(),false);
    var affix = new (cls("combat.damage.api.rule.affix.DamageAffixDefinition"))(Id.parse("tooltip_probe:affix"),display,cls("combat.damage.api.rule.affix.DamageAffixSlot").ITEM,cls("combat.damage.api.rule.affix.DamageAffixRarity").COMMON,list([entry(false)]),cls("combat.damage.api.rule.affix.DamageAffixStacking").STACK,Optional.empty());
    for(var expanded of [false,true]) {
        var text=render([], [affix], expanded);
        check(text.indexOf("AFFIX_SUMMARY")>=0 && text.indexOf(unknownPhrase)<0 && text.indexOf("Entry")<0,"affix opt-out hides nested rules " + expanded);
    }
    var Numbers=cls("presentation.api.TooltipNumbers");
    check(String(Numbers.number(3))==="3" && String(Numbers.number(1.235))==="1.24","integer and decimal formatting");
    check(String(Numbers.number(0.0001))==="<0.01" && String(Numbers.number(-0.0001))===">-0.01","small nonzero values are not displayed as zero");
    check(String(Numbers.percent(0.00001))==="<0.01%","small nonzero probabilities are retained");
    var blocks=list([new (cls("presentation.api.TooltipBlock"))(Component.literal("Effect"),list([Component.literal("SUMMARY")]),list([Component.literal("SUMMARY"),Component.literal("DETAIL")]))]);
    var output=list([]);output=cls("presentation.client.EffectTooltipRenderer").append(output,blocks,true);
    var all=[];for(var line of output)all.push(String(line.getString()).trim());
    check(all.filter(function(s){return s==="SUMMARY";}).length===1 && all.indexOf("DETAIL")>=0,"effect details supplement summary without duplication");
    var Ops=Java.loadClass("com.mojang.serialization.JsonOps"),Json=Java.loadClass("com.google.gson.JsonParser"),Resolver=cls("presentation.client.DisplayTextResolver");
    function decoded(value){return Text.CODEC.parse(Ops.INSTANCE,Json.parseString(JSON.stringify(value))).getOrThrow();}
    function rendered(value){return String(Resolver.resolve(decoded(value)).getString());}
    check(rendered({translate:"tooltip.combatnexus.text.concat",args:[{nested:{translate:"tooltip.combatnexus.effect.periodic",args:[{kind:"DURATION",value:40},{kind:"NUMBER",value:3.25}]}},{kind:"PERCENT",value:0.25}]}).indexOf("25%")>=0,"nested typed percent preserves percent sign");
    check(rendered({translate:"tooltip.combatnexus.text.concat",args:["effect.minecraft.speed","literal"]}).indexOf("effect.minecraft.speed")>=0,"legacy string arguments remain literal");
    var missing=rendered({translate:"tooltip_probe.missing"});
    check(missing===String(Component.translatable("tooltip.combatnexus.unlocalized_display_text").getString()),"missing translation has explicit fallback");
    check(rendered({translate:"tooltip_probe.missing",fallback:"Amount: %s",args:[{kind:"NUMBER",value:4.5}]}).indexOf("4.5")>=0,"authored fallback keeps typed values");
    var rejected=false;try{decoded({translate:"tooltip.combatnexus.effect.duration",args:[{kind:"DURATION",value:-1}]});}catch(e){rejected=true;}check(rejected,"negative duration rejected");
    rejected=false;try{new (cls("presentation.api.DisplayArgument$Numeric"))(cls("presentation.api.DisplayArgument$Kind").NUMBER,NaN);}catch(e){rejected=true;}check(rejected,"nonfinite number rejected");
    var Bindings=cls("presentation.api.DescriptionBindings"),Map=Java.loadClass("java.util.Map"),Arg=cls("presentation.api.DisplayArgument");
    var bound=Bindings.bind(decoded({translate:"tooltip_probe.summary",fallback:"Adopted %s",args:[{bind:"amount"}]}),Map.of("amount",Arg.number(17)));
    check(String(Resolver.resolve(bound).getString()).indexOf("17")>=0,"named summary uses supplied adopted parameter");
    rejected=false;try{Bindings.bind(decoded({translate:"tooltip_probe.summary",args:[{bind:"unknown"}]}),Map.of());}catch(e){rejected=true;}check(rejected,"missing descriptor parameter rejected");
    var Doc=cls("presentation.api.DescriptionDocument"),Rule=cls("presentation.api.DescriptionRule"),Condition=cls("presentation.api.DescriptionRule$Condition"),Kind=cls("presentation.api.DescriptionRule$Kind"),RuleText=cls("presentation.api.DescriptionRule$Text");
    function leaf(name){return new Condition(Kind.PHRASE,Optional.of(new RuleText(Text.literal(name),Text.literal(name))),list([]));}
    var tree=new Condition(Kind.AND,Optional.empty(),list([leaf("A"),new Condition(Kind.OR,Optional.empty(),list([leaf("B"),new Condition(Kind.NOT,Optional.empty(),list([leaf("C")]))]))]));
    var document=new Doc(Optional.of(Text.literal("TITLE")),list([Text.literal("SUMMARY")]),list([]),list([Text.literal("DETAIL")]),Optional.of(Text.literal("FLAVOR")),list([]),list([new Rule(tree,list([new RuleText(Text.literal("OP1"),Text.literal("OP1")),new RuleText(Text.literal("OP2"),Text.literal("OP2"))]),Text.literal("ROLE"))]),true);
    var encoded=Doc.CODEC.encodeStart(Ops.INSTANCE,document).getOrThrow(),round=Doc.CODEC.parse(Ops.INSTANCE,encoded).getOrThrow();
    var expandedLines=cls("presentation.client.DescriptionRenderer").render(round,true),flat=[];for(var line of expandedLines)flat.push(String(line.getString()).trim());
    check(flat.indexOf("SUMMARY")>=0&&flat.indexOf("DETAIL")>=0&&flat.indexOf("FLAVOR")>=0,"summary details and flavor coexist");
    check(flat.indexOf("OP1")<flat.indexOf("OP2"),"wire round-trip preserves operation order");
    for(var key of ["all_of","any_of","not"])check(flat.indexOf(String(Component.translatable("rule_condition_group.combatnexus."+key).getString()))>=0,"wire retains logical group "+key);
    var compactLines=cls("presentation.client.DescriptionRenderer").render(round,false),compactFlat=[];for(var line of compactLines)compactFlat.push(String(line.getString()).trim());
    check(compactFlat.indexOf("SUMMARY")>=0&&compactFlat.indexOf("OP1")<0,"compact prioritizes summary");
    var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),Items=Java.loadClass("net.minecraft.world.item.Items"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack");
    var Components=Java.loadClass("net.minecraft.core.component.DataComponents"),Registries=Java.loadClass("net.minecraft.core.registries.Registries"),Enchantment=Java.loadClass("net.minecraft.world.item.enchantment.Enchantment");
    var sharpness=mc.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Java.loadClass("net.minecraft.world.item.enchantment.Enchantments").SHARPNESS);
    var EnchantmentBuilder=Java.loadClass("net.minecraft.world.item.enchantment.Enchantment$Builder"),EffectComponents=Java.loadClass("net.minecraft.world.item.enchantment.EnchantmentEffectComponents"),LevelValue=Java.loadClass("net.minecraft.world.item.enchantment.LevelBasedValue");
    var multiply=new (Java.loadClass("net.minecraft.world.item.enchantment.effects.MultiplyValue"))(LevelValue.constant(2.5));
    var numericEnchant=new EnchantmentBuilder(sharpness.value().definition())["withEffect(net.minecraft.core.component.DataComponentType,java.lang.Object)"](EffectComponents.DAMAGE,multiply).build(Id.parse("tooltip_probe:multiply"));
    var numericStack=new Stack(Items.DIAMOND_SWORD);
    numericStack.enchant(Java.loadClass("net.minecraft.core.Holder").direct(numericEnchant),1);
    var itemClient=cls("presentation.platform.client.CombatPresentationClient"),numericProjection=itemClient.item(numericStack);
    check(numericProjection.isPresent()&&String(numericProjection.get()).indexOf("2.5")>=0,"multiply reads native factor field through local ItemStack projection");
    var conditionalEnchant=new EnchantmentBuilder(sharpness.value().definition()).withEffect(EffectComponents.DAMAGE,multiply,
        Java.loadClass("net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition").randomChance(0.5)).build(Id.parse("tooltip_probe:unknown_condition"));
    var conditionalStack=new Stack(Items.DIAMOND_SWORD);
    conditionalStack.enchant(Java.loadClass("net.minecraft.core.Holder").direct(conditionalEnchant),1);
    var failed=itemClient.item(conditionalStack),originalLines=list([Component.literal("VANILLA"),Component.literal("OTHER_MOD")]);
    var unchanged=cls("combat.damage.client.tooltip.ItemDamageRuleTooltip").append(conditionalStack,originalLines,true,false,failed);
    check(failed.isEmpty()&&unchanged.size()===2&&String(unchanged.get(0).getString())==="VANILLA"&&String(unchanged.get(1).getString())==="OTHER_MOD","unknown native condition preserves every original line without placeholder");
    for(var item of [Items.WOLF_ARMOR,Items.SHIELD,Items.TOTEM_OF_UNDYING,Items.TURTLE_HELMET]) {
        var description=itemClient.item(new Stack(item));
        check(description.isPresent()&&String(description.get()).indexOf("tooltip.combatnexus.vanilla_combat.")>=0,"registered typed combat component descriptor: "+item);
    }
    var flameStack=new Stack(Items.BOW);
    flameStack.enchant(mc.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Java.loadClass("net.minecraft.world.item.enchantment.Enchantments").FLAME),1);
    check(String(itemClient.item(flameStack).get()).indexOf("vanilla_combat.flame")>=0,"registered enchantment combat text for Flame");
    var enchantRegistry=mc.player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
    for(var combatName of ["fire_aspect","flame","thorns","bane_of_arthropods","channeling","wind_burst","lunge","riptide","loyalty","multishot","piercing","quick_charge","infinity","frost_walker","soul_speed","unbreaking"]){
        var enchantHolder=enchantRegistry.get(Id.parse("minecraft:"+combatName)).orElseThrow();
        for(var combatLevel of [1,enchantHolder.value().getMaxLevel()]){
            var combatStack=new Stack(Items.DIAMOND_SWORD);combatStack.enchant(enchantHolder,combatLevel);
            var combatDescription=itemClient.item(combatStack);
            check(combatDescription.isPresent()&&String(combatDescription.get()).indexOf("vanilla_combat."+combatName)>=0,"typed combat enchantment at level "+combatLevel+": "+combatName);
            for(var expandedCombat of [false,true]){
                var combatLines=cls("combat.damage.client.tooltip.ItemDamageRuleTooltip").append(combatStack,originalLines,expandedCombat,false,combatDescription),combined="";
                for(var combatLine of combatLines)combined+=combatLine.getString();
                check(combatLines.size()>originalLines.size()&&combined.indexOf("tooltip.combatnexus.vanilla_combat.")<0&&String(combatLines.get(0).getString())==="VANILLA"&&String(combatLines.get(1).getString())==="OTHER_MOD","localized combat append preserves existing rows: "+combatName+" level="+combatLevel+" expanded="+expandedCombat);
            }
        }
    }
    var effectText=cls("effect.api.EffectTooltip");
    var healingBase=String(effectText.capability(Id.parse("minecraft:instant_health"),1).get(1).getString());
    check(healingBase.indexOf("8")>=0&&healingBase.indexOf("12")>=0,"instant healing II describes base healing and inverted damage at its actual amplifier");
    var poisonBase=String(effectText.capability(Id.parse("minecraft:poison"),1).get(1).getString());
    check(poisonBase.indexOf("12")>=0&&poisonBase.indexOf("1")>=0,"poison II describes its twelve-tick period and one-health floor");
    check(String(effectText.capability(Id.parse("minecraft:regeneration"),1).get(1).getString()).indexOf("25")>=0,"regeneration II describes its twenty-five-tick period");
    var origin=new (cls("combat.api.source.SourceProvenance"))(null,Id.parse("minecraft:golden_apple"),Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,Id.parse("minecraft:bane_of_arthropods"),new (cls("combat.api.source.SourceProvenance$Block"))(Id.parse("minecraft:overworld"),1,64,-2));
    var originText=String(cls("presentation.api.SourceProvenanceText").describe(origin).getString());
    check(originText.indexOf("minecraft:golden_apple")>=0&&originText.indexOf("minecraft:bane_of_arthropods")>=0&&originText.indexOf("minecraft:overworld")>=0&&originText.indexOf("tooltip.combatnexus")<0,"typed source facts render item slot enchantment and block in selected language");
    check(String(cls("presentation.api.SourceProvenanceText").describe(cls("combat.api.source.SourceProvenance").UNKNOWN).getString()).indexOf("tooltip.combatnexus")<0,"unknown source remains explicitly unknown with localized text");
    return JSON.stringify({passed:checks.length,checks:checks,compact:compact});
})()
