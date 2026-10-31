# KINGDOM SMP — MASTER IMPLEMENTATION DIRECTIVE
## Royal Gilded Rebuild · Minecraft Java 1.21.1 · NeoForge
### Full Kingdom Progression, Construction, Economy, Professions, Mod Integrations, FTB Quests, and Administration

You are the lead Minecraft NeoForge engineer, systems architect, game designer, test engineer, and technical integrator responsible for delivering Kingdom SMP as a polished, genuinely playable multiplayer mod.

**Repository:** https://github.com/MohyMods/Kingdom.SMP  
**Working branch:** `rebuild/royal-gilded`  
**Minecraft:** Java Edition 1.21.1  
**Loader:** NeoForge  
**Java:** 21  
**Target:** Approximately 10 active multiplayer friends  
**Theme:** Medieval fantasy kingdom evolving into a magical and technological civilization  
**UI identity:** Royal Gilded — polished slate, parchment, gold, and bronze interfaces

This directive supersedes conflicting historical planning rules where explicitly stated below. Do not discard working code merely because an older document describes a different design.

## 0. Mission and execution rules

Deliver a cohesive gameplay experience in which all major systems reinforce each other:

**Kingdom tiers → buildings → professions → mod technology → quests → economy → exploration → further kingdom progression.**

Players must feel that their village is expanding and that every tier brings meaningful gameplay.

This is an **implementation authorization**, not merely a request to generate more planning documents. Begin work after auditing the repository and identifying the current implementation.

However, DO NOT attempt to implement the entire project as one enormous untested change. Work in small, playable, well-tested milestones. Implement as much verified functionality as can be safely completed in each execution session, commit that work, and document the next steps.

Mandatory operating rules:

1. Inspect existing source, documentation, tests, commits, and working systems before editing.
2. Read the repository's requirements, decisions, roadmap, construction, progression, and continuation documents.
3. Treat the latest explicitly approved rules in this directive as higher priority than older conflicting documents.
4. Preserve known-working systems, including Aldric, existing construction, durable contributions, and the Royal Gilded UI where possible.
5. Never silently delete or corrupt existing worlds, NPC identities, balances, entitlements, contributions, or construction projects.
6. Never claim a mechanic works unless it has actually been implemented and tested.
7. Distinguish **implemented**, **tested**, **partial**, **planned**, and **blocked** functionality in reports.
8. Never use arbitrary placeholder rewards, fake UI functions, or pretend integrations to claim feature completion.
9. For unresolved minor engineering questions, choose a sensible documented approach and continue.
10. For unresolved gameplay decisions with significant economic, progression, or migration consequences, identify the blocker and avoid inventing policy.
11. Build and test frequently. Preserve reproducible checkpoint commits.
12. Do not merge or push to the default branch without authorization. Work on the designated development branch.
13. Provide a working installable JAR at meaningful gameplay checkpoints, along with exact build instructions and an honest test report.
14. Keep repository documentation synchronized with the actual implementation.
15. Maintain a master feature-completion tracker and a continuation document so future Codex sessions can resume reliably.

The final goal is a complete project, but **each milestone must be functional and verifiable before expanding into dependent systems**.

---

# 1. Existing project and compatibility audit

The project already contains previous implementation work. Before making changes, determine which systems are genuinely operational.

Previously reported functionality includes:

- Starting Builder's Hut and Aldric Stonehand.
- Persistent Tier 1 donations.
- Tier 1 resource counting and contribution leaderboard.
- Tier 1 → Tier 2 progression.
- Existing Founders' Lodge blueprint and construction.
- Reeve Mira Ashford.
- Royal Gilded interface foundations.
- Server-authoritative construction workflows.
- Saved construction recovery.
- Data-driven milestone definitions and dependency validation.

These are historical reports, not permission to assume every feature remains correct. Inspect current code and tests.

**Legacy compatibility is mandatory.**

Existing worlds may contain the Founders' Lodge, Mira Ashford, old blueprints, construction jobs, and historical contribution records.

For **new worlds**, apply the revised Tier 2 Town Hall progression described below.

For **existing worlds**, preserve the original Lodge progression and entitlements rather than automatically overwriting or converting them.

If a migration would change historical rewards, require explicit versioned migration policy. Do not silently grant a duplicate Town Hall or remove Mira.

Preserve stable internal identifiers and separate NPC display names from persistent entity identities.

---

# 2. Kingdom progression — 12 tiers

The kingdom begins at Tier 1 and advances through shared contributions, construction milestones, and meaningful community objectives.

Tier advancement is global, not per player.

A tier should advance automatically when all approved requirements are satisfied. Administrators approve only objectives requiring human judgment, such as certain manually built roads or community construction checks.

