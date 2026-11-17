# KINGDOM SMP — COMPLETE THE ENTIRE PROJECT
## Final End-to-End Implementation Order — Royal Gilded 1.0.0

**Repository:** https://github.com/MohyMods/Kingdom.SMP
**Branch:** `rebuild/royal-gilded`
**Latest confirmed release checkpoint:** 0.6.0-dev
**Source checkpoint:** `25aa1dc`
**Delivery checkpoint:** `1b92169`
**Minecraft:** 1.21.1
**NeoForge:** 21.1.265
**Java:** 21
**Target:** Full Kingdom SMP 1.0.0
**Modpack:** The complete 277-entry mod snapshot recorded in the repository

---

# 1. READ THIS FIRST — I WANT THE WHOLE GAME FINISHED

I am tired of managing this project one small milestone at a time.

**This message is my authorization to complete the entire remaining Kingdom SMP project, not just the next feature.**

I do not want to repeatedly send you prompts asking for the marketplace, then the economy, then professions, then quests, then dimensions.

You already have the master specification, final production directive, existing implementation, decision history, and completion matrix in the repository.

Your job is to finish the entire approved game using those requirements.

Operate as the project's lead developer and technical architect.

### Your instructions

1. Continue from the latest commit on `rebuild/royal-gilded`.
2. Do not rebuild working systems from scratch.
3. Read the existing master implementation directive, final production directive, decision register, completion tracker, modpack snapshot, and Codex continuation documents.
4. Treat the latest explicitly approved user decisions as authoritative.
5. Finish every remaining approved feature.
6. Make reasonable technical decisions independently.
7. Work through all feature dependencies in order.
8. Build, test, fix, and commit after meaningful increments.
9. Keep moving to the next unfinished feature without requiring a new user prompt for each milestone, within your execution environment's limits.
10. Never merge into `main` without my authorization.
11. Deliver an installable, verified final release.

**Do not stop after one small feature and ask me what to implement next. The answer is: continue toward completing everything in this directive.**

If your execution session ends before completion, preserve an exact working checkpoint and continuation instructions. Do not claim you can continue running after your session has ended.

---

# 2. USE THE EXISTING 0.6.0 FOUNDATION

The following systems have already been developed and tested:

- Fresh-world progression profiles.
- Free owner Builder's Hut Blueprint.
- Authenticated blueprint entitlement system.
- Working Builder's Hut.
- Aldric Stonehand.
- Tier 1 contributions.
- Contribution leaderboard and highest-contributor winner.
- Tier 2 advancement.
- Winner's Town Hall Blueprint.
- Full-building holographic placement.
- Rotation and height controls.
- Safe site validation.
- FTB Chunks and OPAC construction protection.
- Progressive Aldric construction.
- 19×15 furnished two-story Town Hall.
- Elara Vale, Royal Steward.
- Town Hall milestone and contribution services.
- Persistent construction across restarts.
- Legacy Founders' Lodge and Mira preservation.
- Existing admin inspection commands.

The most recent documented release passed 146 unit tests and relevant native/client validation.

Do not regress these systems.

The Town Hall already exists, so stop treating its creation as the next development goal.

**Begin with the unfinished Royal Architect Marketplace, durable currency foundation, and mailboxes. Then continue through all remaining gameplay systems.**

Check the current repository HEAD for newer completed work before making changes.

---

# 3. COMPLETE ALL TWELVE KINGDOM TIERS

Implement a complete progression from Tier 1 through Tier 12.

Each tier must include:

- Shared milestone requirements.
- Resource contributions.
- Construction or community objectives where appropriate.
- Correct prerequisite validation.
- Tier-specific feature unlocks.
- World-border expansion.
- Player notifications.
- Royal Gilded progression information.
- FTB Quest objectives.
- Admin testing commands.
- Persistent completion history.

Preserve Tier 1's approved requirements:

- 1,024 logs.
- 2,048 cobblestone.
- 512 wheat.
- 128 iron ingots.

For later tiers, use previously approved quantities where they exist. For genuinely unspecified quantities, create well-balanced, configurable defaults and document them clearly.

Do not silently use superseded milestone designs, buildings, or rewards.

### Required tier schedule

