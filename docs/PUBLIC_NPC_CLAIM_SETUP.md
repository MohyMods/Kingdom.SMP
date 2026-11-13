# Optional public NPC dialogue under the pinned claim mods

The mod does not alter your claim settings automatically. Public kingdom buildings still require deliberate public-service access when a claim owner protects entity interaction. The two-real-client 0.8 test initially demonstrated OPAC refusing an untrusted visitor. This is an expected claim policy, not a reason to grant operator rights or bypass events.

Tested versions: FTB Chunks 2101.1.22 and OPAC 0.33.0, with the same five pinned artifacts as the construction tests. Continue to back up the entire world before configuration changes.

## Explicitly opt in for public dialogue

The [example datapack](examples/public-npc-dialogue) contains only the FTB `entity_interact_whitelist` tag for the four Kingdom NPC types. Copy its directory into the world's `datapacks` and reload/restart only if you authorize public dialogue. It does not whitelist attacks, block placement/breaking or container access. FTB's whitelist has **no empty-hand check**. With FTB alone, third-party capture-item interactions are not certified safe; review the actual modpack and do not assume the tag prevents them.

When OPAC is also active, pair that tag with **empty-hand-only** exceptions. The isolated actual two-client QA used the supported `forcedEntityProtectionExceptionList` entry `"hand$kingdom_smp:resident"`, preserving its pre-existing entries and all other protection options. This authorized only empty-hand interaction with the new resident type across that isolated server. There were no attack (`break$`), chest or building-edit exceptions. Do not copy an entire test configuration over your own server.

For a production claim owner, the narrower per-claim option below was verified in the matching OPAC source/API, but **has not yet been exercised through the real client**:

1. In `config/openpartiesandclaims-server.toml`, append `"hand$Kingdom_NPCs{kingdom_smp:resident,kingdom_smp:steward,kingdom_smp:reeve,kingdom_smp:aldric}"` to `entityProtectionOptionalExceptionGroups`. Preserve existing entries.
2. Append `"claims.protection.exceptions.groups.entity.handInteract.Kingdom_NPCs"` to `playerConfigurablePlayerConfigOptions`. Restart to generate its default denied setting.
3. The actual claim owner can then run `/opac player-config set claims.protection.exceptions.groups.entity.handInteract.Kingdom_NPCs E`. `E` means everyone. A host can target the owner with `/opac player-config for <owner> set claims.protection.exceptions.groups.entity.handInteract.Kingdom_NPCs E`.
4. Where a public-building subclaim is configured, use `/opac player-config sub set <sub-id> claims.protection.exceptions.groups.entity.handInteract.Kingdom_NPCs E` for that subclaim instead. Select that subclaim only for the intended public-building chunks.

Visitors right-click the NPC with an empty used hand. Auction sellers holding an item can use `/kingdom commerce open` while physically at the Bank and select Auctions; this opens the same server-validated service and does not invoke another mod's held-item interaction on the NPC. Remote view does not authorize a trade. Full-pack capture tools, other claim versions and other providers remain unverified.

Never disable protection globally, turn visitors into operators, grant full-access exceptions, fake a claim owner or give construction permission based on this dialogue tag. Construction continues to require the actual owner's authorization under both APIs at the entire footprint and every block step.
