"""Verify the managed lifecycle foundation on an already running disposable AutoJS server."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--port', type=int, default=25580)
    args = parser.parse_args()

    def run(name):
        execution = subprocess.run([
            sys.executable, str(ROOT / 'scripts/run-autojs.py'),
            str(ROOT / 'scripts/autojs' / (name + '.js')), '--port', str(args.port)
        ], capture_output=True, text=True, encoding='utf-8')
        if execution.returncode:
            raise RuntimeError(execution.stdout + execution.stderr)
        result = json.loads(execution.stdout)
        return json.loads(result['value'])

    deadline = time.monotonic() + 30
    while not run('prepare-architecture')['prepared']:
        if time.monotonic() >= deadline:
            raise RuntimeError('Native entity lookup did not become ready')
        time.sleep(0.2)
    reports = {name: run(name) for name in (
        'verify-effect-lifetimes', 'verify-instant-effects', 'verify-effect-damage-floor', 'verify-effect-receipts', 'verify-effect-phases')}
    result = {
        'scope': 'Lifecycle subset only; full takeover and display checks are in verify-capabilities-runtime.py and verify-effect-display.py',
        'environment': {'java': reports['verify-effect-lifetimes']['java'], 'neoforge': '26.1.2.126', 'harness': 'AutoJS dedicated server'},
        'passed': sum(report['passed'] for report in reports.values()), 'reports': reports,
    }
    output = ROOT / 'build/verification/effect-lifetimes.json'
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(result, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
