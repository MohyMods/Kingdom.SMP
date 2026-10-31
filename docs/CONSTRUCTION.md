> **Current Town Hall implementation:** [0.6.0 checkpoint](TOWN_HALL_CHECKPOINT.md) implements pinned fresh-world Hall versus preserved legacy Lodge profiles, free owner Hut plans, progressive furnished Hall and Elara services. Historical text below does not authorize converting existing saves or awarding another historical reward. See the checkpoint validation report for actual executed acceptance and remaining work.

# Physical blueprints and holographic construction — approved design

> **D-023 — current implementation authority:** The [master directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md) authorizes incremental coding and supersedes the D-021/D-022 review-only hold. New-world Town Hall / existing-world Lodge profiles, owner Hut bootstrap, blueprint-market acquisition, automatic server-valid placement, supports up to four blocks, Tier 3 Bank (no new-world Storehouse), updated profession/economy/quest/admin scope apply. End remains Tier 12. The [full replacement mod snapshot](sources/MOD_SNAPSHOT_2026-10-31.txt) now reports FTB Quests installed. See [supersession and actual status](MASTER_SUPERSESSION_MATRIX.md); old descriptions below are historical where they conflict.

> **D-022 — planning only, current dimension authority:** End unlocks at **Tier 12**, superseding every End-at-9 design. Target schedule: Aether 4; Nether 5; Twilight Forest 6; Otherside 8; reviewed Ad Astra stages 10–11; End 12. No mandatory prerequisite for reaching 12 may depend on End resources, including indirect recipes. Dragon/End exploration belongs after the unlock; **Royal Legacy Monument** remains the proposed final construction reward. Existing code/data/worlds are unchanged. See [dimension schedule, dependency graph and open recipe audit](DIMENSION_PROGRESSION.md). Implementation remains on hold.

**D-021, 31 October 2026 (Asia/Riyadh). Design only. Do not implement until the building roadmap, implementation plan and save migration are approved.** This instruction supersedes the former Tier 12 Town Hall / future Tier 2 Founders’ Lodge policy. The existing 0.4.0-dev implementation and saves remain unchanged.

For future gameplay, the **Town Hall is the Tier 1 highest-contributor reward, unlocked at Tier 2**, with a **19 × 15 block footprint and two floors**. It replaces the Lodge in the future roadmap, subject to separately approved migration. Total height, foundation envelope, floor plan, furnishing layout and NPC/service assignments require the remaining building review; do not invent them. An early Town Hall does not automatically unlock banking, trading, Charter or profession services. D-022 subsequently changes End to Tier 12 and supplies the dimension schedule; Nether 5 and Creative Energy Cube Tier 12 plus Master Mechanic remain. Royal Legacy Monument is the proposed Tier 12 final construction reward, with geometry/costs/NPC/award policy pending.

## Coverage and acquisition

Every kingdom building receives a distinct carryable Blueprint item type; each legitimate entitlement also has its own authoritative identity. Distinct artwork/name/item type is not redemption authority. Shared implementation components should serve all building types.

| Building coverage | Acquisition authority | Outstanding roadmap detail |
| --- | --- | --- |
| Builder’s Hut | Initial administrator/owner blueprint | Initial owner identification and Aldric bootstrap policy |
| Town Hall | Tier 1 highest contributor; available at Tier 2; authorized reward materials included | Approved 19 × 15, two floors; remaining structural/service design and migration |
| Royal Bank | Physical blueprint under the eventual approved building roadmap | Acquisition route, tier, banker and services; no early bank gate inferred |
| Royal Storehouse | Physical reward blueprint under approved contributor policy; included authorized materials | D-020 Quartermaster/shared-storage scope retained; final placement in revised roadmap, capacity/access and transaction rules pending |
| Other major tier rewards | Unique highest-contributor blueprints | Final per-tier buildings and awards pending |
| All twelve profession buildings | Approved community construction projects | Each project's prerequisites, approval/evidence, authorized blueprint holder and NPC/building design pending |
| Existing Founders’ Lodge | Preserve original plan type, entitlement, projects and Mira Ashford | Legacy compatibility; no automatic replacement or additional Town Hall grant |

Profession coverage is explicit: **Farmer, Blacksmith, Miner, Builder, Lumberjack, Merchant, Engineer, Mechanic, Mage, Adventurer, Alchemist and Librarian**. This approves blueprint support for their buildings, not twelve invented building names, new unlock tiers or automatic awards. Every major functional building retains a designated named NPC; existing Aldric/Mira identities remain intact.

## Player flow

