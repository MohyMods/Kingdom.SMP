# Kingdom SMP — Founders’ Lodge gameplay checkpoint

Source [`33b62fef7a1ad00a2caeeddf3e0a526ed64d6dda`](https://github.com/MohyMods/Kingdom.SMP/commit/33b62fef7a1ad00a2caeeddf3e0a526ed64d6dda), branch `rebuild/royal-gilded`. **0.3.0-dev · Minecraft 1.21.1 · NeoForge 21.1.265 · Java 21 · protocol 4**. This artifact checkpoint changes no compiled source. It is a tested development milestone; the full project is not complete.

[Download the installable JAR](kingdom-smp-core-0.3.0-dev-33b62fe.jar?raw=true) · [SHA-256 checksums](SHA256SUMS.txt) · [Build/test/runtime evidence](validation-evidence.zip)

JAR SHA-256: `3108000b729863b98aed2a7a636cc60d6fc2ced1299c3f9b8335223df7892562`

## Working gameplay

Preserves the furnished Builder’s Hut, Aldric Stonehand, **clickable five-minute hut confirmation**, Royal Gilded design, consumed First Hearth donations, history, exact normalized leaderboard, deterministic winner and Tier 1 → 2. Original targets remain **1,024 logs / 2,048 cobblestone / 512 wheat / 128 iron**. Donation review is still 30 seconds; it is separate from hut placement.

The highest contributor can now claim or recover the exclusive physical **Founders’ Lodge Blueprint**. Recovery durably invalidates previous copies and approvals. The winner proposes a rotated clear, supported Overworld site; an administrator separately reviews and approves its exact footprint. One-use redemption reserves one project before removing the plan. Materials are included construction authority, never a repeat grant of loose items.

Aldric walks from his original hut to the exterior work post and builds **782 blocks progressively**, one per ten server ticks. Work pauses when required chunks, hut, builder, path or site are unavailable; no permanent chunk loading or remote instant construction. Protected reservations, per-placement protection events, saved progress and conservative inspected recovery prevent overwriting property or regenerating missing credited materials. Weather snow accumulated during construction is brushed away without drops; pre-existing snow and all terrain/player blocks still deny placement. Completed property is never automatically snow-cleared.

The original furnished **17 × 13 × 12 Lodge** has a porch, working entrance, lit meeting chamber, table/benches, empty storage, crafting table, desk/books/bed and milestone lectern. Completion verifies all required blocks, then issues one registered **Mira Ashford**, the Reeve. Her personality dialogue and the lectern open real community milestone/contribution and kingdom screens. Royal Gilded Blueprint/Project/Royal Admin tabs provide live state, reviews, confirmations, progress, pause reasons and scaled readable layouts. The world preview is an advisory footprint/work-post outline; it is not a transparent full-block hologram.

Vanilla server-side realm entry now requires **Nether Tier 5 / End Tier 9**, including actual player changeDimension, cross-realm teleportTo and item transfers. Unknown custom mod routes and imported characters already inside locked realms are not rewritten or claimed covered.

**The ultimate Town Hall and Elara remain Tier 12.** The Lodge grants no banking, Royal Trading, Charter or profession service. Later content and exact numeric policies remain unapproved, as recorded in the master completion tracker and proposed review document.

## Install and test

Back up the **entire world** first. Replace the old Kingdom SMP JAR in `mods`, keeping one copy. Install this exact JAR on the server and every client: protocol 4 requires matching builds. Use Java 21, Minecraft 1.21.1 and NeoForge 21.1.265. This JAR does not bundle NeoForge or any third-party technology mod.

1. Preserve the existing hut/Aldric. In a fresh test world, a permission-level-2 operator uses `/kingdom hut preview <x> <y> <z> north <reason>` at a loaded, clear supported site, then clicks **[Confirm Builder's Hut]** within five minutes. The original checks still apply.
2. In Survival or Adventure, close other containers. Press **K → Contribute**, select eligible inventory/resources and review/confirm donations. Inspect Leaderboard and Your history. Complete all four targets and verify Tier 2 and the public highest-contributor announcement.
3. As that winner, open **K → Construction** or `/kingdom lodge open`. Keep an inventory slot free, choose **Claim blueprint**, read the review and confirm. Carry the current authenticated plan. **Recover blueprint** invalidates every older copy and any prior site approval; it cannot be used after redemption.
4. Prepare a flat clear supported Overworld site near an accessible Aldric. North/south require a **17 × 13** foundation footprint; east/west **13 × 17**. Origin is the minimum foundation corner. The entire **12-block-high volume above its support** must be clear and already loaded, with clear supported space immediately outside the entrance for Aldric. Do not stand inside the footprint or its post. The mod performs **zero terrain replacement**. Use a normal safe ground site rather than copying the sky-platform QA fixture.
5. In Blueprint, enter foundation X/Y/Z, choose rotation and **Preview**. Inspect the world outline, review and confirm the proposal. Alternatively use `/kingdom lodge preview <x> <y> <z> north` (also east/south/west) to open the same server-reviewed flow.
6. A permission-level-2 administrator stands within 64 blocks, opens **Construction → Royal Admin → Review site approval**, checks exact bounds/work post and confirms. Obstruction, changed proposal, lost permission or expired/stale review refuses approval. The initial policy permits no destructive terrain changes.
7. The winner opens **Project → Review construction**, reviews and confirms redemption with the current plan. Its authority is consumed once. Keep the hut, route, work post and site loaded and healthy; watch Aldric walk and work. Default construction takes about **6½ minutes plus travel/pauses** at 20 TPS. Inspect the live progress and precise pause messages.
8. Save/stop/restart during work. The same project should resume when valid loaded conditions return. If credited blocks are missing, inspect the actual site and matching backups; administrator Inspect/resume never blindly replaces extracted/damaged materials. Do not delete journal markers or repeatedly retry an uncertain write.
9. At 782/782, verify the public completion announcement, furnished Lodge and one Mira Ashford. Right-click her or the milestone lectern; open Community milestone and Kingdom overview. Open barrels and the entrance. Save/stop/restart again and verify the same building/NPC. Lodge services do not unlock a bank/trading/Charter.

Back up `data/kingdomsmp_construction.dat` and its `.created` marker together with contribution state/receipts, hut journals, playerdata, level.dat and all entity/chunk files. Missing/corrupt/mismatched identity records fail closed. See [construction recovery behavior](../../FOUNDERS_LODGE_IMPLEMENTATION.md) and [contribution recovery](../../CONTRIBUTION_RECOVERY.md).

## Executed verification

- Full Gradle build **BUILD SUCCESSFUL**: **121 JUnit tests, zero failures/errors/skips**. The evidence includes the actual XML results and complete build log.
- Isolated NeoForge native lifecycle: **all 1 required GameTests passed; BUILD SUCCESSFUL**. This substantial scenario covers the original hut/donation recovery plus winner/full-inventory/copy/old-plan/stale/permission/protection/redemption cases, persistent bounded construction, actual full furnishing, safe Reeve issuance and identity/death, unloaded pause, weather clearing with zero drops, preservation after handover, scheduled block updates and native realm travel denial. Test fixture steps accelerate construction only in the test; they do not change production cadence.
- Actual Minecraft client: claim → placement preview → proposal → exact administrator approval → one-use redemption → Aldric physically walking/working → full Lodge → Mira and functioning milestone services. Construction ran at production cadence. An in-progress complete process restart retained progress and resumed.
- Completed full client/world restart with final protocol: same COMPLETE project, **exactly one Reeve with identical UUID**; dialogue and live services worked. Normal and 427 × 240 GUI-pixel screens were inspected for text/control overlap and clipping. Screenshots are real Minecraft frames.
- Normal dedicated server loaded a complete saved copy at Tier 2/revision 6. **Two real localhost clients**, operator Dev and non-admin DevTwo, joined with the final protocol. They inspected completion; DevTwo used Mira’s actual contribution service with public totals and his own zero contribution history. His construction screen exposes only Blueprint/Project, with no Royal Admin tab. Native selectors counted one Aldric and one Reeve.
- Dedicated console save/stop: **BUILD SUCCESSFUL**. A second start loaded the same single Reeve UUID and Tier 2/revision 6, then saved/stopped: **BUILD SUCCESSFUL**. Simultaneous new construction/redemption is covered by native tests; a second complete two-human building cycle is not claimed.

Live QA found weather snow obstructing a future wall, then residual snow in empty indoor spaces. Both are fixed and native-tested. The already completed development fixture received an exact snow-only interior cleanup; the final handover handles future projects before completion. An earlier camera teleport off the isolated sky platform caused a test-character fall, corrected without changing progression. All images and terrain preparation are from isolated test worlds; no production world was modified.

Known limits: local/offline sessions, nonfatal Authlib service refusals, no audio device. Authenticated remote play, external protection mods, arbitrary partial restores/host loss, Windows storage and season-scale TPS are unverified. Missing-issued NPC resurrection/replacement and destructive repairs need approved semantics; the current system refuses automatic duplicates. Not all twelve playable tiers, professions/288 quests, economy, integrations or general administration are implemented.

## Actual Minecraft screenshots

| Capture | Image |
| --- | --- |
| Server-reviewed world footprint | [Placement outline](placement-footprint.png) |
| Exact administrator review | [Site approval](administrator-site-review.png) |
| One-use winner redemption | [Redemption review](winner-redemption-review.png) |
| Saved in-progress construction | [Progress](progress-before-restart.png) |
| Aldric physically working | [Builder](aldric-working.png) |
| Completed exterior | [Lodge](lodge-exterior.png) |
| Furnished meeting chamber | [Interior](lodge-interior.png) |
| Final client after full restart | [Mira dialogue](reeve-after-restart.png) |
| Smaller actual client window | [Narrow dialogue](reeve-narrow.png) |
| Functional milestone lectern | [Project services](milestone-lectern.png) |
| Mira’s actual contribution route | [Contributions](reeve-contributions.png) |
| Second dedicated client, no admin tab | [Non-admin Reeve](multiplayer-reeve-nonadmin.png) |
| Second client’s authoritative personal/public data | [Multiplayer contributions](multiplayer-contributions-nonadmin.png) |

## Continue development

> Continue Kingdom SMP on `rebuild/royal-gilded` from `33b62fe` or its newer artifact-only commit. Preserve the tested Lodge/Hut/NPCs, atomic donation/construction stores and Royal Gilded UI. Read MASTER_COMPLETION_TRACKER.md, CODEX_CONTINUATION.md and current approved decisions. The full First Hearth → winner blueprint → admin-approved progressive Lodge → Mira loop and vanilla Nether 5/End 9 entry gates are tested. Review the pending Tier 2 → 3 milestone/reward answer; implement only approved values. Otherwise continue sufficiently specified existing-service recovery/admin work, without inventing profession quests, economy rules, later rewards or mod gates. Keep Town Hall/Elara at Tier 12. Build/test native persistence, real client/multiplayer and screenshots, commit/push tested milestones and deliver JARs. Never merge main.
