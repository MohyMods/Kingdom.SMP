# Royal Gilded 0.10.0-dev — funded buy orders

Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21. Protocol 11 requires matching clients and server. Extends 0.9.0 without replacing its canonical economy journal, claim checks, world profiles, buildings or NPCs.

## Player flow

Visit the completed Tier 3 Royal Bank. Open Buy orders, hold a plain supported vanilla sample, set total quantity and price **per item**, review and confirm. The full quantity × unit price is reserved atomically; the sample is kept. Other players select an order, choose up to one stack, review exact removal/tax/proceeds and confirm. Only matching accepted items leave their main inventory. The buyer receives private item mail, the seller receives funded private coin proceeds. Claim these through Mailbox. A partially filled order remains open with exactly its remaining funding.

The buyer can review cancellation of the unfilled remainder. Only unspent coins return through private mail; delivered items, seller proceeds and previously removed tax do not change. Expiry performs the same unspent refund. Filled/closed orders cannot be redeemed again. Tax remains 5% ordinary / 10% Master calculation, rounded up per accepted fill and permanently removed from circulation. No Master equipment is created by this checkpoint.

## Technical policies and configuration

Ten active orders per player; 100 active kingdom-wide. Total quantity 1–2,304; unit price 1–1,000,000,000 coins, also constrained by wallet funds. Fills accept at most 64, the item's actual stack limit, remaining demand and available exact inventory. Only plain default vanilla noncontainer items supported by existing auction validation may be templates; named, enchanted, damaged, customized, modded or ownership-bearing templates refuse. This restriction avoids ambiguous component matching; it is not a claim of full modpack compatibility.

Default expiry 168 hours. World `serverconfig/kingdomsmp-buy-orders.json` is created lazily with `schema: 1` and `durationHours: 168`; integer duration supports 1–720 hours. Restart to reload; each existing order retains its saved deadline. This is a documented numerical default under the final directive, not a new fixed design approval. Lists sort higher unit price first, then creation time and UUID. Sellers explicitly select a matching order; no hidden automatic matching.

Order creation, funding, fills, buyer item mail, seller proceeds, tax and refunds share the canonical identity-sealed economy history. Native seller removal shares the existing prepared-operation / player receipt protocol. Reviews bind server inventory, revision and a short-lived nonce; concurrency and stale reviews refuse. IO uncertainty quarantines trading for checkpoint recovery. Existing old operations retain their identities; new operation kinds require this version or newer and must not be opened by older binaries. Journal admission remains 8 MiB with a 12 MiB hard bound and reserved recovery/close/mail capacity. The bounded maintenance worker handles at most two due auction/order operations per second without chunk tickets.

## Administration

`/kingdom admin orders inspect` reports saved orders, remaining quantities, funded coins and pending inventory work under existing administrator permission.

In an isolated copied test world only, manually create `serverconfig/kingdomsmp-sandbox.enabled`. Level 4 `/kingdom admin orders expire-test <UUID>` reviews the operation; append `confirm <reason>` to close only that order at its saved deadline. Actor/reason/deadline persist canonically. No global clock change, unrelated expiry, free currency or normal-world marker creation occurs. Remove the marker after testing. Missing-marker and ordinary-player rejection are covered by the dedicated suite. This accelerates expiry for QA; it does not demonstrate waiting seven real days.

## Verification

Full Gradle build: 242 JUnit tests, zero failures/errors. Eighteen new tests cover funding, partial/full fills, exact matching, competing sellers, conservation, tax, refunds, replay, old history, restart and native/IO recovery. The expanded normal dedicated-server auction suite passed with both pinned claim integrations. It exercises real inventories and receipt checkpoints, stale reviews, partial fills, full-inventory claim refusal, cancellation, isolated expiry permissions/audit, exact private deliveries and all 101 menu entries at the global cap. Real two-client and restart evidence is recorded in VALIDATION and the delivery manifest.

The completed/reserved construction Blueprint page also uses its full scrollable body for saved location and progress, rather than retaining disabled placement controls that hid details on small screens. Unplaced preview/claim/validation and server permissions are unchanged.

Remaining: dynamic NPC prices/shops/contracts, profession gameplay and quests, remaining approved buildings/tiers, supported mod integrations and full-pack acceptance. This is an installable development checkpoint, not production 1.0.0.