**Tier 1 fixed contributions:**

- 1,024 logs
- 2,048 cobblestone
- 512 wheat
- 128 iron ingots

Preserve existing authorized consumption, cap enforcement, contribution accounting, and tie rules.

Later-tier resource quantities must be reviewed against the actual modpack, playtime targets, and availability. Previously documented later-tier values may be proposals rather than approved executable data.

Design principles:

- Each tier adds new resource types and community challenges.
- Avoid simply multiplying cobblestone and wood requirements indefinitely.
- Rare or difficult resource categories receive greater contribution weight than common resources in later-tier scoring.
- Automated production must be balanced.
- No mandatory resource may require access to a dimension or technology that has not yet unlocked.
- Prevent indirect crafting or machine-processing dependency loops.
- Players must not need every profession mastered to advance the kingdom.
- Approved tier requirements must be fully achievable by approximately 10 players.
- Expanding the world border must remain safe and recoverable.

### Confirmed dimension unlocks

| Kingdom tier | Dimension or capability |
| --- | --- |
| Tier 4 | The Aether — target progression |
| Tier 5 | Nether |
| Tier 6 | Twilight Forest — target progression |
| Tier 8 | Deeper and Darker / Otherside — target progression |
| Tier 9 | Waystones become eligible |
| Tier 10–11 | Reviewed Ad Astra planetary progression |
| Tier 12 | **The End** |

**The End must remain locked until Tier 12.** This supersedes all previous Tier 9 End unlocks.

Reaching Tier 12 must not depend on End-exclusive items, End structures, End crafting ingredients, End-only machine resources, or indirect recipes requiring End access.

The Ender Dragon battle and End exploration belong to the Tier 12 finale, after the dimension becomes accessible.

Inspect and enforce alternate access routes provided by other mods. A dimension gate that can be bypassed through a modded teleport item is not complete.

Do not invent compatibility hooks; verify them against the actual installed mod versions.

---

# 3. Central Town Hall and initial construction

## Builder's Hut — Tier 1

The server owner receives a free physical Builder's Hut Blueprint.

Using the blueprint:

1. Hold the item.
2. Preview the full building hologram.
3. Choose a valid site.
4. Confirm construction.
5. The Hut is established immediately as a bootstrap exception.
6. Aldric Stonehand spawns and becomes the designated kingdom builder.

Aldric's existing name and persistent identity must remain unchanged.

Before the Town Hall exists, Tier 1 contributions are handled through the Builder's Hut and the existing kingdom contribution system.

## Town Hall — Tier 2

The Tier 1 highest contributor receives the exclusive Town Hall Blueprint.

**Town Hall footprint:** 19 × 15 blocks.  
**Floors:** Two.  
**Ownership:** Public kingdom building.

The Town Hall provides:

- Kingdom Contributions Board.
- Tier milestone and leaderboard interface.
- Royal Architect Blueprint Market.
- General NPC shop selling limited common essentials.
- Private player mailboxes with in-person item collection.
- Auction services once their Tier 3 requirements are satisfied.
- Kingdom information, announcements, and records.

A named Royal Steward NPC serves the Town Hall. New NPC display naming follows the blueprint-holder naming rule; preserve previously existing NPCs.

The Town Hall is the kingdom's primary contribution location from Tier 2 onward.

Contributions and communal storage must never be confused: donations are permanently consumed and credited to the contributor. They are not withdrawable items.

Players may inspect milestone progress remotely, but the intended ordinary donation workflow is through the appropriate physical contribution service. Review legacy remote donation behavior and provide a safe transition.

### Tier 3 Royal Bank

A dedicated, separate building supporting:

- Deposits and withdrawals.
- Physical coin/virtual balance conversion.
- Copper = 1, Silver = 10, Gold = 100, Platinum = 1,000.
- Player-to-player bank transfers.
- Secure and recoverable transaction records.
- Remote balance viewing.
- Appropriate protections for full inventories, insufficient balance, retries, crashes, and double spending.

**The Royal Storehouse has been removed from the new design. Do not implement or award it for new worlds.** Preserve existing historical data where needed for compatibility.

---

# 4. Royal Blueprint Market — exclusive first buyer

This is a major approved change.

**All building blueprints after the starting Builder's Hut and initial Town Hall are purchased at the Town Hall.**

The previous rule awarding all tier blueprints to the top contributor is superseded.

### Acquisition

- Builder's Hut: free to the server owner.
- Initial Town Hall: awarded to the Tier 1 highest contributor.
- Every subsequent public and profession building: sold through the Royal Architect counter.

### Market rules

