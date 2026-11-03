# PROPOSED — remaining 264 profession quests, batch review v1

**Status: PROPOSED, NOT APPROVED, NOT ENABLED.** Prepared under the user's explicit request for one complete eleven-profession specification. This is not a recovered historical approval. Farmer's24 approved quests, IDs, objectives, quantities, rewards and existing records are unchanged. Approval of this document is not a claim that mod adapters or the full pack have already passed runtime tests.

Review this document as one batch:264 quests, their quantities, validation rules, prerequisites, reward policy and explicitly proposed availability assumptions. No proposed quest or new building/technology gate activates until approved. If any proposed availability conflicts with a previously approved restriction, the existing restriction wins and the affected quest must be revised before activation; it must never silently bypass that gate.

## Shared progression and rewards

- Exactly 8 Apprentice,8 Journeyman and 8 Master quests per profession. Stable IDs match the existing `kingdom_smp:<profession>/<a|j|m>01..08` slots. Each row is a personally attributed once-only completion, not an FTB team-rank grant.
- Apprentice is the starting rank. All 8 Apprentice proofs unlock Journeyman and its purchasing rights. All 8 Journeyman proofs open Master trials. All 8 Master trials then unlock Master and the right to purchase approved Master equipment. No signature item is granted free, and no quest requires a Master signature to qualify.
- Proposed recognition rewards mirror Farmer's established scale:100/200/300 personal profession XP per A/J/M quest,4800 total. No automatic coin or item rewards. Every row states its XP and any band completion rights. Existing Master prices,50 percent compounded repeat increases, trade/use permissions and tax sinks remain unchanged.
- Maximum 3 active professions, preserved inactive progress,15000 coin switching and active-only new credit/purchasing benefits remain authoritative. Selecting a cosmetic title never changes eligibility.
- A/J/M goals run concurrently within the open band unless an explicit prerequisite is listed. Every J quest also requires all 8A quests; every M quest requires all 8J quests. Final assessments require the other 7 proofs in their band. No retroactive credit from an earlier band unless an already permanent shared milestone is explicitly referenced.
- “Completed premises” means that profession's legitimate kingdom building is completed, intact, staffed and available under the normal authenticated construction/claim rules. A profession does not gain facilities merely from a quest document or client packet.
- Normal owner/claim consent is mandatory for every affected site, entity, machine and route. Inspection never authorizes destruction. Material delivery means permanent reviewed consumption with recovery; machine production needs genuine server attribution and inputs. Shared events may credit different eligible participants once each, but cannot pay the same player's quest twice.

## Availability and effort assumptions — proposed where marked

These are dependencies for review, not enabled unlocks. Existing Builder/Lumberjack building Tier 2 and approved dimension/service gates are retained. The other profession-building and technology stages below are **PROPOSED** parts of this review. They do not finalize kingdom milestone costs or create new mandatory tier objectives.

| Profession | Premises / Apprentice minimum | Journeyman minimum | Master-trial minimum | Estimated active effort after sources unlock |
| --- | --- | --- | --- | --- |
| Blacksmith | Tier 3 (PROPOSED building gate) | Tier 4 | Tier 5 | 14–24 hours, spread over roughly 14–21 days |
| Miner | Tier 3 (PROPOSED building gate) | Tier 4 | Tier 5 | 14–24 hours, spread over roughly 14–21 days |
| Builder | Tier 2 (existing approved building gate) | Tier 3 | Tier 4 | 14–24 hours, spread over roughly 14–21 days |
| Lumberjack | Tier 2 (existing approved building gate) | Tier 3 | Tier 6 | 14–24 hours, spread over roughly 14–21 days |
| Merchant | Tier 3 (PROPOSED building gate) | Tier 4 | Tier 6 | 14–24 hours, spread over roughly 14–21 days |
| Engineer | Tier 4 (PROPOSED building gate) | Tier 6 | Tier 8 | 20–30 hours, spread over roughly 14–21 days |
| Mechanic | Tier 6 (PROPOSED building gate) | Tier 8 | Tier 10 | 20–30 hours, spread over roughly 14–21 days |
| Mage | Tier 4 (PROPOSED building gate) | Tier 6 | Tier 8 | 20–30 hours, spread over roughly 14–21 days |
| Adventurer | Tier 3 (PROPOSED building gate) | Tier 6 | Tier 10 | 14–24 hours, spread over roughly 14–21 days |
| Alchemist | Tier 5 (PROPOSED building gate) | Tier 6 | Tier 8 | 14–24 hours, spread over roughly 14–21 days |
| Librarian | Tier 4 (PROPOSED building gate) | Tier 6 | Tier 8 | 14–24 hours, spread over roughly 14–21 days |

Effort estimates are starting balance assumptions for ten active players, not tested timings or mandatory idle timers. Weekly/daily work naturally spaces service; quantities and cooperation must be tuned through play rather than padded into AFK grinds. Community dimension/technology advancement can delay a band; no kingdom tier may require a Master-only result whose own qualification depends on that tier. The future tier table must be checked against this graph before costs activate.

Procedural-world availability also matters: accessible biomes, dungeons, seedlings and structures must be confirmed within the current border or through legitimate unlocked acquisition/trade routes. No quest creates an admin-spawned resource, free sapling shop or border bypass. If a required source is unavailable, select a reviewed equivalent within its stated category or revise the quest before activation; do not silently award it or remove approved restrictions. Exact rare species/locations below therefore require source checks in the actual seed/pack.

## Capability and mod dependencies

The snapshot establishes mod identity/version presence, not verified registry IDs, recipe closure or runtime compatibility. Implementation must resolve each modded output to actual registered recipe/item/process IDs and audit transitive ingredients against its gate before activation. Missing APIs, recipes or prohibited prerequisites fail closed. Do not substitute arbitrary items or pretend an untested adapter works. Native rocket, boss-order, ritual, breeding and spellbook prerequisites remain mandatory.

| Code | Minimum tier | Mod identity | Required capability / authority |
| --- | --- | --- | --- |
| N5 | 5 | `minecraft` | Nether access; already approved Tier 5 |
| AE4 | 4 | `aether` | Aether access; already approved Tier 4 |
| TF6 | 6 | `twilightforest` | Twilight Forest access; already approved Tier 6; native boss-order prerequisites remain |
| DD8 | 8 | `deeperdarker` | Otherside access; already approved Tier 8; native portal prerequisites remain |
| SPACE10 | 10 | `ad_astra` | Moon/Earth-orbit stage; approved destination split, native launch and recipe prerequisites retained |
| AIR6 | 6 | `immersive_aircraft` | Tier 6 and maintained completed Aerodrome; buyer or legitimate shared crew |
| BH3 | 3 | `minecraft` | Tier 3 and maintained completed Royal Bounty Hall |
| C4 | 4 | `create` | PROPOSED basic Create power, transmission, pressing and milling availability |
| C6 | 6 | `create` | PROPOSED heated processing, filters, assembly and controlled contraptions |
| C8 | 8 | `create` | PROPOSED steam power and advanced integrated production |
| CK6 | 6 | `sliceanddice` | PROPOSED native Slice and Dice/Central Kitchen processing; both exact installed add-ons checked |
| K6 | 6 | `mekanism` | PROPOSED basic Mekanism machines, infusion, basic circuits and ordinary energy storage |
| K8 | 8 | `mekanism` | PROPOSED factories, advanced circuits, alloys, gas handling and multistep processing |
| K10 | 10 | `mekanism` | PROPOSED elite/ultimate circuits and ordinary advanced energy storage; excludes Creative Energy Cube |
| KG6 | 6 | `mekanismgenerators` | PROPOSED safe ordinary generator availability; no mandatory fission/fusion reactor |
| A4 | 4 | `ars_nouveau` | PROPOSED native Novice Spellbook, Tier 1 glyphs, source and effective-cast access |
| A6 | 6 | `ars_nouveau` | PROPOSED native Apprentice Spellbook and Tier 2 glyphs; not Kingdom Master signature equipment |
| A8 | 8 | `ars_nouveau` | PROPOSED native Archmage Spellbook and Tier 3 glyphs available during Master trials |
| AC6 | 6 | `ars_creo` | PROPOSED supported Ars Creo interaction with unlocked Create mechanics |
| I4 | 4 | `irons_spellbooks` | PROPOSED COMMON/UNCOMMON native scroll/casting access |
| I6 | 6 | `irons_spellbooks` | PROPOSED RARE native scroll/casting access |
| I8 | 8 | `irons_spellbooks` | PROPOSED EPIC native scroll and ordinary reagent access during Master trials |
| O6 | 6 | `occultism` | PROPOSED accessible pre-End ritual ingredients and native ritual access |
| B6 | 6 | `productivebees` | PROPOSED eligible productive forest bees/byproducts; native breeding dependencies retained |
| MA4 | 4 | `mysticalagriculture` | PROPOSED basic non-End essence production; excludes any higher-stage seed/product |
| S4 | 4 | `supplementaries` | PROPOSED accessible functional furnishings; no separate kingdom service unlocked |

