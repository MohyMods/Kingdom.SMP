# Kingdom SMP — master completion tracker

> **D-023 — current implementation authority:** The [master directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md) authorizes incremental coding and supersedes the D-021/D-022 review-only hold. New-world Town Hall / existing-world Lodge profiles, owner Hut bootstrap, blueprint-market acquisition, automatic server-valid placement, supports up to four blocks, Tier 3 Bank (no new-world Storehouse), updated profession/economy/quest/admin scope apply. End remains Tier 12. The [full replacement mod snapshot](sources/MOD_SNAPSHOT_2026-10-31.txt) now reports FTB Quests installed. See [supersession and actual status](MASTER_SUPERSESSION_MATRIX.md); old descriptions below are historical where they conflict.

> **D-022 — planning only, current dimension authority:** End unlocks at **Tier 12**, superseding every End-at-9 design. Target schedule: Aether 4; Nether 5; Twilight Forest 6; Otherside 8; reviewed Ad Astra stages 10–11; End 12. No mandatory prerequisite for reaching 12 may depend on End resources, including indirect recipes. Dragon/End exploration belongs after the unlock; **Royal Legacy Monument** remains the proposed final construction reward. Existing code/data/worlds are unchanged. See [dimension schedule, dependency graph and open recipe audit](DIMENSION_PROGRESSION.md). Implementation remains on hold.

> **D-021 — 31 October 2026, current design authority:** Physical blueprint items and full-building crosshair-controlled holograms are approved requirements. Future Tier 1 completion awards the **19 × 15, two-floor Town Hall at Tier 2**, replacing the future Lodge reward subject to separately approved migration. The former Tier 12 Hall requirement is superseded. Existing 0.4.0-dev code, Lodge/Mira, entitlements and saves remain unchanged. **Do not implement yet.** See [construction design](CONSTRUCTION.md) and [approval/migration plan](BLUEPRINT_IMPLEMENTATION_PLAN.md). Service gates and the rest of the building roadmap are not silently changed.

Authority: latest user completion directive (31 October 2026, Asia/Riyadh), the requirements register and subsequent decisions. Preserve current source/persistence. D-021 changes future Hall timing to Tier 2; existing binary behavior is unchanged. Latest published verified source: `7ce19a8` (0.4.0-dev server milestone definitions/validation/inspection, preserving the tested Lodge/weather/realm gates). Historical 0.2.0-dev package: `5baf285`. This tracker records observed implementation, not promises. No complete-project claim.

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
| Server-data requirements catalogue and generic ID/tag matching | TESTED | Strict bounded JSON; registered materials/tags, categories/prerequisites/display; twelve inspectable definitions; future consumption remains disabled |
| Dependency graph and acquisition validation | TESTED | Fixed realm boundaries, source-tier availability, unknown refs, cycles/overlap; unverified mod/recipe/profession paths blocked |
| Catalogue reload / First Hearth history compatibility | TESTED | Invalidate old offers; incompatible/corrupt definitions block new consumption; preserve existing recovery/history/winner |
| Generalized live later-tier transaction/history/scoring adapter | IN DEVELOPMENT | Approved future scoring and pinned definitions; native interruption/receipt tests before any proposed consumption |
| Royal Storehouse legacy compatibility | NOT STARTED | D-023 removes new-world Storehouse implementation/awards; preserve historical records if encountered |
| Generic building/NPC registry and safe recovery | IN DEVELOPMENT | Shared typed identity/assignments, verification, bounded routines, audited conservative recovery |
| Future Tier 2 Town Hall and full physical-plan holograms | NOT STARTED | D-021 design specified; implementation and migration plan require approval first |
| Legacy-world migration / initial Hut bootstrap | BLOCKED — REQUIRES DECISION | Preserve all existing identities/projects; approve profile/conversion/bootstrap rules before implementation |

## All twelve tiers

| Tier | Approved identity/gate | Status | Acceptance / dependency |
| --- | --- | --- | --- |
| 1 | The First Hearth | TESTED | 1,024 logs / 2,048 cobblestone / 512 wheat / 128 iron; consumption/history/winner; advance 2 |
| 2 | Roads of Commerce; Founders’ Lodge privilege | IN DEVELOPMENT | Lodge loop tested; revised eight-resource + Lodge/road costs proposed; Royal Storehouse reward scope specified; scoring/service rules pending |
| 3 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 4 | Aether schedule confirmed; integration/requirements unverified | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 5 | Nether gate | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 6 | Twilight Forest schedule confirmed; integration unverified | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 7 | Name/requirements/reward undefined | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 8 | Otherside schedule confirmed; integration unverified | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 9 | End locked; non-End industry/exploration proposals | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 10 | Ad Astra first reviewed stage; destination split pending | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 11 | Ad Astra next reviewed stage; no End-dependent path to 12 | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |
| 12 | End unlock; proposed Royal Legacy Monument; Cube + active Master Mechanic | BLOCKED — REQUIRES DECISION | Proposed resource/dependency table drafted; approve balance/reward/unlocks; implement sequential transactions, recognition and native persistence tests |

