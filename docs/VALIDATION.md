# Validation checkpoint — 2026-10-30

## 2026-10-31 — Flexible milestone data, dependencies and safe reload

Version 0.4.0-dev / protocol 5. Full build passes **139 JUnit tests, zero failures/errors/skips** (`work/milestones-final-build.log`). The expanded native lifecycle passes **all 1 required GameTests**, BUILD SUCCESSFUL (`work/milestones-runtime.log`), including generic registry/tag matching, pre-reload quote invalidation without consumption, preserved history/winner, actual completed-Lodge objective and public/operator command separation, alongside the entire previous contribution/construction/NPC suite. Final subsequent changes compact only UI text/layout and labels; they passed the full build.

A real matching client joined an isolated normal dedicated-server copy at Tier 1/revision 2. Its progression screen loaded actual server definitions and the revised disabled Tier 2 targets. Operator `/reload` with a deliberately duplicate JSON field rejected the catalogue. Actual donation review was refused; 164 held logs and revision 2 remained unchanged. Restoring the valid datapack and reloading allowed a fresh reviewed/confirmed donation of exactly 64 logs, leaving 100, with revision 3 and unchanged pre-existing iron credit. Console inventory output and actual screenshots substantiate the check. The server saved/stopped with BUILD SUCCESSFUL. The earlier UI showed redundant headings; the final panel removes them and uses clear reward names/full visible lines. The final normal server and real client restarted with Tier 1/revision 3 and 100 remaining logs. Actual final normal/narrow screenshots were inspected; all eight targets fit the normal panel. The subsequent console save/stop passed BUILD SUCCESSFUL. This slice used one actual client; it does not claim a new two-client reload race.

All later costs remain disabled, including the user's eight revised Tier 2 quantities. This checkpoint implements server-data definitions, dependency/registry validation, diagnostics, live inspection and safeguards around existing First Hearth consumption. It does not claim generalized later-tier transactions, approved future score weights, road evidence approval, Royal Storehouse storage, mod recipe compatibility or AFK-provenance enforcement. See MILESTONE_DATA_FORMAT.md and PROGRESSIVE_RESOURCE_BALANCE.md.

## 2026-10-31 — Final Lodge gameplay and rendering verification

Version **0.3.0-dev**, network protocol **4**. Full Gradle build: **BUILD SUCCESSFUL; 121 JUnit tests, zero failures/errors/skips** (`work/lodge-final-build.log`). Expanded native lifecycle: **BUILD SUCCESSFUL; all 1 required GameTests passed** (`work/lodge-final-runtime.log`). The test covers the entire furnished structure, claim/recovery/redemption, permissions/stale requests, interruption, weather cleanup without drops, protected blocks, NPC identity/death, unloaded work and vanilla realm gates. The final weather case also checks that player snow placed after completion is preserved.

Actual Minecraft 1.21.1 / NeoForge 21.1.265 client testing completed the existing world's real claim → preview → proposal → administrator approval → one-use redemption → physical Aldric construction → **782/782 COMPLETE** → Mira Ashford arrival. Construction ran at production cadence, one block per ten ticks, without chunk tickets. The same project survived an in-progress full client process restart and resumed. After completion, another full process restart with the final protocol reloaded the same completed project and **exactly one Reeve with the identical UUID** (`[I; -799299434, -1979298050, -1696212763, 88883981]`); her actual dialogue opened successfully. Doors and empty storage were interacted with, the milestone lectern opened the live completed project, and Mira's Community milestone button opened real saved contribution data. Contribution Back navigation now resubscribes to project updates. Actual normal and narrow interface captures are delivered with this checkpoint.

Exterior/interior captures are actual Minecraft frames from the development sky-platform fixture. Weather snow left in empty interior spaces by the earlier live build was cleared in that isolated fixture using an exact snow-only command. The final native-tested handover now clears those weather layers before completion; it never cleans a completed player's property. Screenshots are not mockups. No banking, Royal Trading, Charter, profession or later-tier service is granted by the Lodge.

