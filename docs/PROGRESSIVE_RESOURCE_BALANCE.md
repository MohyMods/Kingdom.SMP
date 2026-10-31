# Progressive resource balance — proposal for review

> **D-023 — current implementation authority:** The [master directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md) authorizes incremental coding and supersedes the D-021/D-022 review-only hold. New-world Town Hall / existing-world Lodge profiles, owner Hut bootstrap, blueprint-market acquisition, automatic server-valid placement, supports up to four blocks, Tier 3 Bank (no new-world Storehouse), updated profession/economy/quest/admin scope apply. End remains Tier 12. The [full replacement mod snapshot](sources/MOD_SNAPSHOT_2026-10-31.txt) now reports FTB Quests installed. See [supersession and actual status](MASTER_SUPERSESSION_MATRIX.md); old descriptions below are historical where they conflict.

> **D-022, planning only:** End is locked until Tier 12. All mandatory paths to reach 12 must be End-independent, including indirect recipes. Royal Legacy Monument is the proposed Tier 12 final construction reward. See [dimension schedule and dependency audit](DIMENSION_PROGRESSION.md). No executable milestone data or new costs are activated.

> D-021 changes future building reward timing: Town Hall at Tier 2, subject to separately approved save migration. Lodge/road/final-Hall references below remain existing runtime or older proposals and need review for the new profile. No resource quantities are newly approved and no executable milestone data changes in this documentation task. See CONSTRUCTION.md and BLUEPRINT_IMPLEMENTATION_PLAN.md.

Authority: the user's 31 October 2026 (Asia/Riyadh) progressive resource directive. The **philosophy, sequential dependency safety, configurable definitions and Royal Storehouse reward scope are approved**. **All new numerical targets, later rewards, technology/profession availability, objective evidence and future scoring remain proposals.** Only First Hearth costs are active. Nothing in this document authorizes new resource consumption or grants a service.

## Reading the stages correctly

A row identifies the tier **while gathering**, followed by its destination. Tier 4 → 5 is gathered at Tier 4 and cannot require Nether access. Tier 5 → 6 may use Nether materials because entry is already available at Tier 5. Every source-tier milestone from Tier 1 through **Tier 11 → 12** must avoid End-exclusive resources and indirect End dependencies. Only the already-reached Tier 12 finale may require End activity/materials. Tier 12's proposed finale stays at Tier 12; it creates no Tier 13 and does not settle the final Royal Legacy Monument award policy.

A material being introduced as a contribution does not lock its normal recipe until that tier. Copper/glass/bricks/leather already have Overworld acquisition paths; their new requirements add processing, husbandry and coordinated supply. Confirmed future dimensions follow D-022 (Aether 4, Nether 5, Twilight 6, Otherside 8, reviewed Ad Astra stages 10–11, End 12). Charter after Tier 2 remains independently gated; this proposal does not decide its functions or exact boundary.

## Candidate resource table

All bare IDs below use `minecraft:`; `#` denotes an item tag. Modded IDs are **unverified candidates**, not a tested pack manifest. Their exact registered IDs, recipes, loader/version compatibility, profession ownership and approved earlier unlocks must be checked with the actual mods. An unavailable mod ingredient is a blocker, never silently substituted or treated as free.

