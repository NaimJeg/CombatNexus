"""Connect an isolated client and exercise the real C-key panel request/response path."""
import argparse
import json
import time
import urllib.request
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=25591)
parser.add_argument('--game-port', type=int, required=True)
parser.add_argument('--server-port',type=int,default=25594)
parser.add_argument('--compatibility',action='store_true')
parser.add_argument('--output',type=Path,default=Path(__file__).resolve().parents[1]/'targets/neoforge-26.1.2/build/verification/client.json')
args = parser.parse_args()


def evaluate(code,port=None,side='client'):
    request = urllib.request.Request(f'http://127.0.0.1:{args.port if port is None else port}/mcp', json.dumps({
        'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {'name': 'eval_js',
        'arguments': {'side': side, 'code': code, 'timeout_ms': 10000}}}).encode(),
        {'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=20) as response:
        result = json.load(response)
    assert 'error' not in result, result
    block = result['result']
    assert not block.get('isError'), block
    value = json.loads(block['content'][0]['text'])
    assert value['ok'] and not value.get('diagnostics'), value
    return json.loads(value['value'])


evaluate('''(function () {
    var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();
    mc.options.pauseOnLostFocus=false;
    var address="127.0.0.1:PORT";
    var Data=Java.loadClass("net.minecraft.client.multiplayer.ServerData");
    Java.loadClass("net.minecraft.client.gui.screens.ConnectScreen").startConnecting(
        new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(),mc,
        Java.loadClass("net.minecraft.client.multiplayer.resolver.ServerAddress").parseString(address),
        new Data("Architecture verification",address,Data.Type.OTHER),false,null);
    return JSON.stringify({connecting:true});
})()'''.replace('PORT', str(args.game_port)))
deadline = time.monotonic() + 30
while not evaluate('JSON.stringify({connected:Java.loadClass("net.minecraft.client.Minecraft").getInstance().player!==null && Java.loadClass("net.minecraft.client.Minecraft").getInstance().screen===null})')['connected']:
    assert time.monotonic() < deadline, 'Client did not join isolated server'
    time.sleep(.2)
if args.compatibility:
    prepared=evaluate('''(function(){
        var player=AutoJS.server().getPlayerList().getPlayerByName("SettingsVerifier"),Id=Java.loadClass("net.minecraft.resources.Identifier");
        var Stack=Java.loadClass("net.minecraft.world.item.ItemStack"),Items=Java.loadClass("net.minecraft.world.item.Items"),Slot=Java.loadClass("net.minecraft.world.entity.EquipmentSlot");
        var helmet=new Stack(Items.DIAMOND_HELMET),Socket=Java.loadClass("dev.shadowsoffire.apotheosis.socket.SocketHelper");
        var gem=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemRegistry").INSTANCE.holder(Id.parse("apotheosis:the_end/mageslayer")).get();
        var stack=Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.GemItem").createStack(gem,Java.loadClass("dev.shadowsoffire.apotheosis.socket.gem.Purity").PERFECT);
        Socket.setSockets(helmet,1);player.setItemSlot(Slot.HEAD,Socket.socketGemInItem(helmet,stack));
        var attribute=Java.loadClass("net.minecraft.world.entity.ai.attributes.Attributes").MOVEMENT_SPEED;
        player.getAttribute(attribute).setBaseValue(0.25);
        var Modifier=Java.loadClass("net.minecraft.world.entity.ai.attributes.AttributeModifier");
        player.getAttribute(attribute).addTransientModifier(new Modifier(Id.parse("combatnexus:client_toggle_probe"),1,Modifier.Operation.ADD_VALUE));
        var attachment=Java.loadClass("dev.shadowsoffire.apotheosis.Apoth$Attachments").ATTRIBUTE_TOGGLES;
        player.setData(attachment,player.getData(attachment).withSuppressed(Java.loadClass("java.util.Set").of(attribute),true));
        return JSON.stringify({prepared:true,tick:AutoJS.server().overworld().getGameTime()});
    })()''',args.server_port,'server')
    deadline=time.monotonic()+10
    while evaluate('JSON.stringify({tick:AutoJS.server().overworld().getGameTime()})',args.server_port,'server')['tick'] <= prepared['tick']+2:
        assert time.monotonic()<deadline,'Server did not adopt equipment on natural ticks'
        time.sleep(.1)
evaluate('''Java.loadClass("net.minecraft.client.KeyMapping").click(Java.loadClass("com.mojang.blaze3d.platform.InputConstants").Type.KEYSYM.getOrCreate(67)); JSON.stringify({clicked:true})''')
deadline = time.monotonic() + 10
while True:
    status = evaluate('''(function(){
        var screen=Java.loadClass("net.minecraft.client.Minecraft").getInstance().screen;
        if(screen===null || !(screen instanceof Java.loadClass("io.github.naimjeg.combatnexus.presentation.platform.client.CombatPanelScreen")))return JSON.stringify({ready:false});
        var rows=screen.rows(),text=[];
        for(var row of rows)text.push(String(row.getString()));
        return JSON.stringify({ready:rows.size()>1,rows:rows.size(),text:text});
    })()''')
    if status['ready'] and (not args.compatibility or (any('apotheosis:the_end/mageslayer' in text for text in status['text']) and any('apotheosis:attribute_toggle' in text for text in status['text']))):
        break
    assert time.monotonic() < deadline, status
    time.sleep(.2)
assert any('Health' in text or '生命' in text for text in status['text']), status
if args.compatibility:
    assert any('apotheosis:the_end/mageslayer' in text for text in status['text']),status
    assert any('apotheosis:attribute_toggle' in text for text in status['text']),status
report={'passed': 5 if args.compatibility else 3, 'checks': ['client connected', 'C key opens panel',
    'real server snapshot populates panel']+(['frozen gem source shown','raw/effective toggle reason shown'] if args.compatibility else []), 'snapshotRows': status['rows'], 'healthRow': status['text'][0]}
report['screenshots']=[]
if args.compatibility:
    for needle,name in [('apotheosis:the_end/mageslayer','bridge-gem-source.png'),('apotheosis:attribute_toggle','bridge-effective-value.png')]:
        row=next(i for i,text in enumerate(status['text']) if needle in text)
        evaluate('var screen=Java.loadClass("net.minecraft.client.Minecraft").getInstance().screen;for(var i=0;i<100;i++)screen.mouseScrolled(100,100,0,1);for(var i=0;i<'+str(max(0,(row-3)//3))+';i++)screen.mouseScrolled(100,100,0,-1);JSON.stringify({scrolled:true})')
        time.sleep(.3)
        result=evaluate('var mc=Java.loadClass("net.minecraft.client.Minecraft").getInstance();Java.loadClass("net.minecraft.client.Screenshot").grab(mc.gameDirectory,'+json.dumps(name)+',mc.getMainRenderTarget(),1,new (Java.loadClass("java.util.function.Consumer"))({accept:function(message){}}));JSON.stringify({directory:String(mc.gameDirectory.getAbsolutePath())})')
        report['screenshots'].append(str(Path(result['directory'])/'screenshots'/name))
args.output.parent.mkdir(parents=True,exist_ok=True)
args.output.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
