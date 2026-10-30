# Dependency-aware rebuild roadmap

Current authority: S2 authorizes the complete fresh rebuild, ordinary development, builds/tests, and commits on a dedicated branch. Do not push main or merge without approval. Preserve the S1 gameplay baseline and exclusions. Design and review major screen layouts/flows before implementing them; do not invent missing material gameplay rules.

S2 established foundation/UI/progression/profession milestones. S3 then supplies the first authoritative progression values and supersedes immediate sequencing with the phases below. The later feature roadmap remains useful dependency context; its labels do not imply completion or priority ahead of the S3 slice. No dates are promised without approved scope and measured capacity.

## Current S3 delivery sequence

| Phase | Depends on | Required working result | Verification gate |
| --- | --- | --- | --- |
| F1 — Foundation and First Hearth | Verified toolchain and S3 | Real mod registration, side isolation, versioned persistent state starting automatically at Tier 1; Royal Gilded server-backed Overview foundation | Executed build/tests, dedicated-server lifecycle and persistence; client/network/UI validation where supported |
| F2 — Builder’s Hut | F1; designed structure/dialogue | Admin setup of original furnished timber-and-stone hut; stable configurable Aldric identity, home, dialogue, catalogue and safe routine | Setup persistence, no duplicate NPC, dialogue access, structure/interior and pathing checks |
| F3 — Contributions and winner reward | F1–F2 | Fixed four-category First Hearth resource contributions, persisted personal/shared totals, live ranking, unique noncraftable Town Hall blueprint entitlement and permanent winner | Partial/excess/race/restart tests; exact normalized score/tie rules; lost-unused reward and duplicate-copy recovery tests |
| F4 — Blueprint construction | F2–F3; land/protection/collision policy | Registry, rotation/footprint preview, authoritative validation/confirmation, bounded progressive construction and restart resume | Invalid/stale sites, entitlement replay, concurrent work, chunk-unload pause, path failure, no permanent chunk loads, interruption recovery |
| F5 — Completed Town Hall | F4; actual complete structure design and damage/recovery policy | Furnished hall, all required-block verification, completed registry/announcement, Elara arrival, Overview registration, catalogue update and permitted service points only | Structural verification, one completion/one steward, saved identities, damage/tamper/admin recovery; no Charter/Master auto-unlock |
| F6 — Approved expansion | Proven F1–F5 and exact later gameplay specs | Professions, banking/economy, markets, contracts and integrations in dependency order | Relevant milestone gates below; extensible NPC reputation/events are not automatically authorized |

Royal Gilded screens are developed alongside each phase: Overview/contribution/ranking/reward/building status; hut catalogue/preview/confirmation; NPC dialogue; and rotated site validity/confirmation. A screen or counter that only describes future functionality is not a completed service.

## Later feature dependency register

