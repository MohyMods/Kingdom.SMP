# KINGDOM SMP — FINAL PRODUCTION COMPLETION
## BUDGET MODE · ONE CONTINUOUS EXECUTION · RELEASE 1.0.0

You are the lead developer responsible for completing Kingdom SMP, my custom NeoForge 1.21.1 Minecraft mod.

**Repository:** `MohyMods/Kingdom.SMP`  
**Primary development branch:** `rebuild/royal-gilded`  
**Minecraft:** 1.21.1  
**NeoForge:** 21.1.265  
**Java:** 21  
**Target:** Approximately 10 multiplayer users  
**Full modpack:** Approximately 277 mods

# YOUR PRIMARY OBJECTIVE

**FINISH THE ENTIRE KINGDOM SMP MOD AND DELIVER A VERIFIED, INSTALLABLE 1.0.0 RELEASE IN THIS EXECUTION.**

You have already spent considerable time developing this project. Do not restart, redesign completed systems, repeat previous planning, or revisit settled design decisions.

Continue from the latest verified working implementation.

I want actual code, implemented gameplay, completed integrations, passing tests, and the final compiled JAR.

**Do not stop after completing one profession, feature, rank, building, or milestone.**

## 1. BUDGET MODE — MAXIMUM DEVELOPMENT PER CREDIT

You must operate as efficiently as possible.

These rules are mandatory:

1. Read existing project continuation documentation first. Do not exhaustively reread files unless necessary.
2. Inspect Git history and current implementation before changing anything.
3. Reuse existing services, quest adapters, data definitions, validators, and shared frameworks.
4. Implement related functionality in large, logically grouped batches.
5. Minimize redundant repository searches, file reads, and tool calls.
6. Avoid unnecessary commentary, repeated summaries, and progress reports.
7. Do not repeatedly compile after trivial edits.
8. Use fast targeted tests during implementation.
9. Run broader integration tests at major milestones and final acceptance.
10. Do not create unnecessary checkpoints or releases.
11. Avoid rewriting working systems merely to improve code style.
12. Do not waste time polishing cosmetic details while gameplay remains incomplete.
13. Automate repetitive implementation and validation wherever safely possible.
14. Do not repeatedly ask for approval over decisions already documented.
15. Reserve sufficient execution budget for integration, debugging, and producing the final JAR.

**Budget mode means efficient development, not skipping correctness, security, or necessary testing.**

Do not sacrifice world persistence, transaction safety, player permissions, or gameplay reliability to conserve credits.

## 2. RESUME THE EXISTING PROJECT

Read these files:

- `docs/CODEX_CONTINUATION.md`
- `docs/PRODUCTION_COMPLETION_MATRIX.md`
- `docs/RELEASE_EXECUTION_PLAN.md`
- `docs/DECISIONS.md`
- `docs/REMAINING_PROFESSION_QUESTS_PROPOSED.md`
- Relevant final production directives under `docs/sources/`

Check the latest GitHub HEAD and reconcile any developments made since the last checkpoint.

There is also an experimental branch:

`alpha/0.12.0-alpha.1`

It contains alpha packaging, GitHub-based build verification, and Hut integrity diagnostics.

Inspect that branch for useful changes, but do not blindly overwrite newer implementation work on `rebuild/royal-gilded`.

Preserve existing worlds and backward compatibility.

Do not merge into `main` without explicit authorization.

## 3. FIX THE KNOWN ALPHA BUGS FIRST

Real gameplay testing exposed the following problems:

### BUG A — Aldric becomes unavailable

After the Builder's Hut is constructed, right-clicking Aldric's crafting table appears to cause the Hut to fail its structure integrity check.

Afterward, Aldric refuses interaction and displays:

"The builder is unavailable. Check the hut, proximity, and registered identity."

Investigate the exact cause.

Check whether:
- The crafting table or another required block is being modified.
- Normal block interactions are incorrectly treated as structural damage.
- A block-state comparison is too strict.
- Another mod changes workstation properties.
- The integrity system incorrectly pauses NPC interactions.
- Construction protection is interfering with normal furniture use.

Fix the root cause.

Normal interactions with crafting tables, beds, doors, barrels, and other usable furnishings must not break Hut integrity.