1. A blueprint appears when its required kingdom tier is reached and any additional approved conditions are met.
2. Each building has **one primary construction entitlement per kingdom**.
3. The first player to complete a valid purchase receives that entitlement.
4. The listing becomes SOLD OUT for all other players.
5. Prices are between **500 and 15,000 kingdom coins**.
6. Exact individual prices must be centrally configurable and reviewed. Suggested prices in previous discussions are proposals, not automatically final.
7. Concurrent purchase attempts must be atomic: only one succeeds and only the winner is charged.
8. Failed attempts must not deduct coins.
9. A physical blueprint item represents a server-authoritative entitlement; copied items cannot create additional buildings.
10. Lost blueprints must be recoverable without granting duplicate construction rights.
11. Purchasers select placement and may choose the display name of an associated new NPC.
12. Completed buildings become public kingdom property, regardless of who bought the blueprint.
13. Admins may recover or reassign abandoned blueprints using documented, permissioned, audited recovery operations.
14. Do not automatically return money or revoke an entitlement without a defined recovery policy.
15. Highest contributors still receive separately approved recognition, cosmetic rewards, and coin bonuses, but not extra building blueprints.

**Economy bootstrap requirement:** Tier 2 blueprints must be purchasable before the Tier 3 Bank exists. Define an authoritative balance/coin payment workflow that functions without visiting the Bank.

---

# 5. Full physical-blueprint hologram construction

Every kingdom building uses the same polished construction workflow.

- Distinct carryable blueprint for each building.
- Complete translucent 3D preview, including walls, floors, interiors where visible, and roof.
- Crosshair-directed positioning.
- Rotation in 90-degree increments.
- Configurable height adjustments.
- Remappable controls; proposed defaults: R, Shift+R, Page Up, Page Down.
- Green for confirmed valid sites, red for blocked sites, amber for pending/stale checks.
- Highlight obstructing blocks and invalid support columns.
- Explain invalid sites with clear messages.
- Royal Gilded review/confirmation interface.
- Preserve selected position and rotation during confirmation.
- Server revalidation immediately before reserving construction.
- One-time durable entitlement redemption.
- Aldric visibly constructs the building in the world.
- Pause and resume safely after disconnects, chunk unloading, restarts, and interrupted jobs.
- Register building completion only after the required structure has been verified.

**Updated placement approval:** Ordinary public-building placement does not require a routine administrator click if the complete server-authoritative validation succeeds.

Never trust client-only green status. Verify claims, other reservations, structures, world border, dimension, protections, height limits, support, and current entitlement.

**Automatic foundations:** Aldric may create safe supports up to a maximum depth of **4 blocks** underneath planned foundation positions. No destructive excavation or overwriting player structures, containers, or protected blocks. Reject unsupported deep gaps rather than modifying unsafe terrain.

FTB Chunks and any installed protection mods must be considered. Unsupported or ambiguous claim permissions must fail safely.

Manually constructed milestone roads may still require admin inspection/approval.

A blueprint purchase is not permission to bypass construction validation.

---

# 6. Building registry and milestones

All buildings must be centrally registered with stable IDs, blueprint definitions, NPC assignments, prerequisites, construction state, and service unlock rules.

Preserve the approved 12 profession buildings and their relevant tier schedule. Profession buildings should remain compact, fitting a dense medieval village.

Their services include quests, equipment, basic profession shops, progression information, and matching Master-exclusive purchases.

**Master equipment purchases occur at the corresponding profession building, not a single centralized Master Shop.**

### New confirmed buildings

#### Royal Bounty Hall — Tier 3

Inspired by the installed Bountiful mod.

Services:

- Daily and weekly bounty contracts.
- Monster-hunting objectives.
- Material collection and delivery contracts.
- Coin rewards.
- Appropriate profession XP rewards.
- Community-wide rotating bounties.
- Named Bounty Master NPC.
- Integration with kingdom contribution and quest systems without duplicate reward exploits.

Proposed initial blueprint price: 1,500 coins, pending economy balance verification.

#### Fisherman's Wharf — Tier 3

Inspired by installed Aquaculture 2.

Services:

- Fish buying and selling.
- Fishing quests and contracts.
- Fishing equipment purchases.
- Rare-fish collection objectives.
- Fishing competitions/events.
- Named Fishmonger NPC.
- Sensible purchase quotas and anti-exploit economy rules.

Proposed initial blueprint price: 1,000 coins, pending economy balance verification.

**Only these two additional suggested milestone buildings are explicitly approved for this implementation brief.** Do not add every previously brainstormed shrine, museum, greenhouse, power plant, academy, or other proposed building as though approved.

### Royal Aerodrome — Tier 6

**This building is confirmed.**

