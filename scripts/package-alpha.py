#!/usr/bin/env python3
"""Package the compiled Alpha and instructions; never bundles QA saves or third-party mods."""
from pathlib import Path
import hashlib,json,shutil,zipfile
root=Path(__file__).resolve().parent.parent
version='0.13.0-alpha'
jar=root/'build/libs'/f'kingdom-smp-core-{version}.jar'
if not jar.is_file():raise SystemExit('Build the exact Alpha JAR first')
out=root/'dist'/version;out.mkdir(parents=True,exist_ok=True)
published=out/jar.name;shutil.copyfile(jar,published)
manual=root/'docs/releases'/version
for required in ['INSTALL.md','PLAYER_GUIDE.md','ADMIN.md','KNOWN_LIMITATIONS.md','TEST_REPORT.md','MANUAL_ACCEPTANCE.md','CHANGELOG.md']:
 if not (manual/required).is_file():raise SystemExit('Missing release documentation: '+required)
lock=root/'docs/sources/ALPHA_REQUIRED_TEST_ARTIFACTS.json'
rows=json.loads(lock.read_text())
if len(rows)!=29:raise SystemExit('Unexpected compatibility profile size')
zipname=out/f'Kingdom-SMP-{version}-install.zip';prefix=f'Kingdom-SMP-{version}/'
with zipfile.ZipFile(zipname,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 z.write(jar,prefix+'mods/'+jar.name)
 for p in sorted(manual.rglob('*')):
  if p.is_file():z.write(p,prefix+'docs/'+str(p.relative_to(manual)))
 z.write(manual/'INSTALL.md',prefix+'README.md')
 z.write(lock,prefix+'docs/sources/'+lock.name)
 z.write(root/'scripts/download-alpha-mods.py',prefix+'scripts/download-alpha-mods.py')
 if (root/'LICENSE').exists():z.write(root/'LICENSE',prefix+'LICENSE')
 z.writestr(prefix+'JAR-SHA256.txt',hashlib.sha256(jar.read_bytes()).hexdigest()+'  mods/'+jar.name+'\n')
checksum=out/'SHA256SUMS.txt';checksum.write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name+'\n' for p in [published,zipname]))
print(checksum.read_text(),end='')
print('Packaged:',out)
