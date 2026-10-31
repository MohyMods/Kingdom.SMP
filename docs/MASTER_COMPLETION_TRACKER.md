# Kingdom SMP — master completion tracker

Authority: latest user completion directive (31 October 2026, Asia/Riyadh), the requirements register and subsequent decisions. Preserve current source/persistence; Town Hall is Tier 12 only. Latest published verified source: `33b62fe` (0.3.0-dev Lodge/weather/realm gates, final client subscription/scaling and weather handover corrections). Historical 0.2.0-dev package: `5baf285`. This tracker records observed implementation, not promises. No complete-project claim.

Allowed statuses: **NOT STARTED**, **IN DEVELOPMENT**, **IMPLEMENTED — UNTESTED**, **TESTED**, **BLOCKED — REQUIRES DECISION**, **COMPLETE**. TESTED records the documented executed checks; COMPLETE additionally requires all acceptance criteria for that feature. Missing approved content stays blocked even if a framework could compile.

## Platform and current gameplay

| System | Status | Dependencies / acceptance |
| --- | --- | --- |
| NeoForge 21.1.265 / MC 1.21.1 / Java 21 build | TESTED | Full Gradle, dedicated-server isolation, installable JAR, matching client |
| Builder’s Hut and Aldric | TESTED | Furnishings/rotations; protected loaded site; identity/routine; death/refusal/restart; real client (user approved) |
| Five-minute clickable hut confirmation | TESTED | Exact token/expiry, permission/player/distance/stale checks; native chat click action |
| Shared Tier 1 inventory consumption/recovery | TESTED | Prepared history/native inventory receipt; crash boundaries, caps, stale/replay/race, consistent backup |
| Exact scores, leaderboard and history | TESTED | Equal normalized weights; deterministic ties; bounded pages; two real clients and restart |
| Tier 1 → 2 and permanent winner | TESTED | All four caps; exactly one winner; announcement and persisted result |
| Founders’ Lodge unique blueprint claim/recovery | TESTED | Winner-bound generation; durable revoke before recovery; full inventory; cloned/old/other-player denial |
| Rotated blueprint preview and exact-site proposal | TESTED | Server footprint/loaded supported clear volume; client advisory visualization; no property destruction |
| Administrator site review/confirmation | TESTED | Exact current footprint/proposal; permission and proximity; revalidate; audit; no terrain replacement |
| Progressive construction and scheduling | TESTED | One reserved project; bounded work; physical Aldric activity; pause unloaded/blocked paths; persisted resume |
| Construction protection and interruption recovery | TESTED | Stale/edited site, break/explosion/piston/protection cancellation, no item or material duplication |
| Furnished Founders’ Lodge | TESTED | Original structure, working entrance/storage/meeting space/milestone station, required-block verification |
| Reeve identity/dialogue/routines/services | TESTED | Named persistent resident; milestone/kingdom screens; no unapproved service grants; restart/damage checks |
| Generic building/NPC registry and safe recovery | IN DEVELOPMENT | Shared typed identity/assignments, verification, bounded routines, audited conservative recovery |
| Tier 12 Town Hall and Elara | NOT STARTED | Tier 12 award policy and progression first; furnished functional final hall; one steward |

## All twelve tiers

| Tier | Approved identity/gate | Status | Acceptance / dependency |
| --- | --- | --- | --- |
| 1 | The First Hearth | TESTED | 1,024 logs / 2,048 cobblestone / 512 wheat / 128 iron; consumption/history/winner; advance 2 |
| 2 | Roads of Commerce; Founders’ Lodge privilege | IN DEVELOPMENT | Complete Lodge loop; next milestone quantities/reward still require approval |
| 3 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 4 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 5 | Nether gate | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 6 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 7 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 8 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 9 | End gate | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 10 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 11 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |
| 12 | Ultimate Town Hall; Creative Energy Cube + active Master Mechanic | BLOCKED — REQUIRES DECISION | Approved cost/reward/unlock table; sequential transition, recognition and native persistence tests |

World border expansion: **BLOCKED — REQUIRES DECISION** (centers, sizes, dimensions, timing, safety). Charter after Tier 2: **BLOCKED — REQUIRES DECISION** (exact boundary/content); it must not depend on the Tier 12 Hall. Vanilla Nether/End entry enforcement: **TESTED** for approved Tier 5/9 boundaries and native player changeDimension, direct cross-realm teleportTo and item transfers. Unknown mod dimensions and characters already inside a locked realm on import are not rewritten or claimed covered. Third-party routes remain dependent on verified integration APIs.

## All professions and all 288 planned quest slots

