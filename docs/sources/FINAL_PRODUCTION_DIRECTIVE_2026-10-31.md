# KINGDOM SMP — FINAL PRODUCTION BUILD DIRECTIVE
## Royal Gilded 1.0.0 — Complete, Integrate, Test, and Release

**Repository:** https://github.com/MohyMods/Kingdom.SMP  
**Development branch:** `rebuild/royal-gilded`  
**Last confirmed delivery commit:** `3d57cc6`  
**Last verified development version:** `0.5.1-dev`  
**Minecraft:** Java Edition 1.21.1  
**Mod loader:** NeoForge 21.1.265  
**Java:** 21  
**Modpack:** Latest supplied 277-entry modpack snapshot, including FTB Quests  
**Players:** Approximately 10 active friends  
**Target release:** Kingdom SMP 1.0.0 — a fully playable, integrated multiplayer experience

---

# 1. YOUR MISSION

You are taking ownership of the final development, integration, testing, packaging, and release preparation of Kingdom SMP.

**I do not want another planning-only iteration, another partially implemented milestone, or another development build presented as a final product.**

I want the entire Kingdom SMP experience completed to the highest practical standard, with the approved features working together in a real Minecraft environment.

Your responsibility is to:

1. Inspect the current repository and identify every unfinished system.
2. Implement all previously approved Kingdom SMP functionality.
3. Integrate the installed modpack with kingdom progression.
4. Complete all structures, NPCs, professions, equipment, quests, economy, and administration.
5. Make every core gameplay system accessible through an actual interface, interaction, or command.
6. Verify gameplay across a complete kingdom progression.
7. Test with real mod dependencies and dedicated server/client environments.
8. Resolve bugs, duplication exploits, persistence issues, and compatibility problems.
9. Package a complete release with installation instructions.
10. Produce the final JAR, quest data, configuration, changelog, compatibility report, and administrator manual.

**Do not declare Kingdom SMP 1.0.0 complete until the release acceptance criteria in this directive have been met.**

If execution limits prevent completion, save verified progress to the development branch and provide an accurate continuation report. Never label an incomplete release as finished.

You have authorization to implement, test, refactor, and commit required changes on the development branch. Do not merge into main without explicit authorization.

---

# 2. START FROM THE ACTUAL CURRENT CODE

Do not begin from scratch.

Inspect the current HEAD of `rebuild/royal-gilded`, not only the historical checkpoint.

The 0.5.1-dev checkpoint reported:

- 142 passing automated tests.
- Working authenticated full-building holograms.
- Remappable rotation and placement controls.
- Five-minute clickable Builder's Hut confirmation.
- Real FTB Chunks and OPAC claim compatibility.
- Persisted construction across actual process restarts.
- A 782/782-block Founders' Lodge construction.
- Exactly one Aldric and one Mira.
- Working Mira dialogue and milestone service.
- Tier 1 contribution and Tier 2 progression foundations.
- Royal Gilded UI foundations.
- Existing-world compatibility requirements.

Preserve and extend this code.

Do not remove proven functionality simply because a new implementation seems easier.

Read the latest:

- Master development directive and decision register.
- `CODEX_CONTINUATION`.
- Construction and blueprint architecture documents.
- Claim compatibility audit.
- Modpack manifest.
- Development roadmap.
- Milestone and dimension-progression documents.
- Previous checkpoint evidence.
- Relevant automated tests.

Determine what is implemented, what is partial, and what is still missing.

Create a complete feature inventory before beginning implementation.

Older documents may describe superseded requirements. The decisions in this directive and the latest explicit user approvals take precedence.

---

# 3. FINISH THE ENTIRE 12-TIER KINGDOM SYSTEM

Build a complete progression from Tier 1 through Tier 12.

The game must be playable from the moment a new world is created through the final End-related kingdom achievement.

### Tier 1

Starting Owner receives the free Builder's Hut Blueprint.

Confirmed required contributions:

