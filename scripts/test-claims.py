#!/usr/bin/env python3
"""Run actual pinned claim APIs on a fresh normal dedicated server, with embedded test peers."""
import json
from pathlib import Path
import subprocess
import tempfile

root = Path(__file__).resolve().parent.parent
subprocess.run(['python3', str(root / 'scripts/download-claim-test-mods.py')], check=True)
fixture = Path(tempfile.mkdtemp(prefix='claim-server.', dir=root / 'work'))
properties = {
    'server-ip': '127.0.0.1', 'server-port': '0', 'online-mode': 'false',
    'level-name': 'claims-fixture', 'level-type': 'minecraft:flat',
    'generator-settings': json.dumps({'layers': [{'block': 'minecraft:air', 'height': 1}],
        'biome': 'minecraft:plains', 'features': False, 'lakes': False}, separators=(',', ':')),
    'spawn-protection': '0', 'view-distance': '3', 'simulation-distance': '4',
    'sync-chunk-writes': 'true', 'max-players': '10', 'difficulty': 'peaceful',
}
(fixture / 'server.properties').write_text(''.join(f'{k}={v}\n' for k, v in properties.items()))
(fixture / 'eula.txt').write_text('eula=true\n')
command = [str(root / 'scripts/gradle-cloud.sh'), 'runServer',
    f'-PkingdomServerRunDir={fixture}', '-PkingdomClaimTestMods=work/claim-test-mods',
    '-PkingdomTestNamespace=kingdom_smp_claim_tests', '--console=plain']
print(f'Isolated claim fixture: {fixture}', flush=True)
started = passed = stopped = failed = False
with (fixture / 'execution.log').open('w') as log:
    process = subprocess.Popen(command, cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, bufsize=1)
    try:
        for line in process.stdout:
            print(line, end='', flush=True)
            log.write(line); log.flush()
            if 'Done (' in line and not started:
                started = True
                process.stdin.write('execute positioned 0 80 0 run test runall\n'); process.stdin.flush()
            if 'CLAIM_TEST_ASSERTIONS_PASSED:' in line:
                passed = True
            if 'failed at ' in line or 'Game test server crashed' in line:
                failed = True
            if (passed or failed) and not stopped:
                stopped = True
                process.stdin.write('save-all flush\nstop\n'); process.stdin.flush()
        result = process.wait()
    finally:
        if process.poll() is None:
            process.stdin.write('stop\n'); process.stdin.flush()
            process.wait(timeout=30)
if result or not passed or failed:
    raise SystemExit(f'Claim fixture failed: exit={result}, passed={passed}, failed={failed}; {fixture}')
print('CLAIM_COMPATIBILITY_SUITE_PASSED: normal dedicated startup, assertions, flush and clean stop', flush=True)
