# Dependency-aware development roadmap

All implementation milestones require explicit user authorization first. UI implementation additionally waits for the relevant layout/interaction review. This is a prioritized sequence, not a commitment to invent missing gameplay content. No dates are promised without approved scope and capacity.

| Milestone | Depends on | Manageable deliverable | Exit evidence |
| --- | --- | --- | --- |
| M0 — Baseline and review | Available repository and S1 | Audit, requirement register, missing-source list, architecture proposal, first three UI drafts | Documents reviewed; missing/conflicting rules tracked; implementation authorization still required |
| M1 — Verified development foundation | Approved platform/identity choices and explicit authorization | Minimal mod shell, pinned official NeoForge/Gradle toolchain, Java 21 checks, isolated client/server entry boundaries, repeatable build/test instructions | Real Gradle build, clean dedicated-server boot/shutdown, matching client join, one meaningful lifecycle smoke check; exact versions/evidence recorded |
| M2 — Durable state foundation | M1; approved storage, crash model, identifiers | Player/kingdom persistence and schema handling with recovery contract; first versioned domain state | Restart, migration, failed-write and recovery tests; no live money enabled |
| M3 — Kingdom progression vertical slice | M2; approved first-tier milestones/advancement/travel policy; Kingdom UI review | One reviewed tier transition, read-only overview, authoritative milestone flow and travel-gate adapter proof | Two-client final-contribution race, duplicate credit, restart persistence, real travel denial/allow tests |
| M4 — Profession vertical slice | M2 and relevant M3 gates; approved quest/activation rules; Profession UI review | One approved profession through rank transitions with representative quest types; active-set handling | Attribution, inactive progress, reward replay, three-profession cap, disconnect/restart tests. Paid switching waits for M5 |
| M5 — Money and counter banking | M2; reviewed cross-store transfer protocol, coin/tax/rounding/funding rules; Bank UI review | Integer money, real coins, local counter conversion, paid profession switching integration | Fault injection at transfer stages, quota/capacity and counter races, save/restart tests; no duplicate or lost value under approved crash model |
| M6 — Master commerce and NPC controls | M4–M5; approved catalogues/prices/budgets; shop UI reviews | One Master item purchase path, repeat pricing, NPC budget/quota enforcement | Counter/price/payment atomicity, repeat requests, daily reset races, gifting/use policy tests |
| M7a — Fixed-price market | M5; approved listing/item-matching/escrow rules; market UI review | Escrow, fixed-price sale, authoritative taxes, interrupted delivery recovery | Final-item race, full inventory, reconnect/restart, cancellation/refund tests |
| M7b — Timed auctions | M7a; approved timing/bid/refund/offline policies | Bid state machine and settlement | Concurrent bids, outbid funds, deadline boundaries, downtime settlement tests |
| M7c — Buy orders | M7a; approved matching/priority/partial-fill policies | Funded buy orders and fulfillment | Matching correctness, component validation, cancellation/fulfillment races |
| M8 — Integration gates | M3–M6 as relevant; compatible mod matrix and approved offline policy | One supported integration and gate family at a time | Route-by-route bypass tests with pinned mods; observed offline/automation behavior |
| M9 — Content expansion | Tested slices; approved full tier/quest/catalogue tables | All twelve tiers and twelve professions/288 defined quests; expand integrations | Definition integrity checks, representative quest-type tests, gate coverage and content review; counts alone insufficient |
| M10 — Season readiness | M3–M9 plus deployment choices | Balance tuning, broader admin UI, load/security/accessibility/recovery hardening, release/runbooks | Ten-player representative soak, agreed performance budgets, restore drill, adversarial transaction/gate checks, release checklist and known limitations |

Permission checks, audit of sensitive operations, security, tests, configuration, and recovery are part of every relevant milestone; they are not postponed to M10. Design safe developer/admin controls before any live mutation, even if the full admin panel arrives later.

## Recommended first implementation milestone

**M1: verified development foundation**, only after authorization. This is deliberately narrower than professions, banking, or a full modpack. It establishes that the exact Java/Gradle/NeoForge pair can build and run on a dedicated server and matching client before APIs or integrations become architectural dependencies. M1 does not require inventing gameplay values or buying a large dependency stack. If this environment cannot run a graphical client, record client validation as external/pending rather than claiming it passed.

Next gameplay milestone after durable state: the M3 kingdom slice. Start with actual approved source objectives; do not invent the first tier to keep the schedule moving.

## Independent planning work

Once original documents arrive, reconcile them against stable requirement IDs; finish tier/quest/catalogue tables; review the first three screens; build the dependency verification checklist; define transaction and integration threat models. These planning tasks can precede implementation but do not authorize running a mod build.

## Per-module completion standard

Review relevant approved specs → explain dependencies → review applicable UI → enumerate exploit/crash cases → implement one module → compile and run meaningful tests → review behavior/security → update requirements/decisions/log → prepare a named Git checkpoint. Report passed, failed, skipped, and unrun checks separately. No module is done solely because it compiles.

## Current progress

- Completed: repository audit, captured baseline and exclusions, modular architecture and risk proposal, milestone roadmap, structured drafts for the first three screens.
- Awaiting review: proposals and interface drafts; no approval inferred.
- Blockers for gameplay planning completion: missing original design tables, ambiguous action semantics, unverified dependency matrix.
- Blocker for any implementation: explicit user authorization not yet given.
- Remaining: M1–M10; no implementation module is complete.
