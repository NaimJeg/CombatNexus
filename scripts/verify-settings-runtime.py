"""Exercise real datapack reloads via AutoJS on the disposable boundary server."""
import argparse
import json
from pathlib import Path
import tempfile
import time
import urllib.request

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=25590)
parser.add_argument('--client-port', type=int, help='AutoJS endpoint of a client connected to the verification server')
args = parser.parse_args()
run = ROOT / 'targets/neoforge-26.1.2/build/boundary-run'
pack_root = run / 'world/datapacks'
assert pack_root.is_dir(), 'Start the isolated server using scripts/autojs/server.init.gradle first'


def evaluate(code, client=False):
    request = urllib.request.Request(f'http://127.0.0.1:{args.client_port if client else args.port}/mcp', json.dumps({
        'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {
            'name': 'eval_js', 'arguments': {'side': 'client' if client else 'server', 'code': code, 'timeout_ms': 30000}
        }}).encode(), {'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=40) as response:
        message = json.load(response)
    assert 'error' not in message, message
    result = message['result']
    assert not result.get('isError'), result
    value = json.loads(result['content'][0]['text'])
    assert value['ok'] and not value.get('diagnostics'), value
    return json.loads(value['value'])


def measure():
    return evaluate((ROOT / 'scripts/autojs/measure-settings.js').read_text(encoding='utf-8'))



def verify_client(expected):
    if not args.client_port:
        return
    code = r"""(function () {
        var root = "io.github.naimjeg.combatnexus.combat.damage.";
        var settings = Java.loadClass(root + "client.ClientFormulaSettings").current();
        var client = Java.loadClass("java.lang.Class").forName("io.github.naimjeg.combatnexus.presentation.platform.client.CombatPresentationClient");
        var generation = client.getDeclaredField("generation"); generation.setAccessible(true);
        return JSON.stringify({generation:generation.getLong(null), revision:settings.isPresent()?settings.get().revision():0,
            rating:settings.isPresent()?settings.get().ratingPerProtectionScore():null});
    })()"""
    deadline = time.monotonic() + 10
    while True:
        observed = evaluate(code, client=True)
        if observed['revision'] == expected['revision']:
            assert observed['generation'] == expected['revision'], observed
            assert observed['rating'] == expected['rating'], observed
            return
        assert time.monotonic() < deadline, observed
        time.sleep(0.2)


base = measure()
verify_client(base)
assert base['armorK'] == 15 and base['depth'] == 5, base
checks = ['built-in defaults loaded']
if args.client_port: checks.append('login payload and tooltip use server settings')
formulas = {'asymptotic_k': 30, 'resistance_k': 75, 'rating_per_protection_score': 7}
damage = json.loads((ROOT / 'common/src/main/resources/data/combatnexus/settings/damage.json').read_text())
damage['limits']['max_recursion_depth'] = 6
with tempfile.TemporaryDirectory(prefix='combatnexus-settings-', dir=pack_root) as temporary:
    pack = Path(temporary).resolve()
    assert pack.is_relative_to(pack_root.resolve()), 'Verification pack must remain in the isolated world'
    name = 'file/' + pack.name
    (pack / 'pack.mcmeta').write_text(json.dumps({'pack': {'description': 'CombatNexus settings verification',
                                                         'min_format': [101, 1], 'max_format': [101, 1]}}))
    directory = pack / 'data/combatnexus/settings'
    directory.mkdir(parents=True)
    (directory / 'formulas.json').write_text(json.dumps(formulas))
    (directory / 'damage.json').write_text(json.dumps(damage))

    def reload(enable=True):
        return evaluate('''(function () {
            var server = AutoJS.server();
            server.getPackRepository().reload();
            var packs = new (Java.loadClass("java.util.ArrayList"))(server.getPackRepository().getSelectedIds());
            packs.remove(''' + json.dumps(name) + ''');
            ''' + ('packs.add(' + json.dumps(name) + ');' if enable else '') + '''
            var future = server.reloadResources(packs);
            return JSON.stringify({done:future.isDone(), failed:future.isCompletedExceptionally()});
        })()''')

    try:
        assert reload() == {'done': True, 'failed': False}
        changed = measure()
        assert changed['armorK'] == 30 and changed['rating'] == 7, changed
        assert changed['revision'] > base['revision'] and changed['damage'] > base['damage'], changed
        assert changed['depth'] == changed['runtimeDepth'] == 6, changed
        verify_client(changed)
        if args.client_port: checks.append('reload synchronizes client parameters and tooltip')
        checks.extend(['datapack override and revision published', 'actual armor damage changes', 'server combat limits updated'])
        for label, file, content in [
            ('malformed JSON', 'formulas.json', '{'),
            ('JSON comment', 'formulas.json', '// comment\n' + json.dumps(formulas)),
            ('trailing JSON', 'formulas.json', json.dumps(formulas) + '{}'),
            ('missing field', 'formulas.json', json.dumps({'asymptotic_k': 30})),
            ('unknown field', 'formulas.json', json.dumps(formulas | {'typo': 1})),
            ('nonfinite float', 'formulas.json', '{"asymptotic_k":1e999,"resistance_k":75,"rating_per_protection_score":7}'),
            ('numeric string', 'formulas.json', json.dumps(formulas | {'asymptotic_k': '30'})),
            ('formula lower bound', 'formulas.json', json.dumps(formulas | {'asymptotic_k': 0})),
            ('oversized file', 'formulas.json', ' ' * 16385 + json.dumps(formulas)),
            ('budget hard maximum', 'damage.json', json.dumps(damage | {'limits': damage['limits'] | {'max_recursion_depth': 65}})),
            ('fractional budget', 'damage.json', json.dumps(damage | {'limits': damage['limits'] | {'max_recursion_depth': 2.5}})),
        ]:
            (directory / 'formulas.json').write_text(json.dumps(formulas))
            (directory / 'damage.json').write_text(json.dumps(damage))
            (directory / file).write_text(content)
            assert reload() == {'done': True, 'failed': True}, label
            assert measure() == changed, label + ': failed reload changed active settings or damage'
            verify_client(changed)
            checks.append(label + ': previous settings, damage and client generation retained')
        # Repair the pack after rejection and exercise legal boundaries.
        (directory / 'formulas.json').write_text(json.dumps(formulas | {'rating_per_protection_score': 0}))
        (directory / 'damage.json').write_text(json.dumps(damage))
        assert reload() == {'done': True, 'failed': False}
        assert measure()['rating'] == 0
        verify_client(measure())
        checks.append('valid zero protection rating and recovery after failures')
    finally:
        assert reload(False) == {'done': True, 'failed': False}
    restored = measure()
    verify_client(restored)
    assert restored['armorK'] == base['armorK'] and restored['damage'] == base['damage']
    assert restored['depth'] == restored['runtimeDepth'] == base['depth']
    checks.append('disabling override restores built-in defaults')
if args.client_port:
    cleared = evaluate(r"""(function () {
        Java.loadClass("net.minecraft.client.Minecraft").getInstance().disconnect(
            new (Java.loadClass("net.minecraft.client.gui.screens.TitleScreen"))(), false);
        var root = "io.github.naimjeg.combatnexus.combat.damage.";
        var present = Java.loadClass(root + "client.ClientFormulaSettings").current().isPresent();
        return JSON.stringify({present:present});
    })()""", client=True)
    assert cleared == {'present': False}, cleared
    checks.append('disconnect clears server formula cache; item projection tested by verify-item-tooltips.py')
print(json.dumps({'passed' : len(checks), 'checks': checks, 'baseline': base, 'overridden': changed,
                  'restored': restored}, ensure_ascii=False, indent=2))
