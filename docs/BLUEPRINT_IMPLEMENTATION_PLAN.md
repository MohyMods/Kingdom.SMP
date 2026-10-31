# Physical blueprint implementation and migration plan — for approval

**Status: proposed implementation plan, D-021. No gameplay implementation authorized yet.** Authoritative design: [CONSTRUCTION.md](CONSTRUCTION.md). Existing verified source remains `7ce19a8`, packaged by `86828d8` as 0.4.0-dev. Documentation changes do not alter that binary or its saves. Final building roadmap and save migration need approval before coding.

## Existing foundation and concrete gaps

| Observed source | Reuse | Required change after approval |
| --- | --- | --- |
| `KingdomBlueprints` | Noncraftable, carryable authenticated Lodge plan; use/use-on interaction | Distinct item types for all approved buildings, shared plan behavior, held-item placement activation |
| `ConstructionBlueprint` / registry | Immutable versioned blocks, four position/state rotations, footprint/work post | Approved Hall geometry; generic typed registry/manifests and complete ghost meshes for every approved building |
| `ConstructionService` / `ConstructionStore` | Server ownership/generation, exact proposal/admin approval, one-use reservation, atomic journal, bounded progress and NPC issuance | Remove Lodge-specific award/service assumptions through tested adapters; preserve old schema/IDs and same reservations |
| `ConstructionPreview` | Existing advisory footprint/post overlay | Full translucent block geometry, crosshair targeting, vertical control, obstruction/support overlays; current wireframe is not the requested hologram |
| Royal Gilded construction UI/networking | Reviewed confirmed actions, permission checks, correlated replies and timeouts | Natural held-plan workflow and exact-candidate validation/proposal receipts; player coordinate controls removed from ordinary placement |
| Builder’s Hut commands | Existing protected bootstrap installer and original Aldric identity | Initial physical entitlement plus approved bootstrap policy; old commands restricted to admin recovery/debug |
| `MilestoneService.legacyCompatible` | Protects First Hearth's historical quantities/scoring/reward identity | Versioned reward/profile migration; merely changing `founders_lodge` to `town_hall` currently pauses contributions |
| Default milestone data and snapshots | Proposed later costs remain disabled | Review Lodge-completion/connecting-road objectives for future Hall worlds; do not automatically substitute targets or enable revised costs |

Current geometry bounds (64 per axis / 32,768 required blocks) are implementation limits, not an approved maximum for all future structures. Measure full-hologram memory/CPU/GPU use before proposing changes. No new NeoForge hooks or signatures are asserted verified here; inspect actual 1.21.1 sources during the approved implementation phase.

## Proposed architecture and request flow

1. **Definition manifest:** stable building ID/version/content hash, full envelope, ordered placements, foundation/support cells, entrances/work post, NPC/service anchors and resource model references. Render and construction must use the same definition. Only validated manifests from the server/installed mod are allowed; clients cannot supply a building's block list. Bounded sync/cache keyed by hash, with explicit mismatch refusal.
2. **Physical item presentation:** a distinct registered item per building type, using shared behavior. Proposed identifiers for profession items follow their profession IDs; exact IDs/art/name/recipe catalogues remain subject to roadmap approval. Item copies can carry the same reference but acquire no additional authority. Existing Lodge item ID and data remain readable.
3. **Authoritative entitlement:** durable source milestone/project, immutable original winner, authorized placement holder, building/version, issue generation, unused/reserved/redeemed state and project association. Player/NPC display names never substitute for IDs. Preserve the difference between an original award, a recovered item and a completed building.
4. **Target controller:** ray-target a loaded site, snap to a stable foundation anchor, apply 90° rotation and bounded height offset. Movement changes translation; rotation keeps a predictable chosen anchor and shows the exterior/work-post bounds. No-hit/unloaded/out-of-range targets remain invalid. Choose exact anchor/range/limits in the controls review; do not promise unlimited remote placement.
5. **Validation request:** server resolves player identity and currently held entitlement itself; request contains only bounded candidate intent and correlation. Reuse a revision/generation plus definition hash and canonical position/rotation. Proposed throttling: debounce changes and cap server validations rather than scanning a large volume every render frame. Exact rate/diagnostic cell caps are performance parameters to measure, not approved claims.
6. **Validation reply:** same candidate key, valid/blocked/unknown result, short reason codes and bounded collision/support diagnostics. Candidate changes immediately remove green status. Local checks may provide fast provisional hints but never authorize green, approval or construction independently of the server. Recheck entitlement/range/loaded chunks on the server.
7. **Freeze and propose:** Royal Gilded review freezes the exact candidate. Submit commits a proposal, not redemption. Administrator sees building, complete bounds, orientation/height, builder area, protection/support results and zero terrain edits, with a separate confirm action. Permission loss, site changes or a newer proposal invalidate the review.
8. **Begin:** server checks current plan generation, exact approved proposal, inventory/context, reservation conflicts, site/protection and loaded builder context again. Durably bind the one entitlement to one project before removing the physical plan. A stale inventory or process crash cannot redeem a second reward. Failed pre-commit attempts leave it unused; post-commit failures resume the existing reservation.
9. **Build and complete:** reuse bounded physical Aldric work, no chunk tickets/obstruction destruction, prefix verification and protection events. Required-block verification precedes completion/NPC assignment. Services remain gated independently. Preserve conservative missing-issued NPC handling pending separate recovery policy.

