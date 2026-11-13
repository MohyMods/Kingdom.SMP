# Kingdom SMP — build from 6b23ddb

Source commit: [`6b23ddbbf4ea197544868bbca5ad8c9f61cdc250`](https://github.com/MohyMods/Kingdom.SMP/commit/6b23ddbbf4ea197544868bbca5ad8c9f61cdc250). The source tree was clean and at this exact commit during the build. This directory publishes the resulting JAR without changing its compiled source.

[Download the updated JAR](kingdom-smp-core-0.1.0-dev-6b23ddb.jar?raw=true)

## Included fix

- Builder's Hut preview confirmation expires after **5 minutes**.
- The gold, underlined **[Confirm Builder's Hut]** chat link executes the existing server confirmation command. No UUID copying is required.
- Existing operator permission, player ownership, token/expiry, distance, site validation, protection and duplication checks remain enforced.

The compiled JAR was inspected for the native `RUN_COMMAND` click action, confirmation text, and the five-minute `TimeUnit.MINUTES.toNanos(5)` initializer. Runtime test classes/resources are excluded from the JAR.

## Installation

Requires **Minecraft 1.21.1**, **NeoForge 21.1.265**, and **Java 21** (the tested versions). Replace the old Kingdom SMP JAR in the `mods` folder; do not keep both copies. For multiplayer, install this same JAR on both client and server. Back up the world before testing this development build.

An operator with permission level 2 can run `/kingdom hut preview <x> <y> <z> north <reason>`, open chat, and click **[Confirm Builder's Hut]** within five minutes. All confirmation/placement checks still run. Other rotations and the manual confirmation command remain supported.

## Verification

Executed a clean build, then `scripts/gradle-cloud.sh build --rerun-tasks --no-build-cache --console=plain`: **BUILD SUCCESSFUL**, with **96 JUnit tests passed**, zero failures/errors/skips. The forced build re-executed compilation, JAR assembly and tests rather than relying on cached test results.

Executed `scripts/test-runtime.sh` in a fresh isolated world: **BUILD SUCCESSFUL; one required server lifecycle test passed**. It reads the actual chat `RUN_COMMAND` value and tests confirmation security, stale sites, cancellation/rollback, safe resume, replay denial, NPC identity and native saves. No additional physical-client mouse-click check is claimed for this packaging run.

[Build/runtime logs and JUnit results](validation-evidence.zip) · [SHA-256 checksums](SHA256SUMS.txt)

JAR SHA-256: `ba5b019cd62d9d0fa86400ef9c7195f64ce24c1ed1ef4e0bdc1bf5485dffdf8a`

The internal development mod version remains `0.1.0-dev`, exactly as defined by the requested commit. The filename includes `6b23ddb` to distinguish this build from the earlier JAR. Later contributions/Town Hall systems remain outside this development checkpoint.
