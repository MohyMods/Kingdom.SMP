# Tested Kingdom SMP development checkpoint

Source commit: `6a61f8c22e73a9028f4ba359e417d5dbcf1c6050`
Branch: `rebuild/royal-gilded`
Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21
Date: 2026-10-30 (Asia/Riyadh)

The existing project was restored at `89c1ed9`; foundation build fixes are in `ef6ff91`. This checkpoint implements the next approved dependency: the furnished Builder's Hut and persistent Aldric. It is a development build, not a complete season release.

## Downloads

- [Tested development JAR](kingdom-smp-core-0.1.0-dev.jar)
- [Build/runtime evidence and JUnit reports](validation-evidence.zip)
- [SHA-256 checksums](SHA256SUMS.txt)
- [Aldric dialogue](aldric-dialogue.png) / [catalogue](blueprint-catalogue.png)
- [Kingdom overview](kingdom-overview.png) / [progression](kingdom-progression.png)
- [Actual hut on an isolated test platform](builders-hut.png)
- [Narrow dialogue after Page Down](aldric-dialogue-narrow.png)

JAR SHA-256: `b3b5ee33136472bcc31467fe4c14b62db52e41dfd3841c2ecbdec96e211c5eb4`

## Working result

An original **348-block, 11×9×9** timber/stone hut supports four rotations and contains a usable door, bed, empty barrels, crafting table, stonecutter, blueprint desk, furnishings and lighting. Op-only preview/confirmation revalidates the exact loaded clear site and solid support; it never clears terrain. Protection cancellation is honored. One persistent reservation supports non-destructive resume without authorizing another hut or NPC.

Aldric has a registered persistent UUID, configurable name/home and a safe day/night routine. His real Royal Gilded dialogue and catalogue use slate courses, gold/bronze controls, parchment and a rendered portrait. Every topic is validated by the server; bounded correlated replies cannot reopen a closed conversation. Actual client rendering exposed and verified the fix for a late vanilla blur pass affecting both custom screens.

## Executed verification

- Complete Gradle build: **passed; 93 JUnit tests, zero failures/errors/skips**.
- NeoForge dedicated GameTest: **one required lifecycle scenario passed**, covering actual commands, stale-site rejection, protection cancellation/rollback, same-reservation resume, 348-block verification, NPC identity/access/death, entity serialization and native SavedData disk I/O.
- Normal dedicated server: started, saved, restarted the same world, executed status/save commands and stopped through the console with a successful Gradle result. The first run required a termination signal because stdin was not forwarded; this was corrected before the clean restart run.
- Physical client: initialized under Xvfb/software OpenGL, joined an integrated world, installed/interacted with the hut/NPC, navigated all four dialogue topics, refreshed the overview and inspected progression. Screens were visually inspected at **640×400 and 427×240 GUI pixels**, including narrow scrolling.
- Full client/world restart: **exactly one** Aldric, with the exact original UUID `7d97b8f5-e1b1-4d6c-9528-fa429e448f8f`; dialogue reopened successfully.
- Development runtime tests and their structure fixture are excluded from the production JAR.

The previous HTTP 403 dependency blocker is resolved. Java initially contained only a runtime; a checksum-verified full JDK 21 was used. Screenshots are actual Minecraft captures.

## Use

Install the same JAR on the NeoForge client and server. Use Java 21 and back up the complete world.

An operator with level 2 permission prepares a loaded, completely empty 11×9×9 volume with solid ground below, stands outside the footprint within 64 blocks, then runs:

```text
/kingdom hut preview 100 65 100 north starter-camp
/kingdom hut confirm <exact returned token>
/kingdom hut status
```

Rotations are north/east/south/west; quarter turns use 9×11. Origin is the minimum foundation corner. Confirmation expires after 30 seconds. Right-click Aldric for dialogue; **K** opens the kingdom overview. `/kingdom hut name <name>` preserves identity. `/kingdom hut resume <exact hut ID> <reason>` resumes a paused reservation while refusing conflicting edits and reissuance of a committed missing NPC.

Back up the entire world, including `data/kingdomsmp_builder_hut.dat`, `.reserved`, `.issued`, and entity data together. Journal mismatches fail closed. No automatic resurrection or destructive reset is enabled.

## Remaining scope and limits

**Live contributions, leaderboard presentation, physical blueprint delivery/redemption, progressive construction, Town Hall and Elara remain unfinished.** Existing pure Tier 1 accounting is preserved; item consumption needs a proven inventory/save recovery protocol. Later costs and unresolved border, Charter, protection and destructive recovery rules were not invented.

No two-client race/soak, forced host-loss guarantee or specific external protection-mod compatibility is claimed. The headless host has no audio device. Authlib public-key requests log a nonfatal connection refusal; authenticated public multiplayer is unverified. See the repository [validation report](../../VALIDATION.md) for exact scope and historical failures.
