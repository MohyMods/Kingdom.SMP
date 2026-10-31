# Claim integration evidence — 2026-10-31

Current source **0.5.1-dev** replaces the 0.5.0 presence-only refusal with tested optional adapters for **FTB Chunks 2101.1.22** and **Open Parties and Claims 0.33.0**. Both must permit a site; one cannot override the other. No bypass/full-pass flags, fake-player privileges or cached permission decisions are granted.

## Actual authority and safety

Preview/confirmation check the entire footprint volume, ground support and exterior work post against both real APIs. Progressive construction rechecks the whole site before every step; Hut installation also rechecks each block. Existing collisions, support, entities, reservations, loaded chunks, world limits and place-event cancellation remain enforced. Neither API is loaded or called when its mod is absent. Exact tested versions are required; missing/changed APIs, unready managers and unresolved actors refuse safely.

With either claim mod present, **the authoritative owner must be alive, non-spectator and online in the building dimension** so permission checks and place events use that actual server player. Going offline or to another dimension pauses safely without redeeming a second reward or discarding progress. This is the current adapter limitation, not a finalized general offline-automation balance policy. No changes to claim ownership, force-loading, team settings or bypass modes are made by the production adapter.

FTB uses public `FTBChunksAPI.api()`, manager readiness and `ClaimedChunkManager.shouldPreventInteraction(..., Protection.EDIT_BLOCK, ...)`. OPAC uses `OpenPACServerAPI.get(server).getChunkProtection().onEntityPlaceBlock(entity, level, pos)`. Null state ignores item-specific placement exemptions for the whole public site. Optional reflection binds those exact public interface signatures, keeping missing third-party classes off the core class path. Native place events under claims use the actual authorizing owner; Aldric remains the physical worker. The ordinary no-claim event actor remains unchanged.

## Exact artifacts and download diagnosis

[Five-artifact test lock](sources/CLAIM_TEST_ARTIFACTS.json) pins direct public URLs, sizes and SHA-256: Chunks2101.1.22, Library2101.1.39, Teams2101.1.11, Architectury13.0.11 and OPAC0.33.0. OPAC binary/sources additionally matched published Modrinth SHA-512. Third-party binaries are not committed or bundled in Kingdom's JAR.

FTB requests with Python's default agent returned **HTTP403 / Cloudflare1010**. Matching the identified `KingdomSMP-compatibility-audit/0.5` header used for the successful Chunks download resolved Library, Teams and Chunks sources downloads (HTTP200). This is a diagnosed request difference, not an inactive environment or blanket network failure. No repeated blind retries or credential changes were needed. The earlier 0.5 audit manifest remains historical evidence.

## Reproduce executed tests

```sh
python3 scripts/download-claim-test-mods.py
JAVA_HOME=/path/to/jdk21 python3 scripts/test-claims.py
```

The runner creates a new isolated void normal dedicated world and ephemeral port, enables only the claim test namespace, runs assertions, flushes saves and stops. It never reuses production worlds. **Standalone Minecraft GameTestServer fails during OPAC initialization because that harness has a null profile cache.** The normal dedicated fixture supplies the real service rather than patching OPAC or the production mod.

Actual lifecycle passed: unclaimed and owned cross-chunk volumes; each mod independently refusing an unrelated non-operator; OPAC refusal despite FTB ownership; native protected Hut installation/NPC; real progressive placements through both event listeners; changed work-area/footprint claims pausing before a write; restoration resuming; owner logout pausing; exact durable construction prefix reload. Registered ServerPlayer test peers use embedded channels with explicitly declared actual payload IDs; this does **not test a real client handshake**. No such fixture code is packaged in the playable JAR.

A separate actual graphical client loaded the five exact mods, retained the existing copied legacy world and displayed the full green hologram after ordinary FTB/OPAC claim commands. Exact Royal Gilded review and full process restart retained the claims/unused right. The subsequently redeemed project saved at251 and resumed beyond361 after another full process restart with both systems loaded. Final handover is recorded with the delivery. Existing 142 unit tests and the separate core-only native lifecycle also pass.

## Remaining compatibility limits

The full replacement list has **277 entries**, not all installed here. FTB Quests2101.1.37 is reported but its integration is still absent. This subset uses OPAC's default party/permission providers; Argonauts/Prometheus provider behavior, other full-pack configs, authenticated remote play, a two-real-client claim race, maximum building size and season-scale performance remain unverified. No claim adapter result proves Ad Astra recipes, all alternate teleport routes or the rest of the roadmap. New-world Hall profiles/owner Hut/bootstrap/foundations/marketplace are still separate unfinished dependencies.

Final 0.5.1 physical verification: with both claim mods loaded, project74bcbf25-8158-49a1-86ff-7b1a14d1dac1 at18,201,0 North redeemed once through the real Royal Gilded flow, saved at251blocks, survived a full process restart, resumed beyond361 and completed782/782 at10:55:11UTC, introducing Mira. Production cadence was unchanged. Actual selectors counted exactly one Aldric and one Mira; completed exterior/interior and clean save/stop are in the delivery evidence. Spot `/tick query` during work reported target20TPS / average2.0ms; this is not a controlled or season-scale benchmark.

Completed-world full client restart with both claim mods retained the complete structure and actual selectors again counted one Aldric and one Mira. Current NPC work position read30.5,202,8.5, matching the legacy project definition. No new blueprint or resident was issued on that reload.

After the completed-world restart, actual Survival right-click interaction with Mira opened her Royal Gilded resident dialogue and782/782 project state with both claim mods present. Completed-interior screenshots were inspected from a clear aisle. Mira’s actual Community milestone button loaded saved Tier2/revision6, winner/score and76remaining logs with both claim mods present.

Final handover client saved/stopped with BUILD SUCCESSFUL; completed claims/world/NPC service state is preserved. Delivery at docs/checkpoints/c0889d3 includes exact JAR, actual inspected screenshots, logs/JUnit XML, checksums and continuation. No main merge.
