"""Item tooltip acceptance on a disposable AutoJS server/client; never run against a personal world."""
import argparse
import json
from pathlib import Path
import time
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--server-port', type=int, default=25580)
parser.add_argument('--client-port', type=int, default=25581)
parser.add_argument('--game-port', type=int, default=25579)
parser.add_argument('--run-directory', default='build/carried-tooltip-run')
parser.add_argument('--language', choices=['en_us', 'zh_cn'], default='en_us')
args = parser.parse_args()
checks = []


def evaluate(code, client=False):
    request = urllib.request.Request(f'http://127.0.0.1:{args.client_port if client else args.server_port}/mcp', json.dumps({
        'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {'name': 'eval_js',
        'arguments': {'side': 'client' if client else 'server', 'code': code, 'timeout_ms': 20000}}}).encode(), {'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=30) as response:
        message = json.load(response)
    assert 'error' not in message, message
    result = message['result']
    assert not result.get('isError'), result
    value = json.loads(result['content'][0]['text'])
    assert value['ok'] and not value.get('diagnostics'), value
    return json.loads(value['value'])


def wait(code, predicate, client=True):
    deadline = time.monotonic() + 30
    while True:
        value = evaluate(code, client)
        if predicate(value):
            return value
        assert time.monotonic() < deadline, value
        time.sleep(.25)


evaluate('''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();
mc.getLanguageManager().setSelected(''' + json.dumps(args.language) + ''');mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());JSON.stringify({language:mc.getLanguageManager().getSelected()})''', True)
evaluate('''(function(){
var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.options.pauseOnLostFocus=false;
if(mc.player===null){
var Data=Java.loadClass("net.minecraft.client.multiplayer.ServerData"),address="127.0.0.1:PORT";
Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),mc,
Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress").parseString(address),new Data("Tooltip verification",address,Data.Type.OTHER),false,null);
}return JSON.stringify({connecting:true});})()'''.replace('PORT', str(args.game_port)), True)
wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null})', lambda s: s['ready'])
semantic = evaluate((ROOT / 'scripts/autojs/verify-item-tooltip-client.js').read_text(encoding='utf-8'), True)
checks.extend(semantic['checks'])

evaluate('''(function(){
var server=AutoJS.server(),p=server.getPlayerList().getPlayerByName("SettingsVerifier");
server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode creative SettingsVerifier");
var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items");
var stack=new Stack(Items.DIAMOND_SWORD),Id=Java.loadClass("net.minecraft.resources.Identifier");
var Registry=Java.loadClass("net.minecraft.core.registries.Registries"),Key=Java.loadClass("net.minecraft.resources.ResourceKey");
stack.enchant(server.registryAccess().lookupOrThrow(Registry.ENCHANTMENT).getOrThrow(Key.create(Registry.ENCHANTMENT,Id.parse("minecraft:sharpness"))),4);
var Bindings=Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings"),Json=Java.loadClass("com.google.gson.JsonParser"),Ops=Java.loadClass("com.mojang.serialization.JsonOps");
stack.set(Bindings.component(),Bindings.CODEC.parse(Ops.INSTANCE,Json.parseString('[{"id":"tooltip_probe:agility","kind":"EFFECT","definition":"combatnexus:agility","slots":["mainhand"]}]')).getOrThrow());
p.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,stack);p.containerMenu.broadcastChanges();
return JSON.stringify({given:true});})()''')

