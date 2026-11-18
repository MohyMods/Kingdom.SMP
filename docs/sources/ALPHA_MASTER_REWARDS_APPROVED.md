# KINGDOM SMP 0.13.0-ALPHA — FINAL COMPLETION DIRECTIVE
## APPROVED MASTER REWARDS · BUDGET MODE · SHIP THE ALPHA

Continue from commit `7239a9d` on `rebuild/royal-gilded`, or the latest verified descendant if additional work has been completed.

Read `docs/CODEX_CONTINUATION.md` D110 and the current Alpha acceptance matrix. Preserve all completed work.

**The six selected professions now have 144/144 native quest completions. Do not reimplement or redesign them.**

Your remaining priorities are:

1. Implement the five Master rewards specified below.
2. Validate their gameplay and purchase restrictions.
3. Resolve any critical Tier 1–6 bugs.
4. Build and package Kingdom SMP 0.13.0-alpha.
5. Publish a downloadable GitHub pre-release if the required release gates pass.

Do not resume development of the full 1.0 roadmap.

# 1. APPROVED MASTER REWARD SPECIFICATIONS

All Master equipment must be exceptionally powerful, unique, and worth the effort of completing a profession.

Only players with the corresponding **active Master profession** may purchase its reward from that profession's dedicated building.

Purchased equipment may be traded and used by other players.

Preserve the approved compounded repeat-purchase price formula:

`price = base_price × 1.5^(number_of_previous_purchases)`

Use integer coin pricing with consistent rounding, authoritative transaction records, and protection against duplicate purchases.

Prices must be displayed before purchase and verified again on confirmation.

### BLACKSMITH — EXTRACTION ANVIL

Implement a unique purchasable workstation.

Abilities:
- Extract one selected enchantment from an enchanted item.
- Transfer the selected enchantment to an enchanted book.
- Preserve the original equipment and all remaining enchantments.
- Remove only the chosen enchantment from the original item.
- Preserve relevant item components, ownership attributes, and durability.
- Require XP and configurable material costs.
- Support eligible vanilla and modded enchantments through the official enchantment APIs.
- Prevent enchantment duplication, invalid extraction, curses being bypassed without an approved rule, and unsafe component modification.

The item and extracted book must be committed atomically.

Make this substantially more useful than a vanilla grindstone.

### BUILDER — ARCHITECT'S WAND

Implement a powerful construction tool.

Abilities:
- Rapidly place walls, floors, and straight lines of blocks.
- Replace matching blocks with selected materials.
- Show placement previews before committing.
- Consume real materials from inventory.
- Support rotation and reasonable area selection.
- Have configurable maximum operation sizes.

Do NOT implement unrestricted copy/paste or creative-mode spawning of materials.

Respect FTB Chunks, OPAC, other supported protection APIs, world borders, and existing Kingdom building reservations.

A failed validation must not partially consume items or corrupt the build.

### LUMBERJACK — WORLDTREE AXE

Implement a unique, unbreakable Master axe.

Abilities:
- Efficiency VI.
- Chop an entire eligible tree in a single operation.
- Collect the drops properly.
- Automatically replant an appropriate sapling when conditions permit.
- Support compatible vanilla and approved modded tree types.
- Avoid cutting adjacent player-built wooden structures.
- Respect claims and protection rules.
- Use bounded tree detection and configurable harvesting limits.

It must not duplicate logs, leaves, saplings, or other drops.

### MINER — PROSPECTOR'S PICKAXE

Implement a unique, unbreakable Master pickaxe.

Abilities:
- Fortune VI.
- Efficiency VI.
- Bounded ore vein mining.
- Nearby ore detection with a configurable cooldown.
- Clear player feedback for detected ore types or direction.
- Compatible ore detection for approved modded ores.
- Respect normal mining eligibility, block hardness, harvest requirements, and player permissions.

Vein mining must not bypass claims, protected kingdom structures, tier restrictions, or dimension progression.

Ore detection must not reveal inaccessible dimensions or unloaded chunks.

Prevent duplicate drops and excessive server workload.

### LIBRARIAN — ENCHANTER'S CODEX

The owner now approves the following new signature Master reward.

Implement a unique powerful enchanting workstation called the **Enchanter's Codex**.

Abilities:
- Combine compatible enchantments more efficiently than a standard anvil.
- Reduce XP costs for legitimate enchanting operations.
- Remove or relax standard anvil level-cost restrictions within explicitly configured limits.
- Unlock an exclusive advanced enchantment service for compatible equipment.
- Preserve enchantment compatibility rules unless an explicit server configuration allows an exception.
- Support eligible vanilla and compatible modded enchantments using proper APIs.
- Provide clear cost previews and confirmation.
- Consume books, required materials, and XP legitimately.
- Never create enchantments from nothing.