- 1,024 logs.
- 2,048 cobblestone.
- 512 wheat.
- 128 iron ingots.

Maintain the existing contribution accounting, resource consumption, winner calculation, leaderboard, and transaction safety.

Aldric Stonehand is the original kingdom Builder NPC.

### Tier 2

The highest Tier 1 contributor receives the exclusive Town Hall Blueprint.

The Town Hall must:

- Measure 19 × 15 blocks.
- Have two furnished floors.
- Be constructed by Aldric.
- Have a persistent named Steward NPC.
- Include kingdom progress and contribution interfaces.
- Include the Royal Architect Blueprint Market.
- Include a limited common-essentials NPC shop.
- Include the player mailbox system.
- Display kingdom records and leaderboards.
- Provide access to later services as their tiers become available.

From Tier 2 onward, public contributions are handled through the Town Hall.

### Tiers 3–12

Implement all required approved milestone contributions, objectives, unlocks, world-border expansions, building prerequisites, and progression rewards.

The system must have a valid and achievable dependency graph.

For previously unspecified numeric requirements, propose balanced versioned defaults, document them, and ensure they do not contradict fixed approvals. Do not silently activate historical draft quantities as though they were approved.

Design for approximately 10 active players and several weeks of progression.

Do not turn higher tiers into repetitive wood-and-cobblestone collection.

Include modded items, infrastructure projects, exploration requirements, technology development, and meaningful community objectives.

All major unlocks must have visible FTB Quests milestones and clear Royal Gilded progress information.

### Mandatory dimension schedule

- Tier 4: Aether.
- Tier 5: Nether.
- Tier 6: Twilight Forest.
- Tier 8: Deeper and Darker / Otherside.
- Tier 9: Waystones.
- Tiers 10–11: Ad Astra progression.
- Tier 12: End.

The End remains inaccessible before Tier 12.

**No prerequisite for reaching Tier 12 may require End-exclusive resources, directly or through crafting, machines, or cross-mod recipes.**

All applicable restrictions must be enforced server-side, not merely hidden from recipe viewers.

---

# 4. COMPLETE THE BLUEPRINT AND BUILDING SYSTEM

The existing authenticated construction foundation must be generalized into a polished system for every approved kingdom building.

## Blueprint ownership

- Builder's Hut: free owner entitlement.
- Initial Town Hall: Tier 1 highest-contributor entitlement.
- All subsequent public and profession buildings: purchased at the Town Hall.
- Only one initial legitimate blueprint entitlement per building per kingdom.
- First successful buyer wins.
- Sold-out buildings cannot be purchased again.
- Purchased buildings are public kingdom buildings after completion.
- The buyer receives an honorary founder title and may name the new associated NPC.
- Permanent identity and entitlement records must survive restart.
- Lost plans must be recoverable without duplicating authority.
- Administrative abandonment/reassignment must be controlled, audited, and safe.

Every blueprint-market purchase must be atomic.

No double charging. No duplicate rights. No copied blueprint exploit.

Blueprint prices must remain inside the approved 500–15,000-coin range unless a later explicit approval changes that rule.

## Construction experience

Finish the actual gameplay experience:

1. Hold a physical blueprint.
2. See a complete translucent 3D building.
3. Move its position by aiming.
4. Rotate and adjust height.
5. See blocked, valid, and pending placement states.
6. Review exact foundation, footprint, and placement requirements.
7. Confirm construction in Royal Gilded UI.
8. Consume the entitlement exactly once.
9. Watch Aldric physically construct the building.
10. Observe persistent progress.
11. Receive a completed furnished building and exactly one designated NPC.

Safe foundation supports may extend up to four blocks beneath the structure.

Never destroy another player's structures, claims, containers, or unauthorized terrain.

Use existing FTB Chunks and OPAC integrations correctly.

**Approved normal-world placement should not require routine administrator approval if server validation confirms the complete site is valid.**

Keep administrator overrides for debugging and recovery.