tooltip_code = '''(function(){
var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),stack=mc.player.getMainHandItem();
var root="io.github.naimjeg.combatnexus.",lines=new (Java.loadClass("java.util.ArrayList"))();
var projection=Java.loadClass(root+"presentation.platform.client.CombatPresentationClient").item(stack);
lines=Java.loadClass(root+"combat.damage.client.tooltip.ItemDamageRuleTooltip").append(stack,lines,true,false,projection);
var text=[];for(var line of lines)text.push(String(line.getString()));
var ready=Java.loadClass(root+"presentation.client.ClientTooltipDefinitions").generation()>0;
var unavailable=String(Java.loadClass("net.minecraft.network.chat.Component").translatable("tooltip.combatnexus.presentation.unavailable").getString());
return JSON.stringify({projected:projection.isPresent(),refused:projection.isEmpty()||projection.get().has("error"),ready:ready,lines:text,unavailableVisible:text.indexOf(unavailable)>=0});})()'''
normal = wait(tooltip_code, lambda value: value['ready'] and any('2.5' in line for line in value['lines']) and any('10%' in line for line in value['lines']))
assert any('2.5' in line for line in normal['lines']), normal
assert any('10%' in line for line in normal['lines']), normal
checks.append('vanilla ItemStack sync and shared definitions render enchantment and equipment Effect values')
time.sleep(2.1)
assert evaluate(tooltip_code, True)['projected']
wait(tooltip_code, lambda value: value['projected'] and not value['unavailableVisible'])
checks.append('item component description remains immediately available without expiry')
detached_code = tooltip_code.replace('stack=mc.player.getMainHandItem();', 'stack=mc.player.getMainHandItem().copy();var Reg=Java.loadClass("net.minecraft.core.registries.Registries"),Key=Java.loadClass("net.minecraft.resources.ResourceKey"),Id=Java.loadClass("net.minecraft.resources.Identifier");stack.enchant(mc.level.registryAccess().lookupOrThrow(Reg.ENCHANTMENT).getOrThrow(Key.create(Reg.ENCHANTMENT,Id.parse("minecraft:sharpness"))),7);')
detached = evaluate(detached_code, True)
assert not detached['unavailableVisible'] and any('4' in line for line in detached['lines']), detached
checks.append('detached modified copy displays its own enchantment level immediately')


# Distinct adopted affix values and explicit summary bindings cross the real slot projection.
for amount in (7, 19):
    affix = {'id':'tooltip_probe:adopted','display':{'name':{'text':'ADOPTED_AFFIX'},'authored_summary':[{'translate':'tooltip_probe.adopted','fallback':'ADOPTED %s','args':[{'bind':'amount'}]}],'show_rule_breakdown':True},
        'entries':[{'id':'tooltip_probe:entry','display':{'name':{'text':'ADOPTED_ENTRY'}},'rules':[{'id':'tooltip_probe:rule','phase':'base_modification','operations':[{'type':'combatnexus:add_base_damage','channel':'combatnexus:physical','value':amount}]}]}]}
    evaluate('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),root="io.github.naimjeg.combatnexus.";var value=Java.loadClass(root+"combat.damage.api.rule.affix.DamageAffixDefinition").CODEC.parse(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,Java.loadClass("com.google.gson.JsonParser").parseString('+json.dumps(json.dumps(affix))+')).getOrThrow();p.getMainHandItem().set(Java.loadClass(root+"combat.damage.api.item.DamageComponents").affixes(),Java.loadClass("java.util.List").of(value));p.containerMenu.broadcastChanges();JSON.stringify({given:true})')
    observed=wait(tooltip_code,lambda value:any(('ADOPTED '+str(amount)) in line for line in value['lines']))
    assert not any(('ADOPTED '+str(19 if amount==7 else 7)) in line for line in observed['lines']), observed
    checks.append('adopted affix summary and generated rule use server value '+str(amount))
evaluate('var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.getMainHandItem().remove(Java.loadClass("io.github.naimjeg.combatnexus.combat.damage.api.item.DamageComponents").affixes());p.containerMenu.broadcastChanges();JSON.stringify({removed:true})')

