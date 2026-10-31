# Kingdom progression — current authority

> **D-023 — current implementation authority:** The [master directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md) authorizes incremental coding and supersedes the D-021/D-022 review-only hold. New-world Town Hall / existing-world Lodge profiles, owner Hut bootstrap, blueprint-market acquisition, automatic server-valid placement, supports up to four blocks, Tier 3 Bank (no new-world Storehouse), updated profession/economy/quest/admin scope apply. End remains Tier 12. The [full replacement mod snapshot](sources/MOD_SNAPSHOT_2026-10-31.txt) now reports FTB Quests installed. See [supersession and actual status](MASTER_SUPERSESSION_MATRIX.md); old descriptions below are historical where they conflict.

> **D-022 — planning only, current dimension authority:** End unlocks at **Tier 12**, superseding every End-at-9 design. Target schedule: Aether 4; Nether 5; Twilight Forest 6; Otherside 8; reviewed Ad Astra stages 10–11; End 12. No mandatory prerequisite for reaching 12 may depend on End resources, including indirect recipes. Dragon/End exploration belongs after the unlock; **Royal Legacy Monument** remains the proposed final construction reward. Existing code/data/worlds are unchanged. See [dimension schedule, dependency graph and open recipe audit](DIMENSION_PROGRESSION.md). Implementation remains on hold.

> **D-021 — 31 October 2026, current design authority:** Physical blueprint items and full-building crosshair-controlled holograms are approved requirements. Future Tier 1 completion awards the **19 × 15, two-floor Town Hall at Tier 2**, replacing the future Lodge reward subject to separately approved migration. The former Tier 12 Hall requirement is superseded. Existing 0.4.0-dev code, Lodge/Mira, entitlements and saves remain unchanged. **Do not implement yet.** See [construction design](CONSTRUCTION.md) and [approval/migration plan](BLUEPRINT_IMPLEMENTATION_PLAN.md). Service gates and the rest of the building roadmap are not silently changed.

Earlier Town Hall policy was superseded by D-021; dimension policy by D-022. The paragraphs explicitly describing 0.4.0-dev below are retained runtime history. The kingdom begins at **Tier 1 — The First Hearth**. Completing its resources advances to **Tier 2 — Roads of Commerce**. Progression is sequential through twelve tiers; exact later requirements must be approved before activation.

## The First Hearth

Fixed ten-player shared targets: **1,024 `#minecraft:logs`, 2,048 `minecraft:cobblestone`, 512 `minecraft:wheat`, 128 `minecraft:iron_ingot`**. Donations permanently consume only accepted items. Partial donations are capped to outstanding requirements and actual eligible inventory; excess remains with the player. Clients never supply credited totals, identities, ranks, or tiers.

Each category has equal maximum weight 1. Score is the sum of personal quantity/category requirement, maximum 4. Exact integer units use denominator 2,048: logs ×2, cobblestone ×1, wheat ×4, iron ×16. Ties prioritize the first to reach the tied score (last positive accepted sequence), then UUID natural order. Display rounding does not determine ranking. History records accepted quantities and server identity; failed requests earn nothing.

Use the durable protocol in [contribution recovery](CONTRIBUTION_RECOVERY.md) before consuming items. Completion, the permanent winner and Tier 2 advancement derive from one canonical accepted history. Inventory checkpoints and ledger receipts reconcile process interruption without removing resources a second time. Unknown save/restore mismatches fail closed.

## Existing 0.4.0-dev Tier 2 reward — preserved legacy behavior

The user selected **Founders’ Lodge**: a furnished meeting and milestone building, a named **Reeve**, and included construction materials. The highest First Hearth contributor earns its exclusive construction privilege. The building belongs to the kingdom. No banking, Royal Trading, Charter or unrelated service is granted by constructing it.

A permanent winner record is distinct from a claimed physical plan, unused entitlement, reserved site, active construction, and completed building. A copied/lost item must not authorize a second Lodge. Claim/recovery and construction require those state transitions and tests before being called complete. The winner must be publicly recognized. Every later tier also recognizes its highest contributor, but its exact reward needs separate approval.

## Tier register

| Tier | Confirmed name / gate | Requirements / reward |
| --- | --- | --- |
| 1 | The First Hearth | Exact resources above; completion advances to 2; highest contributor earns Founders’ Lodge privilege |
| 2 | Roads of Commerce | Lodge available; revised eight-resource + Lodge/road milestone is proposed, not active; Royal Storehouse reward scope approved |
| 3 | Name pending | Requirements and reward pending |
| 4 | The Aether access | Compatible integration and requirements/reward pending; no Aether material needed to reach 4 |
| 5 | Nether access gate | Requirements and reward pending |
| 6 | Twilight Forest access | Compatible integration/internal progression and requirements pending |
| 7 | Name pending | Requirements and reward pending |
| 8 | Otherside access | Compatible integration and requirements pending |
| 9 | End remains locked | Non-End resource/industry milestone proposal; exact reward pending |
| 10 | Ad Astra progression, first reviewed stage | Compatible release/planet split and End-independent recipe closure pending |
| 11 | Ad Astra progression, next reviewed stage | Requirements to reach 12 must remain entirely End-independent |
| 12 | End access; Creative Energy Cube also requires active Master Mechanic | Post-unlock Dragon/End finale; Royal Legacy Monument proposed final construction reward |

The **Royal Guild Charter remains available after Tier 2**, independently of the Town Hall reward change. Whether that means Tier 2 completion or Tier 3 entry remains unresolved. Do not silently activate it. Earlier banking and Royal Trading need intermediate premises and separately approved unlock gates; a Founders’ Lodge does not grant them.

Later tier tables, names, border dimensions/centers/sizes/timing and profession/technology rules remain configurable design dependencies. Do not populate arbitrary values or imply unimplemented gate enforcement. Historical checkpoint archives preserve earlier designs; they are not current authority.

## Progressive resource framework (D-020)

Use diversity, processing, rarity, acquisition and cooperation as well as quantity. The revised Tier 2 candidates and all later proposed lists, effort assumptions and availability explanations are in [the balance review](PROGRESSIVE_RESOURCE_BALANCE.md). Runtime schema/commands/activation limits are in [server milestone data](MILESTONE_DATA_FORMAT.md). Future costs are inspectable but disabled; modifying an approval label cannot enable them. Existing First Hearth counts, winner, exact normalization and inventory/state recovery are retained. Any generalized live scoring/consumption migration must pin its approved definition before accepting resources.

For future profiles, replace only the approved reward binding after migration review: First Hearth → Tier 2 Town Hall. Current table entries describing Lodge remain existing-runtime/legacy behavior. Initial quantities and historical normalized scores remain unchanged; existing unfinished worlds need an explicit profile decision. Proposed later construction objectives must be re-reviewed, not mechanically renamed.
