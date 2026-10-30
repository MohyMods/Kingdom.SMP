# Professions

Confirmed roster and planned quest counts:

| Profession | Apprentice | Journeyman | Master | Total |
| --- | --- | --- | --- | --- |
| Farmer | 8 | 8 | 8 | 24 |
| Blacksmith | 8 | 8 | 8 | 24 |
| Miner | 8 | 8 | 8 | 24 |
| Builder | 8 | 8 | 8 | 24 |
| Lumberjack | 8 | 8 | 8 | 24 |
| Merchant | 8 | 8 | 8 | 24 |
| Engineer | 8 | 8 | 8 | 24 |
| Mechanic | 8 | 8 | 8 | 24 |
| Mage | 8 | 8 | 8 | 24 |
| Adventurer | 8 | 8 | 8 | 24 |
| Alchemist | 8 | 8 | 8 | 24 |
| Librarian | 8 | 8 | 8 | 24 |
| **Total** | **96** | **96** | **96** | **288** |

Players may have at most three active professions. Switching costs 15,000 coins and preserves inactive progress. Rank names alone do not define rank advancement conditions. No quest objectives, item rewards, XP model, starting rank rules, or Master catalogues are supplied.

## Proposed implementation boundaries

Keep earned progress separate from the active-profession set. Represent profession, quest, and Master item identity with stable IDs. Server validates quest events, eligibility, rank transitions, activation limits, and purchase conditions. Snapshot/diff presentation data must not grant authority to the client.

An activation replacement should validate eligibility, charge the approved funding source, and update the active set atomically. Do not first deactivate then attempt payment. Confirm price and replacement profession before committing. Whether initial activation, deactivation, adding a second/third profession, or replacing several professions incurs a fee is an open decision.

Master purchases require the buyer's corresponding Master profession to be active at commit time. For a proposed interpretation where the first purchase costs base B and n prior successful purchases exist, exact pre-rounding price is `B × (3/2)^n`. This indexing and integer rounding require approval. Avoid floating-point money. Failed/retried purchases must not increment n; item variants and stable catalogue identity need explicit rules.

## Unresolved balance and exploit risks

- Does inactive progress merely persist, or may inactive quests continue progressing? Recommend explicit active-only credit, subject to approval.
- What advancement requirements connect eight quests to each rank? Are all mandatory? What happens after a data revision?
- Can players lend, sell, drop, or use Master equipment while inactive? The confirmed purchase rule does not establish a universal usage ban.
- Can a friend buy equipment cheaply for another player? Per-player escalating prices alone allow intermediary buyers and alternate accounts. Recommend reviewing transfer policy before locking prices.
- How are crafting/kill/donation quests credited to avoid shared-event duplication, place/break loops, repeated crafted-item recycling, or attribution by automated machines?

Future acceptance: all twelve definitions load with correct counts; cap enforced concurrently; inactive progress survives switches/restarts; duplicate quest rewards rejected; Master status rechecked at commit; purchase counters and charges recover together after interruption.