# Read actual native definitions, mutate only an isolated copy, and check both execution-ownership policies.
server_probe = evaluate('''(function(){
var server=AutoJS.server(),level=server.overworld(),root="io.github.naimjeg.combatnexus.";
var world=Java.loadClass(root+"combat.api.CombatAccess").find(level).orElseThrow(),Projector=Java.loadClass(root+"presentation.api.ItemEffectTooltipProjector");
var Id=Java.loadClass("net.minecraft.resources.Identifier"),Reg=Java.loadClass("net.minecraft.core.registries.Registries"),Key=Java.loadClass("net.minecraft.resources.ResourceKey");
var Enchantment=Java.loadClass("net.minecraft.world.item.enchantment.Enchantment"),Holder=Java.loadClass("net.minecraft.core.Holder"),Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items");
var ops=Java.loadClass("net.minecraft.resources.RegistryOps").create(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,server.registryAccess());
var feather=server.registryAccess().lookupOrThrow(Reg.ENCHANTMENT).getOrThrow(Key.create(Reg.ENCHANTMENT,Id.parse("minecraft:feather_falling")));
var data=Enchantment.DIRECT_CODEC.encodeStart(ops,feather.value()).getOrThrow().getAsJsonObject();
data.getAsJsonObject("effects").getAsJsonArray("minecraft:damage_protection").get(0).getAsJsonObject().getAsJsonObject("effect").addProperty("value",42);
var stack=new Stack(Items.DIAMOND_BOOTS);stack.enchant(Holder.direct(Enchantment.DIRECT_CODEC.parse(ops,data).getOrThrow()),4);
function text(managed){var out=[];for(var block of Projector.project(level.registryAccess(),function(id,kind){return Java.loadClass("java.util.Optional").empty();},stack,managed,3.5))for(var line of block.summary())out.push(String(line.getString()));return out.join("\\n");}
var managed=text(true),nativeText=text(false);
return JSON.stringify({managed:managed,nativeText:nativeText,java:String(Java.loadClass("java.lang.System").getProperty("java.version"))});})()''')
assert '42' in server_probe['managed'] and '147' in server_probe['managed'], server_probe
assert '147' not in server_probe['nativeText'], server_probe
checks.append('modified actual enchantment value replaces vanilla constant')
checks.append('managed protection converts adopted score; native policy does not duplicate conversion')

# Exercise real generation invalidation and a missing local definition with a temporary datapack.
pack_root = ROOT / 'targets/neoforge-26.1.2' / args.run_directory / 'world/datapacks'
assert pack_root.is_dir(), 'This test requires the configured disposable world'
with tempfile.TemporaryDirectory(prefix='tooltip-probe-', dir=pack_root) as directory:
    pack = Path(directory)
    name = 'file/' + pack.name
    (pack / 'pack.mcmeta').write_text(json.dumps({'pack': {'description': 'Item tooltip acceptance', 'min_format': [101, 1], 'max_format': [101, 1]}}))
    effect_path = pack / 'data/combatnexus/combatnexus_effects/agility.json'
    effect_path.parent.mkdir(parents=True)
    effect_definition = json.loads((ROOT / 'common/src/main/resources/data/combatnexus/combatnexus_effects/agility.json').read_text())
    effect_definition['attributes'][0]['amount'] = .42
    effect_path.write_text(json.dumps(effect_definition))
    template_path = pack / 'data/tooltip_probe/combatnexus_entry_templates/entry.json'
    template_path.parent.mkdir(parents=True)
    template_path.write_text(json.dumps({'id': 'tooltip_probe:entry', 'display': {'name': {'text': 'SERVER_TEMPLATE'}, 'show_rule_breakdown': True},
        'rules': [{'id': 'tooltip_probe:rule', 'phase': 'base_modification', 'operations': [{'type': 'combatnexus:add_base_damage', 'channel': 'combatnexus:physical', 'value': 9}]}]}))

    def reload_pack(enabled):
        return evaluate('''(function(){var server=AutoJS.server();server.getPackRepository().reload();
var packs=new (Java.loadClass("java.util.ArrayList"))(server.getPackRepository().getSelectedIds());packs.remove(''' + json.dumps(name) + ''');
''' + ('packs.add(' + json.dumps(name) + ');' if enabled else '') + '''
var future=server.reloadResources(packs);return JSON.stringify({done:future.isDone(),failed:future.isCompletedExceptionally()});})()''')
    try:
        assert reload_pack(True) == {'done': True, 'failed': False}
        changed = wait(tooltip_code, lambda value: value['ready'] and any('42%' in line for line in value['lines']))
        assert not any('10%' in line for line in changed['lines']), changed
        checks.append('real datapack reload replaces shared Effect definition with 42%')
        evaluate('''(function(){var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),root="io.github.naimjeg.combatnexus.";
var refs=Java.loadClass(root+"combat.damage.api.item.template.DamageItemTemplateReferences").CODEC.parse(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,
Java.loadClass("com.google.gson.JsonParser").parseString('{"entries":["tooltip_probe:entry"]}')).getOrThrow();
p.getMainHandItem().set(Java.loadClass(root+"combat.damage.api.item.DamageComponents").templates(),refs);p.containerMenu.broadcastChanges();return JSON.stringify({bound:true});})()''')
        wait(tooltip_code, lambda value: value['ready'] and any('SERVER_TEMPLATE' in line for line in value['lines']))
        checks.append('server resolves template references into documents alongside adopted effects')
        effect_definition['attributes'] = [{'attribute': 'minecraft:movement_speed', 'modifier': 'tooltip_probe:m' + str(i), 'amount': .01, 'operation': 'ADD_MULTIPLIED_TOTAL'} for i in range(32)]
        effect_path.write_text(json.dumps(effect_definition))
        bindings = [{'id': 'tooltip_probe:binding' + str(i), 'kind': 'EFFECT', 'definition': 'tooltip_probe:missing', 'slots': ['head']} for i in range(8)]
        evaluate('''var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),Bindings=Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings");
p.getMainHandItem().set(Bindings.component(),Bindings.CODEC.parse(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,Java.loadClass("com.google.gson.JsonParser").parseString(''' + json.dumps(json.dumps(bindings)) + ''')).getOrThrow());p.containerMenu.broadcastChanges();JSON.stringify({bound:true})''')
        assert reload_pack(True) == {'done': True, 'failed': False}
        expanded = wait(tooltip_code, lambda value: value['refused'] and not value['unavailableVisible'])
        checks.append('missing local definition preserves original tooltip without placeholder text')
    finally:
        # Remove the probe reference before removing its definition.
        evaluate('''var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.getMainHandItem().remove(Java.loadClass("io.github.naimjeg.combatnexus.combat.damage.api.item.DamageComponents").templates());p.getMainHandItem().remove(Java.loadClass("io.github.naimjeg.combatnexus.combat.api.behavior.EquipmentBindings").component());p.containerMenu.broadcastChanges();JSON.stringify({removed:true})''')
        assert reload_pack(False) == {'done': True, 'failed': False}
    wait(tooltip_code, lambda value: value['ready'] and any('2.5' in line for line in value['lines']))
    checks.append('removing override recovers normal tooltip')

