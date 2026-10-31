# Codex continuation — Kingdom SMP

Latest verified published gameplay commit: `64d085f` (Lodge native-server checkpoint); latest packaged build remains `5baf285` / source `7e9968a` (0.2.0-dev); branch `rebuild/royal-gilded`. No main merge.

Completed/tested: foundation, original furnished Builder’s Hut/Aldric/identity/dialogue, five-minute clickable admin confirmation; real Tier 1 contributions/recovery, exact leaderboard/personal history, permanent winner and Tier 2 advancement. Full forced build: 108 JUnit tests, zero failures/errors/skips; one substantial native GameTest; actual two-client iron-cap/live-update test; integrated and dedicated restart evidence. JAR/checksums/screenshots under docs/checkpoints/7e9968a.

Current Lodge source has passed a full Gradle build with **121 JUnit tests**, plus the expanded native lifecycle constructing the entire Lodge and one Reeve, testing claim/recovery/approval/redemption/protection/resume/identity and delayed neighbor updates. Real client claim/preview/proposal/approval/redemption and Aldric walking to the site worked. The project persisted across a complete process restart and continued until weather snow exposed an obstruction bug at 354/782. The fix permits only post-reservation weather snow without drops; pre-existing snow/terrain/player blocks still deny. Native weather/NPC safe-home and vanilla travel-gate tests pass. Current client work continues the same saved project; finish physical completion/Reeve/scaling and final restart before marking the Lodge COMPLETE. See ignored work/LODGE_QA_CONTINUATION.txt for exact fixture/log/control details.

Current task: approved Founders’ Lodge blueprint claim/recovery, exact admin-approved location, reusable progressive Aldric construction, furnished completion and named Reeve. Latest user explicitly authorizes clear supported Overworld winner proposals, administrator review/approval before work, no destruction of player property. This resolves the former site-policy blocker. Do not preserve stale “placement policy pending” descriptions as authority.

Vanilla realm entry gates now enforce Nether Tier 5 and End Tier 9 for player changeDimension/direct teleportTo and item transfers. Pure boundary and native-hook tests pass. Legacy characters already in locked realms and unverified third-party custom APIs are not rewritten or claimed covered.

Remaining: see MASTER_COMPLETION_TRACKER.md, including every tier/profession and all 288 planned quest slots. Later milestone costs/rewards, quest/XP catalogues, economy numeric/transaction policy, border values and integration matrix remain unapproved; propose separately and continue unblocked systems. Hall/Elara Tier 12 only; Lodge grants no extra banking/trading/Charter services.

Known baseline limits: authenticated remote sessions/external protection mods/Windows/host-loss/partial restores not tested; arbitrary NPC replacement and destructive repair have no approved semantics. Restore complete matching world backups; never delete receipt/journal markers to bypass recovery. Preserve existing atomic donation format and protocol.

Resume:
```sh
cd /workspace/Kingdom.SMP
export JAVA_HOME="$PWD/work/toolchains/jdk-21.0.13+9"
scripts/gradle-cloud.sh build --console=plain
scripts/test-runtime.sh
```
For real client checks use Xvfb/software GL with capped FPS and stop unused QA clients to conserve resources. Work/cache/worlds are ignored. Read cloud-runtime skill/network policy; preserve proxy and CA trust. Git HTTPS credentials fail on this instance, but `gh api` works. The ignored work/upload-checkpoint-objects.py can upload exact local Git objects; verify hashes and guard the development-ref fast-forward before updating it. Never expose credentials. Commit/push each tested checkpoint, deliver JAR/screenshots and update both tracker files; do not claim background work after ending a task.
