# Decisions, conflicts, and unresolved questions

Source S1 is the user's 2026-10-30 project brief. Confirmed decisions are recorded in [requirements](REQUIREMENTS.md); this file tracks authority and material gaps. No recommendation below is an approved decision.

## Confirmed baseline and exclusions

| ID | Status | Decision |
| --- | --- | --- |
| D-001 | Confirmed, S1 | Minecraft Java 1.21.1 / NeoForge / Java 21 / Gradle / Git; modular server authority. |
| D-002 | Confirmed, S1 | Twelve shared tiers; Nether Tier 5, End Tier 9; cube Tier 12 plus active Master Mechanic. Charter available after Tier 2, boundary unspecified. |
| D-003 | Confirmed, S1 | Twelve named professions, three ranks, 24 planned quests each; three active maximum; switching costs 15,000 with progress retained. |
| D-004 | Confirmed, S1 | Master gear purchased by corresponding active Master; repeat same-item purchases compound +50% per player/item. |
| D-005 | Confirmed, S1 | Coin/bank economy, counter-only transfers, three market modes, 5%/10% taxes, bounded NPC resource buying. |
| D-006 | Excluded, S1 | Weekly Events, Pet Shop, Player Trading, Economy Ledger. No restoration without approval. Older files proposing them would be superseded on this point. |
| D-007 | Confirmed, S1 | No gameplay implementation/build yet; review interface layouts before implementing them. |
| D-008 | Proposal in S1 | Offline automation disabled in current balance proposal; exact meaning and feasibility unapproved. |

## Contradictions and scope tensions requiring review

No prior design documents exist locally to compare. The following are tensions/ambiguities in the available brief, not proven contradictions between unseen documents.

| ID | Issue | Recommended resolution (pending approval) |
| --- | --- | --- |
| Q-01 | Player-driven markets/trading remain goals while Player Trading is excluded | Treat exclusion as removal of a custom direct player-to-player trade interface, retaining the explicitly required auction house. Confirm scope; do not build a direct trade feature meanwhile. |
| Q-02 | Economy Ledger removed, but atomic recovery and admin audit require records | Keep private technical operation/recovery records and required admin audit; no player-facing ledger UI or reporting feature. Confirm intended boundary before storage contract. |
| Q-03 | Charter available “after Tier 2” | Clarify completion of Tier 2 versus reaching Tier 3; do not assign a gate or charter functionality yet. |
| Q-04 | Cube personal entitlement versus shared machines and transfers | Specify acquisition, placement, use, ownership, and automation separately. Purchase eligibility alone is insufficient; do not assume all Master items require active rank to use. |
| Q-05 | Offline automation versus shared infrastructure/mod chunk behavior | Approve an operational definition and validate supported enforcement before promising broad coverage. |

## Missing decisions by implementation dependency

| ID | Needed information | When blocking |
| --- | --- | --- |
| Q-06 | Original approved documents, source hierarchy/version dates, removed-feature history | Full specification reconciliation; cannot claim full historical coverage |
| Q-07 | Package namespace, mod ID, license; target NeoForge/Gradle releases chosen from verified official artifacts | M1 foundation |
| Q-08 | Client mod distribution/updates, expected hosting OS/hardware, backups, permissions model | M1 validation and deployment planning |
| Q-09 | Storage/durability contract, backup consistency/restore targets, internal records boundary | M2; mandatory before real currency |
| Q-10 | Twelve milestone tables, starting tier, charter meaning, contributions, advancement permissions, resets | M3 and progression content |
| Q-11 | Rank advancement, all quest objectives/rewards/attribution, inactive credit, activation/switch charged actions | M4 and full content |
| Q-12 | Currency limits/rounding, valid coins and change, banking selection/full-inventory rules, switch funding | M5 |
| Q-13 | Master catalogue/base costs, price indexing/rounding, item identity/reset, gifting/resale/use rules | M6 |
| Q-14 | NPC price/budget/quota/daily reset tables and timezone, funding and stock semantics | M6 |
| Q-15 | Auction fees/tax timing/recipient, matching, escrow/delivery, bids/refunds/expiry, buy-order priority | M7a–c |
| Q-16 | Exact modpack manifest, magic systems, verified artifacts, global vs personal gates, machine actor/ownership rules | M8 and pack distribution |
| Q-17 | Season duration, tier pacing, economy targets, expected workload and performance acceptance targets | Balance and M10 |
| Q-18 | Review of UI direction and each screen's layouts/interactions | Before the affected UI implementation |

## Architecture recommendations awaiting review

P-01: start with one mod and logical module boundaries; avoid twelve separate profession mods.

P-02: custom client screens with consistent medieval styling, isolated client code, and server-authoritative mutations.

P-03: evaluate an embedded transactional store for financial state; prove the Minecraft inventory/save recovery boundary before enabling coins. This is not a selected dependency.

P-04: M1 foundation first after explicit authorization; then durable state and one kingdom vertical slice before expanding content.

## Decision change procedure

Add a dated entry with decision ID, explicit user approval/source, replaced decision, affected requirement IDs/docs, implementation impact, and migration implications. Preserve old context as superseded; do not silently rewrite history. New source documents must be compared, not automatically merged where they conflict. No recommendations in this checkpoint have user approval yet.
