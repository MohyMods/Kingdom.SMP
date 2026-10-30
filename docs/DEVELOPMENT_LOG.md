# Development log

## 2026-10-30 — F2 furnished hut and Aldric checkpoint

Implemented the next approved dependency after F1: an original 348-block furnished timber/stone hut with four rotated definitions, safe op-only preview/exact-token confirmation, stale-site revalidation, NeoForge multi-place cancellation/rollback, persistent single-hut reservation, non-destructive resume, and private audit messages. Native SavedData stores the hut and stable builder association; one-way filesystem markers prevent a delayed world save from reopening installation or NPC issuance. Corrupt/missing/mismatched reservation metadata fails closed. This is a conservative refusal/recovery boundary, not a claim of lossless arbitrary host-failure recovery.

Aldric is a registered custom villager entity with no natural spawn/egg/summon, vanilla trading, breeding or item pickup. His configurable display name preserves UUID and home; a small day/night routine paths within his hut, pauses on blocked/unloaded/damaged context, and never teleports or force-loads. Persisted death and committed missing entities do not silently spawn replacements. Exact replacement/destructive repair policy remains unapproved.

Connected actual right-click interaction to a Royal Gilded dialogue/catalogue: rendered portrait, subtle slate courses, gold/bronze controls, parchment, normal Minecraft typography, scrolling and Back/Leave. The server validates registered identity, hut integrity, distance and line of sight for every topic. Bounded correlated replies update only the active conversation; no rewards or gameplay mutations are accepted through dialogue. Live in-game testing found and fixed the vanilla late blur pass affecting both dialogue and the existing overview.

Validation: full Gradle build passes **93 JUnit tests** with zero failures/skips. Isolated GameTest lifecycle runs pass against real blocks, commands, NPCs and native disk I/O. Normal dedicated server startup, save, reload of the same world, console status/save/stop, and physical-client integrated-world joins executed. The hut survived a full client/world restart and opened dialogue again. Exact runtime scope, initial failed attempts, and remaining multiplayer/host-loss/authentication limits are recorded in `VALIDATION.md`. Test code/resources are excluded from the production JAR.

F3 live contributions/leaderboard/unique reward is still next; no inventory consumption, Town Hall construction, Elara, border behavior, Charter/Master unlock, or later-tier costs were silently enabled. This checkpoint implements the hut/NPC dependency rather than representing pure accounting or future catalogue descriptions as those completed services.

## 2026-10-30 — Resume and first complete foundation build

Restored remote `rebuild/royal-gilded` at `89c1ed9` without resetting existing code. Reviewed all specifications and the environment handoff. Verified that the selected published environment is running with current enforced unrestricted HTTP access. Official metadata confirms the existing NeoForge 21.1.265 pin; Minecraft server/client/mappings and assets downloaded successfully. Installed a checksum-verified full Java 21 JDK under ignored `work/` because the image has only a runtime.

The real build exposed a Gradle configuration-cache closure violation and missing Minecraft dependencies for the test source set. Corrected metadata expansion and enabled ModDevGradle's supported JUnit integration. The complete Gradle build now succeeds, including the previously unexecuted NBT tests. No gameplay decisions changed. Dedicated-server lifecycle and physical-client rendering remain to be checked before they are claimed.

## 2026-10-30 — Planning baseline checkpoint

Source: S1 user brief. Stage: planning and architecture only.

Completed repository and available workspace audit. Only pre-existing tracked file was `Home` containing a newline; initial working tree was clean. No source documents, implementation, dependencies, builds, or tests were available.

Created the project overview, requirements register, kingdom/profession/economy/technology specifications, UI design draft, modular architecture proposal, dependency-aware roadmap, decision register, audit, and this log. Recorded confirmed exclusions without restoring removed features. Proposed first three screens as structured regions; no UI review approval is claimed.

Implementation: none. No gameplay systems, mod scaffold, Gradle configuration, dependencies, generated assets, or toolchain changes. Existing `Home` retained.

