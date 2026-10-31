# Architecture proposal

> **D-023 — current implementation authority:** The [master directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md) authorizes incremental coding and supersedes the D-021/D-022 review-only hold. New-world Town Hall / existing-world Lodge profiles, owner Hut bootstrap, blueprint-market acquisition, automatic server-valid placement, supports up to four blocks, Tier 3 Bank (no new-world Storehouse), updated profession/economy/quest/admin scope apply. End remains Tier 12. The [full replacement mod snapshot](sources/MOD_SNAPSHOT_2026-10-31.txt) now reports FTB Quests installed. See [supersession and actual status](MASTER_SUPERSESSION_MATRIX.md); old descriptions below are historical where they conflict.

> **D-022 — planning only, current dimension authority:** End unlocks at **Tier 12**, superseding every End-at-9 design. Target schedule: Aether 4; Nether 5; Twilight Forest 6; Otherside 8; reviewed Ad Astra stages 10–11; End 12. No mandatory prerequisite for reaching 12 may depend on End resources, including indirect recipes. Dragon/End exploration belongs after the unlock; **Royal Legacy Monument** remains the proposed final construction reward. Existing code/data/worlds are unchanged. See [dimension schedule, dependency graph and open recipe audit](DIMENSION_PROGRESSION.md). Implementation remains on hold.

> **D-021 — 31 October 2026, current design authority:** Physical blueprint items and full-building crosshair-controlled holograms are approved requirements. Future Tier 1 completion awards the **19 × 15, two-floor Town Hall at Tier 2**, replacing the future Lodge reward subject to separately approved migration. The former Tier 12 Hall requirement is superseded. Existing 0.4.0-dev code, Lodge/Mira, entitlements and saves remain unchanged. **Do not implement yet.** See [construction design](CONSTRUCTION.md) and [approval/migration plan](BLUEPRINT_IMPLEMENTATION_PLAN.md). Service gates and the rest of the building roadmap are not silently changed.

The earlier sections below describe historical design or the existing runtime where they conflict with D-021.


Status: rebuild architecture under S2 implementation authorization. The following boundaries guide incremental code; absent modules are not claimed implemented. One deployable NeoForge mod is recommended initially. Logical modules/packages keep responsibilities separate without premature multi-project Gradle complexity. Use Java 21 and Minecraft 1.21.1; exact dependencies require later verification.

## Proposed layout

```text
Kingdom.SMP/
  docs/
  src/main/java/<approved_package>/kingdom/
    domain/          # IDs, value objects, rules; no Minecraft/client imports
    application/     # use cases, authorization, transaction orchestration
    progression/     # kingdom tiers and community milestones
    professions/     # activity, ranks, entitlement rules
    quests/          # objective tracking, approval, rewards
    contracts/       # profession contract lifecycle
    banking/         # counter access and conversion orchestration
    permissions/     # authorization boundary
    config/          # validated balance/configuration
    economy/         # money, bank, Master pricing
    markets/         # escrow, listings, auctions, buy orders
    commerce/        # NPC budgets, quotas, shops
    technology/      # gate policies and enforcement coverage
    infrastructure/  # counter/town interactions
    construction/    # blueprint registry, site validation, bounded work scheduler
    npcs/            # stable identity, home/building association, dialogue/routine
    persistence/     # repositories, schema migration, recovery
    network/         # validated commands, view data, protocol versions
    integration/     # NeoForge adapter and separate third-party adapters
    administration/  # permission checks, confirmations, audit
    client/          # screens, textures, input, cached view state
  src/main/resources/
    assets/<approved_mod_id>/
    data/<approved_mod_id>/
  src/test/java/...  # pure rule/state-machine tests
  <integration-test location selected after toolchain verification>
```

Package namespace, mod ID, artifact name, license, and testing harness are open. The paths are logical boundaries, not a requirement to create empty placeholder packages. Current implementation structure and tested behavior are recorded in the development log. Avoid splitting every package into a separate published mod. Split Gradle projects only if actual independent builds or dependency isolation justify it.

## Dependency direction

Presentation/network and integration adapters invoke application use cases; use cases depend on domain rules and storage/integration interfaces. Persistence implements those interfaces. Domain logic must not depend on rendering, packets, or third-party classes. Separate optional integration loading from ordinary class initialization. Dedicated-server startup must never load client rendering classes.

Shared services: stable IDs, typed money, time abstraction, validation, permissions, versioned configuration, persistence boundaries, transaction identity, structured audit/recovery records. Do not let UI code mutate storage directly. Each business operation has one entry point used by commands, screens, and adapters.

## Authority and concurrency

Clients submit bounded intentions, not balances, ranks, eligibility claims, or item grants. Validate sender, context, request version, payload size, quantity, permissions, rate, and expected state. Server derives results and rechecks at commit. A nonce or request ID prevents duplicate execution only when durable state tracks it appropriately.

Propose serializing relevant game-state changes through the authoritative server execution context. Off-thread I/O must exchange immutable messages/snapshots and must not access mutable world objects. Exact threading hooks require API verification. Avoid blocking disk/network work on the tick path; asynchronous persistence requires explicit pending, failure, and shutdown handling rather than assuming a task succeeded.

## Persistence decision to resolve before money

| Candidate | Benefit | Limitation |
| --- | --- | --- |
| Minecraft-native player/world persistence | Natural world lifecycle and fewer dependencies | Exact save consistency and schema needs must be verified; no assumed multi-file financial transaction guarantee |
| Embedded transactional database (candidate: SQLite) | Transactions and indexed durable escrow/operation state | Driver/distribution, migration, threading, backup, and crash behavior need verification; does not atomically save Minecraft inventory |
| External database | Shared operational tooling and stronger deployment options | Unnecessary infrastructure likely for ten players; still cannot make inventory saves atomic |