| Gathering tier → destination | Resource candidates | Additional objectives / reward |
| --- | --- | --- |
| **1 → 2, approved and active** | `#minecraft:logs` 1,024; `cobblestone` 2,048; `wheat` 512; `iron_ingot` 128 | Permanent highest contributor earns Founders’ Lodge with included materials and Reeve. Existing awards and four-category scoring remain unchanged. |
| **2 → 3, revised user candidates** | `#minecraft:logs` 2,048; `cobblestone` **3,072**; `wheat` 768; `iron_ingot` **192**; **`copper_ingot` 384; `glass` 256; `brick` 384; `leather` 64** | Completed Founders’ Lodge; admin-approved connecting road. Approved reward scope: Royal Storehouse Blueprint, included construction materials, named Quartermaster and secure shared storage. Costs/scoring/evidence/storage access and capacity are not activated. |
| **3 → 4, proposal** | `#minecraft:logs` 1,536; **`stone_bricks` 1,024; `copper_block` 128; `iron_block` 32; `paper` 512; `book` 128; `gold_ingot` 64; `amethyst_shard` 128** | Proposed masonry/supply-route community project; exact evidence and next reward pending. |
| **4 → 5, proposal** | **`diamond` 16; `redstone` 512;** `gold_ingot` 128; **`lapis_lazuli` 256; `obsidian` 64; `bookshelf` 64; `smooth_stone` 512;** `glass` 512 | Proposed Overworld exploration/industry preparation. No Nether inputs. Completion reaches the approved Nether access tier; exact other unlocks/reward pending. |
| **5 → 6, proposal** | **`quartz` 512; `nether_bricks` 384; `blaze_rod` 32; `soul_sand` 128; `magma_cream` 32;** `iron_ingot` 512; **`redstone_block` 64;** `gold_ingot` 192 | Proposed safe Nether expedition/return route; exact evidence/reward pending. |
| **6 → 7, conditional proposal** | **`create:brass_ingot` 128; `create:precision_mechanism` 24; `mekanism:ingot_steel` 128;** `redstone_block` 96; `diamond` 32; **`quartz_block` 128; `glowstone` 64; `blaze_powder` 96** | Proposed shared processing workshop. BLOCKED until the selected versions and Create/Mekanism gates permit every recipe at or before gathering Tier 6. No new mod unlock is inferred. |
| **7 → 8, conditional proposal** | **`ars_nouveau:source_gem` 128; `occultism:iesnium_ingot` 32; `mysticalagriculture:prudentium_essence` 256; `productivebees:wax` 128; `create:electron_tube` 64; `mekanism:alloy_infused` 128; `amethyst_block` 128; `lapis_block` 64** | Proposed coordinated technology/magic production. All mod IDs/recipes and specialist availability require verification and prior unlocks ≤ Tier 7. |
| **8 → 9, conditional proposal** | **`irons_spellbooks:arcane_essence` 128; `ars_nouveau:abjuration_essence` 32; `occultism:spirit_attuned_gem` 64; `mekanism:alloy_reinforced` 96; `create:sturdy_sheet` 64; `crying_obsidian` 32; `prismarine_crystals` 256; `emerald` 64** | Proposed joint exploration/specialist challenge. No End inputs. Every mod dependency must be available ≤ Tier 8; unknown indirect End recipes block activation. |
| **9 → 10, conditional proposal** | **`prismarine_bricks` 1,024; `polished_blackstone_bricks` 512; `ghast_tear` 24; `honey_block` 32; `diamond_block` 8; `mekanism:alloy_atomic` 64; `ars_nouveau:conjuration_essence` 32;** `create:precision_mechanism` 64 | Proposed ocean/Nether expedition and industry preparation for the first reviewed space stage. End stays locked. Verify all mod components have a non-End path; no Tier 10 planetary ingredient may unlock its own access. |
| **10 → 11, conditional proposal** | **`glowstone` 128; `iron_block` 64;** **`netherite_ingot` 8; `mekanism:elite_control_circuit` 48; `create:railway_casing` 96; `ars_nouveau:wilden_tribute` 4; `amethyst_block` 128; `sea_lantern` 128** | Proposed Master/shared infrastructure and the approved first planetary stage, conditional on exact non-End recipes and Master availability. No Tier 11 destination material may be needed to unlock that destination. |
| **11 → 12, conditional proposal** | `netherite_ingot` 12; `diamond_block` 16; **`mekanism:ultimate_control_circuit` 64; `nether_star` 4; `irons_spellbooks:legendary_ink` 8; `gold_block` 64;** `redstone_block` 256; **`beacon` 2** | Proposed Master cooperation/legendary preparation and reviewed Tier 11 space-stage evidence. The entire route must be End-independent. Reaching 12 unlocks End access; Dragon/End objectives come afterward, not here. |
| **12 finale, conditional proposal** | **`smooth_quartz` 1,024; `deepslate_bricks` 2,048; `gilded_blackstone` 128; `gold_block` 64; `purpur_pillar` 256;** `mekanism:ultimate_control_circuit` 96; **`ars_nouveau:magebloom_fiber` 512;** `irons_spellbooks:legendary_ink` 12 | Post-unlock shared Dragon encounter and End expedition/return, then proposed Royal Legacy Monument construction award. Exact objectives, resource quantities, scoring, entitlement and included-materials policy need review; none can become a prerequisite for reaching 12. |

Bold entries emphasize new or revised requirements in these proposals, not an exclusive unlock. Costs deliberately stop increasing every bulk item monotonically. For example, logs decrease at Tier 3 as manufactured and exploration inputs take over. Rare challenge products have smaller counts; common automated goods provide only one part of completion.