The approved full schedule remains Aether 4, Nether 5, Twilight 6, Otherside 8, Waystones 9, Ad Astra Moon/Earth-orbit 10 and remaining approved planets/orbits 11, End 12. No proposed profession qualification requires End access, elytra, shulker/chorus/Dragon materials or an End-exclusive boss. A Nether quest is never an input to unlocking Nether; the same rule applies to every dimension/technology stage. Native/pack recipes must also be checked for indirect End dependencies. The Ender Dragon remains a post-unlock Tier 12 kingdom finale, not a profession precondition.

Create/Ars/Mekanism stages here are explicitly proposed availability assumptions, not silently finalized global policy. Approving a quest never grants client authority to bypass a stricter existing stage. The Creative Energy Cube is excluded from Mechanic qualification and remains Tier 12 plus the approved Master Mechanic entitlement. Librarian's unspecified signature reward is not invented by this proposal; its quest Master rank/title can be approved independently of a future signature design.

## Server validation vocabulary

Codes in a row are cumulative. They specify required implementation evidence, not APIs already implemented for every mod.

| Code | Required proof and abuse boundary |
| --- | --- |
| NAT | Authoritative successful player break of naturally generated eligible ore; saved placed/generated/replaced-block exclusions, dimension and claims checked; no fake-player/machine-nearest-player credit. |
| TREE | Native natural tree or verified planted-to-mature tree lifecycle; genuine player harvest with claim consent; placed construction logs, repeated placements and copied client tags never qualify. |
| GROW | Saved server planting/growth provenance and successful native harvest; unique lifecycle IDs and appropriate seed/propagule costs; no mature-block transplant credit. |
| MAKE | Actual native recipe/smelting/stonecutting event attributed to the active player or authenticated production job; required output must be delivered when DEL is specified. Mere inventory possession is insufficient. |
| DEL | Reviewed exact-item permanent consumption through native inventory receipt/recovery and canonical once-only quest commit. Return only legitimate emptied containers. Input IDs/components/tags resolve on the real server. |
| BUILD | Server-confirmed retained placements using real supplied materials in an authorized site; count distinct final positions after stability inspection; removing/replacing the same block cannot farm progress. |
| INSPECT | OP2 onsite review and explicit confirmation of actual working project/proofs on consented land; saved actor/project/snapshot audit; no terrain edits or fabricated event counts; reused approvals cannot pay twice. |
| JOB | Authenticated player-started native machine/ritual/processing job, real inputs and registered recipe, native completion/output evidence and unique job receipt. No credit from passive AFK ticks, nearest-player guesses or client machine claims. Offline automation policy itself is unchanged. |
| CONTRACT | Existing canonical completed contracts of this profession in this objective band, with distinct UTC daily/weekly periods as specified; no expired/reset/canceled or duplicated reward credit. |
| REPAIR | Real native repair input, worn-item identity, material/XP costs and improved legitimate durability; distinct service receipts; no duplicated outputs, repeated same-item artificial wear loop or loss of protected enchantments. |
| SMITH | Native smithing result with real template/base/addition consumption; output identity and recipe verified; no copied upgrade tags or Master-item prerequisite. |
| ENCHANT | Native enchant/anvil operation with actual XP/lapis/input debit and compatible authorized result; unique service/output lineage. No enchant/grindstone recycling credit, signature duplication or extraction bypass. |
| BREW | Player-attributed native brewing job with registered recipe, real reagents/fuel/bottles and actual outcome; unique batches. Effect-use tasks require genuine native effects on eligible consenting targets, never fabricated potion tags. |
| EXPLORE | Server-observed dimension/biome/structure positions and legitimate entry/return events; current border/dimension permission, native progression and unique locations. Client coordinates, copied books and admin teleport do not prove exploration. |
| COMBAT | Native eligible hostile/boss entity UUID and actual combat participation; natural/native encounter provenance, valid damage or useful support, no dummy/player-created captive mob farming or compulsory PvP. Boss participation uses at least 10 percent effective contribution or verified meaningful support; per-player once-only credit. |
| MAGIC | Native server spell/ability event with actual mana/source/input spending and successful useful effect; distinct eligible targets/functions. Empty casts, immune targets, duplicate packets and unconsented protected targets give no credit. |
| TRADE | Canonical settled escrow/shop/order receipt with real delivery, correct 5 percent regular/10 percent Master tax burn and actual counterparties. No self-trade, canceled sale or repeated receipt credit; peer-diversity goals cap repeated same-pair credit at 2 per UTC day. |
| BANK | Real canonical deposit/withdrawal/conversion and native item receipt; value conserved. The one explicit tutorial round trip grants its quest once, not repeatable XP or money. |
| HUNT | Existing canonical hunt accepted before combat and settled after genuine attributed eligible kills; no repeated/canceled claim credit. |
| FLIGHT | Existing canonical real registered powered-flight dispatch/scouting/cargo evidence and once-only delivery record. Own or legitimately shared aircraft allowed; normal claims/gates, no client travel proof or teleport credit. |

Production/consumption proofs are server-authoritative, cannot be created by item names/lore/client NBT, and are bound to actual player, recipe/job, stage and unique operation. Generic “crafted count” alone cannot prove consumable output. For field guides and written reports, client book text is descriptive only; actual server exploration/inspection records supply the proof. Contract, auction, bank, flight and cargo objectives reuse their existing canonical authorities rather than adding parallel balances or inventories.

## Complete proposed quest tables

Every row additionally requires: active profession, its maintained completed premises, stated tier/capability, open rank band, and normal server/claim permissions. Extra dependencies refer to slots in the same profession. A dash means no extra dependency or mod capability beyond those shared requirements. All rows and proposed minimums below are awaiting this single batch review.

### Blacksmith —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:blacksmith/a01` | Fuel the forge | Apprentice | Supply coal or charcoal for a commissioned forge batch | 64 fuel | DEL | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:blacksmith/a02` | Clean iron stock | Apprentice | Smelt and deliver personally processed iron ingots | 32 ingots | MAKE+DEL | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:blacksmith/a03` | Copper fittings | Apprentice | Craft and deliver lightning rods for public roofs | 8 rods | MAKE+DEL | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:blacksmith/a04` | Tools for settlers | Apprentice | Forge a varied iron tool commission | 1 pickaxe, axe, shovel and hoe | MAKE+DEL | Tier 3; premises; active; open Apprentice band; a02 | 100 profession XP |
| `kingdom_smp:blacksmith/a05` | Shieldwright | Apprentice | Forge and deliver shields for expeditions | 4 shields | MAKE+DEL | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:blacksmith/a06` | First repairs | Apprentice | Repair genuinely worn iron equipment using actual materials | 4 distinct items | REPAIR | Tier 3; premises; active; open Apprentice band; a04 | 100 profession XP |
| `kingdom_smp:blacksmith/a07` | Apprentice contracts | Apprentice | Complete smithing daily commissions in distinct periods | 3 daily periods | CONTRACT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:blacksmith/a08` | A working smithy | Apprentice | Demonstrate a protected forge, anvil, fuel area and safe access | 1 inspected workshop | INSPECT | Tier 3; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:blacksmith/j01` | Golden workmanship | Journeyman | Forge and deliver a precision gold component commission | 16 gold ingots plus 2 clocks | MAKE+DEL | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:blacksmith/j02` | Diamond edge | Journeyman | Forge expedition-grade diamond tools without Master equipment | 1 pickaxe and 1 axe | MAKE+DEL | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:blacksmith/j03` | Armour commission | Journeyman | Supply a complete personally forged iron armour set | 1 four-piece set | MAKE+DEL | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:blacksmith/j04` | Tempered repairs | Journeyman | Repair worn enchanted gear without deleting enchantments | 4 distinct repair jobs | REPAIR | Tier 4; premises; active; open Journeyman band; j02 | 200 profession XP |
| `kingdom_smp:blacksmith/j05` | Compatible enchantments | Journeyman | Combine compatible ordinary enchantments through legitimate anvils | 4 consumed commissioned tools | ENCHANT+DEL | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:blacksmith/j06` | Pattern keeper | Journeyman | Duplicate and supply a discovered Overworld armour trim template | 2 copies of 1 template | MAKE+DEL | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:blacksmith/j07` | Workshop service week | Journeyman | Fulfil smithing contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:blacksmith/j08` | Journeyman forge | Journeyman | Demonstrate furnace workflow, storage, working anvil and customer access | 1 functional inspection | INSPECT | Tier 4; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07 | 200 profession XP; Master trials open |
| `kingdom_smp:blacksmith/m01` | Nether expedition fittings | Master | Supply fire-resistance expedition hardware from refined Nether resources | 8 nether bricks and 4 gold boots | MAKE+DEL | Tier 5; premises; active; open Master band; N5 | 300 profession XP |
| `kingdom_smp:blacksmith/m02` | Debris to alloy | Master | Process ancient debris into a real netherite alloy commission | 1 netherite ingot | MAKE+DEL | Tier 5; premises; active; open Master band; N5 | 300 profession XP |
| `kingdom_smp:blacksmith/m03` | Upgrade artisan | Master | Apply an authentic netherite upgrade to ordinary diamond equipment | 1 registered upgraded item | SMITH | Tier 5; premises; active; open Master band; m02; N5 | 300 profession XP |
| `kingdom_smp:blacksmith/m04` | Basalt and quartz orders | Master | Supply refined volcanic masonry and precision quartz components | 32 polished basalt and 16 quartz blocks | MAKE+DEL | Tier 5; premises; active; open Master band; N5 | 300 profession XP |
| `kingdom_smp:blacksmith/m05` | Veteran repair bench | Master | Restore damaged equipment for different consenting players | 3 jobs for at least 2 players | REPAIR | Tier 5; premises; active; open Master band; m03 | 300 profession XP |
| `kingdom_smp:blacksmith/m06` | A week of reliability | Master | Complete distinct smithing contracts during Master trials | 5 daily periods | CONTRACT | Tier 5; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:blacksmith/m07` | Community forge project | Master | Build an approved shared smelting and maintenance installation | 1 inspected civic project | INSPECT | Tier 5; premises; active; open Master band; m01,m04 | 300 profession XP |
| `kingdom_smp:blacksmith/m08` | Master smith assessment | Master | Demonstrate forging, repairs and safe compatible enchantment handling | 1 final assessment | INSPECT | Tier 5; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07 | 300 profession XP; Master rank/title; right to buy Extraction Anvil |