**Recommendation for review:** an embedded transactional store for financial operations, escrow, and recovery records, with one canonical owner for each field; select storage for other aggregates after a small durability investigation. Do not duplicate authoritative balances across database and player data. Do not finalize SQLite or a library before dependency and crash-model validation.

Specify what happens on disk-full, failed commit, process kill, host loss, partial inventory save, restore from backup, and version rollback. Define recoverable states and a reconciliation procedure. Retain only internal records needed for integrity/security; this is not approval for the excluded player-facing Economy Ledger. Snapshot world and any database consistently. Backup retention, recovery-point/recovery-time targets, migration rollback, and restore drills need approval and evidence.

## Data and configuration

Use stable content IDs, explicit schema versions, validated definitions, and migration plans. Keep safe balance parameters configurable while rejecting invalid values and incompatible hot reloads. Gate/quest changes after progress exists require reconciliation rules and logs. Currency, timestamps, counts, and limits need bounded representations. Do not assume all data belongs in a hot-reloadable datapack.

## Integration and operational boundaries

Verify NeoForge 1.21.1 sources and official dependency artifacts before choosing any events, menu types, registration, networking, or test APIs. Keep a compatibility matrix with exact versions and evidence during implementation. Broad claims about Create/Mekanism/magic compatibility are currently unverified.

Admin operations use server permission checks on every execution, exact-target confirmation, reason, before/after values, and an audit entry. Choose permission backend, operator policy, audit access/retention, and destructive-operation recovery before implementing overrides. Log no credentials. Rate-limit malformed input without flooding logs. Metrics should cover transaction failures, denied gates, recovery work, tick time, and bounded queues; no separate monitoring stack is selected.

## Primary risks and required mitigations

| Risk | Consequence | Evidence required before enabling |
| --- | --- | --- |
| Inventory/store save mismatch | Money or item duplication/loss | Fault injection and recovery/restart tests |
| Alternative acquisition/use path | Tier/profession bypass | Per-item route coverage and multiplayer tests |
| Automation without attributable actor | Profession restrictions ineffective | Approved ownership/delegation policy and verified hooks |
| Inactive/shared Master equipment | Price/eligibility bypass | Reviewed transfer/use policy, revalidation tests |
| Concurrent markets and retries | Double sales, charges, or refunds | State-machine, race, replay, disconnect tests |
| Unverified dependencies/client classes | Build or dedicated-server failure | Pinned artifact matrix, real builds and server/client smoke tests |
| Inflation and profession overlap | Progression loses meaning | Approved rates/costs, simulations and season playtests |
| UI state treated as authority | Forged purchase/quest claims | Server validation and hostile payload tests |
| Save/schema changes | Irrecoverable season data | Migration, consistent backup, and restore drills |
| Definition/capacity growth | Tick stalls and storage growth | Bounds, pagination, measured workload tests |


## S3 construction/NPC architecture boundary

First Hearth is no longer unspecified: persistent new-kingdom state starts at Tier 1, and [progression](KINGDOM_PROGRESSION.md) defines exact first resources and score rules. Shared totals, individual credited quantities, ranking evidence, winner record and reward entitlement form authoritative state. Inventory copies are never authority for a second construction.

Separate blueprint definitions, placement validation, site reservations, construction lifecycle/scheduler, building registry, and NPC identity/routine from rendering. Persist construction progress and block-result reconciliation; bound work per tick/concurrent jobs, pause unloaded work, and never permanently force-load chunks. NPC path failure must report/pause rather than mutate progress optimistically.

A completed building is registered only after required-block verification. Completion, permitted service availability, damage status and NPC assignment are separate persistent concepts. A Town Hall counter does not implement banking by itself. Land/protection integration and damage/NPC recovery rules remain explicit decisions; missing adapters must not silently grant permission. See [construction](CONSTRUCTION.md) and [NPCs](NPCS.md).


## Initial source checkpoint

Technical identifiers chosen under S2 autonomy: Java package `com.mohymods.kingdomsmp`, mod ID `kingdom_smp`, artifact `kingdom-smp-core`, version `0.1.0-dev`. Mod metadata retains All Rights Reserved; no redistribution license is granted by this technical default. The template license is retained separately. The implemented packages are `kingdom`, `persistence`, `network`, and `client`, plus common initialization. No empty gameplay-module stubs were generated for absent systems.

Native SavedData is used for this bounded kingdom/contribution state only. The immutable TierOneProgress accounting is serialized there and read into the server snapshot, but there is no mutating donation adapter. This does not settle financial storage or cross-save inventory atomicity. Live item transfers need a separate reviewed recovery protocol.

Exact source and runtime evidence is in [validation](VALIDATION.md). The candidate toolchain has not completed Minecraft artifact resolution; source APIs, real side isolation, and persistent world lifecycle still need executable validation.

Before enabling mutation, unify aggregate/view and contribution revision semantics (the initial snapshot currently exposes the kingdom revision while contribution accounting has its own revision). CAS/transaction checks must use the actual authoritative revision; the read-only checkpoint cannot be used as a donation authorization mechanism.

## Current donation persistence boundary

The old read-only kingdom adapter remains a preserved migration seed. Live accepted contributions, revision, history and first winner come from the canonical ContributionLedger; tier derives as at least 2 when First Hearth completes. Inventory baseline, durable preparation, native removal receipt, and final ledger commit follow CONTRIBUTION_RECOVERY.md. Overview, status, dialogue and the new contribution projection read the same authoritative progress. This resolves the earlier revision mismatch for the enabled donation path. It does not implement money or public construction transactions.
