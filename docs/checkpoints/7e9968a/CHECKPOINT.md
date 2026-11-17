# Kingdom SMP — First Hearth gameplay checkpoint

Source: [`7e9968a148a50437e36e2c3e99dd49d5274f8cd8`](https://github.com/MohyMods/Kingdom.SMP/commit/7e9968a148a50437e36e2c3e99dd49d5274f8cd8), development branch `rebuild/royal-gilded`. Version **0.2.0-dev**, Minecraft **1.21.1**, NeoForge **21.1.265**, Java **21**. This directory packages the tested source without changing its compiled classes.

[Download the development JAR](kingdom-smp-core-0.2.0-dev-7e9968a.jar?raw=true) · [Checksums](SHA256SUMS.txt) · [Build/test/runtime evidence](validation-evidence.zip)

JAR SHA-256: `67eedb7cf5e56e4ef0c1d42a2d49490d99eb2aabaef20b1951eb9032ac2ec9e9`

## Included gameplay

- Preserves the original furnished Builder’s Hut, Aldric, Royal Gilded dialogue and **clickable five-minute hut confirmation** with all existing security and placement checks.
- Actual permanent, partial resource donations: **1,024 logs, 2,048 cobblestone, 512 wheat and 128 iron ingots**. The server caps acceptance to eligible inventory and remaining requirements; excess stays with the player.
- Custom Royal Gilded contribution review/confirm, exact weighted leaderboard, paginated personal history and updates to other open viewers. Donation reviews expire after **30 seconds**, separately from the hut’s five-minute placement preview.
- Durable inventory checkpoints, one-use player-bound offers, inventory/revision/expiry checks, accepted-history persistence, interrupted-operation reconciliation and fail-closed recovery.
- All four targets complete **Tier 1 → Tier 2**, announce the permanent highest contributor and establish that player’s eligibility for the approved **Founders’ Lodge** construction privilege.
- **Town Hall and Elara are reserved for Tier 12.** No bank, Royal Trading, Charter, profession or later technology service is silently unlocked.

Physical blueprint claim/recovery/redemption, progressive construction, the furnished Founders’ Lodge and its named Reeve are **not implemented in this JAR**. The winner record establishes eligibility; it is not a delivered or redeemed blueprint. Public placement/protection policy is still awaiting a decision.

## Install and test

Back up the complete world. Replace the old Kingdom SMP JAR in `mods`; keep only one copy. Install this same JAR on the server and every client. Protocol version 2 requires matching client/server builds.

1. Preserve an existing hut/Aldric. For a new test world, an operator can use the existing `/kingdom hut preview <x> <y> <z> north <reason>` flow at a loaded, clear, supported site and click **[Confirm Builder's Hut]** within five minutes. See the repository README for the full placement rules.
2. In Survival or Adventure, carry eligible resources in the main inventory and close other containers. Press **K → Contribute**, choose a resource and 1/16/64/All, then **Review donation → Confirm donation**. No items are removed by Review or Cancel.
3. Check **Leaderboard** and **Your history**. With two players, keep both screens open: a successful donation updates the other player and invalidates an obsolete review. Changed inventory, an expired review or changed kingdom progress requires a fresh review.
4. Fill all four targets. Verify Tier 2, the public winner announcement and permanent winner record. There is no Lodge claim/build control yet.
5. Save/stop/restart and reopen the screens. Totals, history, winner and Tier 2 should remain. Do not delete ledger/identity markers or restore player data independently.

Canonical contribution files are `world/data/kingdomsmp_contributions.dat` and `kingdomsmp_contributions.created`, together with native playerdata receipts. Back up them, the hut journals, entity/chunk data and level.dat as one complete world. An uncertain write pauses donations for reconciliation; do not repeatedly retry or delete its records. See [the recovery contract](../../CONTRIBUTION_RECOVERY.md).

## Actual verification

The full Gradle build passed **108 JUnit tests with zero failures/errors/skips**. After reconnection, `build --rerun-tasks --no-build-cache` executed all nine tasks successfully and reproduced the exact previously tested JAR checksum above. The isolated NeoForge server lifecycle scenario passed **all 1 required GameTests**, covering native inventory saving failures/recovery, stale/copied/replayed offers, concurrent final-resource quotes, completion/winner/history/reload and the existing hut/NPC protections.

Actual Minecraft client donations completed all four targets, advanced to Tier 2 and survived a complete client/world restart. A normal dedicated server loaded, saved, restarted and reported the completed state. Two actual localhost clients reviewed 80 iron each; the first committed 80, the second received a live invalidation, reviewed/committed only the remaining 48 and retained 32. Both rendered the same two-player leaderboard. Native inventory output is included in the logs. Screens were inspected at 640 × 400 GUI pixels and at 427 × 240 with history scrolling.

The evidence records failed setup attempts separately: an initial dedicated run with closed stdin ended with status 143; the subsequent console run passed. The second virtual client required disabling NeoForge’s early loading-window control in its local ignored test config after a GLX handover error. Later environment disconnection interrupted an additional rejoin attempt; it is not claimed as a passing check. The host has no sound device; Authlib service requests log nonfatal failures in these local/offline sessions.

Authenticated remote multiplayer, particular external protection mods, Windows storage, partial backup restores, arbitrary host/power loss and season-scale performance remain unverified. This is a development milestone, not the completed mod or construction loop.

## Real Minecraft screenshots

These are captures from the tested running Minecraft clients, not design mockups. The narrow capture includes the surrounding virtual desktop.

| Screen | Screenshot |
| --- | --- |
| Server-reviewed exact donation | [Donation review](donation-review.png) |
| First Hearth complete at Tier 2 | [Contribution screen](first-hearth-complete.png) |
| Permanent winner and exact totals | [Completed leaderboard](leaderboard-complete.png) |
| Accepted personal donation history | [History](personal-history.png) |
| Completed state after full restart | [Restarted leaderboard](leaderboard-after-restart.png) |
| Smaller window and usable controls | [Narrow history](history-narrow.png) |
| Second client’s stale review invalidated | [Live update](multiplayer-live-update.png) |
| Only the remaining 48 iron accepted | [Capped review](multiplayer-cap-review.png) |
| First actual network client | [Dev leaderboard](multiplayer-leaderboard-dev.png) |
| Second actual network client | [DevTwo leaderboard](multiplayer-leaderboard-devtwo.png) |

## Continue from this checkpoint

> Continue `/workspace/Kingdom.SMP` on `rebuild/royal-gilded` from 7e9968a or its newer artifact-only commit. Preserve the tested hut, Aldric, contribution recovery protocol and Royal Gilded UI. Read current requirements, decisions, construction, recovery and roadmap documents. Tier 1 contributions/history/leaderboard and Tier 2 advancement are implemented. Next implement the approved Founders’ Lodge reward claim/recovery/redemption, reusable progressive construction and named Reeve. Resolve the pending public placement policy before enabling construction: administrator site approval versus winner-confirmed clear supported Overworld sites. Do not assume that decision, invent later milestone values or unlock extra services. Keep Town Hall/Elara at Tier 12. Build/test native persistence, interruptions, multiplayer and actual UI; publish an installable JAR and screenshots to the development branch. Never merge main.
