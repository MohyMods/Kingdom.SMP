# Decisions, conflicts, and unresolved questions

Source S1 is the user's initial 2026-10-30 project brief; S2 is the subsequent complete-rebuild authorization on the same date; S3 then defines First Hearth, Builder’s Hut, construction, and Town Hall. Confirmed decisions are recorded in [requirements](REQUIREMENTS.md); this file tracks authority and material gaps. No recommendation below is an approved decision.

## Confirmed baseline and exclusions

| ID | Status | Decision |
| --- | --- | --- |
| D-001 | Confirmed, S1 | Minecraft Java 1.21.1 / NeoForge / Java 21 / Gradle / Git; modular server authority. |
| D-002 | Confirmed, S1 | Twelve shared tiers; Nether Tier 5, End Tier 9; cube Tier 12 plus active Master Mechanic. Charter available after Tier 2, boundary unspecified. |
| D-003 | Confirmed, S1 | Twelve named professions, three ranks, 24 planned quests each; three active maximum; switching costs 15,000 with progress retained. |
| D-004 | Confirmed, S1 | Master gear purchased by corresponding active Master; repeat same-item purchases compound +50% per player/item. |
| D-005 | Confirmed, S1 | Coin/bank economy, counter-only transfers, three market modes, 5%/10% taxes, bounded NPC resource buying. |
| D-006 | Excluded, S1 | Weekly Events, Pet Shop, Player Trading, Economy Ledger. No restoration without approval. Older files proposing them would be superseded on this point. |
| D-007 | Superseded by D-009, originally S1 | Historical planning-only restriction; not current implementation authority. |
| D-008 | Proposal in S1 | Offline automation disabled in current balance proposal; exact meaning and feasibility unapproved. |
| D-009 | Confirmed, S2 | Fresh codebase authorized, with build/test, ordinary engineering decisions, and commits on a dedicated development branch; preserve existing uncommitted planning work. No main push/merge. Supersedes D-007 planning-only restriction. |
| D-010 | Confirmed, S2 | Royal Gilded visual direction and custom reusable client UI; design layout/flow first, then implement and test under independent development authorization. |
| D-011 | Confirmed, S2 | World border expansion, profession XP, dynamic prices, contracts, admin quest approval and profile/statistics are scope requirements; exact mechanics remain open. |
| D-012 | Superseded in immediate sequencing by D-016, originally S2 | Start with M1 foundation and persistent kingdom data, then M2 server-backed Royal Gilded Overview, M3 progression, M4 first approved profession. |

| D-013 | Confirmed, S3 | New kingdom starts at Tier 1 First Hearth automatically, never Tier 0; Tier 2 Roads of Commerce. Fixed ten-player first milestone quantities and consumed partial no-excess submissions. |
| D-014 | Reward building superseded by D-017/D-018; scoring retained | Equally weighted category-normalized contribution ranking; historical early Town Hall reward is replaced by Founders’ Lodge. Persist winner/reward/claim/recovery state and prevent duplicates. |
| D-015 | Town Hall timing superseded by D-017 | Admin-installed furnished Builder’s Hut/Aldric comes first; server-authoritative blueprint placement and resumable progressive construction. Elara arrives only after verified Tier 12 Town Hall completion. No unrelated service grant. |
| D-016 | Early Hall sequence superseded by D-017 | Current order: existing foundation/hut → contributions and Tier 2 → Founders’ Lodge/reward/construction → sequential approved tiers → Tier 12 Town Hall. NPC reputation/events are not new implementation permission. |

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
| Q-07 | Choose stable package/mod identity under S2 engineering authority; record exact officially verified NeoForge/Gradle releases. Licensing ownership/distribution policy still needs resolution before release. | M1 foundation; release licensing |
| Q-08 | Client mod distribution/updates, expected hosting OS/hardware, backups, permissions model | M1 validation and deployment planning |
| Q-09 | Storage/durability contract, backup consistency/restore targets, internal records boundary | M2; mandatory before real currency |
| Q-10 | S3 resolves initial tier and first consumed resource requirements; later tier tables, charter meaning, broader advancement/reset rules remain missing | Later progression content |
| Q-11 | Rank advancement, all quest objectives/rewards/attribution, inactive credit, activation/switch charged actions | M4 and full content |
| Q-12 | Currency limits/rounding, valid coins and change, banking selection/full-inventory rules, switch funding | M5 |
| Q-13 | Master catalogue/base costs, price indexing/rounding, item identity/reset, gifting/resale/use rules | M6 |
| Q-14 | NPC price/budget/quota/daily reset tables and timezone, funding and stock semantics | M6 |
| Q-15 | Auction fees/tax timing/recipient, matching, escrow/delivery, bids/refunds/expiry, buy-order priority | M7a–c |
| Q-16 | Exact modpack manifest, magic systems, verified artifacts, global vs personal gates, machine actor/ownership rules | M8 and pack distribution |
| Q-17 | Season duration, tier pacing, economy targets, expected workload and performance acceptance targets | Balance and M10 |
| Q-18 | Royal Gilded direction approved by S2; each screen still needs design and interaction review before implementation under independent development authority | Before the affected UI implementation |
| Q-19 | Border centers/sizes, dimension scope, growth timing, player safety on changes | M3 border behavior |
| Q-20 | XP sources/amounts/thresholds, quest approval roles and proof, statistics visibility | M4 and profile/admin screens |
| Q-21 | Dynamic pricing algorithm/bounds/reset and contract lifecycle/rewards/attribution | Commerce/contracts |
| Q-22 | Kingdom land boundaries and external protection adapter policy; allowed terrain replacement/site preparation | Construction placement validation |
| Q-23 | NPC death/recovery and building damage/repair/service suspension semantics | NPC/building recovery |
| Q-24 | Exact stable fallback for contribution ties and full-inventory blueprint claim recovery details; S3 already requires kingdom announcement, permanent winner record, noncraftable reward, kingdom ownership and admin abandonment/tamper recovery | Winner/reward implementation; do not alter first-reaching-score tie rule |

