# Validation checkpoint — 2026-10-30

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
