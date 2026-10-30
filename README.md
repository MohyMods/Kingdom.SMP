# Kingdom SMP Core

Fresh Minecraft Java 1.21.1 / NeoForge / Java 21 rebuild. **Development checkpoint, not a playable release.** Work is on `rebuild/royal-gilded`; no legacy implementation was present or reused.

The Royal Gilded client uses charcoal panels, bronze/gold rules, parchment detail areas, reusable widgets, and vanilla-readable typography. The initial screen is a real read-only server view, not an inventory-menu mockup. Its compiled and rendered behavior is still unverified because the environment cannot download NeoForge artifacts.

## Current scope

- Java 21 Gradle project, common mod entry, physical-client-only entry, and mod metadata.
- Immutable kingdom state, starting at **Tier 1 — The First Hearth**.
- Strict world SavedData adapters for kingdom state and Tier 1 contributor counts/winner. Corrupt or unsupported saves fail rather than silently resetting.
- Read-only, throttled, correlated overview requests. Server selects the world and requesting player; client cannot submit ranks, grants, balances, or target UUIDs.
- Royal Gilded Overview/Progression tabs, twelve inspectable tiers, shared resource counts, personal weighted score, refresh, tooltips, scrolling, and bounded GUI geometry.
- Tier 1 accounting with capped quantities, exact normalized scoring, deterministic tie ordering, and one-time completion state. This module does **not** remove items, advance the world, or deliver a physical blueprint.

Live donations, leaderboard presentation, entitlement delivery, Builder's Hut, NPCs, construction, travel enforcement, economy, and later professions remain unimplemented. Displayed gate descriptions are requirements, not enforcement. No inactive feature is represented by a working-looking action button.

## Build and tests

Requires a full JDK 21 (`javac` as well as `java`). Set `JAVA_HOME` to that JDK.

```sh
./gradlew build
```

For this managed cloud environment, Java must use the session proxy and CA store:

```sh
export JAVA_HOME='/workspace/toolchains/jdk-21.0.13+9'
scripts/gradle-cloud.sh build --console=plain
```

The toolchain directory exists in the current machine; it is not a promise about unrelated machines. The launcher preserves TLS verification and uses the system trust store. It does not grant network access.

An independent test runner is available while Minecraft dependency resolution is blocked:

```sh
scripts/test-domain.sh
```

This downloads a checksum-pinned JUnit runner and compiles only Minecraft-independent domain, request-lifecycle, and layout code. Its 54 passing tests do not replace the full Gradle build, NBT persistence tests, or server/client tests. Reports are generated under ignored `artifacts/domain-tests/`.

## Development runs after dependency access is restored

```sh
./gradlew runServer
./gradlew runClient
```

In cloud, invoke those tasks through `scripts/gradle-cloud.sh`. The server uses `--nogui`; Minecraft's EULA and any account requirements must be handled by the server owner. No EULA acceptance or account credentials are bundled.

On a matching modded client, the overview keybind defaults to **K** and is remappable in Controls. `/kingdom status` reports read-only server tier/revision, including from the console. These entry points are implemented but not runtime-verified yet. Native state is stored at `<world>/data/kingdomsmp_kingdom.dat`; back up the world before experimental runs. No schema migration from temporary pre-S3 development data is implicit.

## Toolchain evidence and blocker

The official `NeoForgeMDKs/MDK-1.21.1-ModDevGradle` template at commit `e93d7f09d4eec12c76d1d7be9bf4eaad0246e9e0` pins NeoForge **21.1.265**, ModDevGradle **2.0.148**, and Gradle **9.2.1**. These are the selected candidate pins. Latest NeoForge release metadata and artifact compatibility remain pending: access to `maven.neoforged.net` returns **403 Forbidden**. Do not describe this as a verified latest NeoForge installation or a compilation error in source.

The Gradle distribution and its version-matched wrapper were checked against Gradle's official SHA-256 values. The full Temurin 21.0.13+9 JDK and JUnit runner were checked against their official release checksums. No artifact/TLS verification was disabled.

See [validation evidence](docs/VALIDATION.md), [requirements](docs/REQUIREMENTS.md), [roadmap](docs/DEVELOPMENT_ROADMAP.md), [construction specification](docs/CONSTRUCTION.md), and [NPC specification](docs/NPCS.md). Original planning sources beyond the user briefs remain unavailable.
