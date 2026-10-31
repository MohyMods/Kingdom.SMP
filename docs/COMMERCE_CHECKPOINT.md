# Royal Gilded 0.7.0-dev — commerce and Tier 3 Bank

Continues source25aa1dc/delivery1b92169. Authority: [full implementation directive](sources/END_TO_END_IMPLEMENTATION_DIRECTIVE_2026-10-31.md), D-026/D-027 and the explicit Tier2 Farmer/Builder/Lumberjack approval. This is a connected development build, **not1.0.0**.

## Actual implementation

- Royal Gilded Architect/Essentials/Mailbox/Bank/Progress screen with bounded server requests, correlated replies, exact reviews, uncertainty refresh, private mail and responsive layouts.
- One canonical identity-sealed economy journal: balances, exclusive first-buyer stock, purchase/founder records and mailbox authority pointers commit together. Insufficient funds, second buyer and replay cannot charge again. Pending native inventory operations exclude other money mutations; uncertain writes quarantine. Bounded history never silently drops replay protection.
- Reviewed early resource sales consume actual plain inventory items using pre/post native player checkpoints and independent receipts, with UTC shared budgets, resource quotas and personal allowance. The purse can be earned/spent before the Bank.
- Purchased plans use the existing authenticated generation/entitlement/preview engine. Lost unused items are recovered through Mailbox→Manage plan→Construction. Materials are included; one active Aldric project at a time. Project/NPC records remain independent and permanent. Original Hut/Hall/Lodge/Mira remain intact.
- Four original purchase definitions: Farmer15×11×11/600blocks; Builder13×11×11/629; Lumberjack13×11×11/635; Bank15×13×13/921. First three release atTier2 (new explicit approval), Bank at3. Rowan Fenwick, Cedric Ashlar, Brenna Oakfall and Oswin Fairmark are original defaults. Profession ranks, quests, perks and Master shops are **not implemented** by adding their premises. Farmer renewable beds/crops can be harvested without regeneration or falsely disabling the house.
- Fresh-profile Tier2→3: a separate pinned-cost journal, eight-resource partial contributions, exact normalized standings, native receipt recovery and durable operator road approval. Actual completion advances the kingdom and publicly recognizes its winner, without granting another building entitlement. No Tier1 score/history rewrite and no later-cost activation.
- Physical copper/silver/gold/platinum items worth1/10/100/1000. At the actual completed Bank/registered Banker, reviewed withdrawal/deposit stages a signed wallet delta, saves and verifies native inventory/receipt, then commits payment. Nearby-player virtual transfers atomically update both balances. Tier, position, NPC, building, inventory/cursor and capacity are rechecked. No banking at a remote Hall screen.

## Configuration and administration

New numerical defaults are documented in D-027 and world `serverconfig/kingdomsmp-commerce.json` / `kingdomsmp-tier-two.json`. Active tier costs are pinned; do not edit them to reinterpret existing credits. Budget/price changes preserve already committed purchase history. Current live requirements use validated vanilla IDs/tags; future draft modded requirements remain inactive.

`/kingdom commerce balance` remotely reads your own purse; `/kingdom commerce open` reads the exchange, but trade confirmations still require a real service counter. Operators: `/kingdom admin economy inspect <online-player>`; level4 `/kingdom admin economy set <player> <coins>` reviews, append `confirm <reason>` to make an audited adjustment. `/kingdom admin milestone inspect` reads actual targets; level4 `/kingdom admin milestone road` reviews, append `confirm <reason>` only after inspecting the actual Hut/Hall connection. Existing real construction/profile/catalogue inspection remains. No duplicate-entitlement reset or fake tier grant is added.

## Persistence contract

The atomic-NBT contract covers documented process interruption with matching whole-world/player backups. It does not certify host/storage-loss durability on every filesystem or inconsistent partial restores. A native receipt from another store or unexpected sequence fails closed. Native sales, bank conversions and Tier2 contributions have independent player receipt chains; their checkpoints preserve all existing persistent data. Native inventory cursors must be empty. Physical coin exchanges cannot be treated as safe merely because a database or mailbox entry exists.

Mail in this milestone supports once-claimed virtual coins and permanent purchased-blueprint authority pointers. General arbitrary-item auction escrow/delivery is not implemented. Blueprint recovery changes the generation and never creates a second construction reward.

## Evidence and limits

Read [validation](VALIDATION.md) and the delivered checkpoint manifest for actual results and Minecraft captures. Full Gradle186JUnit, connected native commerce/Bank loop and legacy lifecycle were executed. Actual graphical tests exercised earning, purchase/soldout/mail, Farmer construction, Tier3 donations/road, Bank construction and physical conversion. Bank testing used an explicitly audited funded QA wallet after demonstrating real resource income. Farmer ran at normal cadence; Bank partially used disclosed vanilla tick acceleration after a normal-rate segment/restart. This is not survival-balance certification.

Remaining release work: auctions/buyorders/itemescrow; dynamic shops/contracts/profession bonuses; full twelve professions/288quests/signature gear; the remaining public buildings; tiers4–12/world borders; safe four-block foundations; travel/technology/recipe integrations; full FTBcampaign; comprehensive admin recovery/sandbox/titles. New Tier2 costs remain inactive on legacy Lodge profiles. Currency icons currently use vanilla source textures. Bank quantity/transfer operations are bounded to2304 units per reviewed batch. The economy journal currently failsclosed at50000 accepted operations and needs a future safe archival/migration strategy for long-term production.

The complete277-entry snapshot includes nested modules and is not a reproducible pack archive/config/hash manifest. [Verified artifact/API audit](VERIFIED_INTEGRATION_ARTIFACTS.md) proves exact artifacts and some identifiers exist, not runtime integration or complete recipe closure. Full-pack and two-real-client acceptance remain unperformed. Do not label this build1.0.0.
