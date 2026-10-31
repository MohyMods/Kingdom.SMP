#!/usr/bin/env python3
"""Fetch only the exact public artifacts in the optional isolated claim-test lock file."""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request

root = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--destination', type=Path, default=root / 'work/claim-test-mods')
args = parser.parse_args()
args.destination.mkdir(parents=True, exist_ok=True)
entries = json.loads((root / 'docs/sources/CLAIM_TEST_ARTIFACTS.json').read_text())
unexpected = {p.name for p in args.destination.glob('*.jar')} - {entry['filename'] for entry in entries}
if unexpected:
    raise SystemExit('Unexpected JARs in isolated fixture; refusing a contaminated test profile: ' + ', '.join(sorted(unexpected)))
for entry in entries:
    name = entry['filename']
    if Path(name).name != name:
        raise SystemExit('Unsafe lock filename')
    target = args.destination / name
    if target.exists():
        data = target.read_bytes()
    else:
        # FTB Cloudflare returns 1010 for Python's default agent; identify this public audit.
        request = urllib.request.Request(entry['url'], headers={'User-Agent': 'KingdomSMP-compatibility-audit/0.5'})
        with urllib.request.urlopen(request, timeout=30) as response:
            data = response.read()
    if len(data) != entry['size'] or hashlib.sha256(data).hexdigest() != entry['sha256']:
        raise SystemExit(f'Artifact mismatch; refusing to overwrite/install {name}')
    if not target.exists():
        temporary = target.with_suffix('.jar.part')
        temporary.write_bytes(data)
        temporary.replace(target)
    print(f'Verified {name}')