Each profession has 8 Apprentice, 8 Journeyman and 8 Master quests. The IDs below identify planned slots only; none is an invented or approved quest definition. All **288** are **BLOCKED — REQUIRES DECISION** until objectives, attribution, XP, prerequisite/reward and approval rules are approved. Each must then pass event/abuse/one-claim/restart tests. Target 2–3 weeks to Master requires playtesting, not timer gates or filler.

| Profession | Apprentice slots (8) | Journeyman slots (8) | Master slots (8) | Status |
| --- | --- | --- | --- | --- |
| Farmer | farmer/a01 farmer/a02 farmer/a03 farmer/a04 farmer/a05 farmer/a06 farmer/a07 farmer/a08 | farmer/j01 farmer/j02 farmer/j03 farmer/j04 farmer/j05 farmer/j06 farmer/j07 farmer/j08 | farmer/m01 farmer/m02 farmer/m03 farmer/m04 farmer/m05 farmer/m06 farmer/m07 farmer/m08 | BLOCKED — REQUIRES DECISION |
| Blacksmith | blacksmith/a01 blacksmith/a02 blacksmith/a03 blacksmith/a04 blacksmith/a05 blacksmith/a06 blacksmith/a07 blacksmith/a08 | blacksmith/j01 blacksmith/j02 blacksmith/j03 blacksmith/j04 blacksmith/j05 blacksmith/j06 blacksmith/j07 blacksmith/j08 | blacksmith/m01 blacksmith/m02 blacksmith/m03 blacksmith/m04 blacksmith/m05 blacksmith/m06 blacksmith/m07 blacksmith/m08 | BLOCKED — REQUIRES DECISION |
| Miner | miner/a01 miner/a02 miner/a03 miner/a04 miner/a05 miner/a06 miner/a07 miner/a08 | miner/j01 miner/j02 miner/j03 miner/j04 miner/j05 miner/j06 miner/j07 miner/j08 | miner/m01 miner/m02 miner/m03 miner/m04 miner/m05 miner/m06 miner/m07 miner/m08 | BLOCKED — REQUIRES DECISION |
| Builder | builder/a01 builder/a02 builder/a03 builder/a04 builder/a05 builder/a06 builder/a07 builder/a08 | builder/j01 builder/j02 builder/j03 builder/j04 builder/j05 builder/j06 builder/j07 builder/j08 | builder/m01 builder/m02 builder/m03 builder/m04 builder/m05 builder/m06 builder/m07 builder/m08 | BLOCKED — REQUIRES DECISION |
| Lumberjack | lumberjack/a01 lumberjack/a02 lumberjack/a03 lumberjack/a04 lumberjack/a05 lumberjack/a06 lumberjack/a07 lumberjack/a08 | lumberjack/j01 lumberjack/j02 lumberjack/j03 lumberjack/j04 lumberjack/j05 lumberjack/j06 lumberjack/j07 lumberjack/j08 | lumberjack/m01 lumberjack/m02 lumberjack/m03 lumberjack/m04 lumberjack/m05 lumberjack/m06 lumberjack/m07 lumberjack/m08 | BLOCKED — REQUIRES DECISION |
| Merchant | merchant/a01 merchant/a02 merchant/a03 merchant/a04 merchant/a05 merchant/a06 merchant/a07 merchant/a08 | merchant/j01 merchant/j02 merchant/j03 merchant/j04 merchant/j05 merchant/j06 merchant/j07 merchant/j08 | merchant/m01 merchant/m02 merchant/m03 merchant/m04 merchant/m05 merchant/m06 merchant/m07 merchant/m08 | BLOCKED — REQUIRES DECISION |
| Engineer | engineer/a01 engineer/a02 engineer/a03 engineer/a04 engineer/a05 engineer/a06 engineer/a07 engineer/a08 | engineer/j01 engineer/j02 engineer/j03 engineer/j04 engineer/j05 engineer/j06 engineer/j07 engineer/j08 | engineer/m01 engineer/m02 engineer/m03 engineer/m04 engineer/m05 engineer/m06 engineer/m07 engineer/m08 | BLOCKED — REQUIRES DECISION |
| Mechanic | mechanic/a01 mechanic/a02 mechanic/a03 mechanic/a04 mechanic/a05 mechanic/a06 mechanic/a07 mechanic/a08 | mechanic/j01 mechanic/j02 mechanic/j03 mechanic/j04 mechanic/j05 mechanic/j06 mechanic/j07 mechanic/j08 | mechanic/m01 mechanic/m02 mechanic/m03 mechanic/m04 mechanic/m05 mechanic/m06 mechanic/m07 mechanic/m08 | BLOCKED — REQUIRES DECISION |
| Mage | mage/a01 mage/a02 mage/a03 mage/a04 mage/a05 mage/a06 mage/a07 mage/a08 | mage/j01 mage/j02 mage/j03 mage/j04 mage/j05 mage/j06 mage/j07 mage/j08 | mage/m01 mage/m02 mage/m03 mage/m04 mage/m05 mage/m06 mage/m07 mage/m08 | BLOCKED — REQUIRES DECISION |
| Adventurer | adventurer/a01 adventurer/a02 adventurer/a03 adventurer/a04 adventurer/a05 adventurer/a06 adventurer/a07 adventurer/a08 | adventurer/j01 adventurer/j02 adventurer/j03 adventurer/j04 adventurer/j05 adventurer/j06 adventurer/j07 adventurer/j08 | adventurer/m01 adventurer/m02 adventurer/m03 adventurer/m04 adventurer/m05 adventurer/m06 adventurer/m07 adventurer/m08 | BLOCKED — REQUIRES DECISION |
| Alchemist | alchemist/a01 alchemist/a02 alchemist/a03 alchemist/a04 alchemist/a05 alchemist/a06 alchemist/a07 alchemist/a08 | alchemist/j01 alchemist/j02 alchemist/j03 alchemist/j04 alchemist/j05 alchemist/j06 alchemist/j07 alchemist/j08 | alchemist/m01 alchemist/m02 alchemist/m03 alchemist/m04 alchemist/m05 alchemist/m06 alchemist/m07 alchemist/m08 | BLOCKED — REQUIRES DECISION |
| Librarian | librarian/a01 librarian/a02 librarian/a03 librarian/a04 librarian/a05 librarian/a06 librarian/a07 librarian/a08 | librarian/j01 librarian/j02 librarian/j03 librarian/j04 librarian/j05 librarian/j06 librarian/j07 librarian/j08 | librarian/m01 librarian/m02 librarian/m03 librarian/m04 librarian/m05 librarian/m06 librarian/m07 librarian/m08 | BLOCKED — REQUIRES DECISION |