Proposed lifecycle:

```text
Awarded → Issued/recovered → Preview (ephemeral) → Proposed → Admin approved
    → Durably reserved/redeemed → Aldric building ↔ Safely paused → Verified complete
```

Canceling preview/review does not revoke the award. Changing a proposed site requires new approval. Recovery before redemption rotates the issue generation and invalidates old copies/approval. Pausing after redemption does not return a redeemable plan. Exact admin abandonment/refund/repair remains outside this proposal.

## Hologram rendering plan

Render full-scale translucent block models for all required shell/roof/furnishing geometry, with green/red tint plus accessible status. Keep the roof visible. Use separate collision/support highlights and an exterior footprint outline; show builder access space distinctly. Pending/stale checks use neutral/amber plus text and never green. Ghosts have no server block entities, inventories, collision, drops, particles that imitate item rewards or live NPCs.

Cache meshes by definition version/rotation/render resources; origin movement uses a transform. Rebuild only changed diagnostics, not the whole building each tick. Cull internal hidden faces and off-screen geometry while retaining the visible complete building. Handle opaque/translucent blocks, waterlogged/directional states and resource reload/disconnect cleanup deliberately. Do not invoke arbitrary live block-entity behavior to draw inventories or services. Verify the actual NeoForge rendering event/model APIs and graphics compatibility before selecting implementation hooks.

Benchmark the actual approved 19 × 15 two-floor Hall and representative large fixtures up to current limits: mesh build time, steady frame time, memory, validation latency/server tick cost, repeated rotation/movement and ten simultaneous player previews. Compare against the same scene without previews on recorded hardware/render settings. Propose numerical budgets after measuring; no FPS/TPS result is claimed. Any adaptive quality mode must preserve complete visible roof/exterior geometry rather than silently falling back to a footprint.

## Royal Gilded UX plan

- World HUD: blueprint/building name, actual key bindings, orientation/height offset, approval state, green/red/unknown symbol and primary actionable reason. Collision positions are debugging aids; errors should explain how to move, clear an existing obstruction manually, prepare support or ask an admin.
- Review: parchment site summary, optional model thumbnail, materials included, public ownership, exact orientation/height and validation/protection summary. Primary action **Submit site**; secondary **Adjust placement** / **Cancel**. A thumbnail does not replace the actual world hologram.
- Pending: inspect the saved proposal and approval status; retrying a timed-out network request refreshes the saved result rather than assuming failure and issuing a second request.
- Admin: permission-gated exact-site review, clear **Approve this site** confirmation, rejection reason and private audit. Approve is not Begin.
- Approved owner view: **Begin Aldric’s construction** review and confirmation. After reservation show actual progress/pause status and building ownership.
- Inventory full/lost plan: clear claim/recovery UI, with old-copy invalidation warning before confirmation and safe uncertain-delivery retry.

Proposed bindings are in CONSTRUCTION.md; validate conflicts, hold/offhand behavior, menus, GUI scale, keyboard/mouse focus and network latency in the real client. No ordinary player action requires a coordinate or UUID. Technical admin diagnostics remain permissioned.

## Save migration — separate approval required

**Recommended default for review: grandfather existing worlds; opt in future worlds explicitly.** Preserve immutable winner/contribution history and old reward definition. Introduce a versioned per-world progression/reward profile before changing First Hearth's future reward binding. Do not infer "new world" merely because the construction store is empty; check contribution history, level/player/entity data and journal identities as a consistent whole. Unknown/partial states fail closed.