| Tier | Required major unlock |
|---|---|
| 1 | Builder's Hut, Aldric, initial community contributions |
| 2 | Town Hall, Elara, Royal Architect Marketplace |
| 3 | Bank, economy expansion, auctions, Bounty Hall, Fisherman's Wharf |
| 4 | Aether dimension |
| 5 | Nether dimension |
| 6 | Twilight Forest, Royal Aerodrome, conditional aircraft access |
| 7 | Advanced industrial progression and Mechanic profession expansion |
| 8 | Deeper and Darker / Otherside |
| 9 | Waystones and eligible fast travel |
| 10 | Initial Ad Astra space progression |
| 11 | Advanced Ad Astra progression |
| 12 | End dimension, Royal Legacy finale, Master Mechanic Creative Energy Cube eligibility |

Also implement the approved profession-building release schedule recorded in the repository.

Every tier must feel rewarding, even if no new dimension unlocks.

Avoid dependency cycles.

**Nothing required to unlock Tier 12 may depend on End-exclusive resources or End access, including indirect recipes.**

The Ender Dragon is a post-Tier-12 finale objective.

---

# 4. FINISH THE ROYAL ARCHITECT BLUEPRINT MARKETPLACE

Implement the marketplace inside the existing Town Hall.

The marketplace must be functional, not a decorative menu.

Requirements:

- Every eligible building has a physical blueprint.
- Builder's Hut and initial Town Hall are special exceptions.
- All other public and profession buildings are acquired through Town Hall purchases.
- One legitimate primary entitlement per building per kingdom.
- First successful buyer receives the entitlement.
- All other buyers see the listing as sold out.
- Every successful purchase is atomic.
- Failed purchases do not deduct money.
- Purchased entitlements persist across restarts.
- Lost blueprints can be safely recovered.
- Copied items cannot create additional buildings.
- Completed buildings become public kingdom property.
- Blueprint buyers receive permanent founder recognition.
- Buyers may select names for new building NPCs, subject to appropriate validation.
- Administrator recovery can safely handle abandoned or corrupted entitlements.

Building blueprint prices must remain within 500–15,000 kingdom coins unless a newer explicitly approved decision supersedes this range.

Do not confuse blueprint prices with much more expensive Master equipment prices.

### Complete construction enhancements

- Safe automatic foundations up to four blocks deep.
- No protected or player-owned block destruction.
- Automatic authorization for genuinely valid placement sites.
- Full holographic preview for every approved building.
- Complete structure rotations.
- Reliable construction interruption and recovery.
- Safe NPC spawning.
- Proper permissions under FTB Chunks and OPAC.
- Building construction progress visible through Royal Gilded UI.

Do not bypass protection systems to permit offline construction.

---

# 5. COMPLETE THE ENTIRE KINGDOM ECONOMY

Implement the previously approved hybrid virtual and physical currency system.

### Currency

- Copper: 1 coin.
- Silver: 10 coins.
- Gold: 100 coins.
- Platinum: 1,000 coins.
- Virtual bank accounts.
- Safe deposits and withdrawals.
- In-person transfers.
- Remote balance inspection.

Ensure Tier 2 players can earn and spend money before the Royal Bank unlocks at Tier 3.

### Royal Bank

Create the complete physical bank building, Banker NPC, and working services.

### NPC economy

Implement:

- Dynamic buying and selling prices within approved bounds.
- Shared NPC daily buying budget.
- Resource-specific quotas.
- Individual player selling limits.
- Apprentice, Journeyman, and Master profession bonuses.
- Limited shop stocks and restocking.
- Profession contracts.
- Daily and weekly reward pools.
- Transaction logging and recovery.

Preserve previously approved economy rules, including daily budgets and allowances from the decision register.

### Auctions

Complete:

- Fixed-price auctions.
- Timed auctions.
- Bidding.
- Minimum bid increases.
- Anti-sniping extensions.
- Buy orders.
- Partial fills.
- Normal and Master equipment taxes.
- Item escrow.
- Seller proceeds.
- Mailbox deliveries.
- Expired listing handling.
- Recovery from server restarts.

The economy must not allow money duplication, infinite buy/sell arbitrage, or duplicate item deliveries.

Use durable transaction semantics and test multiplayer race conditions.

---

# 6. BUILD EVERY APPROVED KINGDOM BUILDING

Every building must include:

1. An actual Minecraft structure.
2. A functional blueprint.
3. A holographic preview.
4. Aldric construction support.
5. A correct unlock tier.
6. An NPC where specified.
7. Real services and interfaces.
8. A founder title.
9. FTB Quest integration.
10. Persistent world records.
11. Admin inspection/testing/recovery.

Complete the approved public buildings:

