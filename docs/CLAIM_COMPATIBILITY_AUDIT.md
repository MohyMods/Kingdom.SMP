# Claim integration evidence — 2026-10-31

The replacement snapshot contains **277 mod entries**, excluding its `Name Version (Mod Id)` heading. It reports both FTB Chunks 2101.1.22 and Open Parties and Claims 0.33.0. They must both authorize a site; permission from one cannot override a refusal from the other. The current 0.5.0-dev implementation deliberately refuses construction while either is loaded. **No permission adapter or full-pack compatibility test is complete.**

## Verified artifacts and remaining access limits

The [artifact manifest](sources/CLAIM_ARTIFACT_AUDIT_2026-10-31.json) records successful download URLs, sizes and SHA-256 digests. Third-party binaries remain outside the repository.

- FTB Maven returned HTTP 200 for `ftb-chunks-neoforge-2101.1.22.jar`. Its embedded NeoForge metadata confirms the exact version, Minecraft ≥1.21.1, NeoForge ≥21.1.0, Architectury ≥13.0.8, FTB Library ≥2101.1.34 and Teams ≥2101.1.9. Public method signatures were inspected with Java 21 `javap`; this verifies their existence, not their complete semantics. The corresponding `-sources.jar` URL returned **HTTP 403**. It was not repeatedly retried. Binary access succeeded, so this is not evidence that the whole cloud environment lacks network access.
- Modrinth version `yWmXD7GW` supplies the exact **NeoForge 1.21.1 / 0.33.0** OPAC binary and sources. Both downloaded successfully and matched the published SHA-512 values. Embedded metadata permits Minecraft 1.21–1.21.1 and NeoForge 21.0.0-beta–before21.2; Create ≥6.0.0 is optional. These ranges include the core target, but do not establish live interoperability.

## API entry points inspected

FTB's `FTBChunksAPI.API` exposes manager readiness; `ClaimedChunkManager.shouldPreventInteraction(Entity, InteractionHand, BlockPos, Protection, Entity)` and `Protection.EDIT_BLOCK` exist in this exact binary. Before using them, inspect implementation semantics, player/team context, bypass behavior and world selection. No bypass flags should be granted by Kingdom construction.

OPAC's published source exposes `OpenPACServerAPI.get(MinecraftServer).getChunkProtection()`. `IChunkProtectionAPI.onEntityPlaceBlock(BlockState, Entity, ServerLevel, BlockPos)` reports whether placement should be protected against. Full-pass APIs exist, but should not be used to evade claims. Verify real claimant, party, server/admin claims and offline construction semantics rather than equating NPC identity with the blueprint holder.

Next implementation must pass both systems for the whole footprint, supports, exterior work area and each progressive write. Changed/revoked permissions must pause safely. Missing actors, unavailable managers, changed APIs and unresolved authority must refuse safely. Test permission-allowed and denied cases, cross-chunk sites, changed claims after confirmation, offline owners, both systems together and save/restart. Existing block collision, reservations, terrain, entity and world-limit checks remain required. Do not remove the current guard until real adapter tests replace it.
