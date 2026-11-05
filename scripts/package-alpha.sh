#!/usr/bin/env bash
# Package only an independently built project JAR; never pass off a checkpoint as current source.
set -euo pipefail
cd "$(dirname "$0")/.."
version=0.12.0-alpha.1
jar="build/libs/kingdom-smp-core-${version}.jar"
test -s "$jar" || { echo "Missing compiled alpha JAR: $jar" >&2; exit 1; }
out="$PWD/build/alpha"
dir="$out/Kingdom-SMP-${version}"
rm -rf "$out"
mkdir -p "$dir/mods"
cp "$jar" "$dir/mods/"
cp docs/ALPHA_README.md "$dir/README.md"
cp docs/VERIFIED_INTEGRATION_ARTIFACTS.md "$dir/VERIFIED_INTEGRATION_ARTIFACTS.md"
cp docs/PUBLIC_NPC_CLAIM_SETUP.md "$dir/PUBLIC_NPC_CLAIM_SETUP.md"
cat > "$dir/KNOWN_LIMITATIONS.txt" <<'EOF'
EXPERIMENTAL ALPHA; NOT KINGDOM SMP 1.0.0.
Normal gameplay progression ends after Tier 3; later tiers have unfinished/disabled milestone costs.
Only a subset of profession objectives is implemented and activated; other quests/ranks/equipment remain unavailable.
FTB Quests includes a nine-objective settlement guide, not a full campaign.
Full 277-mod compatibility, long-term balance, multiplayer load and final release acceptance are NOT certified.
Do not reuse a production world without making a complete backup. Do not downgrade saved worlds.
Inspect the exact GitHub Actions build AND native smoke test result; a JAR is not a runtime acceptance certificate.
EOF
git rev-parse HEAD > "$dir/SOURCE_COMMIT.txt"
printf 'Minecraft Java 1.21.1\nNeoForge 21.1.265\nJava 21\nKingdom SMP %s\n' "$version" > "$dir/VERSIONS.txt"
(cd "$dir" && sha256sum "mods/kingdom-smp-core-${version}.jar" > SHA256SUMS.txt)
python3 - "$out" "$version" <<'PY'
import pathlib, sys, zipfile
root=pathlib.Path(sys.argv[1])
version=sys.argv[2]
source=root / f'Kingdom-SMP-{version}'
with zipfile.ZipFile(root / f'Kingdom-SMP-{version}.zip','w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p in sorted(source.rglob('*')):
        if p.is_file():
            z.write(p,p.relative_to(root))
PY
(cd "$out" && sha256sum "Kingdom-SMP-${version}.zip" > "Kingdom-SMP-${version}.zip.sha256")
echo "Packaged $out/Kingdom-SMP-${version}.zip"
