# Kingdom SMP delivery roadmap — current authority

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
| Tier 12 final building/reward | BLOCKED — REQUIRES DECISION | Former Hall role superseded; retain cube eligibility and sequential progression; no replacement invented |
| Representative profession | Definitions missing | Approved 24 quests/XP/ranks, meaningful 2–3 week Master target, three-active cap, preserved inactive progress; validate one before eleven more |
| Bank/economy/markets | Gates and numeric policies missing | Earlier premises proposed independently of final Town Hall; prove coin/balance recovery, budgets, tax/rounding, contracts and escrow before activation |
| Technology/magic integrations | Exact artifacts/policies missing | Verify each supported mod/version and alternative acquisition routes with that mod installed |
| Royal Administration | Hut and construction site approval/recovery implemented | Permissioned confirmed/audited operations alongside their actual services; no decorative dashboard claiming absent controls |

## Current dependency order

**Design review first; no implementation yet.** Review [the phased plan](BLUEPRINT_IMPLEMENTATION_PLAN.md), initial Hut/Aldric bootstrap, final building roadmap/controls and separate save-migration policy. Existing 0.4.0-dev remains the tested runtime, including the Lodge and former reward binding. After explicit approval, follow compatibility/identity → one complete held-plan hologram → secure Royal Gilded approval/construction → reviewed two-floor Tier 2 Hall → other approved buildings → measured large-building/migration rollout. Later resource/scoring, professions/economy/integration policies remain separate approvals. Do not silently rebind the existing Lodge/road objectives.

Each future implementation checkpoint still requires builds, relevant tests, persistence/multiplayer evidence, real screenshots, installable JAR and a development push. This documentation checkpoint changes no executable resources and claims no new runtime verification.