| Profession framework feature | Status | Acceptance / dependency |
| --- | --- | --- |
| Three-active cap; persistent inactive progress | NOT STARTED | Server validation, concurrent requests, restart; shared profession storage |
| 15,000-coin switching | BLOCKED — REQUIRES DECISION | Charged-action definition/funding; atomic charge and activation; preserve inactive progress |
| XP and Apprentice/Journeyman/Master ranks | BLOCKED — REQUIRES DECISION | Approved thresholds, attribution and rank/quest requirements |
| Quest event/approval/reward engine | BLOCKED — REQUIRES DECISION | Representative approved profession first; no recycling/place-break/claim exploits |
| Journeyman equipment/services | BLOCKED — REQUIRES DECISION | Approved catalogues/gates |
| Master shops and exclusive purchases | BLOCKED — REQUIRES DECISION | Catalogue/base prices/rounding/transfer policy; active matching Master checked on commit |
| Per-player/item compounded +50% repeat price | BLOCKED — REQUIRES DECISION | Approved indexing/rounding; exact arithmetic; recover charge/delivery/count together |

## Economy and markets

| Feature | Status | Acceptance / dependency |
| --- | --- | --- |
| Copper 1 / Silver 10 / Gold 100 / Platinum 1,000 physical currency | NOT STARTED | Registered unforgeable item identities; no unauthorized minting/duplication |
| Virtual balances / transaction recovery | NOT STARTED | Bounded integer arithmetic; proven inventory/store save boundaries; no double spend |
| Bank deposits / withdrawals | BLOCKED — REQUIRES DECISION | Earlier counter premises/tier, denominations/capacity/range/funding; restart/failure/concurrency |
| NPC selling budgets / per-resource quotas / profession allowances | BLOCKED — REQUIRES DECISION | Price/budget/quota/reset tables, identity/eligibility, final-item/reset races |
| Dynamic prices / inflation controls | BLOCKED — REQUIRES DECISION | Formula, bounds, stock/funding, reset and economy simulation |
| Daily / weekly profession contracts | BLOCKED — REQUIRES DECISION | Objectives, attribution, funding, rewards, expiry and eligibility; one completion |
| Fixed-price market listings | BLOCKED — REQUIRES DECISION | Escrow/item matching/cancellation/capacity/fees; last-item and interrupted delivery tests |
| Timed auctions | BLOCKED — REQUIRES DECISION | Bids/increments/anti-sniping/refunds/expiry/offline rules; durable escrow |
| Buy orders | BLOCKED — REQUIRES DECISION | Price/time priority/partial fills/item matching; funded escrow |
| 5% normal / 10% Master tax | BLOCKED — REQUIRES DECISION | Approved rounding/timing/recipient; exact arithmetic and restart |
| Royal Trading Counter | BLOCKED — REQUIRES DECISION | Intermediate premises/gate and actual stock/service rules; independent of final Hall |

