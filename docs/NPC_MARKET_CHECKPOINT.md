# Royal Gilded 0.11.0-dev — dynamic NPC resource market

Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21. Protocol 12 requires matching client and server. Extends the existing Supplies interface at completed kingdom resident counters, available at Tier 2. This does not create a new building or unlock later profession services.

## Functional trading

Supplies now offers separate **Sell:** and **Buy:** entries for the same four plain vanilla resources. Choose a quantity, review the exact total and confirm. NPC buying permanently consumes matching inventory through the existing prepared-sale/native-receipt protocol; shared budget, daily resource quota and personal allowance still bind. NPC selling reserves finite shared stock, debits the actual purse and creates a private item delivery together in the canonical economy journal. Claim the complete delivery at a real resident counter with enough inventory space; nothing is dropped.

Every unit has a marginal price. Four daily utilization bands move the NPC's buying rate downward as its resource quota is used, and its selling rate upward as stock sells. The total sums all units across band boundaries, so splitting a transaction cannot improve its payout or cost. The UI's next-unit price is informative; the reviewed total is authoritative. A stale stock/balance/day/revision or moved player refuses confirmation.

New configurable defaults under D-032:

| Resource | NPC buying floor–ceiling | NPC selling floor–ceiling | Daily selling stock |
| --- | --- | --- | --- |
| Oak log | 1–2 | 4–6 | 256 |
| Cobblestone | 1–1 | 2–3 | 512 |
| Wheat | 1–1 | 2–3 | 256 |
| Iron ingot | 4–8 | 16–24 | 32 |

These are versioned defaults for the original default supplies configuration, not newly fixed balance approvals. Existing `kingdomsmp-commerce.json` buying ceilings, quotas, shared20,000coin budget and personal2,000coin allowance remain configurable and authoritative. New `serverconfig/kingdomsmp-market.json` inside the world contains buying floors, selling floors/ceilings and daily stock. It is generated lazily from the actual original catalogue; restart to reload. A valid selling floor must exceed the maximum NPC buying price, prohibiting profitable NPC roundtrips. Unknown resources/options, fractional/out-of-range values and unsafe spreads refuse. Cobblestone/wheat buying prices remain1 because there is no smaller positive integer coin unit.

UTC midnight resets the derived daily views; it does not erase history or issue stock operations on every tick. Restart within the same day does not refill stock. Purchase coins leave circulation and do not refill the NPC buying budget. NPC stock purchases do not receive auction tax; ordinary5%/Master10% auction taxes remain permanently removed and unchanged. Bonuses, profession contracts and reward pools remain separate unfinished systems.

## Persistence and security

The appended `SHOP_BUY` operation pins the actual item, amount, quantity, UTC day and stock/price policy. Its debit, stock counter and recoverable private delivery are one atomic journal write. Replay cannot purchase twice or reinterpret earlier history. Stock is a replayed projection of the existing identity-sealed journal; no new wallet or escrow store exists. Existing operation kinds and native receipt chains remain unchanged. Payment alone does not advance the player's native item receipt; the eventual `CLAIM_ITEM` does. The existing soft/hard journal limits and reserved future-claim capacity apply.

Only an owned, unclaimed delivery whose originating canonical operation is `SHOP_BUY` may use Tier 2 item claims at a real resident counter. Matching display names, item components or client payloads grant no early access. Auction/order/return item mail retains the actual Tier 3 Bank requirement. Counter distance, sight, building integrity, survival/adventure and container checks remain.

`/kingdom admin market inspect` uses the existing administrator permission and reads actual daily counters, bounds, stock, budget and cumulative NPC purchase coin sink. There is no player-facing Economy Ledger or direct trade UI. No new administrator restock/repair command is claimed; automatic UTC restocking is implemented.

## Verification and limits

Full automated suite:273tests, including13marginal pricing and18shop persistence/recovery tests. These cover batch/split equivalence, funded-prefix bounds, last-stock contention, idempotent replay, independent days/resources, native claim receipts and before/after-rename interruptions. Dedicated and real-client evidence is in VALIDATION and the published manifest. The new native fixture also proves Tier2 shop claims and rejection of an actual auction delivery with an NPC-looking title at the same counter.

This is the four-resource NPC market slice, not a complete profession economy or balanced release. Contracts/bonuses, other approved buildings, profession gameplay/quests/Master rewards, later tiers and full modpack acceptance remain unfinished. Back up matching world/player data before upgrading; older binaries cannot read new shop operations safely.

One native stress run paused a fully placed Farmer house after14of15crop blocks disappeared before handover. Strict prefix validation retained progress and withheld the resident; no materials or crops were regenerated. A fresh rerun completed. The exact environmental cause/repair policy remains unresolved; this is a release limitation rather than a verified fix. The stair geometry fixture was made deterministic by starting the resident grounded at its verified home, and stalled purchase-construction tests now report the actual activity promptly. No production construction validation was weakened. Future profession bonuses must validate their effective buying ceiling against shop selling floors before enabling perks. Legacy-profile code/identities are unchanged; this slice does not claim a new copied-legacy campaign playthrough.
