"""Exercise budget refusal, stale replies and real dimension/death lifecycle on an isolated client."""
import argparse
import json
import time
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--server-port', type=int, default=25590)
parser.add_argument('--client-port', type=int, default=25591)
args = parser.parse_args()


def evaluate(code, client=False):
    port = args.client_port if client else args.server_port
    request = urllib.request.Request(f'http://127.0.0.1:{port}/mcp', json.dumps({
        'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {'name': 'eval_js',
        'arguments': {'side': 'client' if client else 'server', 'code': code, 'timeout_ms': 10000}}}).encode(),
        {'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=20) as response:
        result = json.load(response)
    assert 'error' not in result, result
    block = result['result']
    assert not block.get('isError'), block
    value = json.loads(block['content'][0]['text'])
    assert value['ok'] and not value.get('diagnostics'), value
    return json.loads(value['value'])


def wait_for(code, predicate, client=True):
    deadline = time.monotonic() + 20
    while True:
        state = evaluate(code, client)
        if predicate(state):
            return state
        assert time.monotonic() < deadline, state
        time.sleep(.2)


rows_code = '''(function(){
    var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance(),screen=mc.screen;
    var text=[];
    if(screen instanceof Java.loadClass("io.github.naimjeg.combatnexus.presentation.platform.client.CombatPanelScreen"))
        for(var row of screen.rows())text.push(String(row.getString()));
    return JSON.stringify({rows:text,dimension:mc.level===null?"":String(mc.level.dimension().identifier()),
        entityId:mc.player===null?-1:mc.player.getId(),alive:mc.player!==null && mc.player.isAlive(),health:mc.player===null?0:mc.player.getHealth()});
})()'''
server_prefix = '''var server=AutoJS.server(),player=server.getPlayerList().getPlayerByName("SettingsVerifier");'''


def reopen():
    time.sleep(.7)
    evaluate('''var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();mc.options.pauseOnLostFocus=false;mc.setScreen(null);
        Java.loadClass("net.minecraft.client.KeyMapping").click(Java.loadClass("com.mojang.blaze3d.platform.InputConstants").Type.KEYSYM.getOrCreate(67));
        JSON.stringify({clicked:true})''', True)


checks = []
try:
    evaluate('''(function(){''' + server_prefix + '''
        var attribute=player.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED);
        var Modifier=Java.loadClass("net.minecraft.world.entity.ai.attributes.AttributeModifier"),Id=Java.loadClass("net.minecraft.resources.Identifier");
        for(var i=0;i<300;i++)attribute.addTransientModifier(new Modifier(Id.parse("combatnexus:panel_budget_"+i),0,Modifier.Operation.ADD_VALUE));
        return JSON.stringify({installed:300});})()''')
    reopen()
    refused = wait_for(rows_code, lambda state: len(state['rows']) == 1 and ('could not provide' in state['rows'][0] or '暂时无法' in state['rows'][0]))
    assert 'could not provide' in refused['rows'][0] or '暂时无法' in refused['rows'][0], refused
    checks.append('over-budget capture sends explicit refusal and renders it')
finally:
    evaluate('''(function(){''' + server_prefix + '''
        var attribute=player.getAttribute(Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED);
        var Id=Java.loadClass("net.minecraft.resources.Identifier");
        for(var i=0;i<300;i++)attribute["removeModifier(net.minecraft.resources.Identifier)"](Id.parse("combatnexus:panel_budget_"+i));
        return JSON.stringify({cleaned:true});})()''')
reopen()
normal = wait_for(rows_code, lambda state: len(state['rows']) > 1)
checks.append('removing excess sources restores the panel')
evaluate('''(function(){''' + server_prefix + '''
    var root="io.github.naimjeg.combatnexus.",level=player.level();
    var runtime=Java.loadClass(root+"combat.internal.platform.PlatformWorldLifecycle").existing(level);
    var Payload=Java.loadClass(root+"presentation.network.CombatPresentationPayload");
    var payload=new Payload(Java.loadClass("java.util.UUID").randomUUID(),runtime.sessionId(),level.dimension().identifier(),
        Java.loadClass(root+"combat.damage.api.DamageModuleApi").definitionGeneration(server),-1,
        Java.loadClass("net.minecraft.world.item.ItemStack").EMPTY,'{"rows":["STALE_REPLY"]}');
    Java.loadClass("net.neoforged.neoforge.network.PacketDistributor").sendToPlayer(player,payload);
    return JSON.stringify({sent:true});})()''')
time.sleep(.3)
assert not any('STALE_REPLY' in text for text in evaluate(rows_code, True)['rows'])
checks.append('stale nonce reply cannot replace the visible snapshot')
evaluate('''(function(){''' + server_prefix + '''
    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode creative SettingsVerifier");
    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"execute in minecraft:the_nether run tp SettingsVerifier 0 100 0");
    return JSON.stringify({teleported:true});})()''')
wait_for(rows_code, lambda state: state['dimension'] == 'minecraft:the_nether' and state['alive'])
reopen()
wait_for(rows_code, lambda state: len(state['rows']) > 1 and state['dimension'] == 'minecraft:the_nether')
checks.append('dimension transfer clears old session and accepts a fresh panel')
evaluate('''(function(){''' + server_prefix + '''
    server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"execute in minecraft:overworld run tp SettingsVerifier 0 100 0");
    return JSON.stringify({killed:true});})()''')
wait_for(rows_code, lambda state: state['dimension'] == 'minecraft:overworld' and state['alive'])
before_death = evaluate(rows_code, True)['entityId']
wait_for('(function(){var p=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier");return JSON.stringify({ready:!p.isChangingDimension()&&p.connection.hasClientLoaded()});})()', lambda state: state['ready'], client=False)

assert evaluate('(function(){var server=AutoJS.server(),player=server.getPlayerList().getPlayerByName("SettingsVerifier");server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),"gamemode survival SettingsVerifier");player.setInvulnerable(false);var accepted=player.hurtServer(player.level(),player.damageSources().genericKill(),1000);return JSON.stringify({killed:accepted&&player.isDeadOrDying()});})()')['killed']
death_state = wait_for(rows_code, lambda state: not state['alive'] or state['entityId'] != before_death)
evaluate('''(function(){
    var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();
    var Packet=Java.loadClass("net.minecraft.network.protocol.game.ServerboundClientCommandPacket");
    mc.getConnection().send(new Packet(Packet.Action.PERFORM_RESPAWN));
    return JSON.stringify({respawnRequested:true});})()''', True)
wait_for(rows_code, lambda state: state['alive'] and state['health'] > 0)
reopen()
after = wait_for(rows_code, lambda state: len(state['rows']) > 1 and state['alive'])
checks.append('death and respawn replace player state and repopulate the panel')
evaluate('''(function(){var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();
    Java.loadClass("net.minecraft.client.Screenshot").grab(mc.gameDirectory,"architecture-panel.png",mc.getMainRenderTarget(),1,function(){});
    return JSON.stringify({screenshotRequested:true});})()''', True)
print(json.dumps({'passed': len(checks), 'checks': checks, 'rowsAfterRespawn': len(after['rows'])}, ensure_ascii=False, indent=2))