World border expansion: **BLOCKED — REQUIRES DECISION** (centers, sizes, dimensions, timing, safety). Charter after Tier 2: **BLOCKED — REQUIRES DECISION** (exact boundary/content); its independent gate remains under review and is not granted by the Hall. Vanilla Nether/End entry enforcement: **TESTED only for the historical 0.4.0-dev Tier 5/9 boundaries; the D-022 Tier-12 End change is NOT IMPLEMENTED** and native player changeDimension, direct cross-realm teleportTo and item transfers. Unknown mod dimensions and characters already inside a locked realm on import are not rewritten or claimed covered. Third-party routes remain dependent on verified integration APIs.

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
| Server-backed twelve-tier proposal inspection | TESTED | Actual normal/narrow Royal Gilded rendering, disabled eight-resource targets; dedicated reload/refusal/recovery and restarted revision verified |
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


## D-020 progressive resource checkpoint

PROGRESSIVE_RESOURCE_BALANCE.md contains all twelve resource rows with approved/proposed status, acquisition rationale, dependency graph, explicit mod/Master blockers and indicative ten-player effort. The server catalogue/validator and native matching/reload checks pass a full build with 139 JUnit tests and the expanded required GameTest. Proposed later costs remain disabled, including the user’s revised Tier 2 list. Generic definition support is not a claim of live twelve-tier transactions, Storehouse services, mod compatibility, AFK provenance or approved future scoring. Physical reload/render verification is recorded in VALIDATION.md as executed.

## D-021 design-only checkpoint

CONSTRUCTION.md defines complete physical/holographic placement coverage. BLUEPRINT_IMPLEMENTATION_PLAN.md records observed reusable code/gaps, authoritative requests, complete rendering/performance plan, review UI, approval decisions, phased acceptance and per-state preservation/migration proposals. No code/resources/save changes or new runtime results. The older Lodge entries are tested legacy-runtime facts, not future-roadmap authority. All new hologram/building implementation is paused until approval.

## D-022 review hold

DIMENSION_PROGRESSION.md records the confirmed schedule, revised dependency graph, End-independent pre-12 path, Ad Astra/mod recipe and alternate-teleport audit blockers, post-unlock Dragon/End finale and proposed Royal Legacy Monument. The current binary remains End-9; new dimensions/End-12 enforcement are NOT STARTED. Actual mod recipes/compatibility are UNVERIFIED. No data migration, runtime tests or new resource activation in this documentation checkpoint. Review these dependencies and existing-player/finale credit policies before coding.

## D-023 active feature status (supersedes conflicting historical rows above)

| Feature | Status | Exact scope / next acceptance |
| --- | --- | --- |
| Full replacement mod inventory | RECORDED | Parsed all 277 reported entries; raw snapshot archived; actual artifact/API/pack compatibility remains unverified |
| Held physical Lodge hologram / natural targeting | TESTED | Actual full-scale block models/roof, R rotations, height keys, green server-valid / red blocked / pending amber; connected Royal Gilded review |
| Read-only probe authorization and diagnostics | TESTED | Native identity/copy/current-held checks, no review replacement, collision/support cell bounds; 142 JUnit tests/full native lifecycle |
| FTB Chunks + OPAC permissions | TESTED — PINNED SUBSET | Actual exact APIs tested together on normal dedicated server; independent veto/revocation/resume/offline pause and real client init/owned preview. Owner online/same dimension required; full-pack/providers unverified |
| New-world Town Hall vs existing Lodge profiles | NOT STARTED | New rule approved; versioned classification/pinning and safe legacy detection required before changing reward binding |
| Owner Hut physical blueprint/bootstrap | NOT STARTED | Immediate bootstrap exception now approved; owner identity selection and durable single entitlement pending |
| 19×15 two-floor Hall / Steward / services | NOT STARTED | Implement real structure and services incrementally; preserve Aldric/Mira and old projects |
| Automatic validated site authorization | NOT STARTED | Legacy admin flow remains in this slice; new behavior must recheck full protection/support/reservations without client trust |
| Four-block safe supports | NOT STARTED | Existing prepared-support checks preserved, not a working automatic foundation engine |
| Architect first-buyer market / early payments | NOT STARTED | One primary entitlement; atomic charge/stock, reviewed individual 500–15k prices and pre-Bank funding/payment path |
| Tier 3 Bank / transfers / mailboxes / auctions | NOT STARTED | Real transaction/escrow recovery and reviewed unresolved policies before consumption |
| Bounty Hall / Fisherman's Wharf | NOT STARTED | Confirmed Tier 3 scope; proposed blueprint prices not final; actual service/contracts/economy tests required |
| Royal Aerodrome / aircraft gates | NOT STARTED | Confirmed ≥6 AND completed building; per-aircraft schedule proposed; reported version is not a working adapter |
| Vanilla End Tier 12 gate | TESTED | All-tier boundary unit checks and native pre-unlock transfer denial; no world/history rewrite. Third-party bypasses/legacy-inside handling not verified |
| Waystones Tier 9 / other dimension routes | NOT STARTED | Reported versions only; actual adapters and metadata/recipe reconciliation pending. Inactive old catalogue remains historical, not active requirements |
| Royal Legacy Monument | NOT STARTED | Confirmed capstone scope; exact geometry/award/participation and services pending |
| FTB Quests synchronization | NOT STARTED | Now reported installed 2101.1.37; full loaded quest data/chapters/team/personal/security integration unverified |
| Founder titles / chosen new NPC display names | NOT STARTED | Stable identities and no honorary permission elevation; preserve existing names |
| Complete `/kingdom admin` sandbox/test suite | NOT STARTED | Existing real command subset documented; no fake admin controls or production-history resets |

The new directive authorizes implementation; no review-only hold remains. Exact future prices, quest/XP definitions, economy edge policies and planet/type schedules are still unapproved where specified. Follow Phase B dependencies before expanding services.