Current claimed-site construction depends on the owner remaining online in the same dimension. Investigate whether authorized offline construction can be implemented safely. If so, implement and test it. If not, preserve the proven safe behavior and document the limitation rather than bypassing claims.

All constructions must survive shutdown, restart, permission changes, and chunk unloading.

---

# 5. COMPLETE ALL APPROVED PUBLIC BUILDINGS

Every approved building needs:

- A real full-scale build definition.
- A distinct physical blueprint.
- Correct tier prerequisites.
- An actual NPC where specified.
- A real usable interface.
- Working gameplay services.
- Persistent construction/completion records.
- A founder title.
- FTB Quests integration.
- Administration and recovery support.

Priority buildings include:

### Builder's Hut
Starting construction and Aldric's home.

### Royal Town Hall
Tier 2 kingdom headquarters, architect market, progress board, essentials shop, and mailbox access.

### Royal Bank
Tier 3 deposits, withdrawals, physical currency conversion, and safe money transfers.

### Royal Bounty Hall
Tier 3 rotating bounties, hunting contracts, material deliveries, profession-related rewards, and community bounties. Named Bounty Master NPC.

### Fisherman's Wharf
Tier 3 Aquaculture 2 contracts, fish trading, equipment sales, rare-fish collections, and competitions. Named Fishmonger NPC.

### Royal Aerodrome
Tier 6 aircraft services, purchases, ownership registration, repairs, upgrades, scouting, cargo, and flight contracts.

### All 12 Profession Buildings
Physical progression, contracts, shops, and Journeyman/Master equipment purchase locations.

### Royal Legacy Monument
Tier 12 kingdom finale, permanent community records, and Ender Dragon victory recognition.

Honor the existing documented building unlock schedule where approved.

The Royal Storehouse has been explicitly removed from the new-world design. Do not reintroduce it.

Do not automatically introduce unapproved buildings such as a postal service, academy, power plant, shrine, research institute, or industrial foundry merely because they were previously brainstormed.

---

# 6. COMPLETE THE ECONOMY

Implement the full hybrid economy.

### Currency

- Copper coin: 1.
- Silver coin: 10.
- Gold coin: 100.
- Platinum coin: 1,000.
- Virtual bank balances.
- Secure deposit/withdrawal conversion.
- In-person financial services.
- Remote balance inspection.

### Shops and trading

Complete:

- NPC buying and selling.
- Tier-gated inventory.
- Dynamic resource prices.
- NPC buying budgets and resource quotas.
- Player selling allowances and profession bonuses.
- Daily and weekly contracts.
- Shop restocking.
- Real transaction receipts.
- Safe payment integration before the Tier 3 Bank becomes available.

Preserve previously approved economic numbers and rules from the decision register. Do not replace them with arbitrary defaults.

### Auction system

Include:

- Fixed-price listings.
- 24-hour timed auctions.
- Minimum 5% bid increments.
- Two-minute anti-sniping extensions.
- Player buy orders.
- Partial buy-order fills.
- Maximum 10 active buy orders per player under the approved rule.
- Normal sales tax of 5%.
- Master equipment sales tax of 10%, before any approved Merchant-specific modifiers.
- Mailbox deliveries.
- Automatic seller proceeds.
- Transaction escrow and safe cancellation/recovery.

Prevent money duplication, item duplication, bid manipulation, invalid refunds, and mailbox loss.

Verify multi-client race conditions and restart recovery.

---

# 7. COMPLETE THE 12 PROFESSIONS

Maintain exactly these approved professions:

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

Each profession contains:

- Apprentice rank.
- Journeyman rank.
- Master rank.
- Eight Apprentice progression quests.
- Eight Journeyman progression quests.
- Eight Master progression quests.

Total: **288 profession progression quests**.

Target 2–3 weeks of meaningful activity to become a Master in a profession.

Maximum three active professions per player.

Profession changes cost 15,000 coins.

Inactive profession progress must remain saved, but inactive profession perks and purchasing access must be disabled.