Integrate the installed Immersive Aircraft mod.

Services:

- Aircraft purchasing.
- Aircraft registration and persistent ownership records.
- Repair and refueling services.
- Approved aircraft modifications and upgrades.
- Flight contracts, exploration, scouting, and cargo deliveries.
- Named Flight Engineer NPC.
- Compact hangar office plus suitable outdoor airfield or landing area.

**Aircraft unlock requires BOTH:**
1. Kingdom Tier 6 or higher.
2. Verified completion of the Royal Aerodrome.

Reaching Tier 6 alone is insufficient.

Enforce restrictions against crafting, deployment, and operation of locked aircraft. Test acquisition through loot, commands, trades, existing entities, and other mods where relevant.

Proposed individual aircraft progression:

- Tier 6: Gyrodyne and Airship.
- Tier 7: Biplane.
- Tier 8: Cargo Airship and Quadrocopter.
- Tier 10: Warship.

This per-aircraft schedule is a balance proposal. Verify actual aircraft types, capabilities, identifiers, and available integration hooks before activating it.

The Aerodrome may support Engineer, Mechanic, and Adventurer profession activities, but ownership of the building never automatically grants Master rank.

### Waystones — Tier 9

Before Tier 9, ordinary Waystone teleportation must be disabled.

Enforce the restriction across applicable Waystone blocks, consumables, interfaces, and alternative travel items. Inspect the installed Waystones version and its supported configuration/API.

At Tier 9, the kingdom unlocks Waystone functionality.

A separate Waystone Sanctuary building is only a proposal, **not currently an approved mandatory prerequisite**.

### Royal Legacy Monument — Tier 12

The old Tier 12 Town Hall design is superseded.

The Tier 12 capstone is the Royal Legacy Monument, celebrating:

- Kingdom tiers reached.
- Major contributors.
- Blueprint buyers and founders.
- Completed buildings.
- Profession achievements.
- Major dimension expeditions.
- Ender Dragon victory.

Preserve the Tier 12 End unlock and the existing Master Mechanic Creative Energy Cube progression rule.

---

# 7. Twelve-profession progression

Implement the approved professions:

1. Farmer
2. Miner
3. Blacksmith
4. Builder
5. Lumberjack
6. Merchant
7. Engineer
8. Mechanic
9. Mage
10. Adventurer
11. Alchemist
12. Librarian

Ranks:

**Apprentice → Journeyman → Master**

Each profession has **24 meaningful progression quests**, consisting of:

- 8 Apprentice quests.
- 8 Journeyman quests.
- 8 Master quests.

Total planned profession quests: **288**.

Profession mastery should target roughly 2–3 weeks of meaningful gameplay for an active player, subject to playtesting.

Rules:

- Maximum 3 active professions.
- Switching a profession costs 15,000 coins under the approved replacement rule.
- Inactive profession progress and rank remain saved.
- Inactive profession perks and specialized purchase privileges are disabled.
- Journeyman unlocks useful upgraded equipment/services.
- Master unlocks eligibility to purchase exceptional exclusive equipment.
- Equipment can generally be traded and used by others, unless a particular item has a separately approved restriction.
- Master purchase prices increase by 50% compounded for repeated successful purchases of the same item by the same player.
- Master purchases occur only at the corresponding profession building.

Preserve approved Master reward concepts such as the Fortune X Hoe, Extraction Anvil, Prospector's Pickaxe, Architect's Wand, Worldtree Axe, Royal Trade Ledger, Royal Machinery Core, Archmage's Staff, Relic Hunter's Compass, Philosopher's Brewing Stand, and the Tier 12 Master Mechanic Creative Energy Cube.

The Librarian's complete reward and any other undefined mechanics remain design blockers rather than permission to invent unbalanced equipment.

For each quest, define a real objective, prerequisite, progress attribution, validation, rank XP, rewards, and anti-exploit conditions.

Avoid repetitive filler quests that exist only to reach 288.

---

# 8. Full FTB Quests integration — major requirement

**The entire modpack should have a coherent, interconnected quest experience.**

FTB Quests is intended to be the player's primary progression journal. Kingdom SMP remains the server-authoritative rules engine.

The supplied crash-report mod list confirms FTB Library, FTB Teams, FTB Ranks, and FTB Chunks, but FTB Quests itself was **not confirmed installed** in that snapshot.

Therefore:

1. Verify a compatible FTB Quests release for Minecraft 1.21.1 and NeoForge.
2. Verify dependencies, distribution permissions, and actual server/client compatibility.
3. Report if the mod must be added to the pack.
4. Do not claim integration is complete if only FTB Quests-compatible data files have been drafted.
5. Use supported FTB Quests capabilities and tested integration adapters rather than guessed APIs.
6. Produce real installable quest data and ensure it loads in the actual client.