- Starting Builder's Hut.
- Town Hall.
- Royal Bank.
- Royal Bounty Hall.
- Fisherman's Wharf.
- Royal Aerodrome.
- Royal Legacy Monument.
- All 12 profession buildings.

### Royal Bounty Hall

Tier 3.

Integrate Bountiful-inspired contracts, hunting objectives, resource deliveries, community bounties, and meaningful coin rewards.

Include a named Bounty Master NPC.

### Fisherman's Wharf

Tier 3.

Integrate Aquaculture 2 fishing mechanics, fishing contracts, fish trading, equipment sales, rare-fish collections, and optional competitions.

Include a named Fishmonger NPC.

### Royal Aerodrome

Tier 6.

Integrate Immersive Aircraft with:

- Aircraft purchasing.
- Registration and ownership.
- Repairs and upgrades.
- Flight contracts.
- Cargo deliveries.
- Scouting missions.
- Named Flight Engineer.
- Hangar and airfield construction.

Aircraft must require both Tier 6 and completed Aerodrome construction.

### Royal Legacy Monument

Tier 12.

Record kingdom history, contributors, founding players, profession achievements, completed buildings, and the Ender Dragon finale.

### Explicit exclusion

The Royal Storehouse has been removed from the new-world design.

Do not reintroduce it.

Do not implement additional brainstormed buildings that were never approved as mandatory kingdom content.

---

# 7. IMPLEMENT ALL TWELVE PROFESSIONS

The twelve professions are:

1. Farmer.
2. Miner.
3. Blacksmith.
4. Builder.
5. Lumberjack.
6. Merchant.
7. Engineer.
8. Mechanic.
9. Mage.
10. Adventurer.
11. Alchemist.
12. Librarian.

Each profession must have three ranks:

**Apprentice → Journeyman → Master**

Implement 24 profession progression quests per profession:

- 8 Apprentice.
- 8 Journeyman.
- 8 Master.

**Total: 288 profession quests.**

Profession progression should take roughly 2–3 weeks of meaningful play per profession for an active player, adjustable after balance testing.

Rules:

- Maximum three active professions.
- Switching costs 15,000 coins.
- Progress remains preserved when inactive.
- Inactive perks and purchasing privileges are disabled.
- Each profession has its own NPC and building.
- Profession quests are integrated into FTB Quests.
- Ranks unlock new equipment and services.

### Journeyman equipment

Journeyman rewards should significantly improve the profession, not just offer trivial convenience.

Examples include specialized farming, mining, construction, enchantment, magic, engineering, trading, and exploration tools.

### Master equipment — MUST BE OP

The user explicitly wants Master rewards to be extremely powerful.

Master equipment should be game-changing, desirable, and worth spending weeks unlocking.

Preserve the established signature Master reward concepts:

- Farmer: Fortune X Hoe.
- Miner: Prospector's Pickaxe.
- Blacksmith: Extraction Anvil.
- Builder: Architect's Wand.
- Lumberjack: Worldtree Axe.
- Merchant: Royal Trade Ledger.
- Engineer: Royal Machinery Core.
- Mechanic: Advanced Mekanism progression and Tier 12 Creative Energy Cube.
- Mage: Archmage's Staff.
- Adventurer: Relic Hunter's Compass.
- Alchemist: Philosopher's Brewing Stand.
- Librarian: reward design according to the latest explicit approval.

Alternative reward designs discussed in ChatGPT were not automatically approved. Do not silently replace signature rewards with those alternatives.

If a particular reward remains undefined, mark the missing design precisely and provide a recommended configurable candidate rather than claiming it was already approved.

### Master purchasing

Mastery unlocks the right to purchase exclusive equipment from the matching profession building.

Master items may be traded and used by other players under the established rules.

Repeat purchases of the same item by the same player follow:

`basePrice × 1.5 ^ previousSuccessfulPurchases`

The purchased equipment must be powerful but must not bypass:

- Claim protection.
- Tier access.
- Dimension restrictions.
- Server ownership rules.
- Item transaction safety.
- Restricted technology gates.

Preserve the rare three-minute non-stacking Fortune X Potion and Arcane Fortune Catalyst rules.

Do not nerf all powerful equipment with arbitrary cooldowns.

Prioritize meaningful power and strong technical safeguards.

---

# 8. COMPLETE THE FULL FTB QUESTS CAMPAIGN

The modpack snapshot includes FTB Quests 2101.1.37.

