# Codex continuation — Kingdom SMP

Branch `rebuild/royal-gilded`; Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21. Never merge main. Latest published gameplay checkpoint: `33b62fef7a1ad00a2caeeddf3e0a526ed64d6dda`; installable 0.3.0-dev JAR, actual screenshots and build/test evidence: `docs/checkpoints/33b62fe/`. The artifact-only commit does not change compiled source. Historical 0.2.0-dev package is `5baf285` / source `7e9968a`.

## Completed and tested

- Original furnished Builder’s Hut/Aldric identity/dialogue/routine, five-minute clickable admin confirmation.
- Real consumed Tier 1 contributions, safe prepared inventory receipts/reconciliation, history, exact normalized leaderboard, deterministic permanent winner and Tier 1 → Tier 2.
- Founders’ Lodge unique physical plan claim/recovery, durable nonce revocation before recovery/redemption, no duplicated entitlement/material grants, exact rotated supported Overworld proposal, administrator footprint review/confirmation, one reserved progressive project.
- Actual Aldric walks to the work post and places one block per ten ticks, animating work. Saved construction pauses without chunk tickets. Protection cancellation and missing-prefix damage fail closed; administrator inspect/resume never blindly regenerates credited materials.
- Original furnished 17 × 13 × 12 Lodge, 782 blocks, working doors/storage/meeting space/lectern. Mira Ashford, named Reeve, has permanent project/home identity, routine, personality and actual milestone/kingdom interfaces. No bank, Royal Trading or Charter unlock. Town Hall/Elara remain Tier 12 only.
- Vanilla Nether Tier 5 / End Tier 9 entry gates for player changeDimension, direct cross-realm teleportTo and item transfers. Legacy already-inside characters and unknown custom mod routes are not claimed covered.
- Royal Gilded connected Blueprint/Project/Royal Admin interfaces, advisory world footprint/work-post overlay, live progress, review/confirm, readable portrait/dialogue and normal/narrow rendering. Protocol is **4**; use the same JAR on every client and server.

Latest full build: **121 JUnit tests, zero failures/errors/skips** (`work/lodge-final-build.log`). Expanded native lifecycle: **all 1 required GameTests passed**, BUILD SUCCESSFUL (`work/lodge-final-runtime.log`). It exercises real blocks/entities, inventory/recovery/claims/stale permissions/protection/interruption/unloaded work/full Lodge/NPC identity/death/realm gates. Direct test steps accelerate the fixture only; production client construction ran at normal cadence.

Actual Minecraft testing completed the entire Lodge loop, including Aldric leaving his hut, physical work, 782/782 completion, Mira interaction, real contributions navigation, milestone lectern, door/storage and scaled screens. In-progress and completed full client process restarts retained the project; completed restart preserved exactly one Reeve with identical UUID. First Hearth previously passed a real two-client shared-cap test. Two real matching-protocol clients joined the normal dedicated server, inspected the completed project and opened Mira’s live contribution service; the non-admin interface exposes no Royal Admin tab. The server saved/stopped with BUILD SUCCESSFUL. The normal dedicated restart reloaded Tier 2/revision 6 and the same single Reeve, then saved/stopped with BUILD SUCCESSFUL. Final artifacts are recorded in VALIDATION.md and the delivered CHECKPOINT.md, with actual screenshots/logs/checksums.

## Known limits and decisions

The whole project is **not complete**. See MASTER_COMPLETION_TRACKER.md: all 12 tiers, 12 professions/288 planned slots, economy/markets/integrations/admin/UI dependencies are tracked honestly. Later milestone names/quantities/rewards and borders, profession quest/XP catalogues, economy numeric/transaction policies and actual modpack gate matrix are missing approved definitions. PROPOSED_BALANCE_REVIEW.md supplies review candidates only, never enabled defaults. Do not fabricate quests, balance or service gates.

NPC death/resurrection, missing-issued identity reissuance and destructive repair remain unapproved. Current recovery conservatively refuses duplication; restore complete matching backups. No remote/authenticated multiplayer, external protection-mod compatibility, Windows storage, arbitrary partial restores/host loss or season TPS claim. Local/offline Authlib fetch refusals and no sound device are environment limitations. Thin weather snow may be removed without drops only during active construction/handover, never after completion; original terrain/pre-existing snow/player blocks still deny placement.

## Exact next task

Preserve the published Lodge checkpoint and all save formats. Audit the tracker and proposed balance review against any new user answers. Implement the next sufficiently approved dependency: a reviewed representative Farmer profession with actual approved quest/XP/activation definitions before expanding other professions, or approved Tier 2 → 3 community requirements/reward. If those remain unanswered, continue non-gameplay-changing recovery/administration improvements tied to existing services; do not enable arbitrary financial/progression defaults. General NPC replacement needs the explicit policy above. Keep Town Hall/Elara at Tier 12 and early service locations separately gated.

Resume testing:
```sh
cd /workspace/Kingdom.SMP
export JAVA_HOME="$PWD/work/toolchains/jdk-21.0.13+9"
scripts/gradle-cloud.sh build --console=plain
scripts/test-runtime.sh
```
Use the cloud-runtime skill and enforced proxy/CA route. Xvfb/software GL clients need network-enabled exec for display access, capped FPS (20) and `earlyWindowControl=false` in ignored test configs. Stop unused clients. The exact live fixture details and helper scripts are in ignored work/LODGE_QA_CONTINUATION.txt; latest state supersedes its older BUILDING counts. World `run-client/saves/New World` contains the completed Lodge at 18,201,0 north, original Hut at 0,201,0. Use safe camera positions; do not overwrite terrain with broad fill. Development worlds/cache/work are ignored.

Git HTTPS credentials fail here, but normal authenticated `gh api` works through the proxy. `work/upload-checkpoint-objects.py` uploads exact local Git objects and verifies SHA identities, requires clean checkout and remote HEAD equal local parent, and never changes a ref. Then perform the guarded development-branch fast-forward (force false) and update local remote tracking. Never print credentials. Commit/push tested source, then artifact-only checkpoint with installable JAR, real screenshots, evidence ZIP and SHA256; verify immutable raw JAR download returns HTTP 200 and matches the built bytes. Update tracker/continuation after each milestone. If the task ends, provide the exact next prompt; do not claim automatic background development.