### Quest system architecture

Build integrated chapters for:

- Kingdom Foundation and Tier Progression.
- Town Hall, Blueprints, and Construction.
- Economy, Banking, Auctions, and Commerce.
- All 12 Professions and their ranks.
- Farming and Cooking.
- Aquaculture and Fishing.
- Productive Bees.
- Create and installed Create add-ons.
- Mekanism and Mekanism Generators.
- Mystical Agriculture.
- Ars Nouveau and Ars Creo.
- Iron's Spells.
- Occultism.
- Immersive Aircraft and Royal Aerodrome.
- Aether exploration.
- Twilight Forest progression.
- Deeper and Darker.
- Bountiful contracts.
- Bosses, dungeons, and combat mods.
- Waystones and transportation.
- Ad Astra planetary progression.
- Tier 12 End exploration and kingdom finale.

Cover additional meaningful gameplay mods confirmed in the actual modpack manifest, rather than treating only the most famous mods as important.

Do not create redundant quest chapters for pure libraries, performance mods, or client-only utilities.

### Quest categories

**Kingdom-wide quests:** Shared construction, resource milestones, major expeditions, infrastructure, and global progression.

**Personal quests:** Player achievements, tutorials, exploration goals, crafting, collection, and skill development.

**Profession quests:** The 24 quests per profession, with appropriate active profession/rank rules.

**Optional challenges:** Advanced projects, rare collections, trophy hunts, special contracts, and achievements that reward extra engagement without blocking the whole kingdom.

### Mandatory synchronization

FTB Quests and Kingdom SMP must not become two independent, contradictory progression systems.

Use stable IDs and explicit integration mappings.

Completing a Kingdom SMP milestone should update its corresponding FTB Quest progress. Where the integration supports it, completing verified FTB Quests objectives may notify Kingdom SMP, but the server must independently validate any permission, currency, tier, blueprint, or rank mutation.

A quest reward must never grant an item or ability that bypasses:

- Kingdom tier restrictions.
- Profession restrictions.
- Building completion requirements.
- Aircraft restrictions.
- Waystone locks.
- Dimension gates.
- Currency and shop restrictions.
- Blueprint exclusivity.

Avoid circular requirements between quests, recipes, dimensions, buildings, and technology.

A player who joins late should see correct previously achieved shared milestones without receiving unearned personal rewards.

Design reward claiming, team credit, repeatability, and progress synchronization to prevent duplication.

Provide automated validation of the quest dependency graph and real integration tests with FTB Quests installed.

---

# 9. Fully synergistic modpack progression

Use the supplied installed-mod snapshot as the starting integration inventory.

Important gameplay mods include:

- Create and installed add-ons.
- Mekanism and Generators.
- Mystical Agriculture.
- Productive Bees.
- Farmer's Delight and cooking add-ons.
- Aquaculture 2.
- Ars Nouveau and Ars Creo.
- Iron's Spells.
- Occultism.
- Immersive Aircraft.
- Waystones.
- Aether.
- Twilight Forest.
- Deeper and Darker.
- Ad Astra.
- Cataclysm.
- Mowzie's Mobs.
- Bountiful.
- MineColonies.
- Sophisticated Backpacks/Storage.
- Mob Grinding Utils.
- Dungeon and world-generation mods.

Determine the full list by checking the actual modpack inventory, not just this summary.

### Progression philosophy

Early game: gathering, simple professions, farming, village establishment.

Middle game: workshops, Create machinery, magic, advanced farming, aircraft, alternate dimensions.

Late game: Mekanism industry, advanced rituals, challenging combat, advanced transportation, planetary exploration.

Final game: Tier 12, the End, Dragon encounter, kingdom legacy.

### Restriction coverage

Investigate each relevant mod's:

- Crafting recipes.
- Machine recipes and processing.
- Loot generation.
- Shops and villager trades.
- Quest rewards.
- Placement.
- Item use.
- Entity operation.
- Teleportation.
- Modded dimensions.
- Automated transport.
- Cross-mod conversions.
- Chunk loading.
- Existing placed machines or vehicles.

Where a technology is locked, do not rely solely on hiding recipes from JEI.

Enforce meaningful server-side restrictions, with proper integration tests. If a critical hook does not exist, report the gap and propose a tested compatible enforcement strategy.

Use versions from the installed modpack and verify the actual API before coding adapters.

**Do not assume MineColonies and Kingdom SMP's own NPC/construction systems are interchangeable.** Prevent duplicate NPC authority or overlapping construction ownership.

---

# 10. Titles, roles, recognition, and permissions

