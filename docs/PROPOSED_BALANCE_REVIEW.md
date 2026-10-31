# Proposed balance review — not approved or enabled

This file supplies concrete review candidates where approved definitions are missing. It is **not** a gameplay specification, approved balance, a configured gate or implemented content. No proposed value below is loaded by the mod. Preserve fixed First Hearth requirements and all approved gates. Review against actual ten-player output and the final modpack before activation.

## Next community milestone candidate

Proposed Tier 2 → 3: complete the approved furnished Founders’ Lodge; an administrator verifies a navigable community road connecting it to the Builder’s Hut; donate 2,048 logs, 4,096 cobblestone, 768 wheat and 256 iron ingots. This doubles early structural work while adding a physical community task. The road has no automatic terrain replacement, and its exact evidence/approval rules need review. This is a candidate, not permission to consume these quantities. A new Tier 3 reward/NPC/building must be approved independently; do not invent one in code.

For Tiers 3–11, use progressively broader construction/exploration/technology tasks alongside resource targets rather than exponential logs/cobblestone alone. No Nether-only requirement may precede Tier 5 access; no End-only requirement may precede Tier 9 access. Exact tier names, quantities, rewards, gates and borders are still absent. Require a complete table and playtime targets before enabling each transition. At Tier 12, the final contribution/award milestone awards the ultimate Town Hall under its own approved policy and remains Tier 12; never introduce a thirteenth tier.

## Intermediate service premises candidates

- **Counting house**: a separately approved named banker and physical bank counter, available at an approved earlier gate. It does not require the Tier 12 Hall. Do not build or unlock it until gate/service rules are settled.
- **Market office**: a separately approved named trader, Royal Trading counter and market services at approved gates. Building completion alone grants none of them.
- **Founders’ Lodge**: the approved meeting/milestone premises with Mira Ashford, Reeve. No banking, Royal Trading or Charter grant.
- **Royal Guild Charter**: retain “after Tier 2.” Review whether this means completing Tier 2/reaching Tier 3 and define its actual functions/premises before enabling it.

## Representative Farmer design candidates

Choose Farmer first for the representative profession because its manual activity, resource credit and administrator-reviewed public building tasks exercise several quest paths without requiring an unverified third-party mod. Before implementing its 24 quests, approve a sheet with eight Apprentice, eight Journeyman and eight Master objectives. Each row needs objective identity, event/quantity, attribution, prerequisites, XP, reward, rank requirement and any administrator evidence/approval rule.

Proposed objective mix: mature-crop harvest with replace/break-loop protection; crop variety; animal husbandry with parent/action attribution; real food delivery with consumed resource recovery; and public agricultural construction with administrator evidence. Vary these across the three ranks rather than repeating a harvest counter 24 times. Do not fill the 288 planned slots with arbitrary generated objectives. A 2–3 week Master target is a playtest outcome; do not add mandatory idle timers or exaggerated repeated counters to simulate that duration.

Proposed activation semantics for review: the first three open-slot activations are free, deactivation is free, replacing one active profession costs 15,000 copper units once, inactive progress is retained but inactive professions earn no new quest/XP credit. Never charge before an atomic validated replacement. These details are unapproved; the confirmed price/cap/preservation rules remain unchanged.

## Economy decisions to approve before consumption

Proposed technical baseline: integer copper units, exact bounded arithmetic, one authoritative store and durable operation identity. Deposits/withdrawals and item delivery must pass their own native inventory/store fault tests; the donation journal is not a bank.

For review: Master price index n counts successful prior purchases only, first purchase base B, price `ceil(B × (3/2)^n)` with no failed-request increment. Approve base catalogues, rounding, reset and transfer/resale rules before enabling the already approved +50% compounding rule. Tax remains 5% regular/10% Master; rounding, charging time and recipient require a separate explicit choice. NPC prices/budgets/quotas, contract rewards/funding/reset timezone, escrow/cancellation/bid/expiry policies and bank capacity/change behavior still need values. No arbitrary currency faucet is enabled as a default.

## Integration and operational review

Provide the approved exact pack manifest or approve specific official NeoForge 1.21.1 releases before integration testing. Define each gated content/action, profession ownership/delegation and offline automation scope. Merely finding a compatible download cannot decide gameplay gates. Verify crafting, machines, loot, trades, crop/bee output, use/placement and alternate transfers with the corresponding mod installed.

Open engineering acceptance decisions: production hardware/TPS target and season pacing; permissions backend; consistent backup policy; NPC death/resurrection and destructive repair policy. Current implementation conservatively pauses/refuses uncertain recovery instead of silently duplicating entities or supplied materials.
