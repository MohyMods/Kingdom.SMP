# Environment handoff — published policy not attached to this task

> **Resolved in the resumed published environment.** The active instance now reports current enforced unrestricted access; NeoForge/Minecraft downloads and the complete Gradle build succeed. Java initially contained only a runtime, so a checksum-verified full JDK 21 was installed under ignored `work/toolchains/`. Use an available full JDK 21 via `JAVA_HOME`; the historical `/workspace/toolchains/` path below is not present on this instance. See `VALIDATION.md` and `DEVELOPMENT_LOG.md` for current evidence. The remainder records the previous environment's blocker and preservation history.

The user confirmed reviewing and publishing the required dependency domains, then asked to continue the existing rebuild without resetting it. The resumed task still reports the old live policy: current observations, enforced restricted access, spec revision `1`, and an empty custom-host list. The local startup policy also lacks NeoForge/Minecraft domains. This does not disprove publication; it establishes that this running task has not acquired the new access.

## Actual retry results

- Official NeoForge Maven metadata request: HTTP 403.
- Executed `JAVA_HOME=/workspace/toolchains/jdk-21.0.13+9 scripts/gradle-cloud.sh build --console=plain`.
- Gradle 9.2.1 runs with the full Java 21 JDK, configured proxy, and system CA trust.
- Build fails resolving `net.neoforged:neoform-runtime:2.0.31` because its POM at `maven.neoforged.net` receives HTTP 403.
- No source compilation or Minecraft-dependent tests execute. No new Java compiler error has been observed. Not all dependency destinations can be verified until NeoForge resolution succeeds.
- No source changes, branch reset, main merge, or push were performed during this retry. Only these handoff/validation records were added.

## Preserve before changing tasks

All code and design documents are committed on `rebuild/royal-gilded`. The working tree was clean before the handoff update; no additional nonignored files or game worlds were found. Ignored caches/build reports can be regenerated. Validation logs and JUnit evidence are exported separately.

A full-history Git bundle and tracked-source archive are prepared outside the checkout at `/workspace/scratch/kingdom-smp-rebuild-rescue/`. **Download the bundle before ending this task** unless the development branch is pushed to GitHub. Files merely present in this task's workspace are not proof of durable transfer to another task. The source archive is an additional recovery copy; the bundle preserves commits and branch history.

Alternatively, the user can preserve the branch on the remote before starting a new task:

```sh
git push -u origin rebuild/royal-gilded
```

This pushes only the development branch, not main. The agent has not run this command or assumed push permission from successful Git read access.

## Resume in a new task

Choose the newly published environment and the existing repository. Ask the task to continue `rebuild/royal-gilded`, preserve all changes, verify the live allowlist, and rerun the actual Gradle build. Do not create another fresh codebase.

If the branch was pushed, fetch and check out that branch using normal Git commands, first inspecting any existing local changes. If using the downloaded bundle, upload it to the new task. Inspect local branch state before importing so a stale branch is not overwritten. To import the saved branch under a separate recovery reference in an existing checkout:

```sh
git bundle verify /path/to/kingdom-smp-rebuild.bundle
git fetch /path/to/kingdom-smp-rebuild.bundle \
  refs/heads/rebuild/royal-gilded:refs/heads/recovery/royal-gilded
```

Compare the recovery branch with any existing `rebuild/royal-gilded` before choosing the newer state. If no local development branch exists and the working tree is clean, create it from the recovery reference. Never reset an existing branch merely to match an older snapshot.

The exact exported commit and SHA-256 checksums are in the rescue directory's manifest/checksum files. Restore verification compares the bundle checkout with the original tracked tree.

## Development order after access works

Use the current roadmap and validation report rather than the historical blocker below. The full build now works, the hut/Aldric are user approved, and live Tier 1 contributions and advancement to Tier 2 are implemented. Continue with the approved Founders’ Lodge entitlement/construction/Reeve milestone after resolving public site policy. The user's later decision reserves the ultimate Town Hall and Elara for Tier 12; earlier descriptions do not authorize an early Hall. Do not represent reward delivery or progressive construction as complete.