| Existing state | Recommended preservation behavior, not an approved conversion |
| --- | --- |
| Fresh world created under approved new profile | Initial Hut entitlement; future First Hearth winner earns Hall once, after reviewed rollout |
| Existing world, First Hearth unfinished | Retain existing promised Lodge profile by default; explicit reviewed choice needed to change it; quantities/scoring/history remain fixed |
| Existing winner, entitlement AVAILABLE | Preserve Lodge award; optionally offer an approved one-for-one unused-entitlement conversion |
| CLAIMED, PROPOSED or APPROVED Lodge | Preserve it; any optional conversion must revoke old token and site approval durably before new Hall item authority exists |
| BUILDING / reserved / uncertain redemption | Resume same Lodge version, origin, progress and NPC association; no conversion/refund path |
| COMPLETE Lodge | Preserve structure, Mira, storage and services. No automatic Hall, new entitlement or replacement NPC |
| Corrupt/missing/mismatched journal or partial backup | Refuse migration; restore/inspect matching backup under existing recovery rules |

Optional unused-entitlement conversion would require its own exact-ID, permissioned Royal Gilded confirmation, immutable source-award link and durable migration operation. Prove interruption boundaries so old and new plans can never both redeem, while neither authority is lost after a completed conversion. Do not re-elect a winner, credit donations twice, grant loose materials or issue another legitimate reward for the same earning event. Record one migration decision/audit and re-run idempotently. This option is **not approved** by requesting blueprint support.

Take and validate a complete backup before conversion; run a read-only migration report first. Refuse startup on unsupported/future formats. Test on disposable complete-world copies before production. Rollback requires a complete matching pre-migration backup, not reverting only the JAR or one store file. Do not edit existing JARs, archives, saves or milestone JSON in this documentation checkpoint.

## Approval decisions

1. **Initial Hut bootstrap:** Aldric normally exists only after the Hut. Recommend preserving the current protected bootstrap installer behind the single administrator/owner Hut entitlement for fresh worlds; it would be an explicit exception to progressive Aldric work. Alternatively approve a separate safe initial-Aldric lifecycle. Neither is selected silently.
2. **Migration:** approve grandfathering and new-world profile rules, whether unfinished existing worlds may opt in, and whether unused rewards may convert one-for-one. Completed/reserved Lodge handling and any separate old-world Hall access must be explicit; no automatic extra reward is proposed.
3. **Roadmap:** finalize all building unlock/acquisition mappings, profession-project authority, Bank/Storehouse service gates, Town Hall height/anchors/NPC assignments and what replaces the former Tier 12 Hall role. Decide how pending Lodge/road objectives apply to future Hall worlds; do not mechanically change those targets.
4. **Controls:** review proposed bindings, ray anchor/range/height bounds, main/offhand precedence and the public-building approval flow. Numerical performance limits follow measured results, not assumptions.

## Implementation sequence after approval

| Phase | Concrete result | Required verification before checkpoint |
| --- | --- | --- |
| 1 — compatibility and identities | Versioned building/entitlement adapters; old saves load unchanged; approved profile/initial Hut path | All existing 139 unit tests/native lifecycle plus migration fixtures, replay/issue/recovery and dedicated restart |
| 2 — full hologram on one existing building | Held-plan preview, natural targeting, rotation/height, complete roof/footprint and diagnostics | Actual client screenshots/video, scale/input/resource-reload tests; bounded native placement checks |
| 3 — exact site review and construction | Royal Gilded submit/admin approval/begin, same durable project, admin-only coordinate recovery | Two real clients, stale/changed/protected/entity-conflicting sites, simultaneous requests, packet tampering and save interruptions |
| 4 — reviewed Town Hall | Original 19 × 15 two-floor furnished Hall, winner award under new profile, Aldric construction and approved NPC services | Real full progressive build, doors/stairs/navigation/furnishings, prefix/full verification and restart; no early service grants |
| 5 — shared building coverage | Bank/Storehouse/twelve profession building item/definition support as each roadmap entry is approved | Registry/content/entitlement correctness for all 16 required building types; real services tested separately before calling buildings functional |
| 6 — performance and release | Measured large-building rendering/validation, reviewed migration rollout, installable matched client/server JAR | Recorded client/server performance, migration/restart/multiplayer evidence, screenshots, full build and pushed development checkpoint |

Test matrix includes 0/90/180/270°, all height/build-border edges, support holes/overhangs, protected blocks/containers/fluids, entities, overlapping reservations, unloaded chunks without tickets, held/copy/stolen/expired/superseded items, full inventory, permission changes, close/cancel/disconnect/rejoin, approval changes, receipt/journal crash boundaries, save restart at every migration/project phase, damaged prefixes, lost NPCs, accessible colors and normal/narrow screens. Permissioned recovery tools must pass the same site checks and cannot mint a second entitlement.

No code, assets, game tests, builds, new JARs, save migrations or hologram screenshots are produced by this plan. The previous 139-test result is the existing code baseline, not evidence that this design is implemented.
