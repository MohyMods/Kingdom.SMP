# Kingdom SMP delivery roadmap — current authority

Preserve the working project on `rebuild/royal-gilded`; no main merge. The user has tested and approved the hut, Aldric and Royal Gilded visual direction. The ultimate Town Hall is Tier 12. Tier 1 completion advances to Tier 2 and awards the highest contributor the Founders’ Lodge construction privilege, with a named Reeve and included materials. No early Town Hall reward remains approved.

| Milestone | Current state | Completion gate / next action |
| --- | --- | --- |
| Foundation, Java 21 / NeoForge 21.1.265 | Built/tested; user approved | Preserve dedicated-server isolation, strict persistence and reproducible builds |
| Builder’s Hut and Aldric | Working; user tested and approved | Preserve original furnished structure, identity, dialogue, routines and five-minute clickable admin confirmation |
| First Hearth durable inventory transactions | Implemented and tested | Atomic prepared ledger → verified native inventory receipt → committed credit; native saving-failure recovery and repeated restart tested; see validation limits |
| Contributions, exact scores, history and live leaderboard | Implemented and tested | Royal Gilded review/confirm flow, resource caps, stale/replay/race cases and actual client screenshots; two actual localhost clients verified shared iron cap, live invalidation and synchronized ranking; remote/authenticated sessions remain unverified |
| Tier 1 → Tier 2 | Implemented; native-server test passed | Derive tier, completion and permanent winner from canonical accepted history; no unrelated service grants |
| Founders’ Lodge reward delivery/redemption | Implemented and tested | Durable winner-bound token rotation, full-inventory check, exact admin approval, one redemption; old/copy plans cannot redeem |
| Reusable progressive construction | Implemented and tested for Lodge | Real rotated footprint preview, exact-site review, protected reservation, physical Aldric work, saved resume/unloaded pause; native and client evidence |
| Furnished Founders’ Lodge and Reeve | Implemented and tested | Original 782-block furnished Lodge, Mira Ashford, persistent identity/routine, actual milestone/overview services; no bank/trading/Charter grant |
| Server-data milestone catalogue and dependency graph | Implemented; automated/native tests pass | Registered IDs/tags, strict schema, cycles/forward gates, disabled proposals, actual Lodge objective, pinned historical First Hearth, safe reload guards; physical checks in progress |
| Tier 2 → 3 / Royal Storehouse | Revised user quantities remain proposals; reward scope specified | Eight diverse resources + Lodge/road; review final costs/scoring/evidence; secure Quartermaster storage access/capacity before activation |
| Tiers 3–12 resource proposals | Drafted for review, disabled | Availability rationale and ten-player effort estimates in PROGRESSIVE_RESOURCE_BALANCE; verify actual technology/profession/recipe dependencies before enabling |
| Generalized live later-tier contributions | IN DEVELOPMENT / blocked on approved policy | Per-milestone pinned history/transaction adapter, objective approvals, future score weights and winner entitlements; current generic catalogue does not activate these |
| Tier 12 ultimate Town Hall and Elara | Approved final-building scope; not implemented | Separate Tier 12 contribution/blueprint award policy, full furnished hall, verified completion and one steward |
| Representative profession | Definitions missing | Approved 24 quests/XP/ranks, meaningful 2–3 week Master target, three-active cap, preserved inactive progress; validate one before eleven more |
| Bank/economy/markets | Gates and numeric policies missing | Earlier premises proposed independently of final Town Hall; prove coin/balance recovery, budgets, tax/rounding, contracts and escrow before activation |
| Technology/magic integrations | Exact artifacts/policies missing | Verify each supported mod/version and alternative acquisition routes with that mod installed |
| Royal Administration | Hut and construction site approval/recovery implemented | Permissioned confirmed/audited operations alongside their actual services; no decorative dashboard claiming absent controls |

## Current dependency order

The tested Phase A source checkpoint is `7e9968a`; its packaged JAR, screenshots and evidence are in [the checkpoint directory](checkpoints/7e9968a/CHECKPOINT.md). The Founders’ Lodge loop now runs through Mira’s arrival and functional milestone services. Next: review revised eight-resource Tier 2 balance/future scoring, then generalize the live per-tier inventory/history adapter and implement the Royal Storehouse/Quartermaster storage service under approved access rules. Flexible server definitions and dependency validation are implemented; all future proposals remain disabled. Representative profession and separately gated economy remain dependent on exact definitions. Vanilla Nether 5 / End 9 entry gates are implemented and tested. Town Hall/Elara are reserved for Tier 12. Charter remains available after Tier 2 independently; its exact boundary and other service gates remain explicit decisions.

Each checkpoint requires a real build, relevant unit and server tests, persistence/restart evidence, client screenshots, exploit review, clear limitations, installable JAR and a pushed development commit. A declaration or compiling class does not complete a milestone. If execution ends, push verified work and provide an exact continuation prompt without claiming background work continues.
