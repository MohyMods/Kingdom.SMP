# Server milestone data and validation

> D-021 changes future building reward timing: Town Hall at Tier 2, subject to separately approved save migration. Lodge/road/final-Hall references below remain existing runtime or older proposals and need review for the new profile. No resource quantities are newly approved and no executable milestone data changes in this documentation task. See CONSTRUCTION.md and BLUEPRINT_IMPLEMENTATION_PLAN.md.

The running server loads `data/kingdom_smp/kingdom/milestones.json` from server datapacks, after registry tags are bound. The bundled file defines all twelve milestones: only First Hearth has an implemented, approved consumption adapter. All subsequent costs are inspectable **proposals**, with no donation, tier advancement or reward issuance. Editing `approval` to `approved` cannot activate a future milestone. This is an implemented catalogue/dependency framework, not a claim that all twelve donation engines are complete.

## Installation and reload

To customize a server's proposal definitions, create a normal Minecraft 1.21.1 server datapack, for example:

```text
world/datapacks/kingdom-balance/pack.mcmeta
world/datapacks/kingdom-balance/data/kingdom_smp/kingdom/milestones.json
```

`pack.mcmeta`:

```json
{"pack":{"pack_format":48,"description":"Kingdom milestone definitions"}}
```

Copy the bundled JSON as a starting point. Enable the pack with vanilla datapack controls, then `/reload`. NeoForge `OnDatapackSyncEvent` with a null player runs after the new resources and registry tags are installed. Startup also validates the current resource manager. No client-side balance file is trusted or required; the existing Royal Gilded **K → Progression** screen receives twelve bounded server descriptions. Refresh reads updated data. Existing open donation reviews are invalidated on reload, so confirmation requires a fresh quote.

Public read-only inspection: `/kingdom milestones inspect <1..12>`. Operator diagnostics (permission level 2): `/kingdom milestones validate`. Validation reports active First Hearth compatibility separately from future proposal blockers. These commands do not approve costs, manufacture objective credit, edit progression or issue blueprints. There is no arbitrary client payload for editing the catalogue.

## Schema 1

The root has `schema: 1`, `capabilities` (at most 128), and exactly twelve `milestones`, one per tier. Source tier t normally advances to t+1; the Tier 12 finale stays 12. Namespaced IDs are stable and unique; duplicate definitions/fields/ingredients and unknown fields fail. Lists, numeric targets, string lengths, file size and nesting are bounded.

Capability example:

```json
{"id":"kingdom_smp:nether","available_from_tier":5,"requires":["kingdom_smp:overworld"],"after_milestones":[]}
```

`available_from_tier: 0` means undecided, not Tier 0. Capabilities can depend on other capabilities and completed milestone IDs. Reaching tier n automatically depends on completing milestone n−1. Known vanilla capability tiers are independently checked: catalogue text cannot lower Nether 5 or End 9. Unknown mod/profession capabilities remain unverified even if an author writes a tier number.

Milestone fields:

- `id`, `tier`, `next_tier`, `approval` (`approved` or `proposal`), `title`, `reward` (definition ID).
- `scoring_policy`: `legacy_equal_four` for the existing First Hearth contract, or `pending_review`. No future scoring or normalization default is silently selected.
- `requirements`: 1–16 item/tag targets with unique IDs and no overlapping resolved item sets. The current proposal uses eight meaningful targets per later tier, not a mandate to fill sixteen slots.
- `objectives`: up to eight `building`, `admin`, `exploration` or `profession` entries, with stable target, dependencies and optional description. Existing Founders’ Lodge completion is read from the real durable construction project. Other objective evidence/approval adapters are explicitly pending; listing an objective does not mark it fulfilled.

Requirement example:

```json
{
  "id":"kingdom_smp:glass",
  "item":"minecraft:glass",
  "quantity":256,
  "category":"kingdom_smp:refined",
  "requires":["kingdom_smp:smelting"],
  "tracking":"shared_consumed",
  "display":{"name":"Glass","description":"Processed Overworld supply"}
}
```

Use exactly one of `item` or `tag`; tag values omit the `#` marker inside JSON. Targets are whole numbers 1–1,000,000,000. Category is descriptive, **not an implicit score weight**. Optional display text is bounded plain text, not executable components. `tracking` currently accepts only `shared_consumed`, matching the approved irreversible contribution model. It declares intended tracking for future adapters; it does not enable proposed consumption.

## Validation and activation boundaries

Actual item IDs and nonempty item tags are checked against the running registry. Generic matching uses exact registered item identity or the loaded item tag; glass panes do not satisfy glass. Registration alone proves neither a recipe nor an approved technology gate. Conservative independently coded vanilla acquisition profiles add necessary realm/processing prerequisites even when an author omits them. Unknown ingredient paths, missing items, unverified technologies/professions and unknown unlock tiers are explicit blockers.

The graph covers sequential milestone edges, capability dependencies and prerequisites of requirements/objectives. Concrete cycles and forward-access dependencies are reported. Structural cycles or unknown references anywhere, including unused definitions, make the graph invalid. Malformed JSON publishes an unavailable catalogue instead of leaving a silently stale definition active. New donations pause; saved history, pending-operation reconciliation, tiers and entitlements remain intact. Correct the data and reload normally; never remove contribution receipts or journal markers.

First Hearth's existing recovery/receipt protocol and exact four-category arithmetic remain authoritative. Its data definition is checked against the **pinned historical contract** (IDs/quantities/scoring/reward), rather than dynamically reinterpreting recorded donations. Cosmetic descriptions may change; changing active costs or scoring requires an explicit migration with a versioned per-milestone ledger. This is a deliberate compatibility limitation: the schema represents every tier, but arbitrary Tier 1 cost changes are not applied to the existing consumption engine. Future live generalized inventory transactions, definition pinning, objective approval, weighted scoring and per-tier winner issuance still require approved policies and native interruption tests before activation.

No automatic arbitrary-recipe solver, AFK-provenance proof, profession quest catalogue, mod compatibility adapter, Royal Storehouse service or later-tier advancement is claimed. See [the proposed balance table](PROGRESSIVE_RESOURCE_BALANCE.md) and master tracker.
