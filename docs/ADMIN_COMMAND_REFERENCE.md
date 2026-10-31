# Current administrator and testing command reference — partial

The D-023 comprehensive `/kingdom admin` namespace and isolated sandbox are required but **not implemented**. This file documents actual existing commands only; listing a desired command would not implement it. Existing worlds/history must not be reset to accelerate testing.

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
