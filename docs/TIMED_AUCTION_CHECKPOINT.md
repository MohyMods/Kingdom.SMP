# Royal Gilded 0.9.0-dev — funded timed auctions

Extends source03e1674/delivery6b49174 without replacing fixed-price purchases, inventory receipt chains, world profiles, buildings or NPCs. Minecraft1.21.1/NeoForge21.1.265/Java21; protocol10 requires matching client/server.

## Implemented behavior

The Bank's Auctions screen now offers **Create timed auction** alongside immediate fixed-price listings. A physical vanilla stack enters the same native authenticated inventory checkpoint/escrow protocol. Timed duration starts at exactly24hours. The first bid may equal the starting price; subsequent bids must reach `oldBid + max(1, ceil(oldBid / 20))` (at least5%).

Bids are funded in the canonical economy operation. A new leader pays its full bid; the displaced leader receives funded private coin mail. A current leader raising its own bid pays only the difference. Seller bidding and bid withdrawal refuse. After the first bid, the seller cannot cancel. Late bids set expiry to at least two minutes after the bid; successive late bids can extend it again. Server time, balance, lead and exact reviewed amount are rechecked on confirmation. The UI shows current lead/minimum and accepts a total bid up to the existing1billion-coin input bound; records become read-only if the next minimum exceeds that bound.

A bounded server worker settles at most two due listings per second, independent of player/NPC presence and without chunk tickets. Funded timed expiry creates winner item mail, seller proceeds and permanently removed tax together, without another winner debit. Unsold/no-bid expiry returns items through mail. Fixed listings also receive automatic expiry maintenance. A prepared native inventory operation pauses maintenance; failed IO quarantines and maintenance refuses until matching checkpoint recovery/restart. Original5%ordinary/10%Master rates and D-029 tax sink remain; outbid refunds are not taxed.

All state is still one identity-sealed schema1journal with appended operation kinds. Old fixed payload-free operations remain valid; old listing constructor projection defaults to fixed. New timed native operations share the existing economy receipt chain. The8MiBlisting-admission/12MiBhard bound and reserved settlement/claim capacity remain. Funded refunds/proceeds do not require immediate room in the recipient purse. Item support remains bounded vanilla noncontainer stacks; Master physical items, modded/container escrow and buy orders are not implemented by this mode.

## Real administration and isolated testing

`/kingdom admin auctions inspect` now reports mode, starting/fixed price, funded lead, bidder and saved deadline. Existing permissions remain.

For an **isolated QA world copy**, manually create its empty `serverconfig/kingdomsmp-sandbox.enabled` marker. Level4 may review `/kingdom admin auctions settle-test <listing-UUID>`, then append `confirm <reason>`. This settles only the exact active funded timed listing at its saved deadline. It does not change real server time, other listings, progression gates, item payloads or tax rates. The actor, operation, deadline and reason persist inside the canonical settlement history. Missing marker, ordinary player, invalid/unfunded/already-settled listing or malformed reason refuses. The mod never creates the marker in normal play. Remove it after QA. This command accelerates the test deadline; it is not evidence of waiting24realhours.

## Verification and remaining scope

Full Gradle224JUnit passed. Seventeen new timed tests cover exact duration, receipts/restart, full funding/private refunds, difference-only raises, integer minimums, rolling extensions, self-buy/cancel/rollback/deadline denials, normal/Master tax, contention, IO before/after rename and full-purse funded mail. Native normal-dedicated full Hut/Hall/market/Tier3/Bank/fixed/timed loop passed with both exact claim APIs, actual inventories, three registered test peers, guarded sandbox permission/marker/reason persistence and exact settlement delivery. See VALIDATION and delivered evidence for actual graphical outcome.

Buyorders/partialfills remain the next auction dependency. Dynamic NPC shops/contracts, professions/288quests/signature gear, remaining buildings/tiers/borders, mod integrations, FTBcampaign, broad admin recovery and full277-pack acceptance remain unfinished. This is not1.0.0, no main merge, no claim of all auction or economy requirements complete.
