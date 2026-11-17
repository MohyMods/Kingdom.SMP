#!/usr/bin/env python3
"""Reproduce the approved 264-quest baseline; does not activate any quest or change Farmer."""
import argparse
import collections
import json
from pathlib import Path
import re
import subprocess

BASELINE = '9004b3ea9e92dfeebcb8b7105ad6b440fb8f24e8'
ROOT = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--check', action='store_true')
args = parser.parse_args()
source = subprocess.check_output(['git', 'show', BASELINE + ':docs/REMAINING_PROFESSION_QUESTS_PROPOSED.md'], cwd=ROOT, text=True)
rows = []
for line in source.splitlines():
    if not line.startswith('| `kingdom_smp:'):
        continue
    parts = [s.strip() for s in line.strip('|').split('|')]
    if len(parts) != 8:
        raise SystemExit('Unexpected approved table layout')
    identity, name, rank, objective, quantity, validation, dependencies, rewards = parts
    identity = identity.strip('`')
    profession, slot = identity.removeprefix('kingdom_smp:').split('/')
    tier = int(re.search(r'Tier (\d+)', dependencies)[1])
    prerequisites = sorted({f'kingdom_smp:{profession}/{prior}' for prior in re.findall(r'\b[ajm]0[1-8]\b', dependencies)})
    rows.append(dict(id=identity, profession=profession, rank=rank.lower(), ordinal=int(slot[-1]),
                     name=name, objective=objective, quantity=quantity, validation=validation.split('+'),
                     minimumTier=tier, prerequisites=prerequisites, availability=dependencies,
                     xp={'Apprentice':100,'Journeyman':200,'Master':300}[rank], rewards=rewards))
counts = collections.Counter((r['profession'],r['rank']) for r in rows)
if len(rows)!=264 or len(counts)!=33 or set(counts.values())!={8} or len({r['id'] for r in rows})!=264:
    raise SystemExit('Incomplete approved content')
by_id = {r['id']:r for r in rows}
for row in rows:
    for dependency in row['prerequisites']:
        prior = by_id.get(dependency)
        order = {'apprentice':0,'journeyman':1,'master':2}
        if prior is None or prior['profession']!=row['profession'] or (order[prior['rank']],prior['ordinal']) >= (order[row['rank']],row['ordinal']):
            raise SystemExit('Invalid approved dependency: '+row['id'])
content = json.dumps(dict(schema=1,approvedBaseline=BASELINE,quests=rows),indent=2,ensure_ascii=False)+'\n'
target = ROOT/'src/main/resources/data/kingdom_smp/professions/approved_remaining_quests.json'
if args.check:
    if not target.is_file() or target.read_text()!=content:
        raise SystemExit('Bundled quest content differs from the exact approved baseline; regenerate and review')
else:
    target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(content)
print(f'Approved baseline: {len(rows)} quests, {sum(len(r["prerequisites"]) for r in rows)} explicit dependencies; no activation change')
