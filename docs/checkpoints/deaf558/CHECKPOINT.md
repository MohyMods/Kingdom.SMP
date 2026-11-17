# Kingdom SMP — held blueprint development checkpoint

Source **deaf5589a0804c231f118ab0aaad5fc5245788cc**, branch `rebuild/royal-gilded`. **0.5.0-dev · Minecraft 1.21.1 · NeoForge 21.1.265 · Java 21 · protocol 6**. No main merge.

[Download JAR](kingdom-smp-core-0.5.0-dev-deaf558.jar?raw=true) · [Checksums](SHA256SUMS.txt) · [Execution evidence](validation-evidence.zip)

JAR SHA-256: `aeefb97835bfd0c7e2ed3b60438c57d5e877b425edc2e87e8169f4392c81a382`

**Core test build; not full-pack-ready.** Your replacement list contains 277 reported mod entries and supersedes the old tail. FTB Chunks or OPAC presence currently refuses new sites and pauses construction until verified permission adapters exist. This build must not be presented as working construction in that full pack. Existing world records/buildings/NPCs are preserved; no claims or player structures are cleared. Exact claim artifacts are now downloaded/audited, but no adapter is implemented. [Compatibility evidence](../../CLAIM_COMPATIBILITY_AUDIT.md).

## What changed

Holding the current authenticated **legacy Lodge** plan renders its complete translucent block model/roof and exterior footprint. Aim with the crosshair, rotate in quarter turns, adjust height and use the item to open the exact Royal Gilded server review. Green requires a fresh matching server result; red refuses placement and highlights up to 32 collision/support cells of each kind; amber is pending. Probes are bounded/rate-limited and cannot replace an outstanding review or create a reservation. The immutable GPU model cache holds at most two rotations and releases buffers on eviction, context loss and resource reload.

Fixed Aldric's raised-Hut intermediate route using only already-loaded supported columns. The actual same project survived process restart, resumed physical work and completed 782/782, introducing Mira. This waypoint search is **not automatic construction foundations**. Vanilla End entry now requires Tier 12; Nether remains Tier 5. Unknown mod teleport routes/imported already-inside players are unverified. The five-minute clickable Hut confirmation and existing contributions/recovery remain included.

**Still unfinished:** owner Hut physical plan, versioned new-world Hall profile, the two-floor Town Hall/Elara, automatic site authorization/four-block supports, Architect/payment systems, bank/economy/professions/quests/admin sandbox and actual third-party integrations. The preserved legacy flow still requires administrator site approval. No new-world Hall migration, extra reward, later cost or profession unlock was activated. Automatic approval review rejected unapproved catalogue substitutions; those balance changes were not executed and the inactive catalogue is labeled historical.

## Install and test

Back up the **whole world**, including playerdata, durable kingdom/contribution/construction records and identity/receipt markers. Replace the previous Kingdom JAR with this single JAR on all clients and the server. Protocol 6 must match. Use a separate core-only test profile for this checkpoint; it does not require removing protections from your live pack.

1. Existing legacy winner: press **K → Construction** and claim/recover the legitimate plan. An old copy or ordinary `/give` item is not valid authority. Already redeemed plans cannot build again; test in a separate fresh fixture if your reward is already used. New-world Hall behavior is not implemented yet.
2. Hold the plan over loaded, empty, supported Overworld terrain. **R** rotates clockwise, **Shift+R** reverses; **Page Up/Down** change height. Remap controls under Minecraft's Kingdom SMP category. Client TOML controls targeting reach and height offset bounds.
3. Observe the complete green model on valid ground. Raise onto unsupported terrain for red/orange diagnostics; lower to recover green. Existing structures, entities, terrain, limits, reservations and authority still undergo server checks.
4. **Right-click / Use → review → Confirm** submits the exact site. Retained legacy Royal Admin tab approves that exact proposal; winner then reviews/confirms construction. No coordinates or UUID entry is required in the ordinary held-plan flow. Moving/changing permissions after review is revalidated; failed placement does not redeem the entitlement.
5. Watch Aldric work, unload/reload and restart the test server. **K → Construction → Project** shows saved progress and completion. The Lodge introduces Mira with milestone/overview services; no bank/trading/Charter grant follows.

Owner Hut/bootstrap and the full administrative sandbox remain pending. The retained permission-level-2 `/kingdom hut preview` command has clickable, player-bound confirmation with a five-minute expiry; see [actual commands](../../ADMIN_COMMAND_REFERENCE.md).

## Executed validation

Full Gradle build: **BUILD SUCCESSFUL, 142 JUnit tests, zero failures/errors/skips**. Expanded isolated native lifecycle: **all 1 required GameTests passed**, including current-held/owner/copy validation, collision/support diagnostics, unchanged outstanding review, loaded waypoint regression and the full prior contribution/recovery/redemption/construction/NPC suite.

Actual software-GL Minecraft client: whole model/roof, four rotations/cache eviction, height red/green, natural exact-site review, normal/narrow layouts, hand switching and F3+T resource reload. A complete copied-world legacy project survived full process restart, resumed at production cadence and completed all782blocks. A normal dedicated-server copy started, loaded Tier2/revision6, counted exactly one Aldric and one Mira, flushed and saved/stopped successfully. No real client joined that dedicated run. No new two-client preview race, authenticated remote session, full-pack or maximum-building performance claim. Nonfatal Authlib service refusals and no audio hardware are host limits. Spot software-GL allocation/FPS observations are documented in VALIDATION, not a controlled benchmark.

## Actual Minecraft screenshots

- [Full green hologram](hologram-valid.png) · [rotated model](hologram-rotated.png).
- [Unsupported height highlighted](unsupported-height.png).
- [Smaller client window](hologram-narrow.png) · [exact Royal Gilded placement review](placement-review-narrow.png).
- [Resource reload retains the model](resource-reload.png).
- [Actual completed Lodge exterior](completed-lodge-exterior.png) · [furnished interior and Mira](completed-lodge-interior.png).
- [Saved construction resumed](saved-construction-resumed.png) · [782/782 completion](construction-complete.png).

All are actual frames; exterior/interior inspection uses a spectator camera in the completed copied fixture. Air-only supports and camera pillars are isolated QA staging, not blueprint geometry or live-world changes. Original completed worlds were not replaced.

## Exact continuation

Continue Kingdom SMP from source deaf558 or its newer delivery commit on rebuild/royal-gilded. Read CODEX_CONTINUATION, MASTER_IMPLEMENTATION_DIRECTIVE, replacement MOD_SNAPSHOT and CLAIM_COMPATIBILITY_AUDIT. Implement verified FTB Chunks2101.1.22 plus OPAC0.33.0 permission adapters with actual allowed/denied/revoked/concurrent/restart tests before removing the presence guard. FTB source download returned403 while its exact binary and OPAC binary/sources succeeded; use verified artifacts/APIs rather than repeated retries. Preserve all saves, historical rewards, receipts, NPC identities and collision/reservation checks. Then implement versioned new-world Hall versus legacy Lodge profiles and the owner Hut plan/bootstrap, followed by the reviewed 19×15 two-floor Hall, safe supports and automatic valid-site authorization. Keep later proposed costs disabled, test/build/physically inspect, push checkpoint/JAR/screenshots, never merge main.
