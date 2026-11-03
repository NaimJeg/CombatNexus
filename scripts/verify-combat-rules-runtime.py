"""Validate real datapack rule execution, critical RNG, true damage and cancellation with AutoJS."""
import argparse
import json
from pathlib import Path
import tempfile
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--port', type=int, default=25590)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
pack_root = root / 'targets/neoforge-26.1.2/build/boundary-run/world/datapacks'
assert pack_root.is_dir(), 'Start the isolated verification server first'


def evaluate(code):
    request = urllib.request.Request(f'http://127.0.0.1:{args.port}/mcp', json.dumps({
        'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {'name': 'eval_js',
        'arguments': {'side': 'server', 'code': code, 'timeout_ms': 10000}}}).encode(),
        {'Content-Type': 'application/json'})
    with urllib.request.urlopen(request, timeout=20) as response:
        result = json.load(response)
    assert 'error' not in result, result
    content = result['result']
    assert not content.get('isError'), content
    execution = json.loads(content['content'][0]['text'])
    assert execution['ok'] and not execution.get('diagnostics'), execution
    return json.loads(execution['value'])


with tempfile.TemporaryDirectory(prefix='architecture-rules-', dir=pack_root) as temporary:
    pack = Path(temporary).resolve()
    assert pack.is_relative_to(pack_root.resolve())
    (pack / 'pack.mcmeta').write_text(json.dumps({'pack': {'description': 'CombatNexus rule matrix',
        'min_format': [101, 1], 'max_format': [101, 1]}}), encoding='utf-8')
    directory = pack / 'data/combatnexus/combatnexus_rules'
    directory.mkdir(parents=True)
    for name, phase, operation in (
        ('true_probe', 'base_modification', {'type': 'combatnexus:add_true_damage', 'channel': 'combatnexus:physical', 'value': 5}),
        ('cancel_probe', 'final_override', {'type': 'combatnexus:cancel_damage'}),
    ):
        (directory / f'{name}.json').write_text(json.dumps({'id': f'combatnexus:{name}', 'role': 'any',
            'phase': phase, 'conditions': [{'type': 'combatnexus:source_action_is', 'action': f'combatnexus:{name}'}],
            'operations': [operation]}), encoding='utf-8')
    name = 'file/' + pack.name

    def reload(enable):
        return evaluate('''(function(){var server=AutoJS.server();server.getPackRepository().reload();
            var selected=new (Java.loadClass("java.util.ArrayList"))(server.getPackRepository().getSelectedIds());
            selected.remove(''' + json.dumps(name) + ');' + ('selected.add(' + json.dumps(name) + ');' if enable else '') + '''
            var future=server.reloadResources(selected);return JSON.stringify({done:future.isDone(),failed:future.isCompletedExceptionally()});})()''')

    try:
        assert reload(True) == {'done': True, 'failed': False}
        result = evaluate((root / 'scripts/autojs/verify-rule-matrix.js').read_text(encoding='utf-8'))
        print(json.dumps(result, ensure_ascii=False, indent=2))
    finally:
        assert reload(False) == {'done': True, 'failed': False}