evaluate('''var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),root="io.github.naimjeg.combatnexus.";
var refs=Java.loadClass(root+"combat.damage.api.item.template.DamageItemTemplateReferences").CODEC.parse(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,
Java.loadClass("com.google.gson.JsonParser").parseString('{"entries":["tooltip_probe:missing"]}')).getOrThrow();
p.getMainHandItem().set(Java.loadClass(root+"combat.damage.api.item.DamageComponents").templates(),refs);p.containerMenu.broadcastChanges();JSON.stringify({bound:true})''')
try:
    wait(tooltip_code, lambda value: value['refused'] and not value['unavailableVisible'])
    checks.append('missing template reference leaves tooltip unmodified')
finally:
    evaluate('''var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");p.getMainHandItem().remove(Java.loadClass("io.github.naimjeg.combatnexus.combat.damage.api.item.DamageComponents").templates());p.containerMenu.broadcastChanges();JSON.stringify({removed:true})''')
wait(tooltip_code, lambda value: value['ready'])
evaluate('''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.disconnect(new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),false);JSON.stringify({disconnected:true})''', True)
cleared = evaluate('''var Cache=Java.loadClass("io.github.naimjeg.combatnexus.presentation.client.ClientTooltipDefinitions");
var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items");
JSON.stringify({ready:Cache.generation()>0,formulas:Java.loadClass("io.github.naimjeg.combatnexus.combat.damage.client.ClientFormulaSettings").current().isPresent()})''', True)
assert not cleared['ready'] and not cleared['formulas'], cleared
checks.append('disconnect clears shared tooltip definitions and formula settings')

report = {'passed': len(checks), 'checks': checks, 'java': server_probe['java'], 'neoforge': '26.1.2.126', 'language': args.language,
          'normalTooltip': normal, 'semantic': semantic, 'protection': server_probe,
          'scope': 'real isolated client/server network plus controlled semantic projections'}
output = ROOT / 'build/verification' / ('item-tooltips-' + args.language + '.json')
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
