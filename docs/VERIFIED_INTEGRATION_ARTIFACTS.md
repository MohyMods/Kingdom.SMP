# Verified integration artifacts and inspected APIs — 31 October 2026

This audit inspected exact published binaries matching the reported snapshot. It is **not runtime or full-pack certification**. No production gates, build configuration, or mod versions were changed. Downloads and inspection evidence remain ignored under `work/verified-integration-artifacts/`; no third-party binary is tracked. The already tested FTB claims subset does not certify these additional mods.

## Exact public download sources

| Artifact | Official/public distribution URL | SHA-256 |
| --- | --- | --- |
| FTB Quests NeoForge2101.1.37 | https://maven.ftb.dev/releases/dev/ftb/mods/ftb-quests-neoforge/2101.1.37/ftb-quests-neoforge-2101.1.37.jar | `6b42457d680108d0af5b8deb21e3fe7b92138209f0683c62441dc360d4fc01d5` |
| aether-1.21.1-1.5.10-neoforge.jar | https://cdn.modrinth.com/data/YhmgMVyu/versions/K5X5qMwG/aether-1.21.1-1.5.10-neoforge.jar | `a999a9265eb550a46a0f8eedfee7c3c75371d7f6cf34b7c09ff800e48633e9f8` |
| deeperdarker-neoforge-1.21.1-1.4.1.jar | https://cdn.modrinth.com/data/fnAffV0n/versions/TuD0Zvi3/deeperdarker-neoforge-1.21.1-1.4.1.jar | `eee3f51222b0bcc714def002ff089ac9e131d3cae4575b542fd0a7dd101fe0af` |
| adastra-1.21.1-1.16.26-neoforge.jar | https://cdn.modrinth.com/data/3ufwT9JF/versions/9pnKHdCG/adastra-1.21.1-1.16.26-neoforge.jar | `b8d1441e284db2e1e8ce5ffe739452633c90725312e476db8d8a0a4b64126856` |
| immersive_aircraft-neoforge-1.7.0+1.21.1.jar | https://cdn.modrinth.com/data/x3HZvrj6/versions/xO17G766/immersive_aircraft-neoforge-1.7.0%2B1.21.1.jar | `27c8e60eb1b86eeb12ab916af0d057a08cb64df5cc4a38616a370b42c00a311d` |
| twilightforest-1.21.1-4.8.3345-universal.jar | https://mediafilez.forgecdn.net/files/7797/302/twilightforest-1.21.1-4.8.3345-universal.jar | `493da16d10210f9f53a3c33b3d8fcb4ae9b14016ee420d753c894562bd9aba35` |

## Verified metadata


- FTB Quests NeoForge2101.1.37: `ftbquests.jar`, 1,635,351 bytes, SHA256 `6b42457d680108d0af5b8deb21e3fe7b92138209f0683c62441dc360d4fc01d5`. Official URL in fetch-manifest.json. Mod ID `ftbquests`; requires NeoForge>=21.1.0, Minecraft>=1.21.1, Architectury>=13.0.8, FTB Library>=2101.1.36, Teams>=2101.1.9. Existing pinned claims versions satisfy these declared minimums. `ftbxmodcompat`>=21.1.7 is optional. License All Rights Reserved: do not redistribute the binary with the custom mod. Official sources artifact contains loader-specific sources only, not common APIs.
- Aether NeoForge1.5.10: 40,223,909 bytes, SHA256 `a999a9265eb550a46a0f8eedfee7c3c75371d7f6cf34b7c09ff800e48633e9f8`; dimension `aether:the_aether`; requires Neo>=21.1.72 / Minecraft1.21.1.
- Twilight Forest4.8.3345: 27,154,121 bytes, SHA256 `493da16d10210f9f53a3c33b3d8fcb4ae9b14016ee420d753c894562bd9aba35`; dimension `twilightforest:twilight_forest`; Neo>=21.1.209 / Minecraft1.21.1. Official CurseForge file7797302 and official CDN URL recorded in verified-twilight-artifact.json. Official mod is not the similarly named unofficial Modrinth project; initial slug404s were not treated as unavailable releases.
- Deeper and Darker NeoForge1.4.1: 3,906,057 bytes, SHA256 `eee3f51222b0bcc714def002ff089ac9e131d3cae4575b542fd0a7dd101fe0af`; dimension `deeperdarker:otherside`; Neo>=21.1 / Minecraft1.21.1.
- Ad Astra NeoForge1.16.26: 7,301,791 bytes, SHA256 `b8d1441e284db2e1e8ce5ffe739452633c90725312e476db8d8a0a4b64126856`; actual mod ID is `ad_astra`. Requires ResourcefulLib>=3.0.0, ResourcefulConfig>=3.0.0 and CommonStorageLib>=0.0.10 (all present in reported snapshot, artifacts not independently fetched here). Dimensions: `ad_astra:earth_orbit`, `moon`, `moon_orbit`, `mars`, `mars_orbit`, `venus`, `venus_orbit`, `mercury`, `mercury_orbit`, `glacio`, `glacio_orbit`.
- Immersive Aircraft NeoForge1.7.0+1.21.1: 2,543,237 bytes, SHA256 `27c8e60eb1b86eeb12ab916af0d057a08cb64df5cc4a38616a370b42c00a311d`; mod ID `immersive_aircraft`. Optional controller integrations; no required external mod beyond loader/game declared in inspected TOML.

