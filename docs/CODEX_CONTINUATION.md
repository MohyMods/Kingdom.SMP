# Codex continuation — Kingdom SMP

## Latest dimension instruction — D-022, planning only

**Do not code or activate costs yet.** End unlock target is Tier 12, replacing End 9; Aether 4, Nether 5, Twilight Forest 6, Otherside 8 and reviewed Ad Astra stages 10–11 are the target schedule. Read DIMENSION_PROGRESSION.md and revised PROGRESSIVE_RESOURCE_BALANCE.md before other progression instructions. Mandatory paths through Tier 11 → 12 must have no direct or indirect End dependency. Dragon and End expedition belong after the Tier 12 unlock; Royal Legacy Monument remains the proposed final reward. D-021 future Town Hall at Tier 2 and blueprint review hold remain intact.

Current binary/source still use old End-9 behavior. No Java, JSON, tests, recipes, configuration, JARs or world data were changed. Do not claim End-12 enforcement, alternate mod teleport coverage, Ad Astra compatibility or a verified non-End mod recipe closure. Repository inspection found no pinned actual pack/dependencies for the scheduled dimension mods. Review exact artifacts, planet split and recipes, already-inside players/safe return, historical Dragon/visit credit, finale/Monument award rules, revised resource balance and blueprint migration before implementation.


## Latest instruction — D-021, documentation only

**Do not code yet.** The user approves physical blueprints/full building holograms as design requirements but is finalizing the building roadmap and explicitly requires an implementation plan for approval. Read CONSTRUCTION.md and BLUEPRINT_IMPLEMENTATION_PLAN.md first. Future First Hearth reward is the **Tier 2 Town Hall, 19 × 15 blocks, two floors**; this supersedes the prior Tier 12 Hall/future Lodge policy. Existing 0.4.0-dev and every Lodge/Mira/save/entitlement/project stay unchanged. No migration, datapack edit, new item/structure or gameplay implementation is authorized yet.

Next task: review/clarify the proposed initial Hut/Aldric bootstrap, final building/service/controls roadmap and separate grandfather/profile/optional-unused-entitlement migration plan. Implement only after explicit approval; do not follow old immediate implementation instructions below. D-022 subsequently names Royal Legacy Monument as the proposed Tier 12 reward and revises the dimension schedule; other unmodified profession/service gates remain unchanged. Existing building objectives must be re-reviewed for future profiles, never automatically renamed.


Branch `rebuild/royal-gilded`; Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21. Never merge main. Latest published gameplay checkpoint: `7ce19a818e98452daaf6fe0fd0665032478dd88b`; installable 0.4.0-dev JAR, actual screenshots and evidence: `docs/checkpoints/7ce19a8/`. Previous Lodge package remains `docs/checkpoints/33b62fe/`. The artifact-only commit does not change compiled source. Historical 0.2.0-dev package is `5baf285` / source `7e9968a`.

## Current progressive-resource checkpoint

Current source `7ce19a8`, 0.4.0-dev / protocol 5, adds strict server datapack milestone definitions, registry/tag checks, conservative acquisition profiles, cycle/forward-gate validation, actual Royal Gilded tier inspection and safe guards/review invalidation around First Hearth. Full build: **139 JUnit tests, zero failures/errors/skips**; native expanded lifecycle: all 1 required tests passed. Real invalid datapack/reload refused donation without changing 164 logs/revision 2; valid reload/fresh review consumed 64, left 100 and saved revision 3. Final logs are work/milestones-final-build.log, milestones-runtime.log, milestone-data-server.log and milestone-final-server.log.

D-020 supersedes the earlier Tier 2 proposal: logs 2,048 / cobblestone 3,072 / wheat 768 / iron 192 / copper 384 / glass 256 / brick items 384 / leather 64; completed Lodge and administrator-approved road. **Quantities remain proposed until approval.** Royal Storehouse Blueprint, included materials, named Quartermaster and secure shared storage is the next reward scope; future scoring, road evidence and storage access/capacity are still unsettled. All later data rows are disabled. PROGRESSIVE_RESOURCE_BALANCE.md has all twelve proposed lists, availability/effort/dependency assumptions; MILESTONE_DATA_FORMAT.md records configuration and limits.

Exact next implementation dependency: generalized per-milestone pinned history/inventory transaction adapter under approved future normalized scoring, then road objective approval and Royal Storehouse services under defined access/capacity. Existing Tier 1 legacy contract is deliberately pinned; do not change awarded scores or assume that a configurable catalogue means twelve live progression engines. Unknown mod items/recipes/Master access stay blocked, not substituted. Prepare concrete policy choices if still missing; do not fabricate quests or financial rules.

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

## Prior implementation sequence — superseded by D-021 review hold

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

Final physical check: normal server and real client restarted with final source, retained Tier 1/revision 3 and 100 remaining logs, rendered all eight candidate targets together and inspected narrow layout. Both dedicated saves/stops completed BUILD SUCCESSFUL. This slice used one actual client; prior two-client Lodge evidence remains historical. No new two-client data-reload race claim. Test fixture world run/milestone-data-validation is isolated; original Lodge/world backups remain.
