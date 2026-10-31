# Kingdom SMP delivery roadmap — current authority

> **D-023 — current implementation authority:** The [master directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md) authorizes incremental coding and supersedes the D-021/D-022 review-only hold. New-world Town Hall / existing-world Lodge profiles, owner Hut bootstrap, blueprint-market acquisition, automatic server-valid placement, supports up to four blocks, Tier 3 Bank (no new-world Storehouse), updated profession/economy/quest/admin scope apply. End remains Tier 12. The [full replacement mod snapshot](sources/MOD_SNAPSHOT_2026-10-31.txt) now reports FTB Quests installed. See [supersession and actual status](MASTER_SUPERSESSION_MATRIX.md); old descriptions below are historical where they conflict.

> **D-022 — planning only, current dimension authority:** End unlocks at **Tier 12**, superseding every End-at-9 design. Target schedule: Aether 4; Nether 5; Twilight Forest 6; Otherside 8; reviewed Ad Astra stages 10–11; End 12. No mandatory prerequisite for reaching 12 may depend on End resources, including indirect recipes. Dragon/End exploration belongs after the unlock; **Royal Legacy Monument** remains the proposed final construction reward. Existing code/data/worlds are unchanged. See [dimension schedule, dependency graph and open recipe audit](DIMENSION_PROGRESSION.md). Implementation remains on hold.

> **D-021 — 31 October 2026, current design authority:** Physical blueprint items and full-building crosshair-controlled holograms are approved requirements. Future Tier 1 completion awards the **19 × 15, two-floor Town Hall at Tier 2**, replacing the future Lodge reward subject to separately approved migration. The former Tier 12 Hall requirement is superseded. Existing 0.4.0-dev code, Lodge/Mira, entitlements and saves remain unchanged. **Do not implement yet.** See [construction design](CONSTRUCTION.md) and [approval/migration plan](BLUEPRINT_IMPLEMENTATION_PLAN.md). Service gates and the rest of the building roadmap are not silently changed.

Existing verified runtime milestones below are historical implementation facts. New building-system work is paused for design/roadmap/migration approval.


| Milestone | Current state | Completion gate / next action |
| --- | --- | --- |
| Foundation, Java 21 / NeoForge 21.1.265 | Built/tested; user approved | Preserve dedicated-server isolation, strict persistence and reproducible builds |
| Builder’s Hut and Aldric | Working; user tested and approved | Preserve original furnished structure, identity, dialogue, routines and five-minute clickable admin confirmation |
| First Hearth durable inventory transactions | Implemented and tested | Atomic prepared ledger → verified native inventory receipt → committed credit; native saving-failure recovery and repeated restart tested; see validation limits |
| Contributions, exact scores, history and live leaderboard | Implemented and tested | Royal Gilded review/confirm flow, resource caps, stale/replay/race cases and actual client screenshots; two actual localhost clients verified shared iron cap, live invalidation and synchronized ranking; remote/authenticated sessions remain unverified |
| Tier 1 → Tier 2 | Implemented; native-server test passed | Derive tier, completion and permanent winner from canonical accepted history; no unrelated service grants |
| Founders’ Lodge reward delivery/redemption | Implemented and tested | Durable winner-bound token rotation, full-inventory check, exact admin approval, one redemption; old/copy plans cannot redeem |
| Reusable progressive construction | Implemented and tested for Lodge | Real rotated footprint preview, exact-site review, protected reservation, physical Aldric work, saved resume/unloaded pause; native and client evidence |
| Furnished Founders’ Lodge and Reeve | Implemented and tested | Original 782-block furnished Lodge, Mira Ashford, persistent identity/routine, actual milestone/overview services; no bank/trading/Charter grant |
| Server-data milestone catalogue and dependency graph | Implemented; automated/native tests pass | Registered IDs/tags, strict schema, cycles/forward gates, disabled proposals, actual Lodge objective, pinned historical First Hearth, safe reload guards; physical checks in progress |
| Tier 2 → 3 / Royal Storehouse | Revised user quantities remain proposals; reward scope specified | Eight diverse resources + Lodge/road; review final costs/scoring/evidence; secure Quartermaster storage access/capacity before activation |
| Tiers 3–12 resource proposals | Drafted for review, disabled | Availability rationale and ten-player effort estimates in PROGRESSIVE_RESOURCE_BALANCE; verify actual technology/profession/recipe dependencies before enabling |
| Generalized live later-tier contributions | IN DEVELOPMENT / blocked on approved policy | Per-milestone pinned history/transaction adapter, objective approvals, future score weights and winner entitlements; current generic catalogue does not activate these |
| D-021 physical blueprints / full holograms | Approved requirements; implementation plan pending | Review building roadmap, Hut bootstrap, controls and migration before coding; all 16 named building types covered |
| Future Tier 2 Town Hall | Approved 19 × 15 / two floors; not implemented | Tier 1 winning entitlement; full Hall design/services and approved migration; no automatic Lodge replacement |
| Tier 12 End/finale and Royal Legacy Monument | BLOCKED — REQUIRES DECISION | End-12 design confirmed; future enforcement/mod recipe audit/finale policy pending; Monument remains proposed |
| Representative profession | Definitions missing | Approved 24 quests/XP/ranks, meaningful 2–3 week Master target, three-active cap, preserved inactive progress; validate one before eleven more |
| Bank/economy/markets | Gates and numeric policies missing | Earlier premises proposed independently of final Town Hall; prove coin/balance recovery, budgets, tax/rounding, contracts and escrow before activation |
| Technology/magic integrations | Exact artifacts/policies missing | Verify each supported mod/version and alternative acquisition routes with that mod installed |
| Royal Administration | Hut and construction site approval/recovery implemented | Permissioned confirmed/audited operations alongside their actual services; no decorative dashboard claiming absent controls |

