# Development log

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