Do not disable all structure validation. Preserve actual structural damage detection.

Add a targeted regression test that reproduces and prevents the problem.

### BUG B — Town Hall construction stops

The Town Hall stops being built when Aldric becomes unavailable.

Repair the dependency and ensure construction resumes safely when the Hut is intact and Aldric is available again.

Verify:
- Construction progress persists.
- Existing projects are resumed rather than duplicated.
- No blocks or NPCs are duplicated.
- Server restarts preserve progress.
- Builder navigation works.
- Legitimate pauses display understandable reasons.
- Finished buildings remain functional.

Test the complete Hut → Aldric → Town Hall construction loop.

### BUG C — Blueprint claiming confusion

Verify that claiming the starting Hut blueprint reliably delivers the item to the correct inventory slot.

Improve feedback when a claim requires a second confirmation, is denied due to Creative mode, or fails because of inventory or ownership conditions.

Do not weaken authenticated blueprint ownership.

## 4. COMPLETE ALL 12 PROFESSIONS

The final game must have:

- Farmer
- Blacksmith
- Builder
- Lumberjack
- Miner
- Librarian
- Merchant
- Engineer
- Mechanic
- Mage
- Adventurer
- Alchemist

Each profession requires:

**8 Apprentice quests + 8 Journeyman quests + 8 Master quests = 24 quests.**

Total: **288 profession quests**.

Every quest must have real server-authoritative progression and completion evidence.

Do not count JSON definitions, descriptions, placeholders, or generic commissions as completed gameplay unless they satisfy the approved quest specification.

Implement all outstanding quests efficiently by reusing shared objective systems.

Preserve the following mechanics:

- Maximum three active professions.
- Apprentice → Journeyman → Master.
- Approximately 2–3 weeks of intended progression per profession.
- Profession switching costs 15,000 coins.
- Inactive professions retain their progress but lose active perks.
- Daily and weekly contracts.
- Profession-specific bonuses.
- Actual building and rank restrictions.

### Master rewards

Finish all approved signature Master equipment and stations.

These rewards should be exceptionally powerful and worth mastering the profession, while respecting approved balancing rules.

Preserve their intended purchase restrictions, tradeability, ownership checks, and escalating repeat-purchase prices.

Do not substitute ordinary enchanted items for unique gameplay mechanics.

Do not invent new rewards when approved specifications already exist.

## 5. COMPLETE KINGDOM TIERS 1–12

Implement the complete natural progression of the kingdom.

Every tier needs:

- Real material requirements.
- Increasing resource variety and difficulty.
- Proper contribution tracking.
- Accurate milestone completion.
- Player recognition and rewards.
- Appropriate building unlocks.
- Correct dimension and equipment restrictions.
- Persistence after server restart.

Later tiers must not remain merely disabled proposals.

Preserve approved milestones and unlocks, including:

- Tier 1 starting Builder's Hut.
- Tier 2 Town Hall and early profession buildings.
- Tier 3 Royal Bank.
- Tier 6 Aerodrome and aircraft progression.
- Tier 9 Waystones.
- Tier 12 End access.
- Approved staged dimension progression and Royal Legacy finale.

Use the existing progression architecture. Do not recreate the entire tier system.

Do not allow players to bypass tier restrictions using modded alternatives.

## 6. COMPLETE ALL KINGDOM SYSTEMS

Finish remaining approved work across:

**Economy:** NPC shops, dynamic pricing, stock limits, banking, currency, auctions, buy orders, trading, taxes, contracts, and mailboxes.

**Construction:** All approved buildings, carryable blueprints, holographic previews, first-buyer purchase rules, protected building placement, foundations, NPC residents, and persistent construction.

**Professions:** Complete quest gameplay, progression, permissions, equipment, profession buildings, shops, and contract systems.

**Integration:** Create, Mekanism, Immersive Aircraft, Waystones, Ad Astra, Aether, Twilight Forest, Deeper and Darker, FTB Chunks, OPAC, and all other approved modpack integrations.

**FTB Quests:** Complete kingdom milestones, profession progression, modded advancement campaigns, dependencies, and rewards.