Two real final-protocol Minecraft clients (Dev/operator and DevTwo/non-admin) joined a normal dedicated server loaded from a complete saved copy of the fixture. Both inspected the completed project; DevTwo opened Mira’s actual contribution service, seeing public totals and zero personal contributions. His construction screen had only Blueprint/Project tabs, with no Royal Admin control. Server selectors counted exactly one Reeve and one Aldric; the Reeve UUID matched the integrated-world record. Console save/stop ended with BUILD SUCCESSFUL. A subsequent normal dedicated restart reloaded Tier 2/revision 6 and the same single Reeve UUID, then saved/stopped with BUILD SUCCESSFUL. This checks multiplayer access/rendering/persistence; simultaneous new construction/redemption is covered by native tests rather than a second complete real-client build.

Known limits: local/offline sessions; no authenticated remote session, external protection mod, Windows storage, arbitrary partial restore/host loss or season-scale TPS claim. Conservative missing/issued NPC recovery still refuses automatic replacement; resurrection/destructive repair remains an explicit decision. Later milestones, quests, economy policies, borders and actual integration gate matrices remain unapproved. The whole mod is **not complete**. See the master tracker and continuation document.

## 2026-10-31 — Weather, safe resident arrival and vanilla realm gates

Current full build: **121 JUnit tests passed**, zero failures/errors/skips. The expanded native lifecycle also passes, including rejection of pre-existing snow before approval, brushing only later weather snow without item drops, and delaying Reeve identity issuance while her home is occupied. It verifies actual cancelled EntityTravelToDimension events, ServerPlayer.changeDimension, direct cross-realm teleportTo and item-entity transfers at the current locked tier. Pure tests cover all twelve tier boundaries for Nether 5/End 9. Third-party APIs/custom dimensions and imported characters already inside a locked realm are not claimed covered.

Actual client claim, footprint overlay, proposal, exact administrator review/confirmation and winner redemption worked. Aldric physically walked from the original hut and placed blocks at two per second. A full client/world stop/restart preserved the same project and progressed past the saved ~300 blocks. Winter snow then stopped an unfinished wall at 354/782; server inspection confirmed snow, and the native-tested fix handles only weather snow within the originally empty protected reservation. An operator QA camera teleport outside the prepared sky-platform edge caused a test-character fall, corrected by respawning and using the supported walkway; it is not a construction failure. All fixture ground preparation used air-only fills outside the preserved hut after automatic review rejected a broader replacing fill. Final completion/Reeve/scaling/world-restart checks are still pending.

## 2026-10-31 — Lodge server lifecycle checkpoint

Version 0.3.0-dev / protocol 3. Executed the full Gradle build: **118 JUnit tests, zero failures/errors/skips**. The native-server lifecycle passes **all 1 required GameTests**, now including winner-only/full-inventory/old-copy plan claim and recovery, permission loss, superseded/stale/blocked approvals, one redemption, Aldric proximity, one block per step, protection cancellation and rollback, active break protection, native journal reload, obstruction/missing-prefix refusal, inspected resume, all furnished blocks and one persistent Reeve, damage/death refusal, unloaded work without chunk loading and a delayed neighbor-update check. Direct test-fixture work steps accelerate the full build; production cadence is one block per ten ticks. Real client rendering, actual Aldric travel, live client/admin interaction and full native world restart remain pending for this new slice.

## 2026-10-31 — Reconnected workspace and reproducible build

Recovered the existing workspace without replacing source or worlds. The saved tested JAR has SHA-256 `67eedb7cf5e56e4ef0c1d42a2d49490d99eb2aabaef20b1951eb9032ac2ec9e9`. A forced full build (`build --rerun-tasks --no-build-cache`) executed all nine tasks successfully, including **108 JUnit tests with zero failures/errors/skips**, and reproduced that exact JAR checksum. Earlier physical-client/two-client screenshots and server logs remain intact. The completed normal server stop passed before the interruption; the later multiplayer restart loaded revision 2, but no subsequent two-client rejoin is claimed. Old QA processes were stopped after reconnection.

## First Hearth live contributions and Tier 2 advancement

Current source version: **0.2.0-dev**, Minecraft 1.21.1, NeoForge 21.1.265, full Temurin Java 21. No changes to main.

