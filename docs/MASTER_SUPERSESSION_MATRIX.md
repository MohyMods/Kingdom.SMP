# D-023 supersession, compatibility and implementation status

Authority: exact [master implementation directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md), followed by the [replacement full mod snapshot](sources/MOD_SNAPSHOT_2026-10-31.txt). This is implementation authorization. All previous review-only holds are superseded; missing major prices/quest/economic rules still require decisions, not invented defaults. No main merge.

| Subject | Current approved rule | Actual implementation / next dependency |
| --- | --- | --- |
| Work authorization | Audit, implement and test manageable playable milestones | Coding resumed; retain existing tests and save formats |
| New/old worlds | New worlds: Tier 2 Hall; existing worlds: Lodge awards/projects/NPCs preserved | Versioned profile and new Hall not implemented yet; current legacy runtime remains active |
| Starting Hut | Owner gets one free physical plan; full preview and safe immediate bootstrap; then Aldric | Original protected admin installer exists; owner entitlement/item/bootstrap UI pending |
| Town Hall | Tier 1 winner; 19×15/two floors; public services and named Steward | Lodge only is currently implemented; no silent conversion or extra Hall reward |
| Later plans | Architect market, one primary entitlement per building; first successful buyer only | No market/economy yet. Prices 500–15,000 configurable/reviewed; failed or concurrent losing purchases must not charge |
| Royal Storehouse | Removed for new worlds | Do not implement/award; preserve any historical data. Old disabled draft references are not new authority |
| Bank/auctions | Separate Tier 3 Royal Bank, currency conversion and money transfers; auctions gated at 3 and approved Bank availability | Financial recovery/escrow/mailbox semantics not implemented. No direct item-trading UI restored |
| Placement | Routine admin click unnecessary when complete server validation succeeds | Legacy proposal/admin flow retained in this first hologram slice; automatic flow/four-block supports pending |
| Foundations | Safe non-destructive supports up to depth 4; no unsafe excavation | Existing validator still requires prepared solid support; red unsupported cells, not automatically filled |
| Claims | Respect all installed protections; ambiguous permissions fail safely | Exact FTB Chunks2101.1.22 + OPAC0.33.0 adapters tested together; actual owner must be online/in dimension; full-pack/providers unverified |
| Hologram | Full model/roof, crosshair, 90° rotation, configurable height, remappable controls; green/red/amber plus diagnostic highlights | Implemented for the current Lodge definition; actual rendering QA/performance and broader building coverage are separate acceptance |
| Naming/titles | New NPC display naming by holder; preserve existing stable NPC identities; honorary titles confer no admin power | Existing Aldric/Mira preserved; new naming/title system pending |
| Tier 3 additions | Bounty Hall and Fisherman's Wharf explicitly approved | No services implemented. Proposed plan prices 1,500/1,000 are not finalized; fishing events are this named scope, not blanket restoration of excluded weekly events |
| Aircraft | Tier≥6 AND verified Aerodrome completion; type schedule remains proposed | Immersive Aircraft version reported, adapter/API/live tests pending; no aircraft entitlement granted by rank/building alone |
| Waystones | Tier 9, no mandatory Sanctuary | Version reported; actual routes/config/API unverified |
| Dimensions/finale | Aether4, Nether5, Twilight6, Otherside8, space10–11, End12, Legacy Monument capstone | Vanilla runtime Nether5/End12 is tested; other mod routes and inactive historical catalogue metadata still need coordinated review; no End-only path to reach12 permitted |
| Professions | Three active; preserve inactive; 24 quests each, replacement15k; matching building Master purchases | Quest/XP definitions and switching details not sufficiently defined; no filler quests or invented perk catalogues |
| FTB Quests | Primary journal, server-authoritative synchronized kingdom/personal/profession/optional chapters | Full replacement list **reports installed 2101.1.37**. Earlier 'not confirmed' status superseded. Artifact/API/quest load/team/reward tests still absent |
| Administration | Real `/kingdom admin` sandbox/test operations alongside features, permission/confirmation/audit, no live-history contamination | Existing hut/lodge/milestone commands only; complete admin namespace/test override/sandbox not implemented |

## Baseline and pack evidence

Baseline source 7ce19a8 / packaged 86828d8: 139 JUnit tests, one substantial native lifecycle, actual local client/dedicated persistence and data-reload evidence. Subsequent d2b79a1/d2b6ede were Markdown only. New 0.5 source adds held-Lodge placement and guards; report its actual new test/UI results in VALIDATION and the checkpoint.

MOD_INTEGRATION_MATRIX.csv lists every name/ID/version in the new user snapshot. These are not pins to downloaded/checksummed releases. Current development runs load Kingdom SMP with NeoForge, not the supplied full pack. Do not infer native NeoForge compatibility from a reported name/version, including Ad Astra or Connector/bridged dependencies. Audit real JAR metadata, dependencies, registry IDs, source/API and observed server/client behavior before adapter claims. The actual heap crash report and 4-GB configuration were not supplied with the replacement list; do not blame an individual mod.

Immediate Phase B sequence: tested held-Lodge preview and pinned claim adapters → versioned profile/entitlement preservation and owner bootstrap → new-world Hall → safe supports/automatic confirmation and generalized construction → early economy/Architect market. Keep quest identifiers and administrative test boundaries in those designs; no fake integration or placeholder commands.

Claim artifact audit now has downloaded exact FTB Chunks binary/API signatures and hash-verified OPAC binary/sources; the FTB sources URL returned HTTP 403. See [claim compatibility evidence](CLAIM_COMPATIBILITY_AUDIT.md). Actual adapters/live compatibility remain unverified.

0.5.1 supersedes the historical presence-only refusal: both exact claim APIs and their five-artifact subset passed normal dedicated lifecycle tests; real client initialization/owned preview/review also ran. CLAIM_COMPATIBILITY_AUDIT describes current online-owner constraint and remaining pack/provider limits.