Implement a coherent identity and role system.

Three role families:

### A. Building founder titles

The first successful blueprint buyer receives a permanent honorary founder title associated with that building.

Examples:

- Royal Bank → Royal Financier.
- Royal Bounty Hall → Bounty Commissioner.
- Fisherman's Wharf → Harbormaster.
- Royal Aerodrome → Royal Aviator.

The initial Town Hall winner may receive an appropriate Founding Steward or similar title, subject to naming review.

Founders can choose the display name of a newly introduced building NPC.

Honorary titles must not automatically grant economy-admin powers, control over public storage, rank bypasses, or other broad authority.

Any operational building-management powers must be narrowly defined, permission checked, and explicitly configurable.

### B. Profession ranks

Display meaningful profession titles:

- Apprentice Farmer.
- Journeyman Blacksmith.
- Master Mechanic.

A player may possess several ranks, but only active professions grant their active benefits.

### C. Achievement titles

Recognize milestones such as:

- Highest Tier 1 contributor.
- First completed flight.
- First major dimension expedition.
- Important boss victories.
- Contributions to major kingdom projects.
- Tier 12 finale participation.

Players should be able to choose one earned title for display, with sensible chat/nameplate/UI integration.

Investigate FTB Ranks as an optional integration, but do not assume it offers the full dynamic role/title feature set.

Keep actual server permissions separate from cosmetic or honorary titles.

Persist role changes and history, and expose clear administrative inspection tools.

---

# 11. Economy and community activities

Preserve the previously approved hybrid economy:

- Virtual balances.
- Physical currency denominations.
- In-person banking.
- Player auctions.
- NPC markets.
- Profession contracts.
- Daily/weekly economic activities.

Auction House becomes available at Tier 3 after the Bank's approved availability condition.

Previously selected auction behavior includes fixed-price sales, timed auctions, buy orders, mailbox delivery, anti-sniping extensions, and the designated tax difference between normal and Master equipment.

Preserve approved dynamic NPC buying/selling quotas and profession-related allowances, but verify exact financial policies before implementing transactions.

All financial operations must be server authoritative, durable, auditable internally, and protected from duplication through retries, disconnects, full inventories, and crashes.

Do not reintroduce excluded standalone systems such as a custom direct item-trading interface or player-facing Economy Ledger without authorization.

Make the Royal Bounty Hall and Fisherman's Wharf meaningful coin-earning opportunities, but prevent them from generating unlimited money through trivial automation.

---

# 12. Complete administrator command and testing framework

This is a high-priority requirement.

I must be able to test **every major gameplay feature without having to spend weeks playing normally**.

Provide one coherent administrator command namespace, proposed as:

`/kingdom admin`

The exact registered syntax may be improved for discoverability, but deliver a complete documented command reference.

### Required administrative categories

**Kingdom progression**
- Inspect current tier, active milestones, prerequisites, and contribution records.
- Set kingdom tier from 1 to 12 in a controlled test environment.
- Advance or rewind tier for testing.
- Inspect/complete/reset objectives.
- Simulate approved contributions.
- Test border progression and dimension gates.
- Validate dependency graphs.

**Players and economy**
- Inspect, give, take, and set balances.
- Give physical coin denominations.
- Test bank deposits and withdrawals.
- Simulate safe transfers.
- Test full-inventory and insufficient-funds cases.
- Inspect purchase/audit records.

**Blueprint marketplace**
- List every building and its unlock state.
- Inspect prices and stock/entitlement status.
- Grant a test blueprint.
- Test first-buyer purchase concurrency.
- Recover a lost blueprint.
- Release or safely reassign an abandoned entitlement.
- Test blueprint expiry/invalidation where supported.
- Restore a test fixture without duplicating live entitlements.

**Buildings and NPCs**
- Inspect construction state.
- Preview and validate placements.
- Test foundation depth and collision.
- Start, pause, resume, and inspect construction.
- Test interrupted/restarted builds.
- Verify building completion.
- Repair/recover NPCs when safe.
- Test naming and title assignments.
- Inspect claim/protection failures.

**Professions**
- Inspect active and inactive professions.
- Grant/revoke profession activation for testing.
- Set rank and XP.
- Complete/reset individual quests in an isolated test context.
- Test Apprentice, Journeyman, and Master services.
- Test three-profession cap and profession switching.
- Inspect Master equipment purchase history and pricing.

**FTB Quests**
- Inspect quest mappings.
- Test team and personal quest synchronization.
- Simulate legitimate completion signals.
- Reset isolated test quest progress.
- Verify locked rewards are refused.
- Report missing integrations and broken prerequisites.

