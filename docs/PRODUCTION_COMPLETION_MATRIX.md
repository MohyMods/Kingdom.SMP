# Production feature inventory — D-025 / 0.6.0

Authority: [final production directive](sources/FINAL_PRODUCTION_DIRECTIVE_2026-10-31.md). Source baseline 9fb65ec; the fresh-world settlement checkpoint follows it. Percentages are **explicit coarse acceptance slices**, not estimates of engineering hours or a release-completion claim. A tested legacy slice does not satisfy the new-world Hall requirement.

| Subsystem | Verified slices | Percentage | Observed scope |
| --- | --- | --- | --- |
| Platform / build | 3/3 | 100.0% | Java21, NeoForge, build verified |
| Legacy Tier1 gameplay | 6/6 | 100.0% | Hut, donations, history, winner, Lodge/Mira, restart verified |
| New-world settlement | 4/4 | 100.0% | Profile, owner plan, furnished Hall, Steward; executed scope and restart evidence in checkpoint report |
| 12-tier progression | 1/11 transitions | 9.1% | Only Tier1 advancement is live |
| Blueprint system | 8/10 | 80.0% | Auth, model, controls, progress, claims, profile-specific definitions, owner bootstrap, automatic validation; supports and market pending |
| Public building services | 2/7 | 28.6% | Hut/Aldric and Hall/Steward kingdom services; five later buildings missing; legacy Lodge preserved |
| Economy | 0/5 | 0.0% | Currency, shops, bank, escrow/mailbox, auctions missing |
| Professions | 0/12 | 0.0% | All profession gameplay missing |
| Profession quest content | 0/288 | 0.0% | No approved quest implementations |
| FTB campaign | 0/1 | 0.0% | Reported mod, no tested campaign |
| Integration enforcement | 3/8 | 37.5% | Pinned claims, vanilla Nether, vanilla End; other gates missing |
| Administration | 1/4 | 25.0% | Unified inspection implemented; sandbox, economy and profession controls missing |
| Release acceptance | 0/1 | 0.0% | 1.0.0 not ready |

Critical path: verified Hall delivery → durable payment/mailbox/Architect → safe supports → economy/buildings/professions/quests and remaining tiers → full-pack acceptance. Build actual administrative inspection/recovery paths alongside each service. Existing full completion tracker remains the detailed inventory; this table does not mark entire systems complete.

D-024 permits documented proposed versioned numeric defaults where unspecified, while preserving fixed approvals. It does not authorize silently enabling historical drafts, inventing missing signature abilities as approved, or labeling a partial build1.0.0. Full277pack artifacts/configs and recipe closure remain unverified.

Slice definitions: platform=JDK/build/runtime; legacy=Hut, donations, history/ranking, winner, Lodge/resident, restart; new settlement=profile, owner blueprint, Hall, Steward; blueprints=authentication, full model, controls, saved progression, claims, profile-specific catalogue, automatic authorization, owner bootstrap (implemented/tested), versus supports and market (missing); public buildings=Hut, Hall, Bank, Bounty Hall, Wharf, Aerodrome, Monument; economy=currency, shops/contracts, bank, escrow/mailbox, auctions; integration groups=claims, vanilla Nether, vanilla End (verified), versus other dimensions, aircraft, Waystones, technology/magic, quests (unverified); administration=inspection (verified), versus sandbox, economic, profession controls (missing). These figures measure only the named coarse slices, not every acceptance item within each group.
