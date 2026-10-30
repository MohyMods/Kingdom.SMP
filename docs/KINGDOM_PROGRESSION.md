# Kingdom progression

Confirmed: twelve shared kingdom tiers with increasingly difficult community milestones. No milestone objectives, quantities, starting tier, rewards, or advancement rules have been supplied.

| Tier | Confirmed special gate | Milestones |
| --- | --- | --- |
| 1 | Not specified | Missing |
| 2 | Charter available **after Tier 2**; boundary unresolved | Missing |
| 3 | Not specified | Missing |
| 4 | Not specified | Missing |
| 5 | Nether access unlocks | Missing |
| 6 | Not specified | Missing |
| 7 | Not specified | Missing |
| 8 | Not specified | Missing |
| 9 | End access unlocks | Missing |
| 10 | Not specified | Missing |
| 11 | Not specified | Missing |
| 12 | Creative Energy Cube also requires active Master Mechanic | Missing |

## Proposed model and acceptance

Use one authoritative kingdom progression aggregate with stable milestone IDs and schema versions. Reaching an advancement boundary must happen once even when two players contribute concurrently. Record contributors only where approved rules require it. Never grant progression from client-submitted totals.

Before implementation decide: milestone submission versus automatic observation, whether resources are consumed, who can advance the tier, whether advancement is automatic, charter function, downgrade/reset behavior, and prerequisite dependencies. Use data validation to reject circular or missing references and invalid quantities.

Test duplicate submissions, simultaneous final contributions, restart/reload persistence, inventory interruption during donations, admin overrides, and changing milestone definitions after progress exists. Travel gates must cover portals, commands, teleports, respawn, login location, and third-party routes. A denied travel action needs a safe fallback that cannot strand or repeatedly damage a player; exact implementation is unverified.

Tier 12 is necessary, not sufficient, for the cube. Cube acquisition, placement, use, transfer, automation, and existing world instances need an explicit policy; purchase-only checks do not protect the gate.