Modrinth downloads matched both published SHA512 and SHA1. Exact URLs, metadata, dependencies and dimensions are in verified-modrinth-artifacts.json and verified-dimension-metadata.json. Snapshot versions all exist as actual matching NeoForge1.21.1 artifacts; no version substitutions were made.

## FTB Quests integration API verified against exact binary

`CustomTaskEvent.EVENT.register(event -> { var task=event.getTask(); ... });`

For Kingdom-tagged custom tasks only: `task.setCheck((data,player) -> data.setProgress(authoritativeValue));`, `setCheckTimer(int)`, `setMaxProgress(long)`, `setEnableButton(false)`. Select by `task.hasTag(...)` or stable code string. The callback Data exposes task(), teamData(), getProgress(), setProgress(long), addProgress(long).

`ServerQuestFile.getInstance()`, getAllObjects(), getAllTeamData(), markDirty()/saveNow(); `TeamData.get(player)`, setProgress(Task,long), isCompleted(...), canStartTasks(...), markDirty(). `ClearFileCacheEvent.EVENT` supports cache refresh/reload handling. `CustomRewardEvent.EVENT` exposes reward/player; any monetary or physical reward must still invoke Kingdom's own idempotent authoritative operation. Do not let quest resets mint duplicate rewards.

There is NO FTBQuestsAPI.registerTask in this release. `TaskTypes.register(ResourceLocation, TaskType.Provider, Supplier<Icon>)` is the actual custom type registration API if a dedicated task subclass is needed; built-in custom tasks avoid that complexity.

Important personal-progress limitation: task completion belongs to FTB TeamData, not per-player data. The inspected TeamData.PerPlayerData only contains edit/auto-pin/pinned-quest state. Do not make another player's Master profession unlock depend on shared FTB completion. A proper personal profession guide/adapter must preserve Kingdom's own per-player authoritative records and cannot be certified by marking shared team tasks completed from whichever member was checked first.

ABI evidence: ftbquests-api.txt, ftbquests-progress-api.txt, ftbquests-serverfile-bytecode.txt.

## Dimension and aircraft gate hooks

The verified dimension IDs are suitable for the existing server-authoritative destination gate. Aether/Otherside/Twilight portal code uses vanilla DimensionTransition; Ad Astra's actual PlatformUtils.teleportToDimension invokes Entity.changeDimension(DimensionTransition).

Do not assume cancellation alone is safe for Ad Astra rocket landings. Its ModUtils.land first stops riding and moves the player, performs the transition, then creates a lander and assumes the returned entity is nonnull before moving inventory/discarding the rocket. Add an earlier destination eligibility check in its landing/travel request adapter and run actual launch/restart tests. This audit identifies a potential cancellation-side-effect hazard, not a reproduced crash. Bytecode is preserved in adastra-platform-bytecode.txt and adastra-modutils-bytecode.txt.

Aircraft type inheritance: immersive_aircraft.entity.AircraftEntity -> EngineVehicle -> InventoryVehicleEntity -> VehicleEntity. VehicleEntity has interact(Player,InteractionHand), protected canAddPassenger(Entity), getControllingPassenger(), add/removePassenger. A cancellable NeoForge mounting/use guard can enforce Tier>=6 AND verified Aerodrome completion before ordinary player boarding, with actual type/ownership validation; server tick control checks are needed for continued eligibility. Do not claim these hooks work until the real mod has been loaded and tested.

## End dependency observations

The exact Ad Astra JAR contains613 recipe JSONs with no direct End-exclusive or ender-item references. NASA recipes progress steel->desh->ostrum->calorite. This is NOT a complete modpack tag/recipe dependency closure: actual datapacks, configs and other mod recipes may alter inputs or acquisition.

DeeperDarker soul_elytra explicitly requires vanilla elytra: exclude it from any mandatory pre12 progression. Twilight's two end-like modifiers use ender pearls/eyes; those are obtainable from Overworld+Nether and are NOT End-exclusive. Do not classify every `ender_*` item as inaccessible before Tier12.

## Remaining release blockers

- No actual client/server initialization, travel, aircraft, or FTB quest loading run was performed by this audit.
- No277-pack launch or full recipe/tag/loot/trade bypass closure has been tested.
- The277 snapshot rows include nested FFAPI modules, Connector Extras bridges and CommonStorageLib submodules; they are not277 separate JAR downloads. No actual full-pack archive or resolved artifact/config/datapack/hash manifest is present in the repository/workspace inspected for this audit. Reproducible full-pack acceptance needs that resolved manifest, not one guessed JAR per displayed mod ID. This is a precise **full-pack release acceptance blocker, not a blocker to core coding or isolated integrations**.
- Exact Ad Astra Tier10 versus11 destination split remains a gameplay policy to reconcile before activating planet gates.
- Personal FTB progression adapter, chapter/reward synchronization and repeated/reload/race tests remain necessary.
- Do not redistribute third-party binaries merely because public audit download succeeded. Publish hashes/download scripts or reference the official permitted distributions.