### Miner —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:miner/a01` | Natural coal seam | Apprentice | Mine naturally generated coal rather than placed or recycled ore | 64 ore blocks | NAT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:miner/a02` | Iron beneath the hills | Apprentice | Extract natural iron or deepslate iron ore | 48 ore blocks | NAT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:miner/a03` | Copper survey | Apprentice | Extract natural copper in a separate working area | 64 ore blocks | NAT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:miner/a04` | Safe descent | Apprentice | Reach working depth and return by a safe player-built access route | 1 descent below Y0 and return | EXPLORE | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:miner/a05` | Stoneworker supply | Apprentice | Deliver cut stone for approved community building work | 128 stone bricks | MAKE+DEL | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:miner/a06` | Mine lighting | Apprentice | Install legitimate lighting and protected access in the mine | 1 inspected route with 16 lights | INSPECT | Tier 3; premises; active; open Apprentice band; a04 | 100 profession XP |
| `kingdom_smp:miner/a07` | Mining commissions | Apprentice | Complete personal mining daily contracts in distinct periods | 3 daily periods | CONTRACT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:miner/a08` | An orderly mine | Apprentice | Demonstrate supported access, lighting, equipment and ore sorting | 1 Apprentice mine inspection | INSPECT | Tier 3; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:miner/j01` | Redstone seam | Journeyman | Extract naturally generated redstone ore | 48 ore blocks | NAT | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:miner/j02` | Lapis layer | Journeyman | Extract natural lapis ore for the kingdom's enchanters | 24 ore blocks | NAT | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:miner/j03` | Golden depth | Journeyman | Extract naturally generated Overworld gold ore | 24 ore blocks | NAT | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:miner/j04` | First diamonds | Journeyman | Extract naturally generated diamond ore | 8 ore blocks | NAT | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:miner/j05` | Amethyst study | Journeyman | Harvest grown amethyst clusters without consuming budding blocks | 32 shards from verified clusters | GROW+DEL | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:miner/j06` | Geological diversity | Journeyman | Record distinct Overworld geology in personally visited working areas | 3 areas with different stone or cave settings | EXPLORE | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:miner/j07` | Sustained mining service | Journeyman | Complete mining contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:miner/j08` | Journeyman mine plan | Journeyman | Submit a safe mine plan with separate ore, access and support areas | 1 inspected working mine | INSPECT | Tier 4; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07 | 200 profession XP; Master trials open |
| `kingdom_smp:miner/m01` | Quartz expedition | Master | Extract natural Nether quartz after Nether access unlocks | 64 ore blocks | NAT | Tier 5; premises; active; open Master band; N5 | 300 profession XP |
| `kingdom_smp:miner/m02` | Nether gold survey | Master | Extract naturally generated Nether gold ore | 32 ore blocks | NAT | Tier 5; premises; active; open Master band; N5 | 300 profession XP |
| `kingdom_smp:miner/m03` | Ancient debris survey | Master | Locate and personally extract ancient debris | 4 natural debris blocks | NAT | Tier 5; premises; active; open Master band; N5 | 300 profession XP |
| `kingdom_smp:miner/m04` | Preserved specimens | Master | Supply naturally mined Silk Touch ore specimens using ordinary eligible tools | 1 iron, gold, redstone and lapis ore | NAT+DEL | Tier 5; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:miner/m05` | Rare seam record | Master | Extract and document distinct natural diamond finds | 12 ore blocks across 3 chunk regions | NAT | Tier 5; premises; active; open Master band; j04 | 300 profession XP |
| `kingdom_smp:miner/m06` | Master supply service | Master | Complete distinct mining contracts during Master trials | 5 daily periods | CONTRACT | Tier 5; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:miner/m07` | Community excavation | Master | Complete an authorized excavation while preserving protected structures | 1 inspected excavation and access project | INSPECT | Tier 5; premises; active; open Master band; m04,m05 | 300 profession XP |
| `kingdom_smp:miner/m08` | Master prospecting assessment | Master | Demonstrate geology, safety, valuable extraction and expedition records | 1 final assessment | INSPECT | Tier 5; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07 | 300 profession XP; Master rank/title; right to buy Prospector's Pickaxe |

### Builder —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:builder/a01` | Foundations by hand | Apprentice | Place supplied foundation blocks at an authorized personal project | 128 retained blocks | BUILD | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:builder/a02` | Timber frame | Apprentice | Build a functional frame using actual supplied timber | 64 retained timber blocks | BUILD | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:builder/a03` | Weatherproof roofing | Apprentice | Build a supported roof rather than placing and breaking the same cells | 64 retained roof blocks | BUILD | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:builder/a04` | Working entrance | Apprentice | Install a clear doorway, stairs and practical navigation | 1 inspected entrance | INSPECT | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:builder/a05` | Masonry commission | Apprentice | Craft and supply varied basic masonry | 32 stone-brick stairs and 32 slabs | MAKE+DEL | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:builder/a06` | Road starter | Apprentice | Build a continuous safe route to an existing kingdom service | 48 retained route blocks | BUILD | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:builder/a07` | Builder commissions | Apprentice | Complete distinct Builder daily contracts | 3 daily periods | CONTRACT | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:builder/a08` | Apprentice worksite | Apprentice | Present the completed authorized frame, roof and access work | 1 inspected project | INSPECT | Tier 2; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:builder/j01` | Supported bridge | Journeyman | Build a protected crossing with sound approaches and supports | 1 bridge spanning at least 12 blocks | INSPECT | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:builder/j02` | Window and wall craft | Journeyman | Complete a furnished facade using supplied glass and masonry | 64 wall blocks and 16 glass blocks retained | BUILD | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:builder/j03` | Functional public access | Journeyman | Improve access to an approved existing kingdom building | 1 inspected access project | INSPECT | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:builder/j04` | Interior plan | Journeyman | Furnish a usable meeting or workshop interior without adding a service unlock | 1 inspected room with 6 functional zones | INSPECT | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:builder/j05` | Drainage and grading | Journeyman | Build a safe graded route with drainage and no unauthorized terrain removal | 1 inspected route of 48 blocks | INSPECT | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:builder/j06` | Material variety | Journeyman | Complete different legitimate construction commissions | 3 commissions with timber, stone and glass | BUILD+INSPECT | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:builder/j07` | A reliable contractor | Journeyman | Complete Builder contracts in distinct weekly periods | 2 weekly periods | CONTRACT | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:builder/j08` | Journeyman handover | Journeyman | Document retained materials, access, furnishing and claim consent | 1 inspected handover | INSPECT | Tier 3; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07 | 200 profession XP; Master trials open |
| `kingdom_smp:builder/m01` | Large civic frame | Master | Build an authorized larger project using supplied materials | 384 retained structural blocks | BUILD | Tier 4; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:builder/m02` | Detailed masonry | Master | Complete varied decorative masonry without affecting service restrictions | 96 retained stair, slab and wall blocks | BUILD | Tier 4; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:builder/m03` | Supplementary fittings | Master | Install working Supplementaries furnishings appropriate to the project | 4 different registered fittings | INSPECT | Tier 4; premises; active; open Master band; S4 | 300 profession XP |
| `kingdom_smp:builder/m04` | Layered interior | Master | Furnish a connected multi-level player-built interior with safe circulation | 2 usable floors and 8 functional areas | INSPECT | Tier 4; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:builder/m05` | Civic route network | Master | Connect existing services with safe, consented public routes | 2 inspected connections | INSPECT | Tier 4; premises; active; open Master band; m01 | 300 profession XP |
| `kingdom_smp:builder/m06` | Master builder service | Master | Complete distinct Builder contracts during Master trials | 5 daily periods | CONTRACT | Tier 4; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:builder/m07` | Settlement project | Master | Deliver an approved public plaza or similar non-service civic project | 1 inspected furnished civic project | INSPECT | Tier 4; premises; active; open Master band; m02,m03,m04,m05 | 300 profession XP |
| `kingdom_smp:builder/m08` | Master architect assessment | Master | Demonstrate sound construction, supplied materials and protected-site planning | 1 final assessment | INSPECT | Tier 4; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07 | 300 profession XP; Master rank/title; right to buy Architect's Wand |

### Lumberjack —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:lumberjack/a01` | Oak stewardship | Apprentice | Harvest legitimate natural or mature planted oak trees | 128 qualifying logs | TREE | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:lumberjack/a02` | Birch stand | Apprentice | Harvest a second sustainable timber species | 64 birch logs | TREE | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:lumberjack/a03` | Spruce timber | Apprentice | Supply mature spruce timber from legitimate trees | 64 spruce logs | TREE | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:lumberjack/a04` | Replant the clearing | Apprentice | Plant and grow replacements for harvested trees | 24 verified replacement trees | GROW | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:lumberjack/a05` | Usable timber stock | Apprentice | Craft and supply building timber | 128 planks and 32 stripped logs | MAKE+DEL | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:lumberjack/a06` | Safe forest access | Apprentice | Prepare a protected marked route and safe felling area | 1 inspected forest route | INSPECT | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:lumberjack/a07` | Timber commissions | Apprentice | Complete distinct Lumberjack daily contracts | 3 daily periods | CONTRACT | Tier 2; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:lumberjack/a08` | Apprentice woodland | Apprentice | Demonstrate species diversity, harvest history and growing replacements | 1 woodland inspection | INSPECT | Tier 2; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:lumberjack/j01` | Hardwood survey | Journeyman | Harvest legitimate acacia and dark oak stands | 64 logs of each species | TREE | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:lumberjack/j02` | Tropical timber | Journeyman | Harvest legitimate jungle timber without counting placed-log structures | 96 jungle logs | TREE | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:lumberjack/j03` | Cherry renewal | Journeyman | Grow and sustainably harvest cherry trees | 16 mature planted trees | GROW+TREE | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:lumberjack/j04` | Mangrove nursery | Journeyman | Grow and harvest mangroves from real propagules | 12 mature planted mangroves | GROW+TREE | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:lumberjack/j05` | Charcoal commission | Journeyman | Process personally harvested timber into delivered charcoal | 64 charcoal | MAKE+DEL | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:lumberjack/j06` | Forest supply chain | Journeyman | Supply structural woodwork across several categories | 32 fences, 16 stairs and 16 trapdoors | MAKE+DEL | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:lumberjack/j07` | Sustained timber service | Journeyman | Complete Lumberjack contracts in distinct weekly periods | 2 weekly periods | CONTRACT | Tier 3; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:lumberjack/j08` | Journeyman forestry plan | Journeyman | Demonstrate access, species variety and maintained replanting areas | 1 inspected managed woodland | INSPECT | Tier 3; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07 | 200 profession XP; Master trials open |
| `kingdom_smp:lumberjack/m01` | Mature forest mosaic | Master | Grow and harvest a mixed managed woodland | 6 species with 8 mature trees each | GROW+TREE | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:lumberjack/m02` | Kingdom timber commission | Master | Deliver personally harvested high-variety structural timber | 256 logs with at least 32 from 4 species | TREE+DEL | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:lumberjack/m03` | Pollinator woodland | Master | Establish a functioning Productive Bees forest pollination plot | 1 inspected flowering plot with 2 permitted pre-End resource-bee species | INSPECT | Tier 6; premises; active; open Master band; B6 | 300 profession XP |
| `kingdom_smp:lumberjack/m04` | Forest products | Master | Supply real resource combs from the documented productive apiary | 16 resource combs from each of its 2 eligible species | JOB+DEL | Tier 6; premises; active; open Master band; m03; B6 | 300 profession XP |
| `kingdom_smp:lumberjack/m05` | Renewal cycles | Master | Complete repeated growing and harvesting cycles without tree-placement exploits | 24 verified mature replacement cycles | GROW+TREE | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:lumberjack/m06` | Master timber service | Master | Complete distinct Lumberjack contracts during Master trials | 5 daily periods | CONTRACT | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:lumberjack/m07` | Community forest reserve | Master | Create an authorized accessible managed forest and pollinator reserve | 1 inspected community forestry project | INSPECT | Tier 6; premises; active; open Master band; m01,m02,m03,m04,m05 | 300 profession XP |
| `kingdom_smp:lumberjack/m08` | Master forester assessment | Master | Demonstrate renewable supply, conservation and community service | 1 final assessment | INSPECT | Tier 6; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07 | 300 profession XP; Master rank/title; right to buy Worldtree Axe |

### Merchant —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:merchant/a01` | Count the coinage | Apprentice | Demonstrate legitimate physical coin conversion without changing total value | 1 copper-to-silver and 1 silver-to-gold conversion | BANK | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:merchant/a02` | Open the accounts | Apprentice | Deposit and withdraw a small personally funded bank amount | 50 coins deposited and withdrawn once | BANK | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:merchant/a03` | First public sale | Apprentice | Complete a genuine fixed-price sale to another player | 1 settled sale worth at least 50 coins | TRADE | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:merchant/a04` | The buyer's side | Apprentice | Purchase a useful item from another player's funded listing | 1 settled purchase worth at least 50 coins | TRADE | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:merchant/a05` | Essentials supplier | Apprentice | Supply several distinct approved Essentials resources | 3 settled resource categories | TRADE | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:merchant/a06` | Fulfil an order | Apprentice | Fill another player's legitimate funded buy order | 1 settled order fill | TRADE | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:merchant/a07` | Daily merchant service | Apprentice | Complete Merchant daily contracts in distinct periods | 3 daily periods | CONTRACT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:merchant/a08` | Apprentice trading desk | Apprentice | Demonstrate accurate accounts, mailbox delivery and truthful listing terms | 1 account and service inspection | INSPECT | Tier 3; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:merchant/j01` | Timed auction seller | Journeyman | Complete a real competitive auction with escrow and final delivery | 1 settled timed sale to another player | TRADE | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:merchant/j02` | Timed auction buyer | Journeyman | Win and receive a funded timed auction legitimately | 1 settled winning auction | TRADE | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:merchant/j03` | Trading partners | Journeyman | Complete useful sales to different community members | 4 sales to at least 3 other players | TRADE | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:merchant/j04` | Varied commerce | Journeyman | Trade distinct practical resource or equipment categories | 4 categories with completed deliveries | TRADE | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:merchant/j05` | Funded procurement | Journeyman | Create a funded buy order and receive its requested goods | 1 completely filled order | TRADE | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:merchant/j06` | Account reconciliation | Journeyman | Reconcile sales, purchases, tax burns and unclaimed deliveries | 1 inspected personal ledger | INSPECT | Tier 4; premises; active; open Journeyman band; j01,j02,j05 | 200 profession XP |
| `kingdom_smp:merchant/j07` | Sustained market service | Journeyman | Complete Merchant contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 4; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:merchant/j08` | Journeyman market handover | Journeyman | Demonstrate safe escrow, honest terms and resolved deliveries | 1 commercial assessment | INSPECT | Tier 4; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07 | 200 profession XP; Master trials open |
| `kingdom_smp:merchant/m01` | Community procurement | Master | Fulfil a real authorized multi-category community supply commission | 3 categories with 32 units each | DEL+INSPECT | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:merchant/m02` | Responsible turnover | Master | Complete genuine player trades with diverse counterparties | 2500 coins settled across at least 4 players | TRADE | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:merchant/m03` | Royal shipping supplies | Master | Supply traceable goods for an approved Aerodrome cargo commission | 1 authenticated cargo delivery contribution | DEL+JOB | Tier 6; premises; active; open Master band; AIR6 | 300 profession XP |
| `kingdom_smp:merchant/m04` | Market liquidity | Master | Complete funded buy orders covering different resources | 3 fully settled orders | TRADE | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:merchant/m05` | Professional equipment broker | Master | Broker a legitimately purchased Journeyman item while preserving its provenance | 1 settled certified Journeyman equipment sale | TRADE | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:merchant/m06` | Master merchant service | Master | Complete distinct Merchant contracts during Master trials | 5 daily periods | CONTRACT | Tier 6; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:merchant/m07` | Commercial charter project | Master | Organize an authorized community supply event with auditable transactions | 1 inspected procurement project | INSPECT | Tier 6; premises; active; open Master band; m01,m02,m03,m04,m05 | 300 profession XP |
| `kingdom_smp:merchant/m08` | Master merchant assessment | Master | Demonstrate fair commerce, taxes, escrow and accurate accounts | 1 final assessment | INSPECT | Tier 6; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07 | 300 profession XP; Master rank/title; right to buy Royal Trade Ledger |

### Engineer —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:engineer/a01` | Andesite workshop | Apprentice | Produce a basic Create alloy commission | 32 andesite alloy | MAKE+DEL | Tier 4; premises; active; open Apprentice band; C4 | 100 profession XP |
| `kingdom_smp:engineer/a02` | Shafts and cogs | Apprentice | Produce distinct mechanical transmission components | 32 shafts,16 small cogs and 8 large cogs | MAKE+DEL | Tier 4; premises; active; open Apprentice band; a01; C4 | 100 profession XP |
| `kingdom_smp:engineer/a03` | Water-powered work | Apprentice | Build and operate a safe basic waterwheel drive | 1 operating drive | INSPECT | Tier 4; premises; active; open Apprentice band; C4 | 100 profession XP |
| `kingdom_smp:engineer/a04` | Speed and direction | Apprentice | Demonstrate gear ratios, reversal and a controllable shutoff | 3 distinct mechanical functions | INSPECT | Tier 4; premises; active; open Apprentice band; a02,a03; C4 | 100 profession XP |
| `kingdom_smp:engineer/a05` | Press operator | Apprentice | Complete a personally started pressing commission | 32 pressed metal sheets | JOB+DEL | Tier 4; premises; active; open Apprentice band; C4 | 100 profession XP |
| `kingdom_smp:engineer/a06` | Milling commission | Apprentice | Produce useful milled material from traceable supplied inputs | 64 milled outputs | JOB+DEL | Tier 4; premises; active; open Apprentice band; C4 | 100 profession XP |
| `kingdom_smp:engineer/a07` | Engineering service | Apprentice | Complete Engineer daily contracts in distinct periods | 3 daily periods | CONTRACT | Tier 4; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:engineer/a08` | Apprentice mechanical shop | Apprentice | Demonstrate safe power, transmission, processing and maintenance access | 1 inspected functional shop | INSPECT | Tier 4; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07; C4 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:engineer/j01` | Brass production | Journeyman | Process a legitimate brass commission using correct native heating | 32 brass ingots | JOB+DEL | Tier 6; premises; active; open Journeyman band; C6,N5 | 200 profession XP |
| `kingdom_smp:engineer/j02` | Filtered movement | Journeyman | Move registered goods through controllable filtered transport | 3 independent filters handling 64 items each | JOB+INSPECT | Tier 6; premises; active; open Journeyman band; C6 | 200 profession XP |
| `kingdom_smp:engineer/j03` | Washing and sorting | Journeyman | Run an attributable washing and sorting production batch | 64 verified processed outputs | JOB+DEL | Tier 6; premises; active; open Journeyman band; C6 | 200 profession XP |
| `kingdom_smp:engineer/j04` | Sequenced assembly | Journeyman | Complete real multi-step precision production without fabricated outputs | 8 precision mechanisms | JOB+DEL | Tier 6; premises; active; open Journeyman band; C6 | 200 profession XP |
| `kingdom_smp:engineer/j05` | Useful mechanics | Journeyman | Build an approved contraption with safe controlled motion | 1 inspected contraption and 20 working cycles | JOB+INSPECT | Tier 6; premises; active; open Journeyman band; C6 | 200 profession XP |
| `kingdom_smp:engineer/j06` | Workshop interoperability | Journeyman | Operate a real Slice and Dice or Central Kitchen food-processing batch | 32 delivered recipe outputs | JOB+DEL | Tier 6; premises; active; open Journeyman band; CK6 | 200 profession XP |
| `kingdom_smp:engineer/j07` | Sustained engineering service | Journeyman | Complete Engineer contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 6; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:engineer/j08` | Journeyman production line | Journeyman | Demonstrate inputs, processing, output tracking and emergency stop | 1 inspected multi-step line | INSPECT | Tier 6; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07; C6 | 200 profession XP; Master trials open |
| `kingdom_smp:engineer/m01` | Steam power | Master | Commission and safely operate a native Create boiler/steam-engine system | 1 tested working system | INSPECT | Tier 8; premises; active; open Master band; C8 | 300 profession XP |
| `kingdom_smp:engineer/m02` | Precision mechanisms order | Master | Fulfil supervised precision mechanism production | 32 delivered mechanisms | JOB+DEL | Tier 8; premises; active; open Master band; C8 | 300 profession XP |
| `kingdom_smp:engineer/m03` | Parallel manufacturing | Master | Coordinate different native processing operations without losing provenance | 3 different recipes with 32 outputs each | JOB+DEL | Tier 8; premises; active; open Master band; C8 | 300 profession XP |
| `kingdom_smp:engineer/m04` | Protected logistics | Master | Deliver distinct goods between approved workshops without claim bypass | 3 routes with 32 goods each | JOB+INSPECT | Tier 8; premises; active; open Master band; C8 | 300 profession XP |
| `kingdom_smp:engineer/m05` | Aerodrome maintenance drive | Master | Provide a functional authorized mechanical support installation at the Aerodrome | 1 inspected non-service support project | INSPECT | Tier 8; premises; active; open Master band; C8,AIR6 | 300 profession XP |
| `kingdom_smp:engineer/m06` | Master engineering service | Master | Complete distinct Engineer contracts during Master trials | 5 daily periods | CONTRACT | Tier 8; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:engineer/m07` | Kingdom industrial commission | Master | Deliver an approved integrated production project with measured output | 1 inspected line and 128 commissioned outputs | JOB+INSPECT | Tier 8; premises; active; open Master band; m01,m02,m03,m04,m05; C8 | 300 profession XP |
| `kingdom_smp:engineer/m08` | Master engineer assessment | Master | Demonstrate power accounting, reliable production and safe automation | 1 final assessment | INSPECT | Tier 8; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07; C8 | 300 profession XP; Master rank/title; right to buy Royal Machinery Core |

