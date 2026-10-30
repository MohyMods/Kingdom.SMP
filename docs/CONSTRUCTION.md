# Blueprint and construction specification

Source: S3. This is authoritative scope and required behavior, not a claim of implementation. Exact footprints, build rates, durations, land boundaries and protection integration policy were not supplied.

## First dependency: Builder’s Hut

An administrator sets up the Builder’s Hut before the Town Hall exists. It is an original, fully furnished timber-and-stone building, with a home/work area for Master Builder **Aldric Stonehand**. Setup must not require already owning the Town Hall reward. Provide safe admin placement/setup and persistent building/NPC identity; no unapproved coin/material fee is implied.

The hut is a real building and NPC interaction point, not only a command/menu or decorative mockup. Catalogue, blueprint preview and construction confirmation use Royal Gilded interfaces. See [NPCs](NPCS.md) for identity/dialogue/routine behavior.

## Blueprint registry and Town Hall entitlement

Registry entries need stable blueprint IDs, footprint/required-block definitions, supported rotations, preview data and build sequence. Exact asset formats and APIs must be verified during implementation. Do not create generic registry placeholders and claim finished structures.

The First Hearth highest contributor receives one noncraftable Town Hall blueprint with construction materials included. A persistent server entitlement, not the inventory stack, governs redemption. A cloned/lost/recovered item must never authorize multiple halls. Record the permanent winner, issuance/recovery, selected site, accepted construction, completion and admin recovery history. Once construction begins, recovering an item must not reopen an unused entitlement.

The winner chooses a protected site and the completed building is kingdom-owned. Administrative recovery must address abandonment or malicious placement. Exact recovery policy, NPC death policy, building damage policy and protection boundaries remain open; do not promise arbitrary deletion/rollback is lossless.

## Placement and confirmation flow

1. At the hut, inspect the catalogue entry and reward eligibility.
2. Preview the complete footprint and selected rotation at a candidate site.
3. Server validates entitlement, dimension/site/land/protection policy, collisions, existing buildings and required loaded context. Client preview is advisory only.
4. Show valid/invalid status and actionable reasons; rotate without mutating world blocks.
5. Confirm the exact blueprint, site and rotation. Revalidate all conditions at commit; reserve the entitlement and construction site consistently.
6. Start persistent construction once. A failed or stale validation must not consume an unused reward or begin overlapping work.

Terrain replacement/site preparation, fluid handling, protection mod hooks and block/entity collision policy need explicit implementation rules before production placement. Never interpret absent external integration as proof that a protected location is allowed. Keep validation bounded and avoid scanning arbitrary client-supplied volumes.

## Progressive construction lifecycle

Construction places real blocks progressively under a bounded scheduler. Limit simultaneous work and per-tick work; configure rates without inventing a promised duration. Pause work when required chunks are unloaded; do not permanently force-load chunks. Persist blueprint/site/rotation/progress and resume safely after restart.

Aldric has safe work behavior: reach appropriate work positions where possible, pause/report failure when paths or work sites are blocked, and never use unsafe teleporting or destruction to conceal a path failure. Exact pathing/work synchronization must be implemented and tested using real APIs. Construction must not silently complete because a timer elapsed while unloaded.

Placement and progress updates need crash/retry-safe ordering. Revalidate planned target blocks and do not overwrite unrelated player work, containers or protected areas under a stale preview. Pause/conflict behavior must be visible; unapproved automatic demolition is not allowed. Chunk unload, player obstruction, server stop, failed block placement and exhausted work budgets must leave resumable state, not duplicate scheduler jobs.

## Town Hall structure and completion

The Town Hall must be a complete furnished building, including:

- Grand meeting space and throne/leadership area.
- Kingdom progression interface and milestone contribution area.
- Town Hall Bank counter and designated Royal Trading location.
- NPC areas, lighting, decoration, storage/admin space and navigable paths.

No exact dimensions, floor plan, material palette quantities or duration have been supplied. Design/review the actual structure rather than presenting an empty shell as complete.

On completion verify all required structural blocks against the blueprint. Then persist completed state, announce completion, enable only permitted services, introduce/assign Royal Steward **Elara Vale**, register the building in the Kingdom Overview (and map if a map is later implemented), and update the Builder’s Hut catalogue. Overview registration satisfies discoverability without asserting a new world-map UI is required.

Bank and Royal Trading are the only approved Town Hall service categories at this point. Counter locations do not mean their later economy implementations already work. No automatic Charter, Master services, profession unlocks or unrelated technology rewards.

## Integrity and acceptance

Maintain a persistent building registry with kingdom ownership, blueprint/site/rotation, lifecycle state and associated NPC identities. Completion verification and subsequent damage handling are separate: approved completion does not establish what services should do after destruction. Define service suspension, repairs/rebuilds and NPC displacement/recovery before claiming damage safety.

Required checks: invalid/rotated footprints, collision/protection denial, stale confirmation, cloned blueprint, duplicate request, concurrent site reservations, full restart at each construction boundary, unloaded-chunk pause/resume, no force-load leakage, bounded scheduler work, path failure, conflicting player edits, structural verification, one steward/one building registration, entitlement recovery and admin tamper/abandonment handling. These are future acceptance criteria, not executed results.