## Why the newly introduced materials can belong at each stage

| First required while at tier | New materials and acquisition rationale | Dependency confidence |
| --- | --- | --- |
| 2 | Copper from Overworld mining/smelting; glass from sand smelting; individual **brick items**, not brick blocks, from clay smelting; leather from husbandry/hunting. | Known vanilla paths at Tier 1; no dimension/profession gate invented. The existing four resources remain. |
| 3 | Stone bricks from smelting/crafting; metal blocks from nine ingots each; paper from sugar cane, books from paper/leather; gold from mining/smelting; amethyst from geodes. | Overworld paths. The block quantities include their real ingot cost: 128 copper blocks = 1,152 ingots, 32 iron blocks = 288 ingots. Books add 384 paper and 128 leather beyond the separate paper line. This overlap in inputs is included in review estimates. |
| 4 | Diamonds/redstone/lapis from mining; obsidian from water/lava with a diamond pick; bookshelves from books/planks; smooth stone requires two smelting stages. | Overworld only. The proposed diamond quantity must cover donations **and** tools retained by players; no Nether resource is needed to unlock Nether. |
| 5 | Quartz, soul sand and magma cream from the accessible Nether; nether brick items are smelted/crafted into nether brick blocks for `nether_bricks`; blaze rods require fortress combat. Redstone blocks require nine dust each. | Nether entry was unlocked on reaching 5. Fortresses/terrain and combat create uncertainty; routes must allow safe return. |
| 6 | Brass, precision mechanisms and steel represent processing; quartz blocks/glowstone/blaze powder refine already accessible Nether inputs. | Vanilla paths known. Mod machine/recipe/profession gates **unverified**. Brass may depend on a heated mixer and blaze capture; verify those prerequisites, machine permissions and prior Nether access with the selected Create version. |
| 7 | Source gems, iesnium, essence and bee products mix magical, agricultural and specialist supply; electron tubes and infused alloys add industrial processing. | Exact IDs, crafting/ritual/bee setup and all research/profession prerequisites **unverified**. Merely registering an item does not establish an obtainable recipe or approved unlock. |
| 8 | Arcane/abjuration/spirit products, reinforced alloy and sturdy sheets introduce advanced steps; crying obsidian, ocean-monument crystals and emeralds add exploration/trade variety. | Vanilla exploration paths exist before Tier 8. Mod recipes may contain hidden dimension ingredients; block this row if any needs End access or research unlocked by its own completion. |
| 9 | Prismarine bricks from ocean supplies/crafting; polished blackstone bricks and ghast tears from the already accessible Nether; honey blocks from Overworld beekeeping; diamond blocks from mining. | Proposed substitutes only. No End input; atomic alloy, conjuration and precision mechanisms remain unverified optional slots pending recursive non-End recipe approval. |
| 10 | Glowstone, iron blocks, netherite, ocean lighting and amethyst replace End rods/shells and the unverified stabilizer. Elite circuitry/railway parts and Wilden tribute are conditional candidates. | No End access assumed. Verify all remaining mod recipes/Master eligibility and first-stage Ad Astra prerequisites; Tier 11 destination materials cannot be needed to reach their own unlock. |
| 11 | Ultimate circuitry/legendary ink remain optional audit candidates; gold blocks replace the withdrawn stabilizer. Nether stars/beacons/redstone have vanilla Overworld/Nether paths. | Every prerequisite to attain 12 must have an approved non-End route; planetary/recipe closure is unverified. Do not call the complete row End-independent until the actual pack is audited. |
| 12 | End and purpur inputs may be considered only after the Tier 12 unlock, alongside the Dragon/expedition finale and final architectural supplies. | Royal Legacy Monument remains a proposed reward, not Town Hall. Its exact costs/award rules are unapproved. Gilded blackstone 128 remains a nonrenewable-resource balance concern. |

The implementation has explicit conservative vanilla material profiles and checks declared capability edges. It **does not claim automatic discovery of arbitrary vanilla/mod recipes, machine outputs, loot paths or cross-mod conversions**. Unknown paths fail validation for activation. The profiles describe the current vanilla baseline; a modpack that changes acquisition needs an independently verified adapter/profile before approval.

## Dependency graph and deadlocks

The revised planning graph and dimension/recipe audit are maintained in [DIMENSION_PROGRESSION.md](DIMENSION_PROGRESSION.md). That graph replaces the former End-at-9 diagram.