### Mechanic —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:mechanic/a01` | Osmium workshop | Apprentice | Process personally commissioned Mekanism base material | 32 osmium ingots | MAKE+DEL | Tier 6; premises; active; open Apprentice band; K6 | 100 profession XP |
| `kingdom_smp:mechanic/a02` | Basic control | Apprentice | Produce genuine basic electronic control components | 16 basic control circuits | JOB+DEL | Tier 6; premises; active; open Apprentice band; K6 | 100 profession XP |
| `kingdom_smp:mechanic/a03` | Metallurgic infusion | Apprentice | Run an authenticated native infusion batch | 32 infused-alloy outputs | JOB+DEL | Tier 6; premises; active; open Apprentice band; K6 | 100 profession XP |
| `kingdom_smp:mechanic/a04` | Crusher service | Apprentice | Operate a real crusher on traceable supplied inputs | 64 processed outputs | JOB+DEL | Tier 6; premises; active; open Apprentice band; K6 | 100 profession XP |
| `kingdom_smp:mechanic/a05` | Safe electrical supply | Apprentice | Build a functioning generator, cable and safe load circuit | 1 inspected working circuit | INSPECT | Tier 6; premises; active; open Apprentice band; K6,KG6 | 100 profession XP |
| `kingdom_smp:mechanic/a06` | Energy buffer | Apprentice | Craft and demonstrate an ordinary accessible energy cube | 1 basic energy cube and charge/discharge cycle | JOB+INSPECT | Tier 6; premises; active; open Apprentice band; K6 | 100 profession XP |
| `kingdom_smp:mechanic/a07` | Mechanic commissions | Apprentice | Complete Mechanic daily contracts in distinct periods | 3 daily periods | CONTRACT | Tier 6; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:mechanic/a08` | Apprentice machine bay | Apprentice | Demonstrate machines, access, wiring and shutdown controls | 1 functional inspection | INSPECT | Tier 6; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07; K6 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:mechanic/j01` | Advanced control batches | Journeyman | Produce a native advanced electronics commission | 16 advanced control circuits | JOB+DEL | Tier 8; premises; active; open Journeyman band; K8 | 200 profession XP |
| `kingdom_smp:mechanic/j02` | Refined alloys | Journeyman | Process two different legitimate advanced alloy families | 16 reinforced and 16 atomic-alloy outputs | JOB+DEL | Tier 8; premises; active; open Journeyman band; K8 | 200 profession XP |
| `kingdom_smp:mechanic/j03` | Factory operation | Journeyman | Run a native factory batch with tracked inputs and outputs | 128 processed outputs | JOB+DEL | Tier 8; premises; active; open Journeyman band; K8 | 200 profession XP |
| `kingdom_smp:mechanic/j04` | Gas handling | Journeyman | Demonstrate controlled native gas production, storage and consumption | 1 inspected gas chain with 3 stages | INSPECT | Tier 8; premises; active; open Journeyman band; K8 | 200 profession XP |
| `kingdom_smp:mechanic/j05` | Ore-processing chain | Journeyman | Produce refined metal through a legitimate multi-step native chain | 64 delivered ingots from registered ore input | JOB+DEL | Tier 8; premises; active; open Journeyman band; K8 | 200 profession XP |
| `kingdom_smp:mechanic/j06` | Power maintenance | Journeyman | Diagnose and repair an authorized interrupted production circuit | 2 inspected faults resolved | INSPECT | Tier 8; premises; active; open Journeyman band; K8 | 200 profession XP |
| `kingdom_smp:mechanic/j07` | Sustained machine service | Journeyman | Complete Mechanic contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 8; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:mechanic/j08` | Journeyman machinery line | Journeyman | Demonstrate reliable factories, gas safety and energy accounting | 1 inspected operational line | INSPECT | Tier 8; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07; K8 | 200 profession XP; Master trials open |
| `kingdom_smp:mechanic/m01` | Elite control batch | Master | Produce advanced native control circuitry | 16 elite control circuits | JOB+DEL | Tier 10; premises; active; open Master band; K10 | 300 profession XP |
| `kingdom_smp:mechanic/m02` | Ultimate production order | Master | Produce a small genuine ultimate-circuit commission | 4 ultimate control circuits | JOB+DEL | Tier 10; premises; active; open Master band; m01; K10 | 300 profession XP |
| `kingdom_smp:mechanic/m03` | Advanced energy storage | Master | Commission an ordinary ultimate energy cube, never a creative cube | 1 ultimate cube and measured charge cycle | JOB+INSPECT | Tier 10; premises; active; open Master band; K10 | 300 profession XP |
| `kingdom_smp:mechanic/m04` | Efficient power network | Master | Operate a safe generator mix with metered loads and storage | 1 inspected network with 3 load categories | INSPECT | Tier 10; premises; active; open Master band; K10,KG6 | 300 profession XP |
| `kingdom_smp:mechanic/m05` | Precision machine integration | Master | Supply advanced components for an authorized aerospace or industrial job | 3 component categories with 8 units each | JOB+DEL | Tier 10; premises; active; open Master band; K10,SPACE10 | 300 profession XP |
| `kingdom_smp:mechanic/m06` | Master mechanic service | Master | Complete distinct Mechanic contracts during Master trials | 5 daily periods | CONTRACT | Tier 10; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:mechanic/m07` | Kingdom maintenance project | Master | Commission an authorized recoverable industrial power installation | 1 inspected project with shutdown and recovery demonstration | INSPECT | Tier 10; premises; active; open Master band; m01,m02,m03,m04,m05; K10 | 300 profession XP |
| `kingdom_smp:mechanic/m08` | Master mechanic assessment | Master | Demonstrate advanced machinery and safe recovery without creative machinery | 1 final assessment | INSPECT | Tier 10; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07; K10 | 300 profession XP; Master rank/title; right to buy approved Mechanic equipment/services; Creative Energy Cube still separately requires Tier 12 |