## Architecture recommendations awaiting review

P-01: start with one mod and logical module boundaries; avoid twelve separate profession mods.

P-02: custom client screens with isolated client code and server-authoritative mutations. Custom Royal Gilded presentation is now directed by S2 (D-010); exact screens are designed incrementally.

P-03: evaluate an embedded transactional store for financial state; prove the Minecraft inventory/save recovery boundary before enabling coins. This is not a selected dependency.

P-04: superseded by S2 then S3 milestone order (D-016). Basic persistent state and server-backed Royal Gilded presentation support the Builder’s Hut/contribution/construction/Town Hall slice before unrelated profession expansion.

## Decision change procedure

Add a dated entry with decision ID, explicit user approval/source, replaced decision, affected requirement IDs/docs, implementation impact, and migration implications. Preserve old context as superseded; do not silently rewrite history. New source documents must be compared, not automatically merged where they conflict. S2/S3 approval covers D-009–D-016 as recorded, not unresolved gameplay proposals in this register.

## 2026-10-30 — S2 authorization change

The user explicitly authorized rebuilding from scratch and rejected continuing any prior implementation. D-009 supersedes D-007; PROC-07 and current roadmap authority are updated. Royal Gilded replaces the earlier proposed visual direction. Newly named mechanics are recorded as requirements with missing numerical/behavioral specifications. Existing exclusions and confirmed costs/gates remain in force. No legacy implementation is available locally to migrate; preserve staged planning content and establish a new development branch.


## 2026-10-30 — S3 first kingdom slice

S3 supplies exact First Hearth resources, consumed partial contributions without excess, equal category-normalized scoring and first-achieved-score tie priority. D-013–D-016 replace missing starting-tier/first-cost assumptions and supersede D-012 immediate sequencing. The unique reward is noncraftable, includes materials, belongs to one highest contributor for one redemption, and yields a kingdom-owned Town Hall. Build the admin-installed hut/Aldric dependency before the hall; validate and persist construction and Elara arrival. Later tier values and land/protection/damage/death policies remain unresolved. Broad NPC capability/reputation/event discussion is extension context, not permission to implement extra gameplay or restore removed features.

## 2026-10-30 — D-017, Tier 12 Town Hall (current authority)

The latest explicit user instruction supersedes D-014/D-015 and all earlier First Hearth → Town Hall reward/construction descriptions. First Hearth completion advances sequentially to Tier 2. Each tier recognizes its highest contributor. The ultimate Town Hall and its blueprint belong at Tier 12, under a separately approved award policy. The user subsequently approved the Founders’ Lodge as Tier 1’s Tier 2 construction reward. Preserve the existing hut/NPC/UI and the exact four resource targets. Public construction, named NPC assignment and later tiers follow approved rewards. Charter availability after Tier 2 stays independent of the final hall.

Intermediate-location proposals only: Royal Storehouse for community storage with Quartermaster; Founders’ Lodge for meetings/milestone interaction with Reeve; a separate counting house for banking and a market office for Royal Trading when their respective gates are approved. No coin, bank, trading or Charter unlock is granted by these proposals. Clarification requested for the Tier 2 reward while independent contribution work proceeds. Later tier costs, border sizes, Charter boundary semantics, profession quests and economy tables remain open.

## 2026-10-30 — D-018, approved Tier 2 Founders’ Lodge

The user selected the furnished Founders’ Lodge meeting/milestone building, a named Reeve, included materials and no additional service unlocks. First Hearth’s permanent winner earns this exclusive construction privilege. This resolves the Tier 2 reward identity; public site/protection policy is being clarified separately before enabling placement. Elara and the ultimate Town Hall remain Tier 12.

## 2026-10-31 — D-019, administrator-approved Lodge placement

The complete-project directive authorizes the initial construction policy: the winning player proposes a clear supported Overworld site; an administrator reviews the exact footprint and approves before work begins. No existing player property may be destroyed. Initial implementation performs no terrain changes; a site requiring them is denied, so permission is never inferred. This resolves Q-22 for the initial safe policy while specific external protection-mod compatibility remains unverified. Preserve D-018 reward scope, D-017 Hall at Tier 12 and all existing persistence/security. Maintain MASTER_COMPLETION_TRACKER.md and CODEX_CONTINUATION.md. Independent completion of sufficiently specified systems is authorized; missing balance/content is proposed separately.