Make the reward powerful without allowing unrestricted incompatible combinations, enchantment duplication, infinite XP exploits, or item cloning.

The Codex must preserve custom item data and must not silently strip modded components.

**These five specifications are approved for this Alpha. Do not request another design questionnaire.**

Where exact numeric costs, cooldowns, operation limits, or base prices are undefined, choose reasonable configurable defaults, document them briefly, and continue.

# 2. INTEGRATE THE REWARDS INTO REAL GAMEPLAY

For all five rewards:

- Add actual registered items or workstation blocks.
- Add appropriate recipes or purchase delivery systems.
- Integrate purchases with the existing profession buildings.
- Require active Master status at purchase time.
- Allow legitimate trading and use after purchase.
- Preserve repeat-purchase price scaling across restarts.
- Use existing canonical wallet and inventory transaction systems.
- Add useful tooltips and clear in-game descriptions.
- Ensure appropriate client rendering and server-authoritative behavior.
- Test normal operation, claim denial, inventory shortages, repeated purchases, and save/restart recovery.

Do not count a registered item or menu entry as a completed Master reward unless its signature ability actually works.

Farmer's existing Fortune X Hoe is already implemented. Preserve and regression-test it; do not redesign it.

# 3. FINALIZE THE PLAYABLE TIER 1–6 ALPHA

Verify the complete progression:

Builder's Hut → Aldric → Tier 2 Town Hall → Tier 3 Bank → Tier 4 → Tier 5 → Tier 6 Aerodrome.

The earlier Aldric workstation/Hut-integrity regression must remain fixed.

All six selected professions must be able to complete their 24 quests and reach Master through legitimate gameplay.

The remaining six professions and Tier 7–12 must remain visibly marked **COMING SOON**, with unavailable actions disabled.

Keep working completed lower-rank content intact and do not destroy player progress.

Do not expand the release scope.

# 4. FINAL TESTING — NO FALSE CLAIMS

Run:

- Full Java 21 / NeoForge 21.1.265 build.
- All JUnit tests.
- Dedicated-server progression tests.
- Six Master reward purchase-and-use tests.
- Claim protection and anti-duplication regressions.
- Persistence and restart tests.
- Economy and construction regressions.
- FTB Quest loading and integration checks.
- Available required-mod compatibility tests.

The previous baseline was 747 passing tests, 144 completed selected profession quests, a 29-mod dedicated-server profile, and 175 loading FTB quests.

Ensure all of those continue passing.

Graphical testing was previously blocked by the environment having no display. Check whether a usable headless display, virtual display, or other legitimate graphical-test setup is available. If it isn't, do not invent graphical test results or block every other task indefinitely. Document the unverified graphical-client functionality and provide a concise manual acceptance checklist.

Do not describe the 29-mod profile as validation of the entire 277-mod pack.

# 5. PACKAGE AND DELIVER THE ALPHA

Set the release version to:

`0.13.0-alpha`

Produce:
- Compiled NeoForge JAR.
- Installation ZIP.
- Required dependency/version list.
- Installation instructions.
- Known limitations.
- Actual test report.
- SHA-256 checksums.
- Short changelog.
- Manual in-game acceptance checklist.

Create a GitHub pre-release and attach the downloadable artifacts if publication permissions are available and acceptance gates are satisfied.

Do not merge into `main`.

If a critical blocker prevents an acceptable release, produce an accurately labeled test build rather than falsely declaring completion.

# 6. BUDGET MODE — FINISH IN ONE CONTINUOUS RUN

Do not spend credits on another large planning document.

Avoid repeated full builds after every minor edit.

Reuse working systems and implement related Master equipment functionality in logical batches.

Use focused tests during development, then final integration tests.

Do not stop after implementing one Master reward.

Do not ask for approval over these five finalized specifications.

Do not expand the scope to remaining professions, Tier 7–12, the Royal Legacy finale, or unrelated enhancements.

Preserve completed work and keep commits reviewable.

If execution limits interrupt you, commit the current state and update `docs/CODEX_CONTINUATION.md` with exact remaining work, test evidence, and the next executable step.

**FINAL OBJECTIVE: Deliver a downloadable, functional Kingdom SMP 0.13.0-alpha supporting Tier 1–6, all 144 selected profession quests, all six usable Master rewards, and the Aerodrome.**

Prioritize a playable release over further feature expansion.

**START IMPLEMENTING IMMEDIATELY AND CONTINUE THROUGH BUILD, VALIDATION, AND PACKAGING.**