- Complete Gradle build: **BUILD SUCCESSFUL; 108 JUnit tests, zero failures/errors/skips**. Includes atomic-ledger interruption boundaries before/after replacement, corrupt/missing/future-store refusal, exact completion/winner reconstruction, bounded real packet codecs and all earlier tests.
- `scripts/test-runtime.sh`: **BUILD SUCCESSFUL; all 1 required GameTests passed**. The substantial isolated native-server scenario now also covers creative denial, partial donation, cross-player nonce/replay rejection, inventory drift, concurrent quotes at the final iron boundary, exact excess preservation, native player-saving failure after durable removal, quarantine, restart reconciliation exactly once, complete history pagination, winner/Tier 2 persistence and stale integrated-host inventory reconciliation. Its expected injected saving exception is a recovery test, not an unresolved failure.
- Physical Minecraft client: actual Survival UI donations accepted 64, then 36, then a capped 924 from 1,000 supplied logs; 76 excess logs remained. Cobblestone 2,048, wheat 512 and iron 128 completed the milestone. The server announced Tier 2 and the permanent winner. Contributions, leaderboard and personal history were visually inspected at 640 × 400 GUI pixels; history scrolling also ran at 427 × 240. Actual screenshots are preserved with the checkpoint.
- A full client process/world restart reloaded Tier 2, revision 6, all four exact totals, six accepted history entries and the same winner. The existing furnished hut/Aldric survived. No simulated screen or design mockup is substituted for the screenshots.
- A normal dedicated server loaded a complete copy of that saved test world at Tier 2, revision 6. Its first run had closed stdin and ended through the JVM shutdown hook (exit 143 / Gradle failure); it is not called a passing Gradle run. The subsequent console-enabled start/status/save/stop of the same world completed with **BUILD SUCCESSFUL**.
- **Two real Minecraft clients** joined an isolated localhost dedicated server in Survival as Dev and DevTwo. Both reviewed 80 iron at revision 0. Dev's confirmation committed 80 and pushed revision 1 to DevTwo, discarding the old review. DevTwo's new quote capped to 48, committed once, and retained 32. Both actual clients displayed the same two-player leaderboard at revision 2 with scores 0.6250 and 0.3750. Native entity inventory output verifies the remaining quantities. The server saved/stopped normally with BUILD SUCCESSFUL.

The second virtual display initially failed NeoForge's early-window GLX context handover. Disabling early-window control only in that ignored test client's FML config allowed initialization and the two-client check; no production code workaround was needed. The host has no sound device; development sessions are local/offline and Authlib public-key/profile requests log nonfatal connection refusals. Authenticated remote multiplayer, external protection mods, Windows storage, arbitrary partial restores, host/power loss and season-scale performance are not claimed verified. See the contribution recovery contract for supported interruption boundaries.

This completes the implemented contribution/leaderboard/history and Tier 1 → Tier 2 slice. The permanent winner establishes eligibility for the approved Founders’ Lodge privilege. Physical blueprint claim/recovery/redemption, progressive construction, the Lodge and Reeve are **not implemented**; their pending public site/protection decision remains explicit. Ultimate Town Hall/Elara are Tier 12 only. No bank/trading/Charter or later-tier gameplay gates were silently enabled.

## Five-minute clickable confirmation fix

Executed with the full local JDK 21:

- `scripts/gradle-cloud.sh build --console=plain`: **BUILD SUCCESSFUL; 96 JUnit tests, zero failures/errors/skips**. New tests verify that a preview remains valid after 30 seconds, expires exactly at 300 seconds, and handles monotonic-clock wrap.
- `scripts/test-runtime.sh`: **BUILD SUCCESSFUL; one required GameTest lifecycle scenario passed**. The test reads the native `RUN_COMMAND` click event from the preview's emitted chat component and runs that exact command through the real server dispatcher. It verifies the five-minute review text and denial for lost permissions, another player using the link, a mismatched token, a superseded preview, excessive distance and a stale/blocked site. Existing protection cancellation, reservation/resume, replay, NPC and save checks also pass.
- `git diff --check`: passed. Only preview timing/chat presentation and related tests/documentation changed; placement validation and installation logic were preserved.

