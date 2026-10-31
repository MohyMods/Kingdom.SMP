Latest 0.6.0 fresh-world Town Hall checkpoint: [JAR, actual screenshots, tests and continuation](docs/checkpoints/25aa1dc/CHECKPOINT.md). **1.0.0 is not ready.** [Verified-slice feature inventory](docs/PRODUCTION_COMPLETION_MATRIX.md).

# Kingdom SMP — Royal Gilded development

[Download 0.6.0-dev, actual Minecraft screenshots and execution evidence](docs/checkpoints/25aa1dc/CHECKPOINT.md). Minecraft **1.21.1**, NeoForge **21.1.265**, Java **21**. Development branch: `rebuild/royal-gilded`; never merged into main.

**Tested claim subset; full pack unverified:** exact FTB Chunks2101.1.22 + OPAC0.33.0 adapters now validate both systems over the full site and every progressive step. Revocation pauses and restored access resumes. The actual owner must be alive, non-spectator and online in the building dimension. Other versions/API failures refuse safely. All five locked dependencies ran together on a normal dedicated server and real graphical client; the complete277-entry pack and other providers remain unverified. [Compatibility evidence](docs/CLAIM_COMPATIBILITY_AUDIT.md).

The [master directive](docs/sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md) supersedes previous planning holds. New worlds receive the 19×15, two-floor **Town Hall at Tier 2**; existing Lodge worlds must remain intact. Versioned profiles, the free owner Hut plan, automatic server-valid site authorization, furnished Hall and permanent Elara services are implemented and tested. Irregular foundations, Architect marketplace and early economy remain pending. [Current rules and implementation status](docs/MASTER_SUPERSESSION_MATRIX.md).

## Implemented and tested core gameplay

- Persistent **Tier 1 — The First Hearth**, shared consumed contributions, personal history, exact normalized leaderboard, deterministic permanent winner and advancement to Tier 2. Targets: 1,024 logs, 2,048 cobblestone, 512 wheat and 128 iron ingots. Inventory receipts and durable recovery guard interruptions.
- Furnished 11×9×9 Builder's Hut, Aldric Stonehand, persistent NPC identity/routine and Royal Gilded dialogue. Protected administrator setup has a **five-minute clickable chat confirmation**. Fresh owners claim the authenticated free physical Hut plan through K → Construction.
- Preserved legacy winner's Lodge entitlement: claim/recover one authenticated plan, review/approve an exact site, redeem once, then Aldric physically constructs 782 blocks and introduces **Mira Ashford**, Reeve. Construction persists and pauses safely; no terrain clearing or chunk tickets.
- New-profile highest contributors receive the 19×15 two-floor Hall plan at Tier2. Aldric constructs 1,815 verified blocks and introduces Elara Vale; Steward and lectern open actual kingdom/contribution services. Existing Lodge records are never converted.
- Holding any supported current plan displays a full translucent model/roof and footprint. Aim naturally, **R / Shift+R** rotate, **Page Up/Down** adjust height, **Use** opens the exact Royal Gilded review. Keys are remappable. Green requires a fresh matching server result; red marks refusal, amber is pending. Collision/support diagnostics are bounded. Legacy administrator approval is still required.
- Bounded GPU geometry caching survives rotation eviction, hand switching and resource reload. Actual normal/narrow Minecraft frames and process-restart construction checks accompany this build.
- Vanilla **Nether entry requires Tier 5; End entry requires Tier 12**, server-enforced for tested player/item transfer paths. Unknown mod routes and imported characters already inside locked realms remain unverified. Other dimension adapters/Ad Astra recipes are not implemented.

The whole mod is not complete. Existing worlds were not converted. Bank/trading/Charter, marketplace, professions, auctions and later costs remain inactive. [Checkpoint scope and limits](docs/HELD_BLUEPRINT_CHECKPOINT.md) · [completion tracker](docs/MASTER_COMPLETION_TRACKER.md). Historical builds remain archived separately.

