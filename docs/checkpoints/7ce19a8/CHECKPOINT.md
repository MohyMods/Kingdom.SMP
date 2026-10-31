# Kingdom SMP — progressive resource framework checkpoint

Source `7ce19a818e98452daaf6fe0fd0665032478dd88b`, branch `rebuild/royal-gilded`. **0.4.0-dev · Minecraft 1.21.1 · NeoForge 21.1.265 · Java 21 · protocol 5**. No main merge.

[Download JAR](kingdom-smp-core-0.4.0-dev-7ce19a8.jar?raw=true) · [Checksums](SHA256SUMS.txt) · [Execution evidence](validation-evidence.zip)

JAR SHA-256: `1c0b48491cec719c7593615c9bfcfeb7e70902d0523a8295877572c09deca578`

This build preserves the tested Hut/Aldric, five-minute clickable hut confirmation, atomic contributions/history/leaderboard/winner and progressive Lodge/Mira loop. It adds a strict server-data milestone catalogue, actual item/tag registry checks, dependency/cycle/forward-gate validation, operator diagnostics and connected Royal Gilded progression inspection. It guards real First Hearth quote/confirm against invalid definitions and invalidates reviews on reload without changing saved scoring, inventory receipts or awards.

**Every later cost remains disabled.** The revised eight-resource Tier 2 targets are proposals; the Royal Storehouse/Quartermaster/shared-storage reward scope is specified but its live services are not implemented. Future scoring, road evidence, storage access/capacity and generalized per-tier transaction/reward adapters still require work. The configurable catalogue is not twelve complete playable progression engines. Town Hall/Elara remain Tier 12.

## Review and test

- [Complete proposed resource table, acquisition rationale, effort and deadlocks](../../PROGRESSIVE_RESOURCE_BALANCE.md).
- [Server datapack format and activation limits](../../MILESTONE_DATA_FORMAT.md).
- [Completion tracker](../../MASTER_COMPLETION_TRACKER.md) and [exact continuation](../../CODEX_CONTINUATION.md).

Back up the complete world. Replace the previous mod JAR, keeping one copy, on every client and server. Matching protocol 5 is required. Press **K → Progression → a tier**. Inspect proposed targets and scroll/Page Up/Down for objectives/validation. Inspecting never consumes resources or awards a tier/blueprint. `/kingdom milestones inspect <tier>` is read-only. Permission-level-2 operators may run `/kingdom milestones validate`. A server datapack can override `data/kingdom_smp/kingdom/milestones.json`; vanilla `/reload` revalidates it. Copy the bundled schema and follow the data-format document. Never delete receipt/journal markers or independently restore player files to repair data.

Only approved First Hearth donations are live. Its historical contract is pinned. Changing costs, scoring or reward identity blocks new consumption rather than reinterpreting existing results. Later `approval` labels do not authorize consumption. Valid mod registry identity alone does not verify recipes or approved technology/profession gates. Unknown acquisition paths remain blockers. AFK item provenance and automatic arbitrary-recipe discovery are not implemented.

## Executed checks

Full Gradle build: **139 JUnit tests, zero failures/errors/skips; BUILD SUCCESSFUL**. Expanded native lifecycle: **all 1 required GameTests passed; BUILD SUCCESSFUL**, covering actual registry/tag matching, invalidated pre-reload reviews without consumption, preserved history/winner, completed-Lodge objective, public/admin command separation and all prior donation/construction/NPC security checks. The final subsequent UI-label/layout compaction passed the full build and actual client inspection.

A real client joined an isolated normal dedicated-world copy at Tier 1/revision 2. An intentionally duplicate JSON field rejected the catalogue on actual `/reload`; donation review refused without changing 164 logs or revision 2. Restoring valid data and reloading allowed a fresh confirmation of exactly 64 logs, leaving 100, advancing revision to 3 and preserving existing iron credit. Console inventory output verifies quantities. The server saved/stopped successfully. A normal server/client restart retained revision 3 and the remaining inventory, rendered the final eight-target panel, and saved/stopped successfully again. Normal and narrow screenshots were inspected. This slice used one actual offline localhost client; no new authenticated remote/two-client reload race, mod recipe compatibility or season-scale performance result is claimed. Nonfatal Authlib service refusals/no audio device remain host limitations.

## Actual Minecraft captures

- [All eight revised targets, disabled](tier-two-proposal.png).
- [Proposal objective/reward details](proposal-objectives.png).
- [Smaller client window](progression-narrow.png).
- [Invalid data safely refuses donation](invalid-data-refusal.png).
- [Valid reload and exact accepted donation](restored-safe-donation.png).

These are real Minecraft frames, not mockups. The isolated fixture has not completed its Lodge, so its building objective correctly says not completed; the native full-Lodge fixture tests the completed state.

Continuation prompt: Continue Kingdom SMP from 7ce19a8 or its newer artifact commit. Read CODEX_CONTINUATION, D-020, the proposed balance table and master tracker. Preserve all existing receipt/history/award contracts. Obtain approved Tier 2 costs/future score weights and road/storage service rules before enabling them; implement the generalized pinned per-tier transaction adapter next. Keep Town Hall at Tier 12, test native recovery/multiplayer/UI, push tested development checkpoints and deliver JARs. Never merge main.
