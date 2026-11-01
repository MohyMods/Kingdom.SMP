# Production feature inventory — continuous implementation

Authority: [final production directive](sources/FINAL_PRODUCTION_DIRECTIVE_2026-10-31.md). Source baseline 9fb65ec; the fresh-world settlement checkpoint follows it. Percentages are **explicit coarse acceptance slices**, not estimates of engineering hours or a release-completion claim. A tested legacy slice does not satisfy the new-world Hall requirement.

| Subsystem | Verified slices | Percentage | Observed scope |
| --- | --- | --- | --- |
| Platform / build | 3/3 | 100.0% | Java21, NeoForge, build verified |
| Legacy Tier1 gameplay | 6/6 | 100.0% | Hut, donations, history, winner, Lodge/Mira, restart verified |
| New-world settlement | 4/4 | 100.0% | Profile, owner plan, furnished Hall, Steward; executed scope and restart evidence in checkpoint report |
| 12-tier progression | 2/11 transitions | 18.2% | Tier1→2 and fresh-profile Tier2→3 resources/road; later transitions, borders and FTB integration missing |
| Blueprint system | 9/10 | 90.0% | Prior8slices plus real exclusive paid marketplace; four-block supports pending |
| Public building services | 3/7 | 42.9% | Hut, Hall/exchange and Bank; four later public buildings missing; legacy Lodge preserved |
| Economy | 4/5 | 80.0% | Currency, Bank, supported vanilla escrow/mail and all three auction modes verified; four-resource dynamic shops verified; active profession allowances and daily/weekly native delivery contracts verified; long-term balance remains open |
| Professions | 0/12 | 0.0% | Canonical three-slot roster, paid switches, preserved inactive progress and active allowances verified; no complete profession yet |
| Profession quest content | 0/288 | 0.0% | No approved quest implementations |
| FTB campaign | 0/1 | 0.0% | Nine-task read-only settlement projection loads with exact FTB Quests; real team/client completion and full campaign remain unverified |
| Integration enforcement | 3/8 | 37.5% | Pinned claims, vanilla Nether, vanilla End; other gates missing |
| Administration | 1/4 | 25.0% | Unified inspection implemented; audited economy balance/road controls also implemented; broad sandbox/recovery and profession controls missing |
| Release acceptance | 0/1 | 0.0% | 1.0.0 not ready |

Critical path: completed commerce/Bank delivery → auction/item escrow and dynamic economy → remaining buildings/professions/tier gates and safe supports → economy/buildings/professions/quests and remaining tiers → full-pack acceptance. Build actual administrative inspection/recovery paths alongside each service. Existing full completion tracker remains the detailed inventory; this table does not mark entire systems complete.

D-024 permits documented proposed versioned numeric defaults where unspecified, while preserving fixed approvals. It does not authorize silently enabling historical drafts, inventing missing signature abilities as approved, or labeling a partial build1.0.0. Full277pack artifacts/configs and recipe closure remain unverified.

Slice definitions: platform=JDK/build/runtime; legacy=Hut, donations, history/ranking, winner, Lodge/resident, restart; new settlement=profile, owner blueprint, Hall, Steward; blueprints=authentication, full model, controls, saved progression, claims, profile-specific catalogue, automatic authorization, owner bootstrap (implemented/tested), market is implemented; four-block supports remain missing; public buildings=Hut, Hall, Bank, Bounty Hall, Wharf, Aerodrome, Monument; economy=currency, shops/contracts, bank, escrow/mailbox, auctions; integration groups=claims, vanilla Nether, vanilla End (verified), versus other dimensions, aircraft, Waystones, technology/magic, quests (unverified); administration=inspection (verified), versus sandbox, comprehensive economic recovery and profession controls (missing). These figures measure only the named coarse slices, not every acceptance item within each group.

0.7.0 evidence supersedes the older missing-market slice: actual paid Farmer and Bank plans, wallet/native inventory checkpoints and freshTier3 are implemented. Premises do not count as completed profession gameplay. Bank/new Tier2 legacy rollout, long-term journal migration and full-pack acceptance remain limitations. Percentages remain coarse named acceptance slices, not release readiness or engineering-hour estimates.

0.8 adds a tested fixed-price auction slice and native vanilla item escrow/mail. The auction slice remains partial: timed bidding, anti-sniping and buy orders are absent. D-029 explicitly approves auction taxes permanently leaving circulation, with audit totals retained. Public NPC access requires deliberate claim configuration; full-pack capture-item compatibility remains unverified.

0.9 adds funded timed bids, private outbid refunds, rolling anti-sniping and audited isolated deadline tests. Buyorders/partialfills remain absent, so the auction slice is still partial. Rates remain5%/10%, permanently removed perD029. All full-release gaps remain visible.

0.10 adds funded buy orders, exact partial fills and unspent cancellation/expiry refunds. The coarse auction slice is now verified in supported vanilla scope; it does not imply Master gear, modded/container ownership compatibility or long-term economy balance. Dynamic NPC prices/shops/contracts remain the next economy dependency. Completed/reserved Blueprint details now use the scrollable body at narrow scales. All other release gaps remain open.

0.11 verifies marginal four-resource NPC prices, finite shared daily stock, no positive NPC roundtrip and recoverable Tier2 shop deliveries while retaining Bank-gated auction mail. The coarse economy count stays4/5: profession bonuses/contracts remain absent. One stress fixture lost pre-handover crop blocks and paused correctly; strict in-progress crop-prefix recovery/repair remains a release limitation and is not bypassed or auto-regenerated.

Continuous implementation evidence: full Gradle build309JUnit passed (`work/production-contract-verified-build.log`). Exact claims-mod dedicated crop recovery suite passed (`work/production-crop-native.log`); profession/contract lifecycle passed (`work/production-profession-native-counters.log`). The latter executes free enrollment, allowance, missing-premises denial, stale inventory/replay refusal,64daily/512weekly actual item consumption,880fundedcoinmail and reopen. Dark Farmer construction pauses before invalid crop placement; real owner seed repair restores an already-recorded crop prefix, consuming seeds. Claims/reservations remain enforced. Optional FTB Quests2101.1.37 real dedicated startup loaded one chapter/nine tasks/zero rewards; all bindings inspected. No graphical verification or full-pack claim for this batch. Farmer content/ranks are user-approved D-034; objective engine still pending.
