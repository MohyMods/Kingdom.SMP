# Kingdom progression

Sources: S1 baseline, S2 rebuild, S3 First Hearth slice. Twelve shared tiers remain confirmed; only the first contribution milestone has numerical requirements. Border expansion is scope, but sizes/centers/dimension policy are not supplied.

## Initial kingdom and First Hearth

S3: a new kingdom starts automatically at **Tier 1 — First Hearth**, never Tier 0. **Tier 2 — Roads of Commerce** follows. The first shared milestone is balanced for ten players and does not scale when player count changes.

| Resource category | Required accepted quantity | Matching rule |
| --- | --- | --- |
| Logs | 1,024 | Log tag; exact accepted tag must be documented in implementation |
| Cobblestone | 2,048 | Cobblestone; substitutions not approved |
| Wheat | 512 | Wheat; substitutions not approved |
| Iron ingots | 128 | Iron ingots; substitutions not approved |

Partial submissions are allowed. Accepted items are consumed and count toward shared progress and the submitting player's persisted credited statistics. Accept no more than the remaining category requirement; never remove excess items. Validate inventory, category, current remaining quantity and authoritative player identity on every submission. Clients do not supply trusted totals or credit. Shared and personal totals update live for connected viewers.

No numerical quantities for later milestones, border changes, profession requirements or construction scheduling are implied by this table.

## Contribution score and first reward

Each resource category has equal maximum weight **1**, independent of its required quantity. For credited player quantity `c` and kingdom category requirement `r`, that category contributes `c / r`; the sum of the four category scores is the contribution score, maximum **4**. Inventory submitted beyond the accepted requirement grants no score. Use exact comparisons rather than rounded UI percentages when ranking.

S3 tie rule: **“For ties, prioritize the player who first achieved the tied score, then use a stable deterministic fallback.”** Persist the relevant ranking/time/order evidence; process equivalent simultaneous submissions deterministically. The precise stable fallback implementation must be documented; it must not replace the first-achieved-score rule.

The highest contributor receives the unique **Town Hall blueprint**. Announce the winner to the kingdom and preserve a permanent winner record. The blueprint is noncraftable, includes construction materials, and can be redeemed once. Lost-unused-blueprint recovery must preserve eligibility without allowing duplicate construction. The winner selects a protected location, but the completed Town Hall belongs to the kingdom. Administrative recovery handles abandonment or malicious placement without issuing a second unrestricted build entitlement. See [construction](CONSTRUCTION.md).

Reward eligibility, issued item, placement confirmation, consumed redemption and construction lifecycle need distinct persistent states. An item copy, renamed item, replayed interaction, reconnect, or restored inventory cannot serve as authority to build a second hall. Full-inventory/offline winner delivery and recovery behavior must be implemented explicitly and tested; announcing a winner is not evidence the reward was safely delivered.

## Twelve-tier register

| Tier | Name / confirmed special gate | Milestones |
| --- | --- | --- |
| 1 | First Hearth; automatic initial tier | Shared resources above |
| 2 | Roads of Commerce; Charter available **after Tier 2**, precise boundary unresolved | Later requirements missing |
| 3 | No supplied name | Missing |
| 4 | No supplied name | Missing |
| 5 | Nether unlock | Missing |
| 6 | No supplied name | Missing |
| 7 | No supplied name | Missing |
| 8 | No supplied name | Missing |
| 9 | End unlock | Missing |
| 10 | No supplied name | Missing |
| 11 | No supplied name | Missing |
| 12 | Creative Energy Cube also requires active Master Mechanic | Missing |

Do not award the Charter or Master services when the Town Hall is built. They retain their independent progression/permission gates. Town Hall construction and resource completion must not silently grant unrelated tiers or capabilities.

## Persistence, safety, and future acceptance

Use one authoritative kingdom progression aggregate with stable IDs and schema versions. A completion boundary and its reward election execute once under concurrent contributions. Persist exact accepted quantities, individual credit, winner evidence and reward state. Changes to requirements after contributions exist need reconciliation rules rather than silently changing past scores.

Before later progression implementation define later milestone tables, advancement/consumption/reset policies and charter semantics. Before border implementation define dimension scope, center, tier sizes, timing and safe player/entity handling. The First Hearth resource values and initial tier are no longer missing decisions.

Required tests: partial contributions, insufficient inventory, category matching, excess retained, final-slot race, retry/replay, exact score comparison, both tie-break stages, offline winner, reward loss/recovery, restart at mutation boundaries, admin recovery, and no duplicate reward/construction. Travel-gate tests must cover portals, commands, teleports, respawn, login and third-party routes before those gates are claimed complete.