Build and verify a full, interconnected quest campaign.

FTB Quests must be the main progression guide for the player.

Kingdom SMP must remain the authoritative rules engine.

Create actual playable chapters for:

- Kingdom founding.
- All kingdom tiers.
- Town Hall and blueprints.
- Every approved public building.
- All twelve professions.
- All 288 profession quests.
- Farming and cooking.
- Aquaculture.
- Productive Bees.
- Mystical Agriculture.
- Create and its installed add-ons.
- Mekanism.
- Ars Nouveau.
- Iron's Spells.
- Occultism.
- Exploration and dungeon content.
- Aether.
- Nether.
- Twilight Forest.
- Deeper and Darker.
- Immersive Aircraft.
- Waystones.
- Ad Astra.
- Boss encounters.
- Tier 12 End progression.
- Kingdom Legacy finale.

Review all meaningful gameplay mods in the full supplied snapshot and add coherent progression where appropriate.

Do not create pointless quests for performance mods, libraries, or client-only UI utilities.

### Quest design

Each quest must have:

- A useful objective.
- Valid prerequisites.
- Correct mod-specific items/actions.
- Appropriate team or personal progress.
- Clear descriptions.
- Appropriate rewards.
- Correct kingdom-tier restrictions.
- Proper persistence.
- Tested completion behavior.

No placeholder quests.

No empty chapters.

No rewards that grant locked items before their tier.

No duplicate rewards after reconnection or reload.

Include shared kingdom, personal achievement, profession, and optional challenge quests.

Verify actual quest loading in Minecraft and synchronization between Kingdom SMP and FTB Quests.

---

# 9. COMPLETE MODPACK INTEGRATION

The goal is one cohesive game, not disconnected mod progression.

Inspect the actual mod list and verify relevant APIs, item registries, recipes, and dimension identifiers.

Integrate:

- Create and its add-ons.
- Mekanism.
- Mystical Agriculture.
- Productive Bees.
- Farmer's Delight.
- Aquaculture 2.
- Ars Nouveau.
- Iron's Spells.
- Occultism.
- Immersive Aircraft.
- Waystones.
- Aether.
- Twilight Forest.
- Deeper and Darker.
- Ad Astra.
- Dungeon and boss mods.
- FTB Chunks.
- FTB Teams.
- FTB Ranks where appropriate.
- MineColonies where compatible.
- Sophisticated Storage/Backpacks.
- Other important gameplay mods in the manifest.

All unlocks must be server authoritative.

Audit alternative acquisition routes:

- Crafting.
- Machines.
- Loot.
- Trading.
- Quest rewards.
- Player transfer.
- Item placement.
- Equipment usage.
- Dimension teleportation.
- Automated systems.

Respect tier gates even if an item is obtained through another mod.

Do not pretend integrations work when actual compatibility has not been tested.

---

# 10. COMPLETE ROLES AND TITLES

Implement:

### Building founder titles

The first blueprint buyer earns an honorary building-specific title.

### Profession titles

Apprentice, Journeyman, and Master identities for every profession.

### Achievement titles

Recognition for major contributions, exploration achievements, kingdom milestones, and important boss victories.

Allow players to select and display an earned title.

Building founders may receive limited approved management privileges, but never unrestricted operator powers or access to private inventories.

Separate visual titles from actual permissions.

---

# 11. FINISH EVERY ROYAL GILDED INTERFACE

Make all major systems usable through polished, consistent UI.

Required:

- Kingdom overview.
- Milestone contributions.
- Tier progression.
- Blueprint marketplace.
- Construction previews.
- Construction progress.
- Profession selection.
- Profession quests.
- Profession equipment shops.
- Bank.
- Auctions.
- Mailboxes.
- NPC dialogue.
- Bounty Hall.
- Fisherman's Wharf.
- Aerodrome.
- Player titles.
- Administrator management.
- FTB Quests access and integration.

Maintain the existing parchment, gold, bronze, and dark Royal Gilded theme.

No dead buttons, misleading confirmations, or fake shop entries.

Every interface must operate on server-validated state.

Test normal and smaller resolutions, actual mouse interactions, remappable controls, and network recovery.

---

# 12. COMPLETE THE ADMIN COMMAND SYSTEM

The server owner must be able to test every mechanic without completing the entire game normally.

Extend the existing operator-only commands into a complete admin suite.

Use `/kingdom admin` as the primary namespace where practical.

Required controls include:

