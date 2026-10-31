## Commerce and Tier 3 testing (0.7.0)

Real commands: `/kingdom commerce balance`, `/kingdom commerce open`; `/kingdom admin economy inspect <online-player>`; permission4 `/kingdom admin economy set <player> <coins>` then append `confirm <reason>`; `/kingdom admin milestone inspect`; permission4 `/kingdom admin milestone road` then append `confirm <reason>` after inspecting the actual connection. These mutate real journals, not simulated status. Normal trading/coin conversion requires Survival/Adventure, empty cursor, native inventory, proximity and the registered completed service building. Admin adjustments are audited. Do not use live-world resets to test exclusivity; use the isolated native fixtures.

Native commands: `JAVA_HOME=/jdk21 python3 scripts/test-claims.py --namespace kingdom_smp_bank_tests` runs the full Hut/Hall/earning/Farmer/Tier3/Bank loop with two embedded test peers, actual claim mods and controlled construction acceleration. It is not two real graphical clients. `scripts/test-runtime.sh -PkingdomTestNamespace=kingdom_smp_geometry_tests` validates the remaining workshop geometries in all rotations.

## Fresh-world owner and Hall diagnostics (0.6.0)

`/kingdom admin owner <online-player>` reviews the permanent free-Hut owner assignment; repeat with `confirm` to pin it. Permission level 4 is required. Integrated fresh worlds infer their actual owner. Legacy worlds reject reassignment and never receive this additional entitlement.

`/kingdom admin inspect` now reports the pinned progression profile/owner along with the actual journals. `/kingdom admin construction inspect <project-UUID>` handles Hut, Hall and preserved Lodge projects, including the loaded required-block prefix and resident identity. `/kingdom admin validate` invokes real catalogue validation. Normal holders use K → Construction and physical plans; coordinate commands remain operator recovery/debugging tools.

# Current administrator and testing command reference — partial

The complete administrative suite and sandbox remain unfinished. The D-024 `/kingdom admin` namespace now provides actual operator diagnostics; it does not provide tier/economy/profession overrides. This file documents actual existing commands only; listing a desired command would not implement it. Existing worlds/history must not be reset to accelerate testing.

| Registered path | Purpose | Access / safety |
| --- | --- | --- |
| `/kingdom status` | Read attained tier/revision | Read-only |
| `/kingdom milestones inspect <1..12>` | Inspect actual loaded resource proposal | Read-only; future costs disabled |
| `/kingdom milestones validate` | Report catalogue/prerequisite blockers | Permission level 2; does not activate costs or give tiers |
| `/kingdom hut preview <x> <y> <z> <rotation> <reason>` | Existing bootstrap site review | Permission level 2, loaded/clear/supported/protection checks; clickable confirmation expires in 5 minutes |
| `/kingdom hut confirm <token>` | Confirm the current exact Hut review | Player-bound, permission/site/token checks; ordinary future workflow will use the owner plan |
| `/kingdom hut status` | Inspect current Hut | Existing command; see hut design |
| `/kingdom hut name <name>` | Rename existing Aldric display | Existing admin check, identity preserved |
| `/kingdom hut resume <id> <reason>` | Inspect/resume original reservation | No blind replacement or second NPC |
| `/kingdom lodge open` | Open existing plan/project interface | Server gates each mutation by actual owner/permissions/state |
| `/kingdom lodge preview <x> <y> <z> <rotation>` | Coordinate site debugging/review | Now permission level 2; ordinary held-plan use pre-fills the same server review without typing coordinates |

No balance/tier/rank grants, auction tests, shop restock, aircraft/Waystone overrides, FTB quest reset or test blueprint minting are registered in this slice. Implement these in a durable explicitly isolated test mode alongside their services. Existing native GameTest fixtures are isolated server worlds, not a player-facing admin sandbox.

0.5.1 optional claims: site checks now query exact FTB Chunks2101.1.22 and OPAC0.33.0 APIs together. Under claims the actual authorizing owner must be alive, non-spectator and online in the building dimension; other versions, unready/failed APIs and denied locations refuse. This adds no bypass/reset/claim-ownership command. See CLAIM_COMPATIBILITY_AUDIT for test scope.

## D-024 operator diagnostics

Permission level2 or server console: `/kingdom admin inspect` reports actual tier/revision, Hut/NPC identity, journal revision/quarantine, up to10project IDs/states and installed FTB/OPAC/Quests versions. Installation is not an integration claim. `/kingdom admin construction inspect <project UUID>` reports authoritative identity/placement/NPC state and checks the saved block prefix only if chunks are already loaded. Unknown IDs produce a readable refusal. `/kingdom admin validate` runs the existing catalogue validator without enabling costs. These inspection commands require no destructive-action confirmation because they do not perform repairs or grants. Examples: run inspect, copy a project ID, then run construction inspect with that ID. All other documented Hut/recovery paths remain available.
