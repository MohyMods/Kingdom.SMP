# Kingdom SMP Core

The current milestone adds live First Hearth contributions, rankings and Tier 2 advancement. Installable checkpoint links and evidence are recorded below after packaging; historical builds remain in `docs/checkpoints/`.

Minecraft **1.21.1**, NeoForge **21.1.265**, Java **21**. Development continues on `rebuild/royal-gilded`; the existing project and approved gameplay decisions are preserved. This is a tested development checkpoint, not a complete season release.

## Working features

- Persistent kingdom state starts at **Tier 1 — The First Hearth**. The Royal Gilded overview opens with **K** and supports twelve inspectable tiers, saved resource totals, personal weighted score, refresh, tooltips and scrolling.
- **Contribute** opens a custom donation review/confirmation interface, exact weighted leaderboard and paginated personal history. Shared targets are 1,024 logs, 2,048 cobblestone, 512 wheat and 128 iron ingots. Only accepted resources are consumed, with durable inventory checkpoints and restart reconciliation.
- Completing First Hearth advances to **Tier 2 — Roads of Commerce**, announces the permanent highest contributor and records the earned **Founders’ Lodge** construction privilege. The ultimate **Town Hall is Tier 12**. No banking, trading or Charter service is granted by Tier 1 completion.
- An original furnished **11 × 9 × 9 Builder's Hut** with four rotations, a timber/stone exterior, pitched roof, working door and bed, lighting, empty barrels, crafting table, stonecutter and blueprint desk.
- Admin preview and exact-token confirmation revalidate loaded space, support, world limits, entities and collisions. Setup never clears terrain. NeoForge multi-place cancellation is honored; interrupted work retains one reservation for non-destructive admin resume.
- **Aldric Stonehand** has one persistent registered UUID, configurable name, hut/home association and a safe daytime/nighttime routine. Damaged or unloaded hut context pauses interaction/work. Missing/dead builders are not silently replaced.
- A real custom **Royal Gilded dialogue/catalogue** with slate surfaces, gold/bronze trim, parchment, rendered NPC portrait, readable text and keyboard/mouse scrolling. Topic requests revalidate identity, distance and line of sight on the server. Replies cannot reopen a closed conversation.

**Physical reward claim/redemption, progressive blueprint construction, the furnished Founders’ Lodge and its named Reeve remain unfinished.** Town Hall and Elara belong to Tier 12 and are not implemented. Later tier costs, borders and travel/profession gates are not fabricated or claimed implemented.

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

The complete build passes **108 JUnit tests**. The runtime runner passes one substantial NeoForge GameTest lifecycle scenario in a fresh isolated world, covering donation replay/stale/racing requests, actual inventory consumption, injected native-save failure/recovery, completion/winner/history/reload and all existing hut/NPC checks. Dedicated-server startup/save/restart and physical-client integrated-world contributions have also run. See [validation](docs/VALIDATION.md) for exact scope and limits; compilation is not full multiplayer or release acceptance.

```sh
scripts/gradle-cloud.sh runServer
scripts/gradle-cloud.sh runClient
```

The runtime test mod and fixture are excluded from the production JAR. `scripts/test-domain.sh` remains available for the original Minecraft-independent subset; it is not a substitute for the full build.

## Install the starter hut

An operator with permission level **2** prepares a flat supported site in the Overworld. The entire 11 × 9 × 9 volume above the support must be air and already loaded; stand outside the footprint and within 64 blocks. Quarter turns use a 9 × 11 footprint. Origin is the minimum foundation corner, not the NPC position.

```text
/kingdom hut preview 100 65 100 north starter-camp
/kingdom hut confirm <token shown in the confirmation link's hover text>
/kingdom hut status
/kingdom hut name Aldric Stonehand
```

Rotations: `north`, `east`, `south`, `west`. Click **[Confirm Builder's Hut]** in the preview's chat message to confirm without copying its token; the manual command above still works. Confirmation expires after **5 minutes**. Permission, player identity, token, distance, and all placement checks are revalidated when confirming. It installs 348 blocks, verifies the structure, then issues one registered builder. Right-click Aldric to speak; **K** still opens the kingdom overview.

For an interrupted reservation, inspect the server log and exact site first:

```text
/kingdom hut resume <exact hut ID from status> <reason>
```

Resume fills only air or accepts matching existing blocks. It refuses conflicting edits and never reissues a committed missing builder. There is no destructive reset, automatic resurrection or unrestricted duplicate hut command. Protection-event integration is tested; compatibility with any particular external protection mod is not established.

## Donate in Minecraft

Use the same development JAR on the server and all clients. In Survival or Adventure mode, close other containers, press **K**, choose **Contribute**, select a resource and quantity, then **Review donation** and **Confirm donation**. The review expires after 30 seconds; this is separate from the hut placement’s five-minute preview. The server caps acceptance to inventory and outstanding requirements. Leaderboard and Your history show accepted donations. Donations cannot be withdrawn. After all four targets fill, the kingdom reaches Tier 2 and announces its winner. No Lodge claim/build control is enabled yet.

## Persistence and recovery

Back up the complete world. Kingdom state lives in `data/kingdomsmp_kingdom.dat`; the hut record lives in `data/kingdomsmp_builder_hut.dat`. The one-way `data/kingdomsmp_builder_hut.reserved` and `.issued` journal markers must be retained in the same backup as world/entity data. These markers prevent a lagging native save from authorizing another installation or NPC issuance. Missing/mismatched reservation metadata fails closed and requires restoring a matching backup. They do not promise recovery from arbitrary host loss, partial backups or disk failure.

Live contribution state is canonical in `data/kingdomsmp_contributions.dat` with a matching `data/kingdomsmp_contributions.created` identity marker and native playerdata receipts. Retain these, playerdata and level.dat in the same complete backup. [Contribution recovery](docs/CONTRIBUTION_RECOVERY.md) explains interruption handling, quarantine and the boundaries of the guarantee.

The user approved the furnished Founders’ Lodge, named Reeve and included construction materials as the Tier 2 reward, with no extra service unlocks. Public site/protection policy is being clarified before enabling construction. NPC replacement and destructive repairs remain explicit future decisions.

See [requirements](docs/REQUIREMENTS.md), [roadmap](docs/DEVELOPMENT_ROADMAP.md), [hut design](docs/BUILDER_HUT_DESIGN.md), [dialogue design](docs/BUILDER_DIALOGUE_UI.md), and [development log](docs/DEVELOPMENT_LOG.md).
