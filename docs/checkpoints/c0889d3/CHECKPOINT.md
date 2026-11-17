# Kingdom SMP — claim-aware physical blueprint checkpoint

Source **c0889d3f314ee66f75f56041ec85a87afa910905**, branch `rebuild/royal-gilded`. **0.5.1-dev · Minecraft 1.21.1 · NeoForge 21.1.265 · Java 21 · protocol 6**. No main merge.

[Download JAR](kingdom-smp-core-0.5.1-dev-c0889d3.jar?raw=true) · [Checksums](SHA256SUMS.txt) · [Execution evidence](validation-evidence.zip)

JAR SHA-256: `bd5500f33471a55f1ae93cccaa60f6caeca2c2c27a7d95abb5301558ef041f9a`

This continues the existing project and replaces the previous presence-only construction refusal with actual optional **FTB Chunks2101.1.22 + OPAC0.33.0** adapters. The full replacement 277-entry snapshot is now the compatibility baseline. **The complete 277-mod pack has not been tested**; the five exact claim-related artifacts and default OPAC providers were loaded together for dedicated/client verification. FTB Quests remains reported but unintegrated.

## Actual behavior

Both real claim systems must permit the full footprint, support and exterior work area. Each progressive step rechecks authority; changed claims pause before another write, and restored permissions resume the same project. Hut setup also checks each block. Existing collision, support, entity, reservation, world-limit, token, one-use redemption, recovery and cancellation checks remain. No claims, bypass flags, fake-player permissions or forced chunks are granted.

With either claim mod installed, the authoritative owner must be **alive, non-spectator, online in the building dimension**. Logout or changing dimensions pauses safely. This is an implementation constraint, not finalized offline-automation balance. Other versions, unready managers, failed APIs and unresolved actors refuse safely. Under claims the native place event uses that actual owner; Aldric remains the physical worker. Ordinary no-claim event behavior is preserved.

Includes the whole-model held Lodge hologram, crosshair placement, remappable R/Shift+R rotation and PageUp/Down height, Royal Gilded confirmation, five-minute clickable Hut confirmation, durable First Hearth contributions/leaderboard/Tier2, preserved Lodge/Mira loop, supported route fix and vanilla End12/Nether5 gates. Unknown mod teleports remain unverified.

## Install and test in Minecraft

Back up the **whole world** including playerdata, kingdom receipts/journals/identity markers, claim/team data and configs. Test a copy first. Keep **one** Kingdom SMP JAR; remove older Kingdom0.1/0.5 copies rather than installing duplicates. Use this matching JAR on server and clients with Java21/NeoForge21.1.265. Third-party dependencies are not bundled.

1. Existing legacy winner: **K → Construction → Claim/Recover**. Hold the current legitimate unused plan. Already redeemed rights cannot be claimed again; use a separate test world when testing a completed award. New-world Town Hall behavior is not yet implemented.
2. Aim at empty, loaded, solid supported Overworld ground. **R / Shift+R** rotate, **PageUp/Down** adjust height; remap keys in Kingdom SMP controls. Green requires a fresh matching server decision; red refuses; amber waits.
3. A site in claims needs permission from **both** systems over its whole volume, support and work post. Existing claim tools/configs remain in control. Never remove protections from a live world to bypass a refusal.
4. **Use/right-click → review → Confirm** submits the exact site. The preserved legacy Royal Admin approval phase remains. Winner reviews/confirms construction separately; the plan is redeemed once and materials are included in the project.
5. Watch Aldric and **K → Construction → Project**. Revoked permissions, offline owner or unloaded work pause safely. Restore legitimate access/load and remain in the building dimension to resume. No second reward is issued. Whole-world restart preserves the same project/claims.

Owner Hut item/bootstrap, versioned new-world Hall, two-floor Town Hall/Elara, automatic authorization/four-block supports, Architect market, economy/professions/FTB journal/admin sandbox and later tiers remain unfinished. This is not a complete season release and does not convert old Lodge worlds. Later costs remain inactive historical drafts.

## Executed validation

Full build **142 JUnit tests, zero failures/errors/skips**. Separate core lifecycle **all 1 required GameTests passed**. Pinned claim suite **passed on a normal dedicated server**, then flushed/saved/stopped successfully. Tests cover independent non-operator denials, cross-chunk ownership, OPAC veto despite FTB ownership, spectator/same-UUID fake refusal, actual protected Hut/NPC, progressive writes through real listeners, revocation/resume, offline pause and exact durable prefix reload. Embedded registered test peers declare actual payload IDs; this is not a real handshake test.

Standalone GameTestServer lacks the profile cache OPAC needs and fails during its startup; the claim suite uses a normal dedicated server with the real service, without patching production APIs. Exact FTB dependency/source downloads originally hit Cloudflare1010 with the default Python agent; the identified audit agent resolved them. See [reproducible suite and artifact locks](../../CLAIM_COMPATIBILITY_AUDIT.md).

A real graphical client loaded the five exact mods, claimed the site through ordinary FTB/OPAC commands, retained the claims and valid hologram after a full process restart, and opened the exact Royal Gilded review. A legitimate UI redemption started actual production-cadence construction. The same project saved at 251 blocks, survived a full process stop/restart with both claims loaded, and progressed beyond 361 after rejoining. It completed 782/782 at 10:55:11 UTC and introduced Mira, at unchanged production cadence with both claim mods loaded. Actual selectors counted exactly one Aldric and one Mira; a further full completed-world restart retained that same single-resident state. Mira’s actual Survival dialogue and Community milestone button worked after that restart, showing Tier2/revision6, the saved winner and76remaining logs without new consumption. Final clean save/stop is in the evidence.

## Actual Minecraft captures

- [Owned site, both claim mods loaded](owned-hologram.png).
- [Royal Gilded exact-site review](placement-review.png).
- [Claims and green preview survive process restart](claims-survive-restart.png).
- [Saved production construction before restart](saved-construction.png) · [same project resumed](construction-resumed.png).
- [Completed 782/782 project](construction-complete.png) · [actual finished exterior](completed-exterior.png) · [furnished interior](completed-interior.png).
- [Mira’s actual resident dialogue after restart](mira-dialogue.png) · [her working Community milestone service](mira-milestone.png).

These are real frames from isolated copies, not mockups. Air-only support/camera staging does not alter original approved worlds. Completed exterior/interior inspection uses a spectator camera after handover; the player returned to Survival before saving. [Earlier whole-model rotations/height/scaling and completed Lodge interiors](../deaf558/CHECKPOINT.md) remain archived for the unchanged visual foundation.

Full-pack Argonauts/Prometheus providers, third-party route/recipe integrations, authenticated remote sessions, two-real-client claim races and maximum-building/season-scale performance remain unverified. Automatic approval review rejected unapproved catalogue substitutions earlier; they remain unapplied.

## Exact continuation

Continue Kingdom SMP from c0889d3 or its newer delivery checkpoint on rebuild/royal-gilded. Read current CODEX_CONTINUATION, D-023 master directive, replacement mod snapshot and CLAIM_COMPATIBILITY_AUDIT. Preserve claims, historical winners, receipts, tokens, projects, Aldric/Mira and all old saves. Implement versioned new-world Tier2 Town Hall versus grandfathered Lodge profiles, then the free owner Hut entitlement/bootstrap and original 19×15 two-floor Hall/Elara through the reusable physical blueprint engine. Follow with safe four-block supports and automatic server-valid authorization before Architect/early economy. Retain tested optional claim APIs and conservative offline/version behavior, keep later proposed costs disabled, build/test actual client/server/persistence, push checkpoints/JAR/screenshots and never merge main.