**Kingdom**
- Inspect and set tier.
- Complete/reset test milestones.
- Give/consume test contribution resources.
- Validate requirements.
- Inspect and modify border state safely.
- View progression records.

**Economy**
- Give/take/set coins.
- Inspect player balances.
- Restock NPC shops.
- Create test transactions.
- Inspect/refund test purchases.
- Inspect auctions and escrow.

**Blueprints**
- Give test blueprints.
- Inspect entitlements.
- Recover missing plans.
- Reset isolated test fixtures.
- Test exclusive purchases.
- Inspect construction reservations.

**Buildings**
- Preview and validate sites.
- Inspect/pause/resume projects.
- Verify completion.
- Repair recoverable structure data.
- Inspect/recover NPCs safely.

**Professions**
- Activate/deactivate professions.
- Grant XP.
- Set ranks.
- Complete/reset isolated test quests.
- Give test equipment.
- Inspect Master purchase pricing.

**FTB Quests**
- Inspect quest IDs.
- Test sync.
- Test locked rewards.
- Reset sandbox progression.
- Validate chapter dependency graphs.

**Travel and technology**
- Test dimension gates.
- Override aircraft gates in sandbox mode.
- Test Waystone locking.
- Inspect technology restrictions.

### Administration safety

- Operator permissions.
- Confirmation for destructive actions.
- Audit logs.
- No silent world resets.
- No unrestricted duplicate entitlements.
- Sandbox/test-mode separation.
- Actual command documentation.

Every significant gameplay system must have a reliable administrator test path.

---

# 13. AUTOMATIC INTEGRATION AND QUALITY TESTING

Implement strong automated tests alongside gameplay.

Test at minimum:

- Complete Tier 1–12 progression.
- Milestone dependency validation.
- All blueprint purchases.
- Simultaneous first-buyer races.
- Banking and auction transaction safety.
- Mailbox deliveries.
- All profession ranks and quests.
- Every Master purchase and price multiplier.
- FTB Quests synchronization.
- Aircraft unlock rules.
- Dimension restrictions.
- Waystones.
- Modded recipe bypass attempts.
- FTB Chunks and OPAC protection.
- NPC persistence.
- Construction restart recovery.
- Legacy world compatibility.
- Admin permissions.
- Multiplayer synchronization.
- Client/server compatibility.

Run:

- Full Gradle build.
- JUnit tests.
- NeoForge GameTests.
- Dedicated-server integration tests.
- Actual graphical client tests.
- Multiplayer concurrency tests.
- Save/restart tests.
- Full modpack compatibility tests.

The complete 277-entry modpack must be treated as the release compatibility target.

Do not certify the full pack merely because a reduced subset works.

Use actual matching mod versions and document all test results.

---

# 14. COMPLETE END-TO-END PLAYTHROUGH

Before labeling the game 1.0.0, verify a full progression path.

Start a clean test world.

Perform the following:

1. Establish kingdom ownership.
2. Obtain and place the Builder's Hut Blueprint.
3. Meet Aldric.
4. Gather/contribute Tier 1 requirements.
5. Reach Tier 2.
6. Receive and build the Town Hall.
7. Meet Elara.
8. Earn coins.
9. Purchase an exclusive building blueprint.
10. Construct a public building.
11. Reach Tier 3.
12. Use the Bank.
13. Use NPC markets and auctions.
14. Complete Bounty Hall and fishing contracts.
15. Start professions.
16. Advance a representative profession through all three ranks.
17. Purchase and use a Master item.
18. Complete verified FTB quests.
19. Unlock Aether at Tier 4.
20. Unlock Nether at Tier 5.
21. Complete Aerodrome and unlock aircraft at Tier 6.
22. Unlock Twilight Forest.
23. Progress through industrial content.
24. Unlock Otherside at Tier 8.
25. Unlock Waystones at Tier 9.
26. Progress through Ad Astra at Tiers 10–11.
27. Reach Tier 12 without End-exclusive prerequisites.
28. Unlock the End.
29. Complete the Dragon finale.
30. Complete the Royal Legacy Monument.

Use controlled test acceleration only when justified, and document any portions not completed at normal gameplay speed.

Also validate all remaining profession rewards and major systems separately.

Perform restart tests at critical moments.

Preserve real world saves and test evidence.

---

# 15. WHAT TO DO WHEN YOU ENCOUNTER A PROBLEM

I do not want you to stop for every routine technical decision.

If a problem occurs:

