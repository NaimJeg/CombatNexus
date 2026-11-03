"""Real singleplayer inventory/creative/chest tooltip regression. Uses a copy of a disposable test world."""
import importlib.util
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time

ROOT = Path(__file__).resolve().parents[1]
TARGET = ROOT / 'targets/neoforge-26.1.2'
spec = importlib.util.spec_from_file_location('runtime', ROOT / 'scripts/verify-capabilities-runtime.py')
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)


def main():
    output = ROOT / 'build/verification/item-tooltip-integrated'
    output.mkdir(parents=True, exist_ok=True)
    seed = ROOT / 'build/verification/integrated-tooltip-seed'
    if not seed.exists():
        source = TARGET / 'build/carried-tooltip-run/world'
        if not source.exists():
            raise RuntimeError('Run verify-carried-effect-tooltips.py first to create the disposable world')
        shutil.copytree(source, seed)
    world = 'tooltip-integrated-' + str(time.time_ns())
    shutil.copytree(seed, TARGET / 'build/settings-client-run/saves' / world)
    with socket.socket() as probe:
        probe.bind(('127.0.0.1', 25588))
    checks = []
    log = (output / 'client.log').open('w', encoding='utf-8')
    process = subprocess.Popen([str(TARGET / 'gradlew.bat'), 'runClient', '-I', str(ROOT / 'scripts/autojs/client.init.gradle'),
        '-PverificationClientAutojsPort=25588', '--console', 'plain'], cwd=TARGET, stdout=log, stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)

    def client(code):
        return runtime.evaluate(25588, code, side='client')

    def server(code):
        # AutoJS's integrated backend runs on the render thread: schedule gameplay on the server explicitly.
        return client('var server=Java.loadClass("net.minecraft.client.Minecraft").getInstance().getSingleplayerServer();'
            'server.execute(function(){' + code + '});JSON.stringify({queued:true})')

    def wait(code, predicate=lambda v: v.get('ready'), seconds=120):
        deadline = time.monotonic() + seconds
        while True:
            if process.poll() is not None:
                raise RuntimeError('Client exited; inspect client.log')
            try:
                value = client(code)
                if predicate(value):
                    return value
            except OSError:
                if time.monotonic() >= deadline:
                    raise
            if time.monotonic() >= deadline:
                raise AssertionError(value)
            time.sleep(.25)

    state = '''(function(){var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),root="io.github.naimjeg.combatnexus.";
var stack=mc.player.getMainHandItem(),projection=Java.loadClass(root+"presentation.platform.client.CombatPresentationClient").item(stack);
var flags=new (Java.loadClass("net.neoforged.neoforge.client.ClientTooltipFlag"))(false,false,true,false,false);
var lines=[];for(var line of stack.getTooltipLines(Java.loadClass("net.minecraft.world.item.Item$TooltipContext").of(mc.level),mc.player,flags))lines.push(String(line.getString()));
var unavailable=String(Java.loadClass("net.minecraft.network.chat.Component").translatable("tooltip.combatnexus.presentation.unavailable").getString());
return JSON.stringify({ready:projection.isPresent()&&!projection.get().has("error")&&lines.indexOf(unavailable)<0,lines:lines,screen:String(mc.screen),menu:String(mc.player.containerMenu)});})()'''
    reports = []
    try:
        wait('JSON.stringify({ready:true})')
        client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.options.pauseOnLostFocus=false;'
            'mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);mc.createWorldOpenFlows().openWorld('
            + json.dumps(world) + ',function(){});JSON.stringify({opened:true})')
        wait('JSON.stringify({ready:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null})')
        server('var p=server.getPlayerList().getPlayers().getFirst();p.setNoGravity(true);p.setItemSlot('
            'Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,new (Java.loadClass("net.minecraft.world.item.ItemStack"))('
            'Java.loadClass("net.minecraft.world.item.Items").GOLDEN_APPLE));p.containerMenu.broadcastChanges();')
        wait('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();JSON.stringify({ready:mc.player.getMainHandItem().is(Java.loadClass("net.minecraft.world.item.Items").GOLDEN_APPLE)})')
        for mode in ['survival', 'creative']:
            server('server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode ' + mode + ' SettingsVerifier");')
            wait('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();JSON.stringify({ready:mc.player.isCreative()===' + str(mode == 'creative').lower() + '})')
            client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.setScreen(new (Java.loadClass("net.minecraft.client.gui.screens.inventory.InventoryScreen"))(mc.player));JSON.stringify({opened:true})')
            report = wait(state)
            if mode == 'creative':
                assert 'CreativeModeInventoryScreen' in report['screen'], report
                assert 'ItemPickerMenu' in report['menu'], report
            reports.append(report)
            checks.append(mode + ' inventory actual item renders item component description through real tooltip event')
            time.sleep(2.2)
            reports.append(wait(state))
            checks.append(mode + ' inventory description remains available after two seconds')
            client('Java.loadClass("net.minecraft.client.Minecraft").getInstance().setScreen(null);JSON.stringify({closed:true})')
            reports.append(wait(state))
            checks.append(mode + ' hotbar reads item components')
        # The creative catalogue is not a server inventory. Reproduce an empty-inventory preview.
        server('var p=server.getPlayerList().getPlayers().getFirst();p.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,Java.loadClass("net.minecraft.world.item.ItemStack").EMPTY);p.containerMenu.broadcastChanges();')
        wait('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();JSON.stringify({ready:mc.player.getMainHandItem().isEmpty()})')
        client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.setScreen(new (Java.loadClass("net.minecraft.client.gui.screens.inventory.InventoryScreen"))(mc.player));'
            'var menu=mc.player.containerMenu;menu.items.clear();menu.items.addAll(Java.loadClass("net.minecraft.core.registries.BuiltInRegistries").CREATIVE_MODE_TAB.getValue(Java.loadClass("net.minecraft.world.item.CreativeModeTabs").FOOD_AND_DRINKS).getDisplayItems());menu.scrollTo(0);JSON.stringify({opened:true})')
        catalogue_state = state.replace('stack=mc.player.getMainHandItem()', 'stack=mc.player.containerMenu.items.stream().filter(function(s){return s.is(Java.loadClass("net.minecraft.world.item.Items").GOLDEN_APPLE);}).findFirst().orElseThrow()')
        reports.append(wait(catalogue_state))
        assert 'Creative catalogue preview' not in reports[-1]['lines'], reports[-1]
        checks.append('empty creative inventory displays local catalogue food description')
        potion_state = state.replace('stack=mc.player.getMainHandItem()', 'stack=mc.player.containerMenu.items.stream().filter(function(s){return s.is(Java.loadClass("net.minecraft.world.item.Items").POTION)&&s.getHoverName().getString().contains("Night Vision");}).findFirst().orElseThrow()')
        reports.append(wait(potion_state))
        assert 'Creative catalogue preview' not in reports[-1]['lines'], reports[-1]
        checks.append('creative night vision catalogue potion renders local effect details')
        client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.getLanguageManager().setSelected("zh_cn");mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());JSON.stringify({changed:true})')
        chinese_potion = potion_state.replace('contains("Night Vision")', 'contains("夜视")')
        reports.append(wait(chinese_potion))
        assert '创造物品栏目录预览' not in reports[-1]['lines'], reports[-1]
        assert any('夜视' in line for line in reports[-1]['lines']), reports[-1]
        checks.append('Chinese catalogue potion uses translated local description without unavailable text')
        client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.getLanguageManager().setSelected("en_us");mc.getLanguageManager().onResourceManagerReload(mc.getResourceManager());JSON.stringify({changed:true})')
        time.sleep(2.2)
        reports.append(wait(potion_state))
        checks.append('catalogue description remains available without a server inventory slot')
        client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.player.containerMenu.items.clear();JSON.stringify({cleared:true})')
        detached = client('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();JSON.stringify({empty:Java.loadClass("io.github.naimjeg.combatnexus.presentation.platform.client.CombatPresentationClient").item(new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").GOLDEN_APPLE)).isEmpty()})')
        assert not detached['empty'], detached
        checks.append('detached item renders immediately outside catalogue')
        server('var p=server.getPlayerList().getPlayers().getFirst();p.setItemSlot(Java.loadClass("net.minecraft.world.entity.EquipmentSlot").MAINHAND,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").GOLDEN_APPLE));p.containerMenu.broadcastChanges();')
        wait('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();JSON.stringify({ready:!mc.player.getMainHandItem().isEmpty()})')
        reports.append(wait(state))
        assert 'Creative catalogue preview' not in reports[-1]['lines'], reports[-1]
        checks.append('acquired item uses the same local description')
        server('var p=server.getPlayerList().getPlayers().getFirst(),container=new (Java.loadClass("net.minecraft.world.SimpleContainer"))["(int)"](27);'
            'container.setItem(0,new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").ROTTEN_FLESH));'
            'p.openMenu(new (Java.loadClass("net.minecraft.world.SimpleMenuProvider"))(function(id,inventory,player){return Java.loadClass("net.minecraft.world.inventory.ChestMenu").threeRows(id,inventory,container);},Java.loadClass("net.minecraft.network.chat.Component").literal("Tooltip probe")));')
        wait('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();JSON.stringify({ready:mc.player.containerMenu instanceof Java.loadClass("net.minecraft.world.inventory.ChestMenu")})')
        reports.append(wait(state.replace('stack=mc.player.getMainHandItem()', 'stack=mc.player.containerMenu.getSlot(0).getItem()')))
        assert any('80%' in line for line in reports[-1]['lines']), reports[-1]
        checks.append('chest contents display component food probability')
        reports.append(wait(state))
        checks.append('player inventory inside chest displays local components')
        # Call JEI's actual renderer with ingredients that have no inventory slot.
        jei_state = state.replace('stack=mc.player.getMainHandItem()',
            'stack=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").ROTTEN_FLESH)')
        jei_state = jei_state.replace('stack.getTooltipLines(',
            'new (Java.loadClass("mezz.jei.library.render.ItemStackRenderer"))().getTooltip(stack,')
        reports.append(client(jei_state))
        assert reports[-1]['ready'] and any('80%' in line for line in reports[-1]['lines']), reports[-1]
        checks.append('JEI actual item renderer displays detached food component probability immediately')
        jei_potion = jei_state.replace('ROTTEN_FLESH)', 'POTION)').replace(
            'var flags=', 'stack.set(Java.loadClass("net.minecraft.core.component.DataComponents").POTION_CONTENTS,new (Java.loadClass("net.minecraft.world.item.alchemy.PotionContents"))(Java.loadClass("net.minecraft.world.item.alchemy.Potions").NIGHT_VISION));var flags=')
        reports.append(client(jei_potion))
        assert reports[-1]['ready'] and any('Night Vision' in line for line in reports[-1]['lines']), reports[-1]
        checks.append('JEI actual item renderer displays detached night vision potion immediately')
        missing = jei_state.replace('var flags=',
            'var refs=Java.loadClass(root+"combat.damage.api.item.template.DamageItemTemplateReferences");stack.set(Java.loadClass(root+"combat.damage.api.item.DamageComponents").templates(),refs.CODEC.parse(Java.loadClass("com.mojang.serialization.JsonOps").INSTANCE,Java.loadClass("com.google.gson.JsonParser").parseString(\'{"entries":["tooltip_probe:missing"]}\')).getOrThrow());var flags=')
        report = client(missing)
        assert report['lines'] == ['Rotten Flesh'], report
        reports.append(report)
        checks.append('JEI missing template keeps original tooltip without adding custom lines')
        fallback = client('''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),root="io.github.naimjeg.combatnexus.",Tooltip=Java.loadClass(root+"combat.damage.client.tooltip.ItemDamageRuleTooltip"),Optional=Java.loadClass("java.util.Optional"),Json=Java.loadClass("com.google.gson.JsonParser"),Component=Java.loadClass("net.minecraft.network.chat.Component"),stack=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").POTION);
stack.set(Java.loadClass("net.minecraft.core.component.DataComponents").POTION_CONTENTS,new (Java.loadClass("net.minecraft.world.item.alchemy.PotionContents"))(Java.loadClass("net.minecraft.world.item.alchemy.Potions").STRENGTH));
var checks=[];
for(var input of [null,Optional.empty(),Optional.of(Json.parseString('{"error":"UNAVAILABLE"}').getAsJsonObject()),Optional.of(Json.parseString('{}').getAsJsonObject()),Optional.of(Json.parseString('{"documents":[],"equipment":[],"carried":[{}],"stew":[]}').getAsJsonObject())]){
var lines=new (Java.loadClass("java.util.ArrayList"))(stack.getTooltipLines(Java.loadClass("net.minecraft.world.item.Item$TooltipContext").of(mc.level),mc.player,new (Java.loadClass("net.neoforged.neoforge.client.ClientTooltipFlag"))(false,false,true,false,false)));
lines.add(Component.literal("Other mod line"));var before=new (Java.loadClass("java.util.ArrayList"))(lines);
Tooltip.append(stack,lines,true,false,input);checks.push(lines.equals(before));}
var lines=new (Java.loadClass("java.util.ArrayList"))();lines.add(Component.literal("Original"));Tooltip.append(null,lines,false,false,Optional.empty());checks.push(lines.size()===1);
checks.push(Java.loadClass(root+"presentation.platform.client.CombatPresentationClient").item(null).isEmpty());
JSON.stringify({checks:checks});''')
        assert all(fallback['checks']), fallback
        checks.extend(['fallback preserves tooltip: ' + case for case in
            ['null projection', 'empty projection', 'error projection', 'missing sections', 'malformed document', 'null item rendering', 'null item projection']])
        plain = client('''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),stack=new (Java.loadClass("net.minecraft.world.item.ItemStack"))(Java.loadClass("net.minecraft.world.item.Items").DIRT);
var flags=new (Java.loadClass("net.neoforged.neoforge.client.ClientTooltipFlag"))(false,false,true,false,false),lines=[];
for(var line of stack.getTooltipLines(Java.loadClass("net.minecraft.world.item.Item$TooltipContext").of(mc.level),mc.player,flags))lines.push(String(line.getString()));
JSON.stringify({lines:lines,unavailable:String(Java.loadClass("net.minecraft.network.chat.Component").translatable("tooltip.combatnexus.presentation.unavailable").getString())})''')
        assert plain['unavailable'] not in plain['lines'], plain
        checks.append('ordinary item without descriptions never shows unavailable')
        result = {'passed': len(checks), 'checks': checks, 'reports': reports, 'environment': 'Windows / Java 25 / NeoForge 26.1.2.126 / AutoJS 1.0.1 / integrated server'}
        (output / 'report.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
        print(json.dumps({'passed': len(checks), 'checks': checks}), flush=True)
    finally:
        try:
            client('Java.loadClass("net.minecraft.client.Minecraft").getInstance().stop();JSON.stringify({stopping:true})')
        except Exception:
            pass
        try:
            process.wait(timeout=30)
        except subprocess.TimeoutExpired:
            process.terminate()
        log.close()


if __name__ == '__main__':
    main()
