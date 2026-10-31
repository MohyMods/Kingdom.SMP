# Kingdom SMP delivery roadmap

Current authority: [D-023 master directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md), followed by the [replacement full mod list](sources/MOD_SNAPSHOT_2026-10-31.txt). Earlier planning-only holds are superseded. New worlds require the Tier 2 Town Hall; existing Lodge records remain intact. End is Tier 12. Proposed resource costs and unresolved major economic/quest policies remain inactive.

| Milestone | Actual status | Next acceptance |
| --- | --- | --- |
| Java 21 / MC 1.21.1 / NeoForge 21.1.265 | Built and tested | Preserve isolation, saves and full builds |
| Furnished Builder's Hut / Aldric / Royal Gilded dialogue | Implemented; user approved | Preserve five-minute clickable administrator confirmation and NPC identity |
| First Hearth contributions, normalized leaderboard/history, winner and Tier 2 | Implemented and tested | Preserve durable receipts, exact limits, recovery and prior awards |
| Legacy Lodge plan / construction / Mira | Implemented and tested | Preserve one-use entitlement, exact site approval, physical saved construction and grandfathered worlds |
| Held Lodge full hologram / natural review | Implemented and physically tested, source deaf558 | Whole model/roof, remappable rotation/height, bounded read-only server probes, red/green/pending states; GPU cache and normal/narrow UI tested |
| Raised-Hut construction route recovery | Fixed and native/physically tested | Loaded supported waypoints; same restarted project completed 782/782; no terrain edit or chunk tickets |
| Server-data milestone catalogue | Implemented and tested | Registry/dependency/cycle checks, safe reload, historical First Hearth pinned; later costs disabled |
| Replacement 277-entry inventory | Recorded; compatibility incomplete | Exact FTB Chunks binary and OPAC binary/sources downloaded/audited; full pack not installed |
| FTB Chunks + OPAC permission adapters | Tested exact five-mod subset | Independent vetoes, cross-chunk volume, revocation/resume/offline pause and real client initialization; owner online/in dimension required; full pack/providers unverified |
| Versioned new-world Hall / legacy Lodge profiles | Not implemented | Classify/pin safely; no conversion, deleted records or second historical reward |
| Owner physical Hut plan/bootstrap | Not implemented | Durable owner entitlement, whole preview, approved safe immediate bootstrap; then Aldric |
| 19×15 two-floor Town Hall / Steward | Not implemented | Original furnished structure, reusable engine, named NPC/service gates; no legacy Lodge replacement |
| Four-block non-destructive foundations / automatic valid-site authorization | Not implemented | Full server validation and protection at confirm/write; retain old checks; no routine admin click in completed new flow |
| Architect first-buyer market / pre-Bank payment | Not implemented | One primary entitlement, atomic stock/charge, reviewed 500–15k building prices and recovery |
| Tier 3 Royal Bank / economy / auctions / transfers | Not implemented | Approved service gates; transaction/escrow recovery and unresolved exact policies |
| Tier 3 Bounty Hall / Fisherman's Wharf | Approved scope; not implemented | Their real services; proposed blueprint prices remain unapproved |
| Later contribution/resource tiers | Proposed, inactive | Diverse acquisition; no inaccessible or circular prerequisites; approve balance/scoring/objectives before activation |
| Twelve professions / 288 quests | Not implemented | Approved definitions/XP/rank data; validate one representative profession first; no filler quests |
| FTB Quests journal integration | Reported installed, not integrated | Exact 2101.1.37 artifact/API, real chapters/team/personal sync and no duplicate reward paths |
| Vanilla realm entry | Nether5 / End12 tested | Unknown mod routes, already-inside policy and actual Tier12 arrival remain unverified |
| Aether4 / Twilight6 / Otherside8 / space10–11 / Waystones9 | Not integrated | Exact artifacts, recipes, alternate routes and non-End prerequisites |
| Aerodrome / aircraft | Not implemented | Tier≥6 AND completed Aerodrome; actual Immersive Aircraft adapter, reviewed type schedule |
| Royal Legacy Monument / End finale | Approved scope; not implemented | Exact construction/award/participation policy; no End resource needed to reach12 |
| Royal Administration sandbox | Partial existing commands only | Durable isolated test mode and real permissioned/confirmed/audited controls alongside features |

The Royal Storehouse is removed from **new-world** progression by D-023. Old inactive catalogue references are historical, not authority to award it. Preserve any legacy records. All other unresolved unlock gates remain separate; no building name silently grants a service.

Continue: **versioned world profiles and owner bootstrap → new-world Hall → safe supports/automatic authorization/generalized structures → Architect/early economy**, expanding actual admin/test and quest identifiers with each service. Full third-party APIs/live compatibility must be tested before marking integration complete.

Visual baseline checkpoint: 0.5.0-dev/protocol6, **142 JUnit tests, one substantial native lifecycle passed**, actual client full-model/rotation/height/review/resource-reload/scaling and full-process resumed construction; normal dedicated startup/NPC counts/save/stop. No new two-client preview race or full-pack test. [Validation](VALIDATION.md) · [checkpoint download/evidence](checkpoints/deaf558/CHECKPOINT.md) · [continuation](CODEX_CONTINUATION.md).

0.5.1 adds the verified optional claim adapters on top of that 0.5 preview baseline. See CLAIM_COMPATIBILITY_AUDIT and current delivery evidence; do not deploy the historical 0.5.0 binary expecting these adapters.

Latest source c0889d3 / 0.5.1-dev passed full142JUnit, core native lifecycle and separate actual two-claim normal-dedicated lifecycle. Real legacy construction with both mods survived in-progress process restart and completed782/782 with Mira. [Latest download, screenshots and evidence](checkpoints/c0889d3/CHECKPOINT.md). Next dependency: versioned new-world Hall versus legacy Lodge profiles and owner bootstrap; the complete mod is still unfinished.