Excluded custom features remain excluded: Weekly Events, Pet Shop, direct Player Trading and player-facing Economy Ledger. Internal recovery/audit records are required for security, not restoration of an excluded product feature.

## Technology and magic

| Integration | Status | Acceptance / dependency |
| --- | --- | --- |
| Create | BLOCKED — REQUIRES DECISION | Exact compatible artifact/dependencies and approved content/tier/profession gate matrix; actual mod installed in client/server tests |
| Mekanism | BLOCKED — REQUIRES DECISION | Exact compatible artifact/dependencies and approved content/tier/profession gate matrix; actual mod installed in client/server tests |
| Occultism | BLOCKED — REQUIRES DECISION | Exact compatible artifact/dependencies and approved content/tier/profession gate matrix; actual mod installed in client/server tests |
| Mystical Agriculture | BLOCKED — REQUIRES DECISION | Exact compatible artifact/dependencies and approved content/tier/profession gate matrix; actual mod installed in client/server tests |
| Productive Bees | BLOCKED — REQUIRES DECISION | Exact compatible artifact/dependencies and approved content/tier/profession gate matrix; actual mod installed in client/server tests |
| Ars Nouveau | BLOCKED — REQUIRES DECISION | Exact compatible artifact/dependencies and approved content/tier/profession gate matrix; actual mod installed in client/server tests |
| Iron’s Spells | BLOCKED — REQUIRES DECISION | Exact compatible artifact/dependencies and approved content/tier/profession gate matrix; actual mod installed in client/server tests |

Acquisition/use route coverage: **NOT STARTED** (crafting, machines, loot, shops, crops/bees/summons, transfers/automation, existing blocks). Ownership/delegation and offline automation: **BLOCKED — REQUIRES DECISION**. Creative Energy Cube: **BLOCKED — REQUIRES DECISION** for exact item and permitted actions; Tier 12 + active Master Mechanic eligibility remains fixed. No integration is called working without its actual dependency.

## Player interfaces and Royal Administration

| Interface/service | Status | Acceptance / dependency |
| --- | --- | --- |
| Reusable Royal Gilded controls/layout | TESTED | Slate/gold/parchment, normal readable font, focus/help/scrolling and actual scaled screenshots |
| Overview / progression / contributions / ranking / history | TESTED | Server-backed values, correlation/timeout/reconnect, actual two-client rendering |
| Blueprint / placement / construction / Reeve interfaces | TESTED | Connected secure actions, progress/pause reasons, advisory preview, multiple sizes and screenshots |
| Profession grid / quest trees / profile/statistics | NOT STARTED | Actual approved progression and privacy rules; no fabricated quest content |
| Banking / NPC shop / Master shop / market / contract interfaces | NOT STARTED | Actual secure services first; reviewed confirmations/quotes; no generic chest substitute |
| Royal Administration shell | IN DEVELOPMENT | Start with real construction site approval; per-action permissions, exact revision confirmations, audit |
| Kingdom / milestone / border administration | BLOCKED — REQUIRES DECISION | Approved override/migration/border policy; confirmed audited operations |
| Player profession / quest approval administration | BLOCKED — REQUIRES DECISION | Quest definitions, roles and evidence policy |
| NPC assignments / construction recovery administration | IN DEVELOPMENT | Exact registry IDs, conservative restoration; no silent duplicate NPC/project |
| Economic correction / funds / audit administration | NOT STARTED | Proven transaction engine and exact-target confirmed repair; admin-only technical records |
| Technology unlock administration | BLOCKED — REQUIRES DECISION | Verified gates and override/machine rules |

## Project acceptance

Release remains **NOT STARTED**: all approved content functional, all 12 tiers/professions/288 approved quests/economy/integrations/admin/interfaces usable; native restart and multiplayer/exploit tests; actual screenshots; full build and installable release JAR/install guide; honest limitations. A green build alone cannot mark the project COMPLETE.

Current validation: full build passes 121 JUnit tests. The substantial native lifecycle covers contributions, claims/recovery, admin approval, redemption, full progressive construction, protection, interruption, identity, scheduled updates and realm-entry denial. Actual Minecraft UI claim → proposal → exact administrator approval → redemption → physical Aldric work → 782-block furnished Lodge → Mira dialogue/lectern services ran. An in-progress process restart retained progress; the final completed-world restart retained exactly one original Reeve. Two actual clients joined the normal dedicated server; Mira’s real contribution service worked for the non-admin, with Royal Admin controls hidden. Details are recorded in VALIDATION.md. Next task: continue approved unblocked framework work after the delivered Lodge checkpoint. Missing numeric/content rules receive a separate proposed review specification, not silently enabled defaults. Update this tracker and CODEX_CONTINUATION.md after meaningful checkpoints.
