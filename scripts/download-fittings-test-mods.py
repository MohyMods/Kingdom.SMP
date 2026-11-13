#!/usr/bin/env python3
"""Exact tested optional Supplementaries furnishings binary plus the existing pinned claim adapters; not a full-pack installer."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import urllib.request

root=Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser()
parser.add_argument('--destination',type=Path,default=root/'work/fittings-test-mods')
args=parser.parse_args()
destination=args.destination.resolve()
subprocess.run([sys.executable,str(root/'scripts/download-claim-test-mods.py'),'--destination',str(destination)],check=True)
for artifact in json.loads((root/'docs/sources/FITTINGS_TEST_ARTIFACTS.json').read_text())['artifacts']:
    path=destination/artifact['filename']
    data=path.read_bytes() if path.exists() else urllib.request.urlopen(artifact['url']).read()
    if hashlib.sha512(data).hexdigest()!=artifact['sha512']:
        raise SystemExit('Pinned artifact checksum mismatch: '+artifact['filename'])
    if not path.exists():
        path.write_bytes(data)
    print('Verified '+artifact['filename'])