Validation: local Git/file inspection, documentation link and requirement-ID consistency checks, confirmed profession quest arithmetic (12 × 3 × 8 = 288), and `git diff --check`. No application build, automated gameplay tests, multiplayer behavior, UI rendering, performance, or integration compatibility has been validated. See the final checkpoint report for command outcomes.

Validation outcome: all relative Markdown links resolve across 12 documents; 46 requirement IDs are unique; original Home is unchanged; staged `git diff --cached --check` passes. These are documentation checks only.

Checkpoint: all 12 documentation files staged for Git review; not committed or pushed. Next action is user review of findings and priority decisions. Implementation remains blocked until explicitly authorized.


## 2026-10-30 — Fresh rebuild, S2 and S3

Inspected staged work and preserved it in commit `979c582` on the new `rebuild/royal-gilded` branch. No old implementation was available or continued. S2 authorizes code/build/UI/commits; S3 defines The First Hearth and the living town-building direction. Updated requirements/decisions/roadmap and added construction/NPC specifications. S3 supersedes the temporary unestablished starting-state proposal: initial tier is now 1, never 0.

Created actual Gradle/NeoForge registration, isolated client entry, immutable domain state, strict SavedData adapters, bounded read-only request/snapshot networking with request correlation, and reusable Royal Gilded widgets/Overview. Added Tier 1 capped accounting, exact normalized scores, tie ordering and immutable winner state; persisted contribution data is exposed by the snapshot. No live item consumption, advancement, entitlement/item grant, NPC, hut, placement or construction path is enabled. Those features remain staged in dependency order, not falsely completed.

Provisioned a full checksum-verified Temurin JDK because the original image had only a JRE. Verified Gradle distribution and replaced the MDK's older wrapper with the checksum-verified wrapper for Gradle 9.2.1. Added reproducible independent test and proxy/CA-aware Gradle launch scripts. The complete Minecraft-independent suite ran: 54 tests started and passed, none skipped or failed. Full Gradle build failed before source compilation on HTTP 403 fetching NeoForge's NeoForm runtime. Fixed a separate CA-trust setup issue; final build only reports the access denial. Minecraft-dependent NBT tests, game rendering, server startup, multiplayer, crash recovery and integration behavior remain unrun. See [validation](VALIDATION.md).

Source review corrected command translation arguments and late-response handling. Existing-save fallback guard avoids silently replacing corrupt/future state. Fixed quantities are the approved S3 initial balance; configurable later-tier definitions and safe live transaction adapters are not implemented. Major screen design precedes code; no game screenshots or texture-quality approval is claimed.

Saved required NeoForge/Minecraft network domains to the cloud draft; save confirmed, runtime application/publication not established. No push or merge. Current work will be checkpointed with its unresolved build blocker; M1/M2 are not marked complete. Next external action: apply reviewed network settings, then rerun full build/runtime validation before continuing the starter hut and contribution services.

Final review also corrected timeout feedback when a response arrives at the expiry boundary: the controller now resolves timeout/navigation state before completion consumes the request. Pure tracker tests cover the timing rules; Minecraft presentation behavior remains unrun. Documentation links, 64 unique requirement IDs, translation format arity, original Home preservation, shell syntax, and Git whitespace checks passed.

## 2026-10-30 — Published-environment retry and preservation handoff

User reported publishing the required domain changes. Rechecked the actual running task: live observations remain current for enforced spec revision 1 with no custom hosts. Official NeoForge metadata and the real proxy/CA-aware Gradle build still return HTTP 403 for NeoForge dependency downloads. The build fails before source compilation at `neoform-runtime:2.0.31`; no new Java compiler results exist. Did not bypass the proxy, weaken verification, re-save already requested settings, or discard source.

Read-only preservation audit confirmed a clean development branch, all source/planning committed, no nonignored untracked work and no world saves. Preserving a full-history bundle, tracked-source archive, and supplementary test/build evidence for transfer to a new task with the published environment. The bundle must be downloaded or the development branch pushed before disposing of this task; workspace-local files alone are not an external backup. See [handoff instructions](ENVIRONMENT_HANDOFF.md). No push or merge performed, and no additional gameplay implementation is claimed while dependencies remain inaccessible.