## Flexible milestone definitions

Server datapacks now define all twelve inspectable resource/objective proposals. **K → Progression** shows server targets, approved/proposed status and actual building completion. `/kingdom milestones inspect <tier>` is read-only; permission-level-2 operators can use `/kingdom milestones validate`. Invalid definitions block new donations safely; existing First Hearth scoring/history/recovery remain pinned. Proposed later costs are **disabled**.

See [the proposed twelve-tier resource/effort table](docs/PROGRESSIVE_RESOURCE_BALANCE.md) and [the server-data schema](docs/MILESTONE_DATA_FORMAT.md). Generic live later-tier transactions and Storehouse storage are not enabled by these definitions.

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

The complete build passes **142 JUnit tests**. A separate opt-in normal-dedicated lifecycle runs both exact claim APIs, independent denials, cross-chunk ownership, revocation/resume/offline pause and actual native writes; `JAVA_HOME=/path/to/jdk21 python3 scripts/test-claims.py` reproduces it with checksum-pinned downloads. The runtime runner passes one substantial NeoForge GameTest lifecycle scenario in a fresh isolated world, covering donation replay/stale/racing requests, actual inventory consumption, injected native-save failure/recovery, completion/winner/history/reload and all existing hut/NPC checks. Dedicated-server startup/save/restart and physical-client integrated-world contributions have also run. See [validation](docs/VALIDATION.md) for exact scope and limits; compilation is not full multiplayer or release acceptance.

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

Use the same development JAR on the server and all clients. In Survival or Adventure mode, close other containers, press **K**, choose **Contribute**, select a resource and quantity, then **Review donation** and **Confirm donation**. The review expires after 30 seconds; this is separate from the hut placement’s five-minute preview. The server caps acceptance to inventory and outstanding requirements. Leaderboard and Your history show accepted donations. Donations cannot be withdrawn. After all four targets fill, the kingdom reaches Tier 2 and announces its winner. Choose Construction to claim the winner’s plan and review placement. The winner must hold the current valid plan; an administrator with permission level 2 must approve the exact site before construction. The plan includes building materials, not loose item grants.

## Persistence and recovery

Back up the complete world. Kingdom state lives in `data/kingdomsmp_kingdom.dat`; the hut record lives in `data/kingdomsmp_builder_hut.dat`. The one-way `data/kingdomsmp_builder_hut.reserved` and `.issued` journal markers must be retained in the same backup as world/entity data. These markers prevent a lagging native save from authorizing another installation or NPC issuance. Missing/mismatched reservation metadata fails closed and requires restoring a matching backup. They do not promise recovery from arbitrary host loss, partial backups or disk failure.

Live contribution state is canonical in `data/kingdomsmp_contributions.dat` with a matching `data/kingdomsmp_contributions.created` identity marker and native playerdata receipts. Retain these, playerdata and level.dat in the same complete backup. [Contribution recovery](docs/CONTRIBUTION_RECOVERY.md) explains interruption handling, quarantine and the boundaries of the guarantee.

The legacy runtime preserves the previously approved furnished Lodge, named Reeve and included construction materials without extra service unlocks. D-023 requires a distinct new-world Hall profile before changing future rewards. The retained legacy placement policy requires a clear supported Overworld site and exact administrator approval; all terrain replacement is disabled. Construction state and its `.created` identity marker live in `data/kingdomsmp_construction.dat` and must be backed up with the whole world. See [construction behavior and recovery](docs/FOUNDERS_LODGE_IMPLEMENTATION.md). NPC replacement and destructive repairs remain explicit future decisions.

See [requirements](docs/REQUIREMENTS.md), [roadmap](docs/DEVELOPMENT_ROADMAP.md), [hut design](docs/BUILDER_HUT_DESIGN.md), [dialogue design](docs/BUILDER_DIALOGUE_UI.md), and [development log](docs/DEVELOPMENT_LOG.md).