1. Diagnose it.
2. Inspect logs and source.
3. Reproduce it where possible.
4. Fix it.
5. Add a regression test.
6. Rebuild.
7. Continue with the next dependent feature.

If a dependency cannot be downloaded, investigate legitimate alternate repositories, versions, mirrors, or cached artifacts. Do not bypass TLS verification or distribute unauthorized binaries.

If an external API cannot support a feature, implement the strongest supported safe alternative or report a specific release blocker.

If a previously unspecified numerical value is required, select a documented, configurable default when permitted by the decision register.

Do not overwrite explicitly approved values.

If gameplay requirements conflict, follow the latest explicit user approval and record the resolution.

Do not silently invent major new gameplay features or replace approved signature rewards.

Avoid large risky rewrites when a smaller extension of working code is possible.

---

# 16. DO NOT STOP AT ANOTHER DEVELOPMENT CHECKPOINT

Checkpoints are useful for preserving working progress, but they are not the final objective.

Once a feature is implemented and tested, proceed to the next feature in the dependency chain without waiting for me to provide another prompt, for as long as the current execution session permits.

Required sequence:

**Marketplace → currency → mailboxes → Bank → auction/economy → other buildings → professions → Master rewards → remaining tiers → mod integrations → FTB Quests → administration completion → end-to-end testing → final release.**

Parallelize independent work when supported safely.

Do not sacrifice save compatibility, testing, or correctness merely to advance faster.

Maintain an up-to-date implementation tracker with verified completion evidence.

At session boundaries, leave precise continuation instructions so the next authorized Codex run can resume without another planning exercise.

---

# 17. RELEASE REQUIREMENTS

The final target is:

**Kingdom SMP 1.0.0**

Release it only when all approved release-critical requirements have been implemented and verified.

Deliver:

- Final NeoForge JAR.
- Required FTB Quests files.
- Required configs/datapacks.
- Server installation guide.
- Client installation guide.
- Player guide.
- Full admin command manual.
- Progression and profession documentation.
- Building and blueprint documentation.
- Compatibility report for the full modpack.
- Test results and logs.
- Actual gameplay screenshots.
- SHA-256 checksums.
- Changelog.
- Known limitations.
- Safe legacy-world migration instructions.
- Release tag and Git commit SHA.

Put the downloadable build and installation information somewhere obvious, preferably in the GitHub release area or the repository's release documentation.

Do not merge into main without authorization.

If release acceptance fails, publish a clearly labeled development or release-candidate build instead and keep the failing criteria visible.

---

# 18. FINAL DEFINITION OF DONE

The game is finished only when:

- All twelve tiers work.
- All approved public buildings are physically constructible and functional.
- The Blueprint Market works.
- Banking and auctions work.
- All twelve professions work.
- All 288 profession progression quests work.
- Journeyman and approved Master equipment work.
- Master rewards are genuinely OP.
- FTB Quests provides a functioning interconnected campaign.
- Important gameplay mods are integrated.
- Aircraft and Aerodrome restrictions work.
- Waystones unlock at Tier 9.
- The End unlocks at Tier 12.
- The Royal Legacy finale works.
- NPCs and construction survive restarts.
- Multiplayer economies and entitlements are secure.
- Admin tools can test the game.
- Existing worlds remain safe.
- The client and dedicated server are compatible.
- The full modpack has been tested.
- No known critical release-blocking bugs remain.
- The final JAR and documentation are ready for players.

Everything must be backed by real implementation and verifiable evidence.

Do not claim a subsystem is complete based on documentation, scaffolding, or unexecuted tests.

---

# 19. FINAL INSTRUCTION

You have permission to complete the entire approved Kingdom SMP project on the development branch.

**Do not ask me to send another prompt after each milestone.**

I want you to act as the lead developer, independently handle routine engineering decisions, and work toward a complete finished game.

Be proactive about finding and fixing issues.

Keep the code stable, preserve saves, and verify the actual gameplay.

I care about having a real, polished, playable 10-player Kingdom SMP server more than having impressive development summaries.

At each execution boundary, preserve your progress and report precisely what is implemented, tested, and still incomplete.

Do not pretend you have completed work that has not actually been done.

Do not promise asynchronous execution after the session stops.

**Your goal is the complete Kingdom SMP 1.0.0 experience.**

Start from the current repository HEAD, follow the critical path, implement as much of the complete project as your execution environment allows, and do not voluntarily stop at a planning-only response.

# BEGIN THE FULL IMPLEMENTATION.