| Milestone | Depends on | Working deliverable | Exit evidence |
| --- | --- | --- | --- |
| M0 — Baseline and branch | Existing staged planning and S1/S2 | Preserve inspected uncommitted work, new development branch, current scope/decision records | Cleanly identified changes; no deletion or continuation of unavailable legacy code |
| M1 — Foundation | Verified official Java 21 / NeoForge 1.21.1 / Gradle toolchain | Actual mod registration, client/server isolation, domain architecture, basic versioned persistent kingdom state, reproducible build | Executed compile/tests, dedicated-server boot/shutdown and persistence checks; client initialization/join when supported; exact versions/evidence recorded |
| M2 — Royal Gilded UI vertical slice | M1; layout/flow design | Shared buttons/panels/tabs/progress/tooltips, navigation/scaling, Kingdom Overview backed by authoritative server state | Real networking/request validation, screen behavior, scaling/state tests, client/screenshots when supported; clearly record unavailable visual or multiplayer checks |
| M3 — Functional kingdom progression | M1–M2; actual milestone resource tables and advancement rules | Configurable twelve-tier framework, authoritative contributions/validation/persistence, Overview integration; gates/border as their policies are approved | Rule tests, duplicate/race/restart and contribution interruption tests, real travel/gate coverage; border safety checks; no fabricated milestone costs |
| M4 — First complete profession | M1–M3 as applicable; approved profession/quest/XP rules; profession UI design | Profession/rank/quest/XP framework plus one complete approved profession and working interface | Attribution, rank progression, inactive progress, reward replay, active cap and restart tests. Paid switching cannot charge until money is proven |
| M5 — Durable money and bank | Basic state plus reviewed cross-store recovery protocol, currency/funding/rounding rules; bank UI design | Integer balances, coins, counter-local deposit/withdrawal, paid switch integration | Fault injection and recovery at every transfer stage, full inventory/access/race/replay tests; no duplication/loss claims beyond tested crash model |
| M6 — Master/NPC commerce and contracts | M4–M5; catalogues/pricing/budgets/contract definitions; shop UI designs | Master entitlement/repeat pricing, bounded NPC buying, approved dynamic-price and contract lifecycle one at a time | Exact pricing/rounding, daily resets, budget races, gifting/use rules, contract attribution and duplicate claims tested |
| M7a — Fixed-price market | M5; approved listing/matching/escrow/tax/delivery rules; market UI design | Escrow, authoritative sales/tax, persistent delivery | Final-item race, full inventory, replay/disconnect/restart, cancellation/refund tests |
| M7b — Timed auctions | M7a; approved bid/deadline/offline/refund policy | Bidding state machine and settlement | Concurrent bids, funding, deadline boundaries and downtime settlement tests |
| M7c — Buy orders | M7a; approved matching/priority/partial-fill policy | Funded orders and validated fulfillment | Item-component matching, partial fulfillment, cancellation races and recovery tests |
| M8 — Technology/mod integration gates | Progression/profession entitlement; verified exact mod artifacts; approved offline/ownership rules | One supported integration and gate family at a time | Route-by-route bypass tests with pinned mods, machine actor handling, observed automation/offline behavior |
| M9 — Approved content expansion | Proven slices and complete source tables | All twelve tiers, twelve professions and 288 specified quests; catalogues/integrations | Definition integrity, representative objective-type tests, content/gate review, measured balance playtests; counts alone insufficient |
| M10 — Season release readiness | All required slices and deployment decisions | Complete required admin/profile UI, balance/load/security/accessibility hardening, release/runbooks | Ten-player representative soak, agreed performance targets, backup/restore drill, compatibility matrix, known limitations and release checklist |

Permission checks, sensitive-operation confirmation/audit, security, configuration, persistence, tests and documentation belong to every relevant milestone. Admin quest approval is implemented alongside affected quest flows, not deferred until the admin dashboard is decorative. Design no placeholder panel as a complete feature.

## Current target

The immediate target is F1, then the F2 Builder’s Hut and F3 fixed-cost contribution/reward slice, with F4 construction and F5 completed Town Hall following their safety dependencies. S3 supplies First Hearth requirements and initial tier; do not continue treating them as missing. Later season milestone costs and profession quests remain absent. Missing future tables do not block foundation/UI/hut work.

The authoritative record of actual commands, test counts, runtime results, commits and limitations is [development log](DEVELOPMENT_LOG.md). This roadmap is not evidence that a milestone passed. A graphical client or multi-client check that cannot run here stays explicitly unverified.

## Per-module completion standard

Review approved specs → explain dependencies → design/review applicable UI → enumerate exploit/crash cases → implement a manageable module → compile and run meaningful tests → inspect runtime and fix failures → review correctness/security → update docs/log → create a logical Git checkpoint. Keep passed, failed, skipped and unrun outcomes distinct. Compilation alone never completes a feature.

## Critical source dependencies

The original design documents remain absent. Required before the affected feature: later milestone/advancement/border policy, land/protection/collision and building/NPC damage/recovery policy, first profession's 24 quest definitions and XP/rank rules, switch-charge semantics, coin transfer and tax rules, Master catalogues, NPC budgets/dynamic-price rules/contracts, auction settlement/matching, machine ownership/offline rules, and exact compatible third-party mod artifacts. See [decision register](DECISIONS.md).