### Mage —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:mage/a01` | Novice spellcraft | Apprentice | Create and commission a legitimate Ars Nouveau novice spellbook | 1 novice spellbook | MAKE+DEL | Tier 4; premises; active; open Apprentice band; A4 | 100 profession XP |
| `kingdom_smp:mage/a02` | Source gathering | Apprentice | Operate a legitimate source-generation and storage setup | 1 inspected source chain | INSPECT | Tier 4; premises; active; open Apprentice band; A4 | 100 profession XP |
| `kingdom_smp:mage/a03` | Meaningful cantrips | Apprentice | Use novice utility magic successfully on distinct authorized targets | 16 distinct effective targets | MAGIC | Tier 4; premises; active; open Apprentice band; A4 | 100 profession XP |
| `kingdom_smp:mage/a04` | Elemental practice | Apprentice | Land legitimate novice combat effects on naturally spawned hostile mobs | 12 unique eligible enemies | MAGIC+COMBAT | Tier 4; premises; active; open Apprentice band; A4 | 100 profession XP |
| `kingdom_smp:mage/a05` | Spell inscription | Apprentice | Learn and successfully use distinct native Tier 1 Ars glyph patterns | 4 different learned glyphs with useful effects | MAGIC | Tier 4; premises; active; open Apprentice band; A4 | 100 profession XP |
| `kingdom_smp:mage/a06` | Scribe's supplies | Apprentice | Supply native Iron's Spells novice casting components | 8 legitimate novice scrolls or equivalent outputs | MAKE+DEL | Tier 4; premises; active; open Apprentice band; I4 | 100 profession XP |
| `kingdom_smp:mage/a07` | Daily magical service | Apprentice | Complete Mage daily contracts in distinct periods | 3 daily periods | CONTRACT | Tier 4; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:mage/a08` | Apprentice sanctum | Apprentice | Demonstrate safe source, casting, study and protected work areas | 1 inspected existing profession premises | INSPECT | Tier 4; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07; A4,I4 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:mage/j01` | Advanced spellbook work | Journeyman | Create an eligible native intermediate Ars spellbook | 1 intermediate spellbook | MAKE+DEL | Tier 6; premises; active; open Journeyman band; A6 | 200 profession XP |
| `kingdom_smp:mage/j02` | Composed utility | Journeyman | Demonstrate distinct useful multi-effect spells without empty-cast credit | 4 compositions with 4 successful targets each | MAGIC | Tier 6; premises; active; open Journeyman band; A6 | 200 profession XP |
| `kingdom_smp:mage/j03` | Runic fieldwork | Journeyman | Record a legitimate magical expedition in Twilight Forest and return | 1 documented expedition | EXPLORE | Tier 6; premises; active; open Journeyman band; TF6 | 200 profession XP |
| `kingdom_smp:mage/j04` | Experienced spellwright | Journeyman | Learn and successfully use distinct native Tier 2 Ars glyph patterns | 4 different learned glyphs with useful effects | MAGIC | Tier 6; premises; active; open Journeyman band; A6 | 200 profession XP |
| `kingdom_smp:mage/j05` | Protect the party | Journeyman | Provide effective healing or defensive spell assistance during real combat | 8 distinct legitimate assists | MAGIC+COMBAT | Tier 6; premises; active; open Journeyman band; A6,I6 | 200 profession XP |
| `kingdom_smp:mage/j06` | Spell-powered work | Journeyman | Commission an actual Ars Creo-compatible source/mechanical interaction | 1 functional inspected demonstration | JOB+INSPECT | Tier 6; premises; active; open Journeyman band; AC6 | 200 profession XP |
| `kingdom_smp:mage/j07` | Sustained magical service | Journeyman | Complete Mage contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 6; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:mage/j08` | Journeyman magic assessment | Journeyman | Demonstrate utility, combat, resource use and safe spell composition | 1 functional assessment | INSPECT | Tier 6; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07; A6,I6 | 200 profession XP; Master trials open |
| `kingdom_smp:mage/m01` | Archmage preparation | Master | Produce an eligible advanced native spellbook without Master equipment | 1 advanced spellbook | MAKE+DEL | Tier 8; premises; active; open Master band; A8 | 300 profession XP |
| `kingdom_smp:mage/m02` | High-order spellcraft | Master | Learn and successfully use distinct native Tier 3 Ars glyph patterns | 4 different learned glyphs with useful effects | MAGIC | Tier 8; premises; active; open Master band; A8 | 300 profession XP |
| `kingdom_smp:mage/m03` | Otherside field study | Master | Enter the unlocked Otherside, record a native observation and return | 1 genuine expedition | EXPLORE | Tier 8; premises; active; open Master band; DD8 | 300 profession XP |
| `kingdom_smp:mage/m04` | Occult cooperation | Master | Complete an authorized Occultism ritual with actual ingredients and outcome | 1 verified accessible ritual | JOB+INSPECT | Tier 8; premises; active; open Master band; O6 | 300 profession XP |
| `kingdom_smp:mage/m05` | Expedition battle magic | Master | Contribute meaningfully to an eligible Twilight Forest boss encounter | 1 qualified Naga or Lich victory | MAGIC+COMBAT | Tier 8; premises; active; open Master band; TF6 | 300 profession XP |
| `kingdom_smp:mage/m06` | Master magical service | Master | Complete distinct Mage contracts during Master trials | 5 daily periods | CONTRACT | Tier 8; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:mage/m07` | Community spellwork | Master | Deliver an approved source-powered utility installation with safe boundaries | 1 inspected working community project | INSPECT | Tier 8; premises; active; open Master band; m01,m02,m03,m04,m05; A8,AC6 | 300 profession XP |
| `kingdom_smp:mage/m08` | Master mage assessment | Master | Demonstrate advanced useful spellcraft, cooperation and protected casting | 1 final assessment | INSPECT | Tier 8; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07; A8,I8 | 300 profession XP; Master rank/title; right to buy Archmage's Staff |