**Technology and travel**
- Inspect and override aircraft gates in test mode.
- Verify Aerodrome Tier 6 and building-completion rules independently.
- Test specific aircraft tiers.
- Inspect/relock Waystone gates.
- Inspect/relock each dimension gate.
- Test Tier 12 End restriction.
- Test magic/machine gates and automation bypasses.

**Commerce and services**
- Restock and inspect NPC shops.
- Simulate bounties and fishing contracts.
- Inspect and test auctions, bids, buy orders, and mailboxes.
- Validate delivery and transaction recovery.

### Safe testing design

Provide an **admin-only sandbox/test mode** or equivalent isolated test environment.

It must allow rapid inspection and progression while preventing contamination of normal multiplayer history.

Requirements:

1. Commands require explicit operator permissions.
2. Dangerous actions require confirmation.
3. All privileged changes are logged.
4. Support dry-run and inspection modes where practical.
5. Test overrides are clearly labeled and cannot be mistaken for normal unlocks.
6. Destructive resets must refuse unsafe operation in production unless deliberately authorized.
7. Never silently erase live player progress.
8. Prevent test-granted duplicate blueprints from becoming legitimate second public buildings.
9. Make failure cases easy to reproduce.
10. Document practical test scripts, commands, expected results, and rollback steps.

**Deliver a complete in-game help/reference interface or command listing that explains what every admin command does.**

Do not leave buttons that only display “coming soon” while claiming the admin suite is finished.

---

# 13. Visual design and player experience

Keep the Royal Gilded visual identity across:

- Kingdom overview.
- Contribution screens.
- Blueprint shop.
- Building hologram HUD.
- Construction confirmation.
- Profession selection and progression.
- NPC dialogue.
- Banking.
- Auctions.
- Mailboxes.
- Bounty Hall.
- Fisherman's Wharf.
- Aerodrome.
- Admin panels.
- Quest integration entry points.

The UI should feel like one cohesive mod rather than unrelated menus.

Design for common screen sizes and scaling settings. Make text readable and interactive controls clear.

Each major player action should communicate:

- What the player is doing.
- Why something is locked.
- What is required to unlock it.
- Whether money/items will be consumed.
- Whether progress is shared or personal.
- Whether the action succeeded.
- How to recover from failure.

Display actual server-confirmed state, not fake local estimates.

Show useful error messages rather than raw internal identifiers.

---

# 14. Performance, compatibility, and crash safety

The supplied modpack is large and includes many client, gameplay, library, and performance mods.

The user supplied a crash report showing a Java heap out-of-memory failure with a 4 GB heap limit. That is not by itself proof that any specific mod is defective.

Do not automatically blame Moonlight, Sinytra Connector, or another mod based solely on a stack trace.

Perform measured integration and performance validation.

Requirements:

- Dedicated-server startup without client-only classloading failures.
- Clean multiplayer connections.
- Persistence across restart.
- Multiple simultaneous players purchasing, donating, previewing, and interacting.
- No double-spend or duplicate blueprint entitlements.
- No duplicate NPC spawns.
- No unsafe world modifications.
- Bounded hologram memory and rendering work.
- Rate-limited server validation.
- Safe chunk-loading behavior.
- Protection against malicious or stale packets.
- Clear reports when third-party mod compatibility cannot be verified.
- Performance testing using representative modpack conditions.
- Reproducible builds using pinned dependency versions.

Prefer fixing actual measured problems to speculative optimization.

---

# 15. Required implementation sequence

Follow dependencies rather than choosing unrelated easy tasks and declaring the project nearly complete.

## Phase A — Audit and reconcile

- Inspect repository and current builds.
- Verify modpack snapshot.
- Identify outdated documentation rules.
- Create an explicit supersession/migration matrix.
- Preserve legacy worlds.
- Define current playable baseline.
- Update master completion tracker.

## Phase B — Kingdom progression and generalized blueprints

- Implement current new-world Tier 2 Town Hall transition.
- Preserve existing-world Lodge compatibility.
- Implement physical held blueprints and full holograms.
- Implement safe foundation support up to four blocks.
- Implement automatic valid-site confirmation.
- Implement secure construction entitlement lifecycle.
- Verify Aldric's recovery and persistence.

## Phase C — Town Hall and core economy

- Complete Town Hall structure and services.
- Implement the Royal Architect Blueprint Market.
- Implement first-buyer purchase concurrency and recovery.
- Implement early virtual/physical coin payment.
- Implement Royal Bank and mailbox foundation.
- Make contribution locations and UI coherent.

## Phase D — Professions and essential Tier 3 buildings

