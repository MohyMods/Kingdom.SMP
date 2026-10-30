# Architecture proposal

Status: proposed boundaries, not code or API commitments. One deployable NeoForge mod is recommended initially. Logical modules/packages keep responsibilities separate without premature multi-project Gradle complexity. Use Java 21 and Minecraft 1.21.1; exact dependencies require later verification.

## Proposed layout

```text
Kingdom.SMP/
  docs/
  src/main/java/<approved_package>/kingdom/
    domain/          # IDs, value objects, rules; no Minecraft/client imports
    application/     # use cases, authorization, transaction orchestration
    progression/     # kingdom tiers and community milestones
    professions/     # activity, ranks, quests, entitlement rules
    economy/         # money, bank, Master pricing
    markets/         # escrow, listings, auctions, buy orders
    commerce/        # NPC budgets, quotas, shops
    technology/      # gate policies and enforcement coverage
    infrastructure/  # counter/town interactions once specified
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

Package namespace, mod ID, artifact name, license, and testing harness are open. The paths are a design example only; no scaffolding is created now. Avoid splitting every package into a separate published mod. Split Gradle projects only if actual independent builds or dependency isolation justify it.

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

Verify NeoForge 1.21.1 sources and official dependency artifacts before choosing any events, menu types, registration, networking, or test APIs. Keep a compatibility matrix with exact versions and evidence once work is authorized. Broad claims about Create/Mekanism/magic compatibility are currently unverified.

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