**Administration:** Complete inspection, safe test-world progression controls, economy management, profession management, construction recovery, tier management, and audit logs.

**Multiplayer:** Server-authoritative gameplay, stable persistence, appropriate permissions, secure inventory operations, and reasonable performance for 10 players.

## 7. ADD SAFE ADMIN TESTING COMMANDS

Testing progression manually is unnecessarily time-consuming.

Implement a restricted sandbox administration system for isolated testing worlds.

It should support commands equivalent to:

- `/kingdom admin test tier complete <tier>`
- `/kingdom admin test profession rank <player> <profession> <rank>`
- `/kingdom admin test quest complete <player> <quest>`
- `/kingdom admin test construction accelerate`
- `/kingdom admin test resources grant <tier>`

Exact syntax may differ to fit the existing command architecture.

Requirements:

- Disabled by default.
- Requires operator-level permission.
- Enabled only through an explicit test-world configuration.
- Must preserve save-data consistency.
- Must not duplicate items or NPCs.
- Must not create impossible quest or tier states.
- Must log every mutation.
- Must never silently affect normal multiplayer gameplay.

Make it possible to test all 12 tiers without gathering thousands of resources manually.

## 8. FINAL INTEGRATION AND QUALITY ASSURANCE

After implementation, verify:

1. Complete Java/NeoForge compilation.
2. All relevant unit tests.
3. Native Minecraft GameTests.
4. Fresh-world progression from Tier 1 to Tier 12.
5. All 12 professions reaching Master.
6. All 288 quest objectives.
7. Signature Master equipment behavior.
8. Economy safety and transaction recovery.
9. Persistence across server restarts.
10. Construction and NPC identities.
11. Dimension and modded progression restrictions.
12. FTB Quests synchronization.
13. Claim protection and multiplayer permissions.
14. Full modpack compatibility where the actual artifacts are available.
15. Release installation and startup.

Do not falsely claim successful tests.

If external mods or client environments are unavailable, clearly identify the missing validation instead of calling it complete.

Correct critical issues before release.

## 9. FINAL RELEASE DELIVERABLES

Produce:

**Kingdom SMP 1.0.0**

Include:

- Compiled production `.jar`
- Complete source changes
- Required configuration files and datapacks
- Installation README
- Modpack dependency/version requirements
- Full feature changelog
- Administrator command guide
- Known limitations
- Actual test and validation results
- SHA-256 checksum
- Release ZIP package

Save the release artifacts in an accessible location.

Do not merge to `main` without my permission.

If the final release meets its acceptance criteria, prepare a GitHub release candidate or pre-release, with an explicitly stated verification status.

## 10. EXECUTION RULES — FINISH EVERYTHING IN ONE GO

This is one continuous implementation assignment.

Do not stop to ask whether you should continue after each feature.

Do not end after finishing Farmer, Blacksmith, the remaining profession quests, or an individual kingdom tier.

Do not spend the remaining execution budget writing another enormous project plan.

**IMPLEMENT THE GAME.**

Use the following order:

1. Fix the Alpha bugs.
2. Complete all unfinished profession quests and Master rewards.
3. Complete Tier 4–12 progression.
4. Complete remaining gameplay systems and integrations.
5. Complete FTB Quests and admin tools.
6. Run final integration and regression tests.
7. Produce the verified release artifacts.

Make independent technical decisions within previously approved requirements.

Continue implementing until either the complete project passes release acceptance or a genuine execution limit prevents further work.

If you reach a tool, time, context, or credit limit, do not claim success. Commit the finished changes, preserve a reproducible build, and write a concise continuation record containing the exact remaining tasks and test results.

**Never replace actual implementation with placeholders solely to mark a feature complete.**

## FINAL INSTRUCTION

I have spent enough time planning and testing incomplete versions of Kingdom SMP.

I want the complete game.

Use budget-efficient development, minimize unnecessary tool usage, reuse existing work, finish every approved system, test the integrated game, and deliver the final compiled mod.

**Begin implementation immediately. Do not ask me another questionnaire. Do not stop at another intermediate development checkpoint. Finish as much of Kingdom SMP 1.0.0 as the execution environment genuinely allows, prioritizing a fully playable and verified release.**