All profession buildings need working NPCs, progression interfaces, profession shops, contracts, and exclusive equipment purchases.

## Journeyman and Master equipment

**Journeyman items must be powerful and genuinely useful.**

**Master equipment must be OP.**

Mastering a profession should feel like a major achievement. Master equipment should noticeably transform gameplay, not merely add tiny percentage bonuses.

Examples of intended power levels:

- Miner: massive vein mining, high Fortune, powerful ore detection.
- Farmer: extraordinary crop harvesting and replanting.
- Builder: rapid large-scale construction using real supplied materials.
- Lumberjack: exceptional full-tree harvesting and replanting.
- Mage: formidable magic and powerful area effects.
- Alchemist: extremely strong exclusive potions and brewing.
- Engineer: major Create machinery advantages.
- Merchant: extraordinary trading and contract privileges.
- Adventurer: powerful exploration and expedition abilities.
- Librarian: exceptional enchantment management and creation services.

**Important distinction:** The user reviewed several alternative OP reward packages but did not finalize replacements. Preserve previously approved equipment and abilities unless the latest decision register explicitly supersedes them.

Existing signature rewards include:

- Farmer: Fortune X Hoe.
- Miner: Prospector's Pickaxe.
- Blacksmith: Extraction Anvil.
- Builder: Architect's Wand.
- Lumberjack: Worldtree Axe.
- Merchant: Royal Trade Ledger.
- Engineer: Royal Machinery Core.
- Mage: Archmage's Staff.
- Adventurer: Relic Hunter's Compass.
- Alchemist: Philosopher's Brewing Stand.
- Mechanic: Mekanism services and later-tier machinery progression.
- Librarian: complete reward design remains subject to the latest decision register.

Complete the Journeyman item tier and previously approved Master implementations.

If a reward remains undefined, create a clear proposed implementation for review rather than falsely attributing new abilities to an approved design.

### Master purchase rules

Mastery unlocks the **right to buy**, not a free item.

Master equipment is tradeable and usable by other players where previously approved.

Each additional same-item purchase by the same player costs:

`basePrice × 1.5 ^ previousSuccessfulPurchases`

Equipment must be genuinely strong without causing item duplication, claim bypasses, unauthorized teleportation, invalid dimension access, or unchecked server lag.

Do not weaken every powerful item with arbitrary cooldowns.

Balance primarily through mastery effort, purchase prices, crafting/operation costs when relevant, kingdom gates, and exploit protections.

Preserve the special Fortune X Potion rule: three-minute duration, rare Arcane Fortune Catalyst, and no additive Fortune stacking.

Preserve the Tier 12 Master Mechanic Creative Energy Cube restriction and verify its intended behavior before activation.

---

# 8. FULL FTB QUESTS INTEGRATION

FTB Quests version `2101.1.37` is present in the updated user-supplied mod list.

Verify that this exact version and its dependencies load correctly on the actual target client and server.

**Create a complete, coherent FTB Quests campaign spanning the entire relevant gameplay modpack.**

Quest chapters must cover:

- Starting a Kingdom.
- Builder's Hut and Aldric.
- Tier 1 contributions.
- The Town Hall.
- All 12 kingdom tiers.
- The blueprint marketplace.
- Public building construction.
- All 12 profession questlines.
- Banking and NPC commerce.
- Auctions and contracts.
- Royal Bounty Hall.
- Fishing and Aquaculture 2.
- Farmer's Delight and cooking.
- Productive Bees.
- Mystical Agriculture.
- Create and installed add-ons.
- Mekanism and Generators.
- Ars Nouveau and Ars Creo.
- Iron's Spells.
- Occultism.
- Immersive Aircraft.
- Aether.
- Twilight Forest.
- Deeper and Darker.
- Waystones.
- Ad Astra.
- Cataclysm, Mowzie's Mobs, and meaningful dungeon content.
- Major multiplayer achievements.
- Tier 12 End progression.
- Royal Legacy Monument.