- Establish full profession system.
- Implement representative profession quests end-to-end.
- Expand through all 12 professions.
- Add Royal Bounty Hall.
- Add Fisherman's Wharf.
- Implement their actual NPC services and contracts.
- Validate economy balance.

## Phase E — Modpack integration and major gates

- Implement verified mod-specific progression adapters.
- Implement Tier 6 Royal Aerodrome and aircraft restrictions.
- Implement Tier 9 Waystone gating.
- Implement dimension schedule and Tier 12 End restriction.
- Implement meaningful mod progression without bypasses.
- Test interactions between professions, machines, magic, and dimensions.

## Phase F — FTB Quests and titles

- Add and verify compatible FTB Quests.
- Create actual quest chapters and data.
- Connect kingdom, personal, and profession progression.
- Integrate ranks and founder titles.
- Validate quest reward security, synchronization, and late joiners.
- Ensure chapter objectives match the actual installed modpack.

Quest definitions and progress mapping should be designed during earlier phases so FTB integration does not require a full rewrite.

## Phase G — Full administration and validation

Admin testing commands should begin in earlier phases and expand alongside each feature. This final phase is for comprehensive coverage, not for building the entire admin system from scratch.

- Full admin command reference.
- Sandbox/test progression.
- Integration tests.
- Multiplayer race-condition tests.
- Save/restart recovery tests.
- Dimension and mod-gate exploit testing.
- End-to-end progression testing from Tier 1 to Tier 12.
- Final playable JAR, deployment instructions, and known-issues report.

---

# 16. Definition of done

A feature is only considered complete if:

1. Its code and required data exist.
2. It works in an actual Minecraft client/server environment where relevant.
3. It respects kingdom restrictions.
4. It persists correctly.
5. Its UI or commands provide usable access.
6. Failures are handled safely.
7. Relevant automated tests pass.
8. Manual reproduction steps are documented.
9. It does not break previously completed systems.
10. Its completion state is accurately recorded.

For external mod integrations, tests must use the actual supported dependencies where possible. Unit tests against fake adapters do not prove full live integration.

For FTB Quests, a quest JSON/SNBT file existing on disk is not sufficient: verify the pack actually loads and the relevant progression works.

For the Aerodrome, a locked crafting recipe alone is not sufficient: verify deployment and operation restrictions.

For blueprints, showing a hologram is not sufficient: verify unique purchase, safe construction, persistence, and recovery.

For admin tools, listing a command in documentation is not sufficient: verify it is registered, authorized, and functional.

---

# 17. Reporting and continuation protocol

At the start:

1. Give a concise assessment of the repository's actual state.
2. Identify all new rules superseding historical code/docs.
3. State the first implementation milestone and why it is next.
4. Identify third-party integration blockers or missing dependencies.
5. Then begin implementing the first safe milestone.

After each milestone, report:

- Features implemented.
- Files changed.
- Tests executed and results.
- Dedicated-server/client verification actually performed.
- Screenshots or logs where relevant.
- Commit SHA.
- Exact JAR location, if built.
- Known incomplete work.
- Next dependency-ordered implementation step.

Maintain:

- A master completion tracker.
- A current decision register.
- A verified mod-integration compatibility matrix.
- A tier and recipe dependency matrix.
- A comprehensive admin command reference.
- A continuation guide for later Codex sessions.

Do not use vague phrases like “fully implemented” or “production-ready” when systems are only scaffolded or unit-tested.

If the session cannot finish everything, preserve and report the working checkpoint. Continue implementation in the next authorized session from actual repository state, not from assumptions.

---

# FINAL PRODUCT VISION

I want Kingdom SMP to feel like a living civilization that my friends build together.

At the beginning, we're surviving, gathering resources, and establishing a village.

As the kingdom grows, we earn money, purchase exclusive public-building blueprints, construct services, develop professions, build machinery, learn magic, explore dangerous worlds, and eventually take to the skies.

By Tier 6, the Royal Aerodrome lets us enter the age of aviation.

At Tier 9, Waystones introduce advanced fast travel.

At Tiers 10–11, the kingdom reaches the space age.

At Tier 12, we finally unlock the End, fight the Ender Dragon, and complete the Royal Legacy Monument.

FTB Quests should make that entire journey understandable, connected, rewarding, and exciting.

The economy should make earning coins useful.

The blueprint marketplace should make building opportunities meaningful.

Professions should give every player a valuable role.

The Royal Gilded UI should make the experience feel professionally developed.

The administrator tools should let me test everything thoroughly without spending weeks progressing through the normal gameplay.

**Do not simplify this into disconnected mods, decorative buildings, and unfinished menus. Build one cohesive, secure, testable multiplayer experience.**

Begin with the repository audit, protect existing functionality, then implement the first meaningful, verifiable milestone.