## Current dependency order

**Design review first; no implementation yet.** Review [the phased plan](BLUEPRINT_IMPLEMENTATION_PLAN.md), initial Hut/Aldric bootstrap, final building roadmap/controls and separate save-migration policy. Existing 0.4.0-dev remains the tested runtime, including the Lodge and former reward binding. After explicit approval, follow compatibility/identity → one complete held-plan hologram → secure Royal Gilded approval/construction → reviewed two-floor Tier 2 Hall → other approved buildings → measured large-building/migration rollout. Later resource/scoring, professions/economy/integration policies remain separate approvals. Do not silently rebind the existing Lodge/road objectives.

Each future implementation checkpoint still requires builds, relevant tests, persistence/multiplayer evidence, real screenshots, installable JAR and a development push. This documentation checkpoint changes no executable resources and claims no new runtime verification.

## D-022 review hold

DIMENSION_PROGRESSION.md records the confirmed schedule, revised dependency graph, End-independent pre-12 path, Ad Astra/mod recipe and alternate-teleport audit blockers, post-unlock Dragon/End finale and proposed Royal Legacy Monument. The current binary remains End-9; new dimensions/End-12 enforcement are NOT STARTED. Actual mod recipes/compatibility are UNVERIFIED. No data migration, runtime tests or new resource activation in this documentation checkpoint. Review these dependencies and existing-player/finale credit policies before coding.

## D-023 execution sequence — current

Phase A audit/supersession and full replacement inventory are recorded in MASTER_SUPERSESSION_MATRIX and MOD_INTEGRATION_MATRIX.csv. Phase B has begun with a connected held-Lodge full model preview and read-only server probes; actual client full-model/rotation/unsupported-height/Royal Gilded review passed. Next: actual both-claim-system adapters, durable new/legacy world profiles, owner Hut item/bootstrap, new-world two-floor Hall, four-block foundations and automatic valid-site confirmation. Then early payment/Architect market → Bank/mailbox foundations → representative profession and Tier 3 services → aircraft/Waystones/dimension integrations → real FTB Quests/title chapters → full admin and end-to-end validation. Admin testing tools expand with each real feature. Current partial implementation is not the full Phase B or release.