Review the full 277-entry snapshot and cover each **meaningful gameplay mod** with appropriate quests. Libraries, client-only UI enhancements, and performance mods do not require artificial gameplay quest chapters.

### Quest standards

Every quest must have:

- A useful title.
- Clear description.
- Actual objective.
- Valid prerequisites.
- Correct individual/team/kingdom attribution.
- An appropriate reward or progression purpose.
- A tested completion condition.
- Valid compatibility with the installed mods.
- No bypass of Kingdom SMP restrictions.

Use meaningful tutorial quests, progression quests, challenges, and optional achievements.

Avoid hundreds of repetitive filler quests.

### Synchronization

FTB Quests must not become a separate authority that contradicts Kingdom SMP.

Kingdom SMP controls:

- Tiers.
- Building entitlements.
- Construction completion.
- Profession ranks.
- Currency.
- Equipment restrictions.
- Dimension unlocks.
- Aircraft permissions.
- Waystone permissions.

FTB Quests reflects these systems and awards only authorized rewards.

Test late joiners, shared quests, personal quests, reward claiming, resets, duplicate completion events, and restart synchronization.

An FTB Quest must never give a player an item or unlock that bypasses a future kingdom tier.

---

# 9. COMPLETE MODPACK RESTRICTIONS

Make the entire modpack feel cohesive.

Review all relevant installed mods and map their systems to appropriate kingdom tiers, buildings, professions, and quests.

Pay special attention to:

- Create machinery.
- Mekanism technology.
- Mystical Agriculture resource farming.
- Productive Bees resource production.
- Ars Nouveau and Iron's Spells.
- Occultism.
- Immersive Aircraft.
- Waystones.
- Dimension portals and modded teleportation.
- Mob Grinding Utils.
- Loot generation and dungeon rewards.
- MineColonies interactions.
- Sophisticated Storage.
- Third-party automation and item movement.

Do not enforce progression through recipe visibility alone.

Audit alternative crafting recipes, machine processing, loot sources, NPC trades, quest rewards, equipment use, placement, and automation pathways.

Respect existing mods' protections and gameplay APIs.

### Aircraft

All aircraft require Tier 6 **and** completed Royal Aerodrome.

Verify individual aircraft availability and tier schedules against Immersive Aircraft 1.7.0.

Test vehicle crafting, placement, spawning, interaction, and use.

### Waystones

All ordinary Waystone access remains locked until Tier 9.

Test alternate sources and methods of teleportation where they would bypass the intended progression.

### Dimensions

Aether Tier 4.  
Nether Tier 5.  
Twilight Tier 6.  
Otherside Tier 8.  
End Tier 12.

Review relevant cross-mod dimension travel.

### Performance

The full modpack is substantial. Prevent unbounded scans, uncontrolled chunk loading, excessive network packets, and runaway automation.

Do not disable installed mods or delete content simply to make the pack pass without documenting and obtaining approval for a change.

---

# 10. ROLES, TITLES, AND SPECIAL PRIVILEGES

Complete the role system.

### Building founders

Blueprint buyers receive permanent honorary titles linked to their completed kingdom achievements.

Examples:

- Royal Financier.
- Bounty Commissioner.
- Harbormaster.
- Royal Aviator.

Any building-management privileges must be explicitly bounded, permission checked, and configurable.

Founder titles do not grant operator access or authority over private property.

### Profession titles

Support Apprentice, Journeyman, and Master titles.

Retain earned ranks for inactive professions while disabling their inactive benefits.

### Achievement titles

Recognize major kingdom contributions, successful expeditions, first flights, important boss kills, and major construction achievements.

Players must be able to select their preferred earned display title.

Investigate FTB Ranks integration if it helps, but verify the API and keep cosmetic identity separate from actual permissions.

---

# 11. COMPLETE ROYAL GILDED USER INTERFACES

All major systems must be accessible in-game.