The executable graph includes each milestone, every capability, sequential prior milestones, declared capability dependencies and known material prerequisites. A capability unlocked at Tier 5 depends on completing Tier 4. Putting quartz into Tier 4 creates `Tier 4 → Nether capability → Tier 4`; the validator reports the concrete cycle and forward-access error even when the author omits an explicit Nether prerequisite. The same rule catches End materials before access, self-dependencies, unknown references and cyclic technology capabilities. Even unused circular definitions are reported. The executable validator still reflects the old End-9 baseline. D-022 requires an End-12 capability depending on completion of Tier 11; change code/data/tests together only after approval. Fixed gates must not be lowered by metadata.

Outstanding deadlock checks: approved technology/magic tier matrix; full recipe/machine/loot dependency closure for actual versions; quest/rank eligibility; profession ownership/delegation on shared machines; access to required biome/structure within the eventual border; safe return/portal materials; limited unique or nonrenewable resources. No raw item-ID validation can prove all of these. An unresolved check keeps its proposal disabled.

## Indicative effort for ten active players

These are **planning hypotheses**, not measured balance or required timers. Aggregate active player-hours include manual resource work and the proposed shared objective, excluding prior profession-rank training, normal play and waiting for approvals. Ten equally focused players could divide the hours by ten, but travel, crop growth, breeding, machinery startup and uneven roles affect elapsed time. Players are not forced to wait a fixed number of days.

| Gathering tier | Aggregate active player-hours, proposal | Main uncertainty |
| --- | --- | --- |
| 1 | 6–10 | Fresh-world tools, farm setup and ten-player coordination |
| 2 | 8–14 | Clay/leather processing and road construction |
| 3 | 12–20 | Metal quantities, book inputs and geodes |
| 4 | 16–26 | Diamond mining and community objective |
| 5 | 18–30 | Fortress access, combat and safe travel |
| 6 | 20–35 | Actual machine recipes and batch throughput |
| 7 | 24–40 | Research, rituals, bee setup and profession permissions |
| 8 | 28–44 | Rare exploration/advanced crafting paths |
| 9 | 30–48, re-estimate | Revised ocean/Nether inputs and pre-space preparation; no End travel |
| 10 | 35–55, re-estimate | Master eligibility and reviewed first planetary stage, with no End dependency |
| 11 | 40–65, re-estimate | Verified non-End recipe closure and reviewed next planetary stage |
| 12 | 45–75, re-estimate | Post-unlock Dragon/End expedition and proposed Monument project |

Before activation, record production rates with the exact modpack and ten-player server: median manual gathering rate, practical processing throughput, travel variance, rare-resource availability and cost per accepted category. Inspect low/high automation scenarios. Reduce common bulk quotas where they become busywork; adjust diverse objectives rather than increasing every stack count. The existing 2–3 week profession Master target needs its own representative-profession playtest.

## Scoring and AFK fairness — unresolved policy, not enabled

First Hearth retains its exact four equal normalized weights and deterministic first-achieved-score tie handling. Never rescore existing history or re-elect its winner.

D-023 requires greater contribution weight for rare/difficult categories than common ones in later tiers. Review exact coefficients and thematic grouping before accepting future contributions; equal weighting of every later resource is no longer a default proposal. Eight individual equal weights can overreward an easy automated line relative to a rare one; broad categories can underrecognize multiple real supply chains. Set weights **before the first contribution**, pin the definition/scoring version for that milestone, use exact rational/integer arithmetic and retain a permanent per-milestone winner/history. No future scoring default is selected in this build.

Inventory items alone do not prove manual origin. This framework does not invent an "AFK-free" flag or claim to reject farmed materials. Diverse challenge/construction objectives can prevent one cheap farm completing a tier, but meaningful attribution and the remaining offline-automation policy still require approval and tests. Secure shared-storage contents must never grant contribution credit without a separate reviewed inventory transaction.

### D-022 replacement audit

Removed End stone 1,024, purpur blocks 512, shulker shells 24/32, dragon breath 32 and End rods 128 from all pre-12 proposal rows. The replacement counts above are new **unapproved review candidates**, not numerical equivalences or a claim of measured balance. Withdrew both Occultism stabilizer candidates pending recipe audit. All other modded slots remain non-mandatory/unverified until their complete non-End acquisition paths are proven. Aether/Twilight/Otherside/planet-specific materials are not invented before compatible versions, IDs and internal progression are pinned. No Java/JSON/configuration changed.
