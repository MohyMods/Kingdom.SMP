# Royal Gilded 0.8.0-dev — fixed-price auctions and native item mail

Extends the published 0.7.0 checkpoint (source a211428, delivery 6415ee5). This is a connected development slice, not the complete auction suite or 1.0.0. Network protocol is 9; update both client and server. Back up the entire world first. After new escrow operations, do not downgrade to 0.7 with the newer data; restore a matching whole-world/player backup instead.

## Working player flow

At kingdom Tier 3, visit the intact completed Royal Bank and registered Banker. **Auctions → List held item** accepts one supported vanilla stack, quantity and a total asking price. Review/confirm moves only the exact reviewed items into escrow. Another player sees the actual listing, reviews its whole-stack price and confirms purchase. One canonical journal operation debits the buyer, closes the listing, creates private buyer item mail, removes tax from circulation and creates funded seller coin mail. Neither seller logout nor a full seller purse invalidates the sale. The buyer claims exact items separately through **Mailbox**, at the actual Bank with sufficient main-inventory space. The seller claims proceeds to the purse once. Inventory operations require an empty cursor and containers closed.

The seller can cancel an unsold listing; its item returns to private mail. A 24-hour fixed-listing expiry returns unsold escrow on market maintenance/open/review. Expiry is server wall-clock based and never depends on a client timer. No listing fee is charged. Listings do not match or remove items by display name. Custom names, ordinary damage and supported vanilla components are retained through registry-aware serialization.

This slice rejects kingdom coins/plans, modded items, shulker boxes, bundles, containers, entity/block-entity/custom ownership data and charged-projectile contents. It does not claim safe arbitrary mod inventory support. One stack is limited to 64 items and its actual max stack size; serialized data is bounded to 64 KiB. Defaults: 30 active kingdom listings, asking price 1–1,000,000,000 coins, 24-hour expiry. These are documented technical defaults, not previously approved blueprint prices. The separate blueprint price range remains 500–15,000.

Tax rates remain the approved **5% ordinary / 10% Master**. Fractional tax rounds up to an integer copper unit and is included within the asking price. This initial playable slice accepts ordinary vanilla items only, so its physical listings use 5%. Pure store tests also cover the 10% calculation; this does not implement Master equipment. **Tax destination is explicitly approved in D-029: permanently remove it from circulation.** It never credits the treasury or a player. Tax totals remain in audit history; the earlier non-spendable QA reserve requires no additional debit or history rewrite.

## Durability and concurrency

Balances, listing escrow, sold status, exact item-mail ownership, funded proceeds and tax all derive from the same identity-sealed atomic NBT history. Old schema-1 operations without item payloads remain readable and retain the same store UUID/history/receipt chain. There is no independent auction database with a separate wallet commit.

Native listing/claim operations use `KingdomEconomyReceipt`: verified pre-state save → durable pending canonical operation → exact inventory change → verified post-state save/receipt → canonical commit. Startup resolves the pending operation from the actual saved player file. A matching previous receipt cancels; a matching new receipt commits; unexpected/mixed receipts fail closed. No dropped-item fallback or double mail delivery. Sales, coin conversion and item operations share the receipt chain and exclude other pending money mutations. Known pre-write capacity/input refusal can report normally; uncertain IO quarantines.

New listing admission stops at an 8 MiB uncompressed journal threshold. A 12 MiB hard serialized bound and reserved operation/byte capacity leave room to settle/return and claim existing escrow. No replay history is silently evicted. The existing 50,000-operation bound remains. A very large pre-0.8 journal above the new bound is preserved but requires explicit archive/recovery rather than being silently rewritten. The documented process-crash contract still requires matching whole-world/player backups and does not certify arbitrary partial restores or storage loss.

## Administration and claims

`/kingdom admin auctions inspect` shows the real journal/revision, active listings, seller/price/expiry, pending native receipt, tax removed and quarantine state. It requires the existing operator namespace. There is no command to grant ordinary users free auction items or duplicate escrow. Existing audited QA funding and economy inspection remain.

Construction permissions are unchanged. Public NPC dialogue may require deliberate claim configuration. Read [optional public NPC setup](PUBLIC_NPC_CLAIM_SETUP.md); the example datapack is opt-in and is not installed automatically. The actual two-client test used both pinned claim mods, an explicitly authorized FTB NPC interaction tag and OPAC's empty-hand-only resident exception. The visitor was still denied an attack and the Banker retained 20 health. Third-party capture tools/full pack remain unverified. This is not blanket claim access.

## Evidence and incomplete work

Full build and native results are recorded in [validation](VALIDATION.md). The actual client seller listed 16 host-supplied diamonds for 100 coins; the second non-op client used an explicitly audited 500-coin test wallet. The paid sale survived a real normal dedicated-server process restart before either delivery was claimed, with every kingdom authority file byte-identical. Buyer received exactly 16 diamonds and retained 400 coins; seller claimed 95, reaching 2,595; tax removed 5. Original structures/progression/entitlements and all four residents remained intact. Actual normal and 854×480 screens were inspected.

**Not implemented:** timed bidding, 5% bid increments, anti-sniping, funded buy orders/partial fills, dynamic NPC prices, profession bonuses/contracts, full 277-pack escrow compatibility, general admin repair/archival. These remain on the critical path; do not count the whole auction subsystem complete. The previous legacy-world validation remains attached to the 0.7 source; this slice does not convert profiles or costs.
