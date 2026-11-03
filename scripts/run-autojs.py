"""Execute a checked-in script using a local AutoJS MCP endpoint; fail on script errors."""
import argparse
import json
from pathlib import Path
import urllib.request

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('script', type=Path)
parser.add_argument('--port', type=int, required=True)
parser.add_argument('--side', choices=('server', 'client'), default='server')
parser.add_argument('--timeout-ms', type=int, default=10000)
args = parser.parse_args()
if not 1 <= args.timeout_ms <= 30000:
    parser.error('--timeout-ms must be between 1 and 30000')
request = urllib.request.Request(
    f'http://127.0.0.1:{args.port}/mcp',
    json.dumps({'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {
        'name': 'eval_js', 'arguments': {'side': args.side, 'timeout_ms': args.timeout_ms,
                                      'code': args.script.read_text(encoding='utf-8')}
    }}).encode(), {'Content-Type': 'application/json'})
with urllib.request.urlopen(request, timeout=args.timeout_ms / 1000 + 10) as response:
    message = json.load(response)
if 'error' in message:
    raise SystemExit(message['error'])
result = message['result']
for item in result['content']:
    if item['type'] == 'text':
        print(item['text'])
        if result.get('isError'):
            raise SystemExit(1)
        evaluation = json.loads(item['text'])
        if not evaluation.get('ok') or evaluation.get('diagnostics'):
            raise SystemExit(1)
