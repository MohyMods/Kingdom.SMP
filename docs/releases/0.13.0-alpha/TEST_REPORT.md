# Actual Alpha test report

Build: Java 21.0.13 / Minecraft 1.21.1 / NeoForge 21.1.265. Exact release version `0.13.0-alpha`. Implementation `947cb0a` (parent `7239a9d`), branch `rebuild/royal-gilded`; no main merge. Full `build compileGameTestJava`: **754 JUnit tests, zero failures/errors/skips**. See evidence/FULL_BUILD.log and JUNIT_SUMMARY.txt.

## Native dedicated-server acceptance

The final integration batch passed on the exact 29-artifact profile pinned in the dependency manifest. All worlds were stopped private fixtures or session-locked copies. Raw supplies, ordinary vanilla enchanting XP, QA money and controlled terrain were explicit inputs. Profession XP, completed quests, source evidence and Master ranks were never injected. Earlier documented contract-period/growth acceleration is not acceptance of natural community duration.

| Executed path | Actual result |
| --- | --- |
| Fresh kingdom Tier 1–6 | Full free Hut/Aldric, First Hearth donations/winner/Town Hall, road, paid Bank, all 24 early resource families, sequential Tier 4/5/6 transitions; changed inventory/replay/damaged-Bank refusal, bank-pending isolation, native receipts and 2-JVM restoration pass (`master-alpha-tier-six-regression.log`) |
| Farmer unchanged | Fresh real 24-objective loop to 4,800 XP/Master, both earned paid tools and receipt reopen pass (`master-farmer-regression.log`) |
| Blacksmith Extraction Anvil | Genuine saved 24-quest Master pays two compounded purchases, claims physical mail, trades to non-Master; selected enchantment/book/source durability/name/custom owner/other enchantments preserved, actual XP/book/lapis consumed, stale inventory and independent FTB/OPAC refusals, actual Ars `mana_boost` extraction and 2-JVM restoration pass (`master-blacksmith-modded-native.log`) |
| Builder Architect's Wand | Same real paid/traded path; line, 3×3 wall, 2×2 floor and matching replacement consume real materials. Shortage, after-review independent FTB/OPAC revocation and replay refuse without partial blocks/payment. A real prepared world checkpoint deliberately stops before inventory debit; second JVM restores original pending cells/materials and preserves earlier paid geometry/receipts (`master-builder-claims-native.log`) |
| Lumberjack Worldtree Axe | Genuine Master paid/traded path; actual bonemeal-grown oak and Aether Skyroot, exact native growth/trunk positions, native whole-trunk removals and appropriate seed-funded replant; independent FTB/OPAC denial and 2-JVM restoration pass (`master-lumberjack-claims-native.log`) |
| Miner Prospector's Pickaxe | Genuine Master paid/traded path; five actual native diamond ore removals, conserved Fortune VI drops including normal pickup, no durability loss, survey/cooldown replay and independent FTB/OPAC denial; 2-JVM restoration pass (`master-miner-claims-native.log`) |
| Librarian Enchanter's Codex | Genuine Master paid/traded path; Sharpness V + real book V yields VI with actual XP/lapis/book debit, source durability/name/owner/Unbreaking preserved, stale inventory and independent claims deny, actual Ars `mana_boost` book accepted. Native creation plus separately resumed session-locked saved-copy restart pass (`master-librarian-modded-native.log` creation; `master-librarian-modded-restore.log` restoration) |
| Canonical transaction models | New Master-work receipt commit/abort/replay/archive/after-rename uncertainty, malformed snapshots and ineligible purchases; old Farmer/economy/permissions/save tests retained in full 754-test suite |

All five new reward paths check correct local active Master purchasing, exact personal/item repeat pricing, once-only confirmation, private native mail and legitimate native pickup/use by another non-Master. Signature item prices default to 50,000 coins; native second purchases cost 75,000. Native protection tests retain both supported independent claim systems. The operator-inspection/native profession frameworks are unchanged.

## Retained prior accepted evidence

All 144 selected quest adapters already have actual full-quantity native 24-quest/4,800-XP/Master loops recorded in D110 and earlier continuation entries. They were not redesigned or replaced. The unchanged Farmer loop was rerun after the new global native hooks. Prior combined29 earned-Tier6 Aerodrome acceptance passed three JVMs: no-hangar denial, sole paid plan, original Aldric progressive partial/resume/complete, sole Corin, native biplane board/damage denial/exact repair and final identities/purse/receipt restoration (`alpha-required-aerodrome-native.log`). Prior actual FTB 175-quest/144 validated projections/untrained-reader denial/reset/reprojection/2-JVM Root integrity also remains accepted (`alpha-ftb-native-current.log`). Those unchanged aircraft/FTB slices were not falsely reported as newly rerun in this equipment batch.

## Graphical client

Actual Minecraft client launched twice on the exact profile using Xorg dummy + Mesa software rendering. A session-locked private copy of the earned Library world retained its purchased workstation, physical traded gear and canonical records. The first visual pass found a misleading Locked label on the instructions; the final build corrects it to an informational Workstation guide and separates available enchanting services.

The final graphical pass rendered and inspected the real Codex model, guide, exact Mana Boost I→II / 2 XP / 4 lapis / donor-book review, and successful confirmation at 1440×900, GUI scale 3. Resizing the live window to1280×720 correctly switched to the stacked scrolling layout with visible controls; screenshots are in evidence/. Native player-file inspection after the UI confirmation independently verified XP76→74, lapis8→4, donor book1→0, helmet damage23 retained and actual Ars `mana_boost`1→2. Ordinary book input was explicitly supplied through the real `/give` command in that private copy. No profession evidence was injected. The world was saved and the integrated server stopped.

This is a real graphical client/integrated-server check, not a mockup or certification of every screen, full flight or two human networked clients. Existing third-party resource/doc-cache warnings remain in the client log; no Kingdom Master asset/model failure blocked this run. Full277-pack validation remains open.

## Limits of acceptance

The profile contains **29 pinned artifacts**, not the full owner 277-mod pack. Multi-week natural play duration, final multiplayer economy calibration, every modded enchantment/species/ore and aircraft flight under every additional mod remain follow-up play testing. Only oak/Skyroot and registered Ars `mana_boost` integration are newly certified for the corresponding signatures. Native tests execute server gameplay APIs and simulated real server players; they are not a claim of two human networked clients. See KNOWN_LIMITATIONS.md and MANUAL_ACCEPTANCE.md.

Reproduce the full Java build with Java 21 and `bash scripts/gradle-cloud.sh build compileGameTestJava`. `scripts/download-alpha-mods.py` verifies the exact isolated profile. Native progression: `python3 scripts/test-claims.py --namespace kingdom_smp_early_tier_tests --mods work/alpha-required-test-mods`. Private Master fixtures and complete logs remain in the inherited cloud workspace; they are not production saves or installation content. Passed native assertion excerpts are included in evidence/NATIVE_RESULTS.txt.