1. Claim an authorized physical plan, or recover the same unused entitlement through a Royal Gilded service. No player must copy a UUID or enter coordinates.
2. Holding the plan activates its complete, full-scale translucent building hologram. Crosshair targeting moves the candidate naturally over terrain. Previewing never edits world blocks, spawns real construction entities, creates chunk tickets or consumes an entitlement.
3. Rotate by 90° and adjust vertical placement using configurable controls. An on-screen legend names the player's actual bindings, rotation, height offset and current status. Input details below are proposals pending plan approval.
4. Show the **entire building, including roof, both Town Hall floors and exterior footprint**. A boundary-only wireframe is insufficient. A full model may naturally occlude its own interior; a cutaway must not replace the complete exterior preview.
5. Green means the exact current candidate passed server validation; red means blocked. Highlight collision cells and unsupported terrain columns, and explain failures with readable text/icons. Pending, stale, unloaded or unavailable checks must never appear as valid green.
6. Open a Royal Gilded confirmation screen. Freeze the exact candidate while reviewing; summarize building, orientation, height, footprint, materials-included status, obstruction/support results and approval state. Cancel returns to preview without consuming the plan.
7. Submit the exact candidate for administrator site approval. Public buildings cannot begin work before a permissioned administrator reviews and confirms that exact site. A moved/rotated/height-adjusted proposal invalidates prior approval. Profession/community buildings use the same safe approval workflow unless a later approved policy explicitly defines an exception.
8. After approval, the authorized holder confirms construction in Royal Gilded UI. The server revalidates and commits one reservation/redemption before consuming the physical plan. Aldric physically constructs it progressively using the existing bounded work framework. Initial-Hut bootstrap remains a review decision, not a silently implemented exception.
9. Progress, interruption/pause reasons and completion remain inspectable. Verify required blocks before final building registration and NPC assignment. A building's completion does not implement or unlock an absent service.

Coordinate placement commands become **administrator-only recovery/debugging tools**, preserving permissions, exact-target review, audit and all normal checks. Ordinary placement must use held plans, crosshair targeting and interfaces. Technical record IDs may remain in private administrative diagnostics; players need not type them.

## Hologram and input contract

- Keep Royal Gilded slate, gold/bronze and parchment styling; use green/red for placement geometry/status, with text and symbols for color accessibility.
- Proposed defaults for approval: **R** rotates clockwise, **Shift+R** reverses; **Page Up/Page Down** adjust height by one block; blueprint **Use/right-click** opens review; **Escape** cancels review. All remappable; do not consume normal camera movement or rebind attack to confirm. Exact initial targeting range, offset limits and offhand precedence remain configuration proposals, not settled gameplay rules.
- Preview permission is separate from item appearance. A copied/obsolete plan must show an invalid/recoverable status and cannot submit an authorized project. Bound caches and requests to connection, held entitlement generation and blueprint version.
- Revalidate the complete foundation/support and three-dimensional occupied/reserved envelope, entrances and builder work area. Ninety-degree rotations transform both positions and directional states. Height changes move the whole candidate rather than floating or cutting off the foundation.
- A server-validated green result expires when the candidate, relevant permissions or world conditions change. Approval is never a guarantee of future placement: begin and each physical placement recheck the current state.
- Hold the last candidate only as a clearly stale preview if context is lost. Close/hide it on disconnect, incompatible blueprint data, loss of item/entitlement or inappropriate UI context. Handling hand swaps and menus must not consume authority.

## Server authority and security

Bind each physical plan to a durable world/server construction record, building/version, authorized holder, source award/community project and current issue generation. Never trust client-supplied owner, winner, validity color, material credit, building block list, approval flag or copied item data. Exact record fields and compatibility format are proposed in the implementation plan.

Revalidate complete block collisions, protected locations, terrain support, build height/world border, player/mob/item entities, existing buildings/reservations and builder work space on the server. Unknown protection capability is not permission to bypass it. Preview diagnostics must be bounded and limited to the relevant loaded/permitted site; do not send inventories, arbitrary block-entity data or remote-world scans to the client.

No terrain or player structure is destroyed without explicit authorization. The initial recommended implementation retains **zero terrain replacement**; sites requiring excavation/grading stay red. The existing narrow post-reservation weather-snow behavior is a documented compatibility rule, not permission to clear pre-existing snow/terrain. Any expanded terrain-change workflow requires its own exact change list, approval and protection review.

Failed, stale or canceled pre-construction placement leaves the entitlement unused and recoverable. Once construction is durably reserved, interruptions resume the same project; they do not refund a second blueprint or loose materials. A retry after an uncertain reply inspects the saved result. Lost unused items are recovered by durably revoking the previous issue generation before delivery; all old copies and approvals cease to authorize work. Full-inventory and interrupted delivery cases must remain recoverable.

## Persistence and migration

Preserve all existing structures, footprint/version definitions, earned winners, redeemed/unused entitlements, construction progress, original building/NPC identities, homes, inventories and world files. Never relabel a built/in-progress Lodge as Town Hall, expand it into neighboring terrain, auto-spawn another NPC, re-elect the winner or replay included materials.

**No migration strategy is approved by this document.** Recommended review strategy: preserve existing-world Lodge entitlements/projects by default, use the new Hall reward only for explicitly selected future-world profiles, and separately review any optional one-for-one conversion of unused legacy entitlements. Completed or redeemed projects cannot be converted through the unused-plan path. Existing unfinished First Hearth worlds also require an explicit profile choice; their promised reward must not change silently. See [the implementation and migration plan](BLUEPRINT_IMPLEMENTATION_PLAN.md).

## Acceptance

Actual full-building rendering, four rotations, height/crosshair control, green/red/unknown states, obstruction/support highlights, accessible errors, Royal Gilded normal/scaled screens and no coordinate/UUID workflow. Native server tests must exercise copied plans, replay/concurrency, approvals, protection, interrupted delivery/redemption, pause/unload and restart. Migration must preserve fixtures at every legacy stage. Performance testing must use the actual two-floor Hall and representative larger structures before claiming large-building support. No screenshots, performance result, API verification, new JAR or implementation completion is claimed for this design-only update.
