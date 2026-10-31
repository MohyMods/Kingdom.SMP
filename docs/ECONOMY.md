> **0.7.0 current checkpoint:** [Commerce and Tier3 Bank](COMMERCE_CHECKPOINT.md) implements the new approved Tier2 paid premises, resource income, recoverable plan mailbox, fresh-profile Tier2→3 and physical banking. [Validation](VALIDATION.md) / [current matrix](PRODUCTION_COMPLETION_MATRIX.md) qualify tested scope; historical missing-status text below is superseded only for these implemented slices. Full1.0 remains incomplete.

# Economy

> **D-023 — current implementation authority:** The [master directive](sources/MASTER_IMPLEMENTATION_DIRECTIVE_2026-10-31.md) authorizes incremental coding and supersedes the D-021/D-022 review-only hold. New-world Town Hall / existing-world Lodge profiles, owner Hut bootstrap, blueprint-market acquisition, automatic server-valid placement, supports up to four blocks, Tier 3 Bank (no new-world Storehouse), updated profession/economy/quest/admin scope apply. End remains Tier 12. The [full replacement mod snapshot](sources/MOD_SNAPSHOT_2026-10-31.txt) now reports FTB Quests installed. See [supersession and actual status](MASTER_SUPERSESSION_MATRIX.md); old descriptions below are historical where they conflict.

> **Current authority — 2026-10-30 progression correction:** Town Hall and its blueprint are Tier 12 only. Tier 1 completion advances to Tier 2 and recognizes the highest contributor; its approved building reward is the Founders’ Lodge. No early Town Hall, Elara, Bank or Royal Trading dependency may be implemented from historical descriptions below. Earlier bank/trading locations and exact gates require separate approval; the Charter remains available after Tier 2 independently of the Town Hall. The existing hut, Aldric and Royal Gilded design are approved.

## Confirmed baseline

| Denomination | Kingdom coin value |
| --- | --- |
| Copper | 1 |
| Silver | 10 |
| Gold | 100 |
| Platinum | 1,000 |

Physical coins coexist with virtual balances. Deposits and withdrawals require visiting an approved physical bank counter. An earlier counting house and market office are proposals; their premises and gates need approval independently of the Tier 12 Town Hall. The auction house includes fixed-price listings, timed auctions, and buy orders. Normal auction sales tax is 5%; Master equipment tax is 10%. NPC resource purchases have shared budgets, resource quotas, and per-player daily limits. Master equipment purchases require the corresponding active Master profession; repeat price increases are described in [professions](PROFESSIONS.md).

Excluded: custom Player Trading and Economy Ledger features. Player-driven markets and NPC commerce remain required. Whether Player Trading means direct trade UI or a broader ban needs review. A player-facing economy ledger must not be reintroduced by naming internal recovery records a feature.

## Proposed money and transaction rules (not yet approved)

Use integer copper units for all balances, bounded with overflow-safe arithmetic. Reject negative values, malformed quantities, and arithmetic overflow. Rounding policy, tax recipient, and whether tax is charged within the listing price remain open. Do not round independently on client and server. Coins must be identified by registered item identity and approved data, not display name or lore.

Use server-generated operation IDs, idempotent retries, bounded transactions, and one authoritative transaction coordinator. At commit, recheck quote validity, balance, eligibility, current item identity, capacity, location, and counter access. Preview quotes cannot authorize stale purchases.

Bank verification must use the server's actual player position and counter identity, including on every mutation after opening a screen. Decide valid range, line of sight, counter placement permissions, and behavior if the counter breaks. Do not invent these values.

A database commit does **not** atomically commit Minecraft inventory/player/world saves. Before enabling coin conversion, define a durable staged transfer and recovery protocol with delivery identifiers, reconciliation, and tested save-order behavior. An inbox is useful for interrupted delivery but is not by itself an exactly-once guarantee. Fail closed when durable writes fail; do not acknowledge successful delivery before the approved persistence guarantee holds. No claim of duplication safety is justified until crash testing proves the protocol under the documented crash model.

## Market design dependencies

Propose server-held escrow for listed items and funded buy orders so leaving, dying, or spending elsewhere cannot invalidate a sale. Unclaimed proceeds and items need durable delivery and capacity rules. Timed auctions need authoritative deadlines and an offline/restart policy, not a client countdown as authority. Recheck price, remaining quantity, funding, and eligibility on commit. Two buyers of the final item must yield exactly one sale.

Open decisions: bid increments, reservation/refunds, outbid handling, anti-sniping, cancellation, expiry, fees/tax timing, partial fills, order priority, cross-profession Master sales, matching rules for item components/damage/containers, listing limits, and storage bounds. Recommend restricting complex container items initially until component-aware escrow is verified.

## Dynamic prices and profession contracts (S2)

Both systems are confirmed scope, not fully specified mechanics. Do not select a dynamic-price formula, elasticity, floor/ceiling, update interval, stock model, or reset policy silently. Profession contracts need eligibility, objectives, resource consumption, funding, reward amounts, attribution, assignment limits, expiry/cancellation and duplicate-claim policy before implementation. They are distinct from the removed Weekly Events feature.

## NPC inflation controls

Need exact resource prices, buy/sell direction, budget funding/refill, quota scope/reset, per-player limits, day/timezone definition, and whether stock is persistent. Evaluate marginal cost under automated production; static high buy prices can become an unlimited currency faucet even with loose individual limits. Reset all related limits consistently; reject double spending at reset boundaries. Admin changes must be permission-checked and audited.

## Required future tests

Money bounds and rounding; compounded price growth; duplicate requests; concurrent purchases/bids; insufficient balance/inventory; full inventory during withdrawal; counter removed or player moves; disconnect during transfer; restart and forced interruption at every durable transaction stage; escrow expiry/refunds; quota/budget races; failed writes and recovery replay. These tests have not run.