### Adventurer —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:adventurer/a01` | Survey the homeland | Apprentice | Personally visit different Overworld environments without teleport credit | 4 distinct biomes | EXPLORE | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:adventurer/a02` | Safe expedition camp | Apprentice | Prepare an authorized navigable camp and return route | 1 inspected temporary expedition site | INSPECT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:adventurer/a03` | Natural hostile encounters | Apprentice | Defeat varied naturally spawned hostile mobs | 24 enemies with at least 4 from 4 species | COMBAT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:adventurer/a04` | Dungeon reconnaissance | Apprentice | Complete a real accessible Overworld dungeon exploration | 1 documented non-End dungeon | EXPLORE+INSPECT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:adventurer/a05` | Expedition supplies | Apprentice | Supply consumed useful travel provisions through a real commission | 16 cooked food,16 torches and 1 compass | MAKE+DEL | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:adventurer/a06` | First bounty | Apprentice | Complete a real Royal Bounty Hall hunt with valid attribution | 1 settled eligible hunt | HUNT | Tier 3; premises; active; open Apprentice band; BH3 | 100 profession XP |
| `kingdom_smp:adventurer/a07` | Daily expedition service | Apprentice | Complete Adventurer daily contracts in distinct periods | 3 daily periods | CONTRACT | Tier 3; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:adventurer/a08` | Apprentice expedition report | Apprentice | Present actual route, encounter, supplies and return records | 1 reviewed expedition report | INSPECT | Tier 3; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:adventurer/j01` | Aether pathfinder | Journeyman | Enter unlocked Aether, record locations and return safely | 1 genuine expedition | EXPLORE | Tier 6; premises; active; open Journeyman band; AE4 | 200 profession XP |
| `kingdom_smp:adventurer/j02` | Nether reconnaissance | Journeyman | Explore different Nether settings and return through an authorized route | 2 Nether biomes | EXPLORE | Tier 6; premises; active; open Journeyman band; N5 | 200 profession XP |
| `kingdom_smp:adventurer/j03` | Twilight survey | Journeyman | Enter unlocked Twilight Forest and document safe navigation | 1 genuine expedition | EXPLORE | Tier 6; premises; active; open Journeyman band; TF6 | 200 profession XP |
| `kingdom_smp:adventurer/j04` | Naga encounter | Journeyman | Participate meaningfully in a legitimate Twilight Forest Naga defeat | 1 qualified boss victory | COMBAT | Tier 6; premises; active; open Journeyman band; TF6 | 200 profession XP |
| `kingdom_smp:adventurer/j05` | Dungeon variety | Journeyman | Explore different accessible Overworld or Twilight dungeon structures | 2 distinct structures | EXPLORE+INSPECT | Tier 6; premises; active; open Journeyman band; TF6 | 200 profession XP |
| `kingdom_smp:adventurer/j06` | Royal air dispatch | Journeyman | Complete a real registered powered-flight dispatch, own or shared aircraft | 1 canonical Corin dispatch reward | FLIGHT | Tier 6; premises; active; open Journeyman band; AIR6 | 200 profession XP |
| `kingdom_smp:adventurer/j07` | Sustained expedition service | Journeyman | Complete Adventurer contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 6; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:adventurer/j08` | Journeyman field report | Journeyman | Present genuine multi-dimension routes, encounters and return evidence | 1 reviewed field report | INSPECT | Tier 6; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07 | 200 profession XP; Master trials open |
| `kingdom_smp:adventurer/m01` | Otherside explorer | Master | Enter the unlocked Otherside, survey native locations and return | 1 expedition with 3 observed regions | EXPLORE | Tier 10; premises; active; open Master band; DD8 | 300 profession XP |
| `kingdom_smp:adventurer/m02` | Lich expedition | Master | Participate in an eligible Twilight Forest Lich encounter after native prerequisites | 1 qualified boss victory | COMBAT | Tier 10; premises; active; open Master band; TF6 | 300 profession XP |
| `kingdom_smp:adventurer/m03` | Royal reconnaissance | Master | Complete a genuine new-region aircraft scouting report | 1 canonical shared-region report | FLIGHT | Tier 10; premises; active; open Master band; AIR6 | 300 profession XP |
| `kingdom_smp:adventurer/m04` | Lunar expedition | Master | Visit the unlocked Moon through native space progression and return | 1 recorded Moon expedition | EXPLORE | Tier 10; premises; active; open Master band; SPACE10 | 300 profession XP |
| `kingdom_smp:adventurer/m05` | Relic field guide | Master | Catalogue genuine accessible relic finds from distinct approved structures | 3 verified non-End finds from 3 structures | EXPLORE+INSPECT | Tier 10; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:adventurer/m06` | Master expedition service | Master | Complete distinct Adventurer contracts during Master trials | 5 daily periods | CONTRACT | Tier 10; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:adventurer/m07` | Community expedition | Master | Organize an authorized group expedition with supplied access and return plan | 1 inspected expedition with 3 participants | EXPLORE+INSPECT | Tier 10; premises; active; open Master band; m01,m02,m03,m04,m05 | 300 profession XP |
| `kingdom_smp:adventurer/m08` | Master adventurer assessment | Master | Present verified achievements and demonstrate safe shared exploration | 1 final assessment | INSPECT | Tier 10; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07 | 300 profession XP; Master rank/title; right to buy Relic Hunter's Compass |

### Alchemist —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:alchemist/a01` | A fueled laboratory | Apprentice | Commission legitimate brewing with working stand, water and blaze fuel | 1 inspected laboratory | INSPECT | Tier 5; premises; active; open Apprentice band; N5 | 100 profession XP |
| `kingdom_smp:alchemist/a02` | Awkward foundations | Apprentice | Brew and deliver real awkward potions with consumed ingredients | 12 potions | BREW+DEL | Tier 5; premises; active; open Apprentice band; a01; N5 | 100 profession XP |
| `kingdom_smp:alchemist/a03` | Healing commission | Apprentice | Brew and deliver legitimate healing potions | 12 potions | BREW+DEL | Tier 5; premises; active; open Apprentice band; a02; N5 | 100 profession XP |
| `kingdom_smp:alchemist/a04` | Swiftness commission | Apprentice | Brew and deliver legitimate swiftness potions | 12 potions | BREW+DEL | Tier 5; premises; active; open Apprentice band; a02; N5 | 100 profession XP |
| `kingdom_smp:alchemist/a05` | Strength commission | Apprentice | Brew and deliver legitimate strength potions | 12 potions | BREW+DEL | Tier 5; premises; active; open Apprentice band; a02; N5 | 100 profession XP |
| `kingdom_smp:alchemist/a06` | Fire-safe travellers | Apprentice | Brew and deliver legitimate fire resistance potions | 12 potions | BREW+DEL | Tier 5; premises; active; open Apprentice band; a02; N5 | 100 profession XP |
| `kingdom_smp:alchemist/a07` | Daily apothecary service | Apprentice | Complete Alchemist daily contracts in distinct periods | 3 daily periods | CONTRACT | Tier 5; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:alchemist/a08` | Apprentice apothecary | Apprentice | Demonstrate tracked ingredients, brewing, containers and safe storage | 1 inspected brewing assessment | INSPECT | Tier 5; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07; N5 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:alchemist/j01` | Extended protection | Journeyman | Brew extended-duration legitimate protective potions | 12 extended fire-resistance potions | BREW+DEL | Tier 6; premises; active; open Journeyman band; N5 | 200 profession XP |
| `kingdom_smp:alchemist/j02` | Empowered healing | Journeyman | Brew stronger native healing recipes with correct glowstone costs | 12 level-II healing potions | BREW+DEL | Tier 6; premises; active; open Journeyman band; N5 | 200 profession XP |
| `kingdom_smp:alchemist/j03` | Water expeditions | Journeyman | Brew and deliver native underwater support potions | 8 water-breathing and 8 night-vision potions | BREW+DEL | Tier 6; premises; active; open Journeyman band; N5 | 200 profession XP |
| `kingdom_smp:alchemist/j04` | Splashcraft | Journeyman | Produce legitimate splash potions while consuming actual gunpowder and brews | 12 beneficial splash potions | BREW+DEL | Tier 6; premises; active; open Journeyman band; N5 | 200 profession XP |
| `kingdom_smp:alchemist/j05` | Bee reagents | Journeyman | Supply resource combs from supervised permitted Productive Bees work | 16 resource combs from eligible pre-End species | JOB+DEL | Tier 6; premises; active; open Journeyman band; B6 | 200 profession XP |
| `kingdom_smp:alchemist/j06` | Essence reagents | Journeyman | Grow and harvest Inferium as a legitimate magical reagent commission | 32 Inferium essence from verified mature crops | GROW+DEL | Tier 6; premises; active; open Journeyman band; MA4 | 200 profession XP |
| `kingdom_smp:alchemist/j07` | Sustained alchemy service | Journeyman | Complete Alchemist contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 6; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:alchemist/j08` | Journeyman apothecary | Journeyman | Demonstrate varied brewing, ingredient traceability and container accounting | 1 inspected functioning laboratory | INSPECT | Tier 6; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07 | 200 profession XP; Master trials open |
| `kingdom_smp:alchemist/m01` | Expedition elixirs | Master | Deliver a varied legitimate beneficial potion commission | 8 healingII,8 swiftnessII and 8 fire-resistance potions | BREW+DEL | Tier 8; premises; active; open Master band; N5 | 300 profession XP |
| `kingdom_smp:alchemist/m02` | Rare reagent expedition | Master | Collect legitimate pre-End brewing reagents through real exploration | 4 ghast tears and 4 glowstone dust from recorded Nether work | EXPLORE+DEL | Tier 8; premises; active; open Master band; N5 | 300 profession XP |
| `kingdom_smp:alchemist/m03` | Spellbook reagents | Master | Source and supply native Arcane Essence for magical reagent work | 32 Arcane Essence | DEL | Tier 8; premises; active; open Master band; I8 | 300 profession XP |
| `kingdom_smp:alchemist/m04` | Occult preparation | Master | Prepare and complete an accessible authorized Occultism reagent ritual | 1 witnessed native recipe cycle | JOB+INSPECT | Tier 8; premises; active; open Master band; O6 | 300 profession XP |
| `kingdom_smp:alchemist/m05` | Beneficial field use | Master | Apply legitimate beneficial brews during genuine group combat or exploration | 8 qualified distinct ally effects | BREW+COMBAT | Tier 8; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:alchemist/m06` | Master apothecary service | Master | Complete distinct Alchemist contracts during Master trials | 5 daily periods | CONTRACT | Tier 8; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:alchemist/m07` | Kingdom expedition pharmacy | Master | Organize an inspected useful reagent and potion supply commission | 1 project covering 3 potion families | BREW+INSPECT | Tier 8; premises; active; open Master band; m01,m02,m03,m04,m05 | 300 profession XP |
| `kingdom_smp:alchemist/m08` | Master alchemist assessment | Master | Demonstrate strong legitimate brewing and nonduplicating container recovery | 1 final assessment | INSPECT | Tier 8; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07 | 300 profession XP; Master rank/title; right to buy Philosopher's Brewing Stand |

### Librarian —24 PROPOSED quests

| Stable quest ID | Name | Rank | Objective | Required quantity | Validation | Dependencies | Reward |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `kingdom_smp:librarian/a01` | Paper and bindings | Apprentice | Craft and commission personally produced books | 32 books | MAKE+DEL | Tier 4; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:librarian/a02` | Reading room | Apprentice | Create an authorized readable area with practical shelves and light | 1 inspected room with 12 bookshelves | INSPECT | Tier 4; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:librarian/a03` | Enchanting foundations | Apprentice | Commission a native enchanting setup with genuine lapis and XP costs | 1 working accessible enchanting area | INSPECT | Tier 4; premises; active; open Apprentice band; a02 | 100 profession XP |
| `kingdom_smp:librarian/a04` | First inscriptions | Apprentice | Enchant distinct useful items through actual native operations | 4 distinct commissioned items | ENCHANT+DEL | Tier 4; premises; active; open Apprentice band; a03 | 100 profession XP |
| `kingdom_smp:librarian/a05` | Compatible volumes | Apprentice | Combine compatible ordinary enchanted books with genuine inputs and XP | 4 commissioned resulting books | ENCHANT+DEL | Tier 4; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:librarian/a06` | Truthful catalogue | Apprentice | Document registered ordinary enchantment compatibility and limits | 1 inspected catalogue covering 6 enchantments | INSPECT | Tier 4; premises; active; open Apprentice band; a04,a05 | 100 profession XP |
| `kingdom_smp:librarian/a07` | Daily library service | Apprentice | Complete Librarian daily contracts in distinct periods | 3 daily periods | CONTRACT | Tier 4; premises; active; open Apprentice band | 100 profession XP |
| `kingdom_smp:librarian/a08` | Apprentice library | Apprentice | Demonstrate books, readable access, real enchanting and accurate records | 1 inspected library assessment | INSPECT | Tier 4; premises; active; open Apprentice band; a01,a02,a03,a04,a05,a06,a07 | 100 profession XP; Journeyman rank/title and eligible Journeyman purchase rights |
| `kingdom_smp:librarian/j01` | Useful enchantment collection | Journeyman | Commission legitimate books from different enchantment families | 4 books across protection,tool,weapon anddurability families | ENCHANT+DEL | Tier 6; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:librarian/j02` | Maximum ordinary craft | Journeyman | Combine ordinary books to native maximum levels without signature items | 2 legitimate maximum-native-level books | ENCHANT+DEL | Tier 6; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:librarian/j03` | Author's field guide | Journeyman | Record personally visited approved dimension observations in a guide | 1 Aether or Twilight field guide | EXPLORE+INSPECT | Tier 6; premises; active; open Journeyman band; AE4 | 200 profession XP |
| `kingdom_smp:librarian/j04` | Ars research shelf | Journeyman | Source and catalogue legitimate native Tier 2 Ars glyph items | 4 different glyph items | DEL+INSPECT | Tier 6; premises; active; open Journeyman band; A6 | 200 profession XP |
| `kingdom_smp:librarian/j05` | Spellbook indexing | Journeyman | Catalogue legitimate native RARE Iron spell scrolls | 4 different spell IDs at RARE rarity | DEL+INSPECT | Tier 6; premises; active; open Journeyman band; I6 | 200 profession XP |
| `kingdom_smp:librarian/j06` | Responsible restoration | Journeyman | Provide real compatible enchantment maintenance to other consenting players | 3 jobs across at least 2 players | ENCHANT | Tier 6; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:librarian/j07` | Sustained library service | Journeyman | Complete Librarian contracts across distinct weekly periods | 2 weekly periods | CONTRACT | Tier 6; premises; active; open Journeyman band | 200 profession XP |
| `kingdom_smp:librarian/j08` | Journeyman library | Journeyman | Demonstrate safe books, research, compatibility and service records | 1 inspected functional library | INSPECT | Tier 6; premises; active; open Journeyman band; j01,j02,j03,j04,j05,j06,j07 | 200 profession XP; Master trials open |
| `kingdom_smp:librarian/m01` | Scholar's compendium | Master | Commission varied legitimate high-level ordinary enchantment books | 6 books with at least 3 native enchantment families | ENCHANT+DEL | Tier 8; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:librarian/m02` | Advanced glyph archive | Master | Source and catalogue legitimate native Tier 3 Ars glyph items | 4 different glyph items | DEL+INSPECT | Tier 8; premises; active; open Master band; A8 | 300 profession XP |
| `kingdom_smp:librarian/m03` | Royal spell archive | Master | Catalogue legitimate native EPIC Iron spell scrolls | 4 different spell IDs at EPIC rarity | DEL+INSPECT | Tier 8; premises; active; open Master band; I8 | 300 profession XP |
| `kingdom_smp:librarian/m04` | Otherside field volume | Master | Visit unlocked Otherside and document actual native observations | 1 genuine expedition and field volume | EXPLORE+INSPECT | Tier 8; premises; active; open Master band; DD8 | 300 profession XP |
| `kingdom_smp:librarian/m05` | Conservation services | Master | Resolve legitimate compatibility or maintenance jobs without duplication | 4 distinct jobs for at least 2 players | ENCHANT | Tier 8; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:librarian/m06` | Master library service | Master | Complete distinct Librarian contracts during Master trials | 5 daily periods | CONTRACT | Tier 8; premises; active; open Master band | 300 profession XP |
| `kingdom_smp:librarian/m07` | Kingdom reference collection | Master | Create an authorized readable collection documenting accessible kingdom systems | 1 inspected collection with 6 useful subjects | INSPECT | Tier 8; premises; active; open Master band; m01,m02,m03,m04,m05 | 300 profession XP |
| `kingdom_smp:librarian/m08` | Master librarian assessment | Master | Demonstrate accurate research, legitimate enchanting and safe preservation | 1 final assessment | INSPECT | Tier 8; premises; active; open Master band; m01,m02,m03,m04,m05,m06,m07 | 300 profession XP; Master rank/title; right to buy approved Librarian catalogue only; undefined signature reward remains a separate design blocker |

## Dependency and completeness checks

Authoring validation counted **264 unique stable IDs**, exactly 24 per profession and 8 per band; checked nonempty objectives/quantities/validation/rewards; checked all same-profession explicit prerequisites exist and point backward; checked capability minimums against each band's minimum tier; checked every referenced mod identity against MOD_SNAPSHOT_2026-10-31.txt. Explicit quest prerequisite edges: 304. The conceptual prerequisite graph is acyclic. Band edges go only Apprentice→Journeyman→Master; capability edges point to already available or explicitly proposed prerequisites, never to a reward being earned.

This structural check does not certify mod recipe/API compatibility or procedural-source availability. Before enabling each approved batch: resolve native IDs/processes; audit transitive recipes and dimension/profession gates; reject cycles against the actual kingdom tier graph; retain completed IDs/history; test evidence attribution, financial/native inventory recovery, restart/save integrity and claims appropriate to that batch. Ordinary counter/UI changes get targeted tests; broad/full-pack regression belongs at integration/release milestones.

## Approval request scope and implementation order

Requested batch approval covers these 264 objective structures, quantities as configurable starting values, listed dependencies/availability assumptions and XP/rank-purchase rewards. It does not authorize changing Farmer 24, enabling proposed kingdom costs, bypassing claims, or inventing a Librarian signature ability. Any requested revisions can be applied to this single document before activation.

After approval, implement shared adapters and profession batches: (1)Builder/Lumberjack/Miner/Blacksmith using retained placement, natural harvest, crafting/consumption, contract and inspection adapters; (2)Merchant/Librarian/Alchemist using canonical economy, enchanting/brewing and safe deliveries; (3)Engineer/Mechanic/Mage/Adventurer using verified native machine/spell/dimension/combat adapters. Each batch includes actual premises/NPC services and rank/reward purchase integration, with risk-based testing. The FTB campaign reads the same authoritative definitions and personal completion records; FTB reset/team changes cannot fabricate profession rank.