Required functional interfaces include:

- Kingdom Overview.
- Tier Progression.
- Contribution Board.
- Blueprint Market.
- Hologram Placement.
- Construction Progress.
- Profession Selection.
- Profession Quests.
- Journeyman/Master Shops.
- Building NPC Services.
- Bank.
- Auctions.
- Mailboxes.
- Bounty Hall.
- Fisherman's Wharf.
- Aerodrome.
- Title Selection.
- Administrator Controls.
- FTB Quests entry or integration guidance.

Use the established Royal Gilded aesthetic.

No dead buttons.

No decorative fake services.

No unimplemented interfaces presented as functional.

Every failed interaction needs an understandable explanation.

Test actual GUI navigation, scaling, controls, packet handling, stale screens, and multiplayer synchronization.

---

# 12. FULL ADMINISTRATION AND TESTING

This is mandatory.

Create a complete operator-only command and testing suite under a discoverable namespace such as:

`/kingdom admin`

It must provide actual control over:

### Progression
- Kingdom tier.
- Milestone completion.
- Resource contributions.
- World-border progression.
- Dimension gates.

### Economy
- Player balances.
- Coins and denominations.
- NPC shop stocks.
- Contract pools.
- Auction state.
- Mailboxes.
- Transactions.

### Buildings
- Blueprint entitlements.
- Blueprint purchases.
- Blueprint recovery.
- Placement validation.
- Construction progress.
- Building registration.
- NPC restoration.
- Founder titles.

### Professions
- Profession activation.
- XP.
- Rank.
- Quest progress.
- Equipment access.
- Master purchase counts.

### FTB Quests
- Quest progress inspection.
- Team/individual synchronization testing.
- Reward validation.
- Safe test completion/reset.

### Mod restrictions
- Aircraft.
- Waystones.
- Dimensions.
- Technology.
- Magic.
- Recipe gates.

### Diagnostic tools
- Persistence inspection.
- Validation failure reasons.
- Missing dependencies.
- Integration status.
- Server compatibility.
- Protection checks.

Implement an administrator sandbox/test mode or equivalent isolated test fixtures so I can quickly verify all tiers and equipment without investing weeks of normal gameplay.

Protect production data:

- Permission checks.
- Confirmation for destructive actions.
- Audit logs.
- Dry-run capability where practical.
- Clearly marked test overrides.
- No unintentional irreversible resets.

**Every implemented feature must have an administrator testing path.**

Include a complete command manual with real examples.

---

# 13. REAL MULTIPLAYER VALIDATION

The target is not singleplayer alone.

The target is a dedicated NeoForge server with approximately 10 friends.

Test:

1. A clean new-world installation.
2. A copied legacy world.
3. Builder's Hut bootstrap.
4. Tier 1 donations.
5. Awarding the Town Hall.
6. Blueprint purchases by multiple players.
7. Simultaneous purchase races.
8. Construction and permission checks.
9. NPC persistence.
10. Tier advancement.
11. Profession activation and switching.
12. Journeyman and Master purchases.
13. FTB Quest synchronization.
14. Auction and bank concurrency.
15. Claim protections.
16. Player disconnects.
17. Server restarts.
18. Chunk unloading.
19. Aircraft restrictions.
20. Waystone restrictions.
21. Dimension restrictions.
22. Late joining players.
23. Tier 12 End unlock and finale.

Use multiple genuine client sessions for tests that require real clients. Where a true 10-client test is impractical, use representative real-client tests plus measured load testing, and explicitly disclose the remaining gap.

Never claim full-pack compatibility based solely on a reduced-mod test environment.

---

# 14. COMPLETE 277-MOD COMPATIBILITY TEST

Use the actual complete modpack snapshot.

Before releasing:

- Verify every mod and dependency is present at the intended version.
- Check for duplicate Kingdom SMP JARs.
- Test clean client launch.
- Test dedicated-server launch.
- Test actual client/server connection.
- Test world creation.
- Test save/load.
- Test kingdom UI.
- Test FTB Quests.
- Test major content integrations.
- Investigate crashes, missing registry entries, packet mismatches, mixin conflicts, and loader problems.
- Test prolonged gameplay and restarts.
- Document any incompatible or unverified components.

The prior crash showed a 4 GB Java heap running out of memory. Test appropriate realistic heap allocations and document recommendations; do not assume every observed memory issue comes from Kingdom SMP.

Avoid silently changing the modpack.

Provide a compatibility matrix documenting each important installed gameplay mod and the verified scope of its integration.

---

# 15. AUTOMATED QUALITY AND EXPLOIT TESTS

Create and execute tests covering:

- Contribution arithmetic and caps.
- Winner determination.
- Currency conservation.
- Blueprint purchase uniqueness.
- Item entitlement validation.
- Network packet authorization.
- Duplicate clicks and retry requests.
- Simultaneous transactions.
- Claim permission changes.
- Safe foundation generation.
- NPC duplicate prevention.
- Interrupted construction.
- Data migration.
- Profession limits and switching.
- Master price escalation.
- FTB Quest reward synchronization.
- Dimension bypass attempts.
- Aircraft bypass attempts.
- Waystone bypass attempts.
- Cross-mod recipe dependencies.
- Item duplication.
- Test-mode isolation.
- Restart recovery.

Use NeoForge GameTests, unit tests, integration tests, and real Minecraft runs where appropriate.

A passing Gradle build does not prove multiplayer correctness.

An external mod integration does not count as verified without evidence from the real dependency or a documented equivalent test demonstrating the actual behavior.

---

# 16. FINAL RELEASE REQUIREMENTS

Prepare a complete **Kingdom SMP 1.0.0** release candidate.

The release deliverables must include:

### A. Final compiled JAR

An actual installable NeoForge mod JAR.

Ensure client/server compatibility and correct metadata.

### B. FTB Quests content

Working quests, chapter structures, icons, dependencies, rewards, and installation instructions.

If quest data belongs in world/server configuration or a separate distribution, package it appropriately.

### C. Installation package

Provide an easy-to-follow guide covering:

- Required Minecraft version.
- Required NeoForge version.
- Java version.
- Server installation.
- Client installation.
- Dependencies and modpack compatibility.
- Removal of old Kingdom SMP JARs.
- World backups.
- Fresh-world setup.
- Owner initialization.
- FTB Quests setup.
- Configuration.
- Common errors.
- Recovery steps.

### D. Administrator guide

Document all commands, permission levels, sandbox tools, restoration procedures, and troubleshooting methods.

### E. Player guide

Explain:

- How the kingdom begins.
- How tiers advance.
- How to contribute resources.
- How to earn coins.
- How to buy blueprints.
- How construction works.
- How professions work.
- How Master equipment works.
- How FTB Quests guides progression.
- How to access special dimensions and aircraft.
- How to complete the Tier 12 finale.

### F. Gameplay verification evidence

Include actual screenshots or logs of representative completed functionality:

- Town Hall.
- Builder's Hut.
- Blueprint Market.
- Construction.
- Profession menus.
- Master equipment.
- FTB Quests.
- Banking.
- Auctions.
- Aerodrome.
- Tier progression.
- Admin controls.

Do not use mockups as proof of working gameplay.

### G. Release integrity

Include:

- Commit SHA.
- Exact release tag.
- Final JAR filename.
- SHA-256 checksum.
- Automated test results.
- Dedicated-server test results.
- Actual client test results.
- Modpack compatibility results.
- Known limitations.
- Migration guidance.
- Changelog.

No fabricated screenshots, tests, hashes, or compatibility claims.

---

# 17. DEFINITION OF "READY"

Do not describe the build as complete unless all the following conditions are satisfied:

- [ ] Fresh-world Tier 1 to Tier 12 progression works.
- [ ] Legacy-world data remains safe.
- [ ] Builder's Hut and Town Hall work.
- [ ] All approved public buildings are functional.
- [ ] Blueprint Market works without purchase duplication.
- [ ] Construction works with actual protection mods.
- [ ] All 12 professions work.
- [ ] All 288 profession progression quests are functional.
- [ ] All approved Journeyman and Master rewards work.
- [ ] Master equipment is meaningfully powerful.
- [ ] Banking and auction services work.
- [ ] Bounty Hall and Fisherman's Wharf work.
- [ ] FTB Quests loads and synchronizes correctly.
- [ ] Important gameplay mods have meaningful quest integration.
- [ ] Aerodrome and aircraft restrictions work.
- [ ] Waystones remain locked until Tier 9.
- [ ] Dimensions unlock at the correct tiers.
- [ ] Tier 12 End progression works.
- [ ] NPCs survive restarts without duplication.
- [ ] Admin tools can test every major mechanic.
- [ ] No critical duplication exploits are known.
- [ ] Dedicated-server and client tests pass.
- [ ] Full-modpack integration is tested or remaining gaps are accurately disclosed.
- [ ] Final JAR and required companion data are packaged.
- [ ] Installation and administration documents are complete.

Do not mark an item complete because it has a design document or partial implementation.

If an approved mechanic remains blocked by an unresolved specification, identify the exact blocker, offer a concrete balanced solution, and continue independent work.

If a release-critical item cannot be completed or verified, do not falsely declare 1.0.0 ready. Deliver the latest verified release candidate, clearly identify the blocker, and preserve the next continuation step.

---

# 18. EXECUTION INSTRUCTIONS

**Begin immediately.**

Do not respond with only a design document or a massive list of planned tasks.

First:

1. Fetch the latest branch state.
2. Audit current code and recent commits.
3. Generate the completion matrix.
4. Identify the critical path to a complete release.
5. Begin implementing the next unfinished feature.

Then continue in dependency order.

Build and test after meaningful changes.

Commit working checkpoints.

Keep continuation documentation current.

When a milestone is complete, move to the next dependent milestone rather than ending with a list of suggestions.

Do not replace tests with claims that the code "should work."

Do not repeatedly ask for approval on settled decisions.

Use engineering judgment for ordinary implementation details.

Preserve explicit user-controlled gameplay decisions.

Focus on real finished functionality.

At the end of each execution session, provide:

- Current completion percentage by subsystem, based on verified requirements.
- Actual features completed.
- What was tested.
- What failed.
- What remains.
- Latest commit.
- Available installable artifact.
- Exact recommended continuation action.

Do not invent a date or promise that background work will continue after the session ends.

---

# 19. THE EXPERIENCE I WANT

Kingdom SMP should feel like a complete multiplayer RPG civilization built inside Minecraft.

When my friends join, we begin as a small settlement.

We build the Builder's Hut, meet Aldric, gather resources, and work toward the Town Hall.

We compete and cooperate to earn coins and purchase the first available blueprints.

Our town grows with profession buildings, commerce, quests, NPCs, and real infrastructure.

Each person develops a profession and eventually earns extraordinary Master equipment.

FTB Quests guides us through Create, Mekanism, magic, exploration, aviation, farming, and the rest of the modpack.

Our kingdom evolves from medieval survival into magic, industry, aircraft, and space exploration.

We unlock Waystones at Tier 9.

We finally unlock the End at Tier 12, defeat the Dragon, and celebrate our civilization through the Royal Legacy Monument.

I want players to have reasons to trade, specialize, explore, build, compete, and cooperate.

I want every building to have a purpose.

I want Master rewards to be genuinely OP and worth pursuing.

I want the server owner to have complete control over testing and administration.

I want the entire experience to look cohesive and professional.

**The objective is no longer to demonstrate individual systems. The objective is to deliver the complete playable Kingdom SMP experience.**

Make it real, make it stable, make it fun, and prove that it works.

**START THE FINAL IMPLEMENTATION NOW.**