Timing boundaries use a controlled monotonic timestamp in unit tests; the runtime test does not wait five wall-clock minutes. No additional physical-client mouse-click or two-client check is claimed for this fix. The earlier F2 archive remains unchanged and is explicitly historical.

## Resumed published environment — foundation build verified

The attached published environment now reports current, enforced unrestricted HTTP access. Official NeoForge Maven and Minecraft downloads succeed. Maven metadata confirms **21.1.265** as the latest stable 1.21.1 NeoForge release. The installed OpenJDK 21 is runtime-only; a full Temurin 21.0.13+9 JDK was downloaded and verified against Adoptium SHA-256 `d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c`.

Executed `JAVA_HOME="$PWD/work/toolchains/jdk-21.0.13+9" scripts/gradle-cloud.sh build --console=plain`: **BUILD SUCCESSFUL**. Fixed the actual Gradle 9 configuration-cache violation in resource metadata expansion, and enabled documented ModDevGradle JUnit support so Minecraft/NBT types are available to tests. Main source compilation, resource processing, JAR assembly, and all existing tests now execute. Runtime/client/multiplayer validation remains separate. The earlier blocked reports below are historical.

## F2 implementation verification

- Full build after the hut integration: **93 JUnit tests, 0 failed, 0 skipped**. Includes NBT persistence, four-rotation structure integrity, journal reissuance/corruption, and bounded dialogue codecs.
- `scripts/test-runtime.sh`: **1 required GameTest lifecycle scenario passed**, with multiple assertions against real server blocks/entities/commands and native disk saves. Each invocation uses a fresh ignored test directory. The test mod and structure fixture are isolated from the production JAR.
- Runtime scenario covers op denial; all rotation footprints; unloaded-site refusal; container obstruction; stale confirmation without reservation; protection-event cancellation and rollback; same-reservation resume; 348-block structural verification; single registered NPC; replay denial; configurable name preserving UUID; nearby/remote/damaged-hut dialogue rules; native SavedData disk write/read; entity UUID/home serialization; death marking and refusal to use restored copies after death.
- A normal dedicated server reached `Done`, loaded Tier 1, stopped through its shutdown hook, restarted the same saved world, executed `/kingdom status` and `/save-all flush`, then stopped via the console with **BUILD SUCCESSFUL**. The initial run ended with process status 143 because its stdin was not forwarded; console forwarding is now configured. This is not classified as a successful initial Gradle run.
- Physical client initialization and integrated-world join ran under Xvfb with software OpenGL. Actual interaction opened Aldric's custom dialogue. This exposed vanilla `Screen.render` applying its blur after custom drawing; both custom screens now suppress that late background pass.
- A full client/world restart reloaded the existing hut and Aldric and opened the corrected dialogue. Real screenshots record the rendered result; no screenshot is a simulated web mockup.
- After restart, `/execute if entity @e[type=kingdom_smp:aldric]` returned **count: 1**. Native entity UUID data reconstructed to the exact UUID recorded during the original setup. All four dialogue topics, catalogue, overview, refresh and progression navigation were exercised on the physical client.

Runtime limitations: headless host has no audio device, so sound is disabled by Minecraft. Mojang public-key fetch logs a nonfatal connection refusal from Authlib; isolated development used local/offline sessions, so authenticated public multiplayer remains unverified. No two-client race, forced host-loss, external protection-mod compatibility, path-failure stress, or season performance claim is made. The protection-cancellation test verifies NeoForge's event contract, not every protection mod. Native saves and journal markers must be backed up together; journal mismatches fail closed rather than inventing a new NPC or hut.

## Historical checkpoint before dependency access

Branch: `rebuild/royal-gilded`. S2 fresh rebuild and S3 First Hearth/NPC construction authorization. This report distinguishes implemented source from observed runtime behavior.

