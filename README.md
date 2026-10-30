# Kingdom SMP Core

Minecraft **1.21.1**, NeoForge **21.1.265**, Java **21**. Development continues on `rebuild/royal-gilded`; the existing project and approved gameplay decisions are preserved. This is a tested development checkpoint, not a complete season release.

## Working features

- Persistent kingdom state starts at **Tier 1 — The First Hearth**. The Royal Gilded overview opens with **K** and supports twelve inspectable tiers, saved resource totals, personal weighted score, refresh, tooltips and scrolling.
- An original furnished **11 × 9 × 9 Builder's Hut** with four rotations, a timber/stone exterior, pitched roof, working door and bed, lighting, empty barrels, crafting table, stonecutter and blueprint desk.
- Admin preview and exact-token confirmation revalidate loaded space, support, world limits, entities and collisions. Setup never clears terrain. NeoForge multi-place cancellation is honored; interrupted work retains one reservation for non-destructive admin resume.
- **Aldric Stonehand** has one persistent registered UUID, configurable name, hut/home association and a safe daytime/nighttime routine. Damaged or unloaded hut context pauses interaction/work. Missing/dead builders are not silently replaced.
- A real custom **Royal Gilded dialogue/catalogue** with slate surfaces, gold/bronze trim, parchment, rendered NPC portrait, readable text and keyboard/mouse scrolling. Topic requests revalidate identity, distance and line of sight on the server. Replies cannot reopen a closed conversation.

Tier 1 capped accounting, exact equally weighted category scores, deterministic tie ordering and winner persistence exist. **Live donations, leaderboard presentation, reward issuance/redemption, progressive blueprint construction, Town Hall and Elara remain unfinished.** The catalogue describes their approved rules and shows construction as unavailable. Later tier costs, borders and travel/profession gates are not fabricated or claimed implemented.

## Build and verification

Use a full **JDK 21**, including `javac`:

```sh
./gradlew build
```

In the managed cloud environment, preserve its proxy and CA trust:

```sh
export JAVA_HOME="$PWD/work/toolchains/jdk-21.0.13+9"
scripts/gradle-cloud.sh build --console=plain
scripts/test-runtime.sh
```

That JDK path is local to this instance; select an available full JDK on another machine. The launcher does not grant network access or disable TLS verification.

The complete build passes **93 JUnit tests**. The runtime runner passes one substantial NeoForge GameTest lifecycle scenario in a fresh isolated world, covering actual commands, placement/cancellation/resume, NPC identity/access/death and native disk saves. Dedicated-server startup/save/restart and a physical-client integrated-world join have also run. See [validation](docs/VALIDATION.md) for exact scope and limits; compilation is not full multiplayer or release acceptance.

```sh
scripts/gradle-cloud.sh runServer
scripts/gradle-cloud.sh runClient
```

The runtime test mod and fixture are excluded from the production JAR. `scripts/test-domain.sh` remains available for the original Minecraft-independent subset; it is not a substitute for the full build.

## Install the starter hut

An operator with permission level **2** prepares a flat supported site in the Overworld. The entire 11 × 9 × 9 volume above the support must be air and already loaded; stand outside the footprint and within 64 blocks. Quarter turns use a 9 × 11 footprint. Origin is the minimum foundation corner, not the NPC position.

```text
/kingdom hut preview 100 65 100 north starter-camp
/kingdom hut confirm <exact token returned by preview>
/kingdom hut status
/kingdom hut name Aldric Stonehand
```

Rotations: `north`, `east`, `south`, `west`. Confirmation expires after 30 seconds. It installs 348 blocks, verifies the structure, then issues one registered builder. Right-click Aldric to speak; **K** still opens the kingdom overview.

For an interrupted reservation, inspect the server log and exact site first:

```text
/kingdom hut resume <exact hut ID from status> <reason>
```

Resume fills only air or accepts matching existing blocks. It refuses conflicting edits and never reissues a committed missing builder. There is no destructive reset, automatic resurrection or unrestricted duplicate hut command. Protection-event integration is tested; compatibility with any particular external protection mod is not established.

## Persistence and recovery

Back up the complete world. Kingdom state lives in `data/kingdomsmp_kingdom.dat`; the hut record lives in `data/kingdomsmp_builder_hut.dat`. The one-way `data/kingdomsmp_builder_hut.reserved` and `.issued` journal markers must be retained in the same backup as world/entity data. These markers prevent a lagging native save from authorizing another installation or NPC issuance. Missing/mismatched reservation metadata fails closed and requires restoring a matching backup. They do not promise recovery from arbitrary host loss, partial backups or disk failure.

NPC replacement, destructive repairs and public building-site/protection policy remain explicit future decisions. The next approved dependency is the live contribution/unique-reward slice, with a proven inventory/save recovery protocol before items are consumed.

See [requirements](docs/REQUIREMENTS.md), [roadmap](docs/DEVELOPMENT_ROADMAP.md), [hut design](docs/BUILDER_HUT_DESIGN.md), [dialogue design](docs/BUILDER_DIALOGUE_UI.md), and [development log](docs/DEVELOPMENT_LOG.md).
