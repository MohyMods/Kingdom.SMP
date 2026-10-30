# Confirmed requirements

Source: S1 in [overview](PROJECT_OVERVIEW.md). IDs are stable references, not implementation-completion labels. All items below are confirmed unless explicitly labeled otherwise. None is implemented.

## Product and platform

| ID | Requirement |
| --- | --- |
| BASE-01 | Kingdom SMP Core targets Minecraft Java Edition 1.21.1, NeoForge, Java 21, Gradle, and Git. |
| BASE-02 | Support approximately ten friends, with room for expansion, using maintainable modular server-authoritative architecture. |
| BASE-03 | Deliver a long-term medieval-fantasy shared kingdom progressing toward technological and magical civilization. |
| BASE-04 | Preserve meaningful exploration, cooperation, specialization, trading, and shared town infrastructure throughout a season. Powerful automation and equipment must feel earned. |
| BASE-05 | Use real verified NeoForge 1.21.1 APIs and dependencies; never fabricate APIs or compatibility. Prefer established mod APIs and maintainable solutions; justify invasive patches. |

## Gameplay

| ID | Requirement |
| --- | --- |
| KING-01 | Twelve kingdom tiers with increasingly difficult shared milestones. |
| KING-02 | Royal Guild Charter becomes available after Tier 2. Exact boundary semantics remain open. |
| KING-03 | Nether unlocks at Tier 5; End unlocks at Tier 9. |
| KING-04 | Creative Energy Cube requires Tier 12 and active Master Mechanic status. Exact registered item and permitted actions remain open. |
| PROF-01 | Twelve professions: Farmer, Blacksmith, Miner, Builder, Lumberjack, Merchant, Engineer, Mechanic, Mage, Adventurer, Alchemist, Librarian. |
| PROF-02 | Each profession has Apprentice, Journeyman, and Master ranks. |
| PROF-03 | Each profession has 24 planned progression quests, eight per rank: 288 quests total. Objectives/rewards are unspecified. |
| PROF-04 | At most three professions active simultaneously. Switching costs 15,000 kingdom coins and preserves inactive profession progress. Exact charged action is open. |
| PROF-05 | Master-exclusive equipment is purchased, not automatically awarded. Buyer must have the corresponding Master profession active. |
| PROF-06 | Repeated purchases of the same Master item increase price by 50% compounded, tracked per player and item. Rounding, starting prices, and reset rules remain open. |
| ECO-01 | Hybrid physical coins and virtual bank balances. Copper=1, Silver=10, Gold=100, Platinum=1,000 kingdom coins. |
| ECO-02 | Bank deposits and withdrawals require visiting the Town Hall Bank Counter. |
| ECO-03 | Auction house supports fixed-price listings, timed auctions, and buy orders. |
| ECO-04 | Normal auction sales tax is 5%; Master equipment tax is 10%. Charging and rounding rules remain open. |
| ECO-05 | NPC resource purchases use shared budgets, resource quotas, and per-player daily limits to control inflation. |
| TECH-01 | Technology progression depends on kingdom tiers and appropriate professions. |
| TECH-02 | Design includes Create, Mekanism, Occultism, Mystical Agriculture, Productive Bees, and selected magic systems. Exact compatible versions and remaining systems are unverified. |
| TECH-03 | Restrictions cover alternative acquisition paths, not only crafting. |
| TECH-04 | Offline automation is disabled in the current balance proposal. This is a proposal baseline, not a defined enforcement policy. |
| SCOPE-01 | Weekly Events, Pet Shop, Player Trading, and Economy Ledger are excluded from custom scope; no restoration without approval. Player-driven markets and NPC commerce remain required. |

## User experience

| ID | Requirement |
| --- | --- |
| UX-01 | Polished medieval-fantasy MMORPG identity; avoid generic cluttered inventory menus. |
| UX-02 | Consistent layouts, visual hierarchy, colors, symbols, navigation, and contextual help across professions, kingdom, quests, bank, markets, Master Shops, and admin panels. |
| UX-03 | Distinct locked, unlocked, completed, selected, unavailable states; useful hovers and detailed item descriptions; clear progression indicators. |
| UX-04 | Confirm important actions, protect against accidental purchases, provide success/failure feedback, and support proper back navigation. |
| UX-05 | Symmetrical profession layouts where practical; good performance without visual clutter. |
| UX-06 | Produce visual mockups or structured slot layouts for every major screen before implementing it; layout and interactions must be reviewed first. |
| UX-07 | Explain inventory menus versus custom client screens, including distribution/compatibility. Design within actual Minecraft 1.21.1 capabilities. |

## Reliability and security

| ID | Requirement |
| --- | --- |
| SEC-01 | Server-authoritative gameplay and transactions; atomic economy operations and duplication prevention. |
| SEC-02 | Persist player and kingdom data; separate gameplay, storage, networking, and presentation. |
| SEC-03 | Balance values configurable. |
| SEC-04 | Permission-check admin commands; confirmation and audit logs for sensitive admin operations. |
| SEC-05 | Safely handle disconnects, crashes, and interrupted transactions. |
| SEC-06 | Explicit integration boundaries; avoid fragile assumptions and unnecessary invasive patches. |
| SEC-07 | Automated tests for critical calculations and state transitions; report real outcomes without fabrication. |

## Delivery process

| ID | Requirement |
| --- | --- |
| PROC-01 | Audit files and supplied designs before code; preserve confirmed requirements, proposals, unresolved questions, obsolete designs, contradictions, and decisions in version-controlled Markdown. |
| PROC-02 | Dependency-aware incremental milestones; no attempt to generate the entire mod at once. |
| PROC-03 | Each phase reviews approved specs, explains architecture/dependencies, designs applicable UI, assesses exploits/edge cases, implements one manageable module, compiles/tests, reviews security/correctness, documents results, and commits or prepares a clear Git checkpoint. |
| PROC-04 | Do not equate compilation with feature completion; do not rewrite unrelated working systems without justification. |
| PROC-05 | Maintain a development log and update documents after approved decisions change. Distinguish completed work, blockers, remaining tasks, proposals, and verified versus unverified behavior. |
| PROC-06 | Recommend and challenge designs honestly; ask only material decisions; never silently alter approved gameplay. Actual source documents and subsequent approved decisions take precedence. |
| PROC-07 | Current permission covers planning/docs/design only. No gameplay implementation or beginning the mod build until explicit authorization. |

## Evidence expected later

Feature acceptance must include relevant unit/state-machine tests, in-game functional checks, two-client race/disconnect tests, and recovery tests where persistent mutations occur. Exact test plans belong to each reviewed phase; proposed examples appear in the subsystem specifications. These are future checks, not test results.