| Check | Actual outcome |
| --- | --- |
| Official template | Read from NeoForgeMDKs/MDK-1.21.1-ModDevGradle, commit e93d7f09d4eec12c76d1d7be9bf4eaad0246e9e0 |
| Candidate NeoForge/ModDevGradle | 21.1.265 / 2.0.148 from that template; latest NeoForge metadata blocked, no compatibility claim |
| Gradle 9.2.1 | Wrapper executed successfully using Temurin JDK 21.0.13+9 |
| Wrapper and distribution | Official SHA-256 values verified; MDK's older wrapper replaced with verified 9.2.1 wrapper extracted from verified distribution |
| Java compiler | Full Temurin JDK downloaded from official Adoptium GitHub release, SHA-256 verified; Java 21 compilation executed for independent tests |
| Independent suite | **54 started, 54 successful, 0 skipped/failed/aborted** via scripts/test-domain.sh |
| Suite scope | KingdomState, TierOneProgress, OverviewRequestTracker, RoyalLayout |
| Full Gradle build | **Failed before compilation:** neoform-runtime:2.0.31 resolution received HTTP 403 from maven.neoforged.net |
| Session trust | Downloaded JDK initially lacked session CA; cloud launcher now uses system Java CA store. Final build no longer reports that TLS error. TLS verification retained. |
| Minecraft/NBT unit tests | Written, **unrun**; the independent runner deliberately excludes Minecraft-dependent tests |
| Dedicated server/start/save/restart | **Unrun**, blocked by Minecraft/NeoForge artifacts |
| Client initialization and rendered UI | **Unrun**; no game screenshots or visual-quality approval claimed |
| Actual networking/two-client checks | **Unrun**; only pure request lifecycle is tested |
| Donations/reward/NPC/construction | Not enabled/implemented as live gameplay; no success claim |

## Independent test coverage

- Initial Tier 1 and all twelve representable tiers; invalid schema/revision/tier rejection.
- Each resource cap and excess preservation plan; partial contributions, negative/huge inputs, immutable state, exact weights, equivalent category scores, first-reached ties and deterministic fallback, revision bounds, final winner consistency, one-time completion event.
- Request ID correlation, duplicate/stale replies, timeouts, reconnects, screen changes, connection identity, monotonic-clock wrap handling.
- Panel/control bounds across common GUI-scaled dimensions and capped panel size. These are geometry tests, not rendered text or screenshot tests.

NBT adapters also contain tests for partial/final progress round trips, malformed tags, duplicate/oversized contributor lists, and preservation of existing corrupt files. These need the real Minecraft dependencies and have not executed.

## Network configuration

A draft save was confirmed for the following custom domains, preserving the package-manager preset: maven.neoforged.net, maven.minecraftforge.net, libraries.minecraft.net, piston-meta.mojang.com, piston-data.mojang.com, resources.download.minecraft.net, assets.minecraft.net, launchermeta.mojang.com, launcher.mojang.com.

The tool returned `requires_publish: true`. Saving the draft does not apply access or publish a snapshot. The user must review/save the environment settings and publish as prompted. Then retry the specific metadata and build requests. No duplicate secret or GitHub token is needed based on this failure. No prototype finalize/publish API has been invoked.

Current-instance checks do not establish publication or fresh-machine restoration. No install_script/start_skill change was saved as a verified startup recipe while the required full build remains blocked.

## Remaining acceptance gates

1. Resolve network access, read official latest compatible metadata, adjust candidate version only if supported, and resolve pinned artifacts.
2. Execute Gradle build and all tests; investigate every actual API/compiler/test failure.
3. Dedicated-server start/stop/restart, actual SavedData round trip, corrupt/future-save refusal, matching client join.
4. In-game Overview screenshots and GUI scale/input/resize inspection; two-client refresh/disconnect and hostile payload checks.
5. Implement the admin-controlled starter hut/NPC before enabling the full contribution/reward milestone flow; establish the durable inventory-consumption/recovery contract.

M1/M2 and the live Tier 1 feature are **not complete** at this checkpoint. Do not deploy this source as a production season release.

## Resumed task after user-reported publication

Retried the actual Gradle build after the user reported publishing dependency domains. The current running task still reports enforced revision 1 with no custom hosts; official metadata and `neoform-runtime:2.0.31` resolution still return HTTP 403. This task has not acquired the new network policy. Source compilation, NBT tests, server/client execution, and complete dependency verification remain blocked. See [environment handoff](ENVIRONMENT_HANDOFF.md) for preservation and safe continuation in a new task. No existing passing test result is reclassified as a full mod build.
