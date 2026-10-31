# Fresh-world Town Hall — 0.6.0 development checkpoint

Authority: the final production directive and the explicit follow-up to baseline `9fb65ec`. This checkpoint changes fresh worlds only; it is not the complete 1.0 release.

## Implemented behavior

A version-1 world profile pins an immutable world UUID and either ROYAL_HALL or LEGACY_LODGE. The NBT record and independent creation seal must agree; unknown schemas and lost/mismatched records fail closed. Existing kingdom data or a nonzero world clock selects the legacy profile on first introduction. No construction project, contribution, reward, NPC UUID, or existing structure is converted. Back up the entire world before upgrading; do not downgrade protocol-7 worlds or clients piecemeal.

An integrated fresh world uses its singleplayer owner. A dedicated host explicitly reviews `/kingdom admin owner <online-player>`, then adds `confirm` (permission level 4). The assignment cannot be transferred. K → Construction offers that owner the free authenticated Hut plan. Lost unused plans can be reissued; older generations become invalid. Only one starting Hut can be redeemed. The approved bootstrap exception establishes the original furnished Hut and its registered Aldric immediately; subsequent buildings use progressive construction.

First Hearth remains 1,024 logs, 2,048 cobblestone, 512 wheat and 128 iron ingots, with the existing normalized score, tie resolution, durable inventory recovery and permanent winner ledger. New-profile donations require the starting Hut. Completion advances to Tier 2 and gives the actual winning contributor a Royal Town Hall entitlement, regardless of who owns the Hut. Legacy winners retain their Lodge entitlement.

The original Town Hall is 19 × 15 with two furnished floors, a complete roof, meeting furniture, Steward workstation, storage, lighting, staircase, and milestone lecterns. The held physical plan uses the existing authenticated full-model hologram, R/Shift-R rotation, Page Up/Down height controls and Royal Gilded reviews. When looking above the terrain, the last ground site remains visible so the complete tall roof can be inspected. Server distance, complete footprint/support, collision, protected-location, entity, world-bound and reservation checks remain authoritative. Normal valid new-profile sites are authorized automatically; legacy administrative approval remains unchanged. No destructive terrain replacement or forced chunk loading is introduced.

Aldric walks to the exterior work post and progressively places the supplied blocks. The existing bounded cadence, per-step permissions, saved prefix, interruption recovery and unloaded-chunk pause remain active. Only verified completion issues the new permanent Steward entity with the saved resident UUID. Default identity is Elara Vale, Royal Steward. Before construction, the holder may choose a cosmetic display name using `/kingdom lodge name <name>`; this never changes the persistent identity. Existing Mira Ashford and Aldric identities are untouched.

The Steward and milestone lecterns open genuine kingdom overview, shared contributions/history/leaderboard, and milestone interfaces. Architect marketplace, banking and mailbox remain visibly locked and unimplemented; this building does not activate them.

## Administrator inspection and test procedure

Use `/kingdom admin inspect` to inspect the profile, owner, kingdom revision, native Hut, construction journal and installed optional mods. `/kingdom admin construction inspect <project-UUID>` reports the actual blueprint/version, owner, saved count, resident issue/death markers and loaded prefix verification. `/kingdom admin validate` runs the real milestone catalogue validator. No fake tier/reward grants or duplication-prone reset commands are added.

For a new test world, confirm its owner, claim and redeem the free Hut through K → Construction, then donate all four First Hearth categories using reviewed inventory transactions. Check Tier 2 and the winner on the leaderboard. Claim the Hall plan, aim at a large supported empty site, rotate/adjust height, use it and confirm the exact placement. In Project, review and confirm construction. Stay in loaded range and give Aldric a walkable route. Restart during partial construction and inspect the same project prefix; after completion, interact with Elara and the lecterns, then restart again and count the saved residents. On upgrade, test a copy of a previously completed Lodge world and verify its profile, project/reward/NPC identities and contribution records remain legacy.

## Verification evidence

See the checkpoint artifact manifest and validation report for executed tests, exact screenshots, restart results, hashes and remaining limitations. Native dedicated fixtures use registered test peers; they are supplementary to the actual client session and are not presented as a full-modpack acceptance run.
