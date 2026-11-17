> Current space authority/evidence: D050/D051 and SPACE_RUNTIME.md now implement the approved10/11planet split, early native handover guards and launch persistence fixes. Waystones and aircraft scopes are recorded in their runtime documents. The older open-scope statements below are historical where superseded; full-pack closure remains open.

# Actual destination enforcement

The existing native NeoForge `EntityTravelToDimensionEvent` guard now covers the approved Aether4, Nether5, Twilight6, Otherside8 and End12 schedule. It queries current canonical kingdom state at each transition, including direct `ServerPlayer.changeDimension` and cross-dimension `teleportTo`, and denies transferred item entities too. There is no automatic OP bypass. Overworld return remains unrestricted; same-dimension movement is unchanged. Existing world tiers, dimensions, inventories, rewards and historical records are not converted or deleted. Already-inside players are not forcibly relocated.

Royal Gilded progression includes the three additional realm explanations; denial names the actual realm and required/current tier. The exact real administrator inspection command is documented in ADMIN_COMMAND_REFERENCE.md. No future contribution requirement is enabled by this change.

Pinned optional test profile: Aether1.5.10, Twilight Forest4.8.3345, Deeper and Darker1.4.1, Accessories1.1.0-beta.54+1.21.1 and owo0.12.15.6-beta.1+1.21, with the existing pinned FTB/OPAC subset. Official public URLs, byte counts and SHA256 are in `sources/DIMENSION_TEST_ARTIFACTS.json`. `python3 scripts/download-dimension-test-mods.py` verifies the private fixture; no third-party JAR is bundled or redistributed. Aether includes earlier Accessories internally; the explicit snapshot version wins normal dependency resolution.

Run with Java21:

```sh
JAVA_HOME=/path/to/jdk21 python3 scripts/test-claims.py --namespace kingdom_smp_travel_tests --mods work/dimension-test-mods
```

This uses two actual dedicated JVMs and embedded ordinary Survival peers. Boundary tiers are explicitly seeded only in the test source set through native saved data; it does not claim earned Tier1–12 progression or add a production tier-grant API. It checks each actual registered realm, denial one tier early through native transition/direct teleport/item transfer, permitted transition and return at the boundary, unchanged inventory and administrator-only inspection. The second JVM loads the savedTier12 baseline and repeats actual unlocked travel/return.

Known limits: no full277pack, full modded portal/vehicle/passenger/login/respawn route closure, natural earned later progression, recipe dependency closure or complete dimension questline is certified by these tests. Ad Astra needs the approved10vs11 split and an early landing adapter before canceling its rocket handover; Waystone actions and aircraft/Aerodrome remain separate work. Future mandatory milestones must still prove an End-independent acquisition path.

Executed: full Gradle build399JUnit, zero failures/errors (`work/production-dimension-ui-build.log`). Exact snapshot-profile native2JVM acceptance passes (`work/production-dimension-snapshot-native.log`), with clean saves/stops. The first successful earlier profile used Aether’s embedded Accessoriesbeta.48; final evidence uses explicit beta.54. Initial missing-owo and test-only split-package failures remain in prior logs and were corrected, not reported as passes.

A real non-OP SurvivalHallWinner client connected to a normal fresh Tier1 dedicated world with the exact dimension subset. Console-assisted cross-dimension attempts to each of the three modded realms left the player in `minecraft:overworld`; no operator bypass or world tier change was used. Actual Royal Gilded overview/tier details were inspected at1280×800 and960×640 (`work/dimension-final-aether-progress.png`, `work/dimension-final-compact-progress.png`); confirmed entry rules appear before inactive proposal details and remain readable. Client and server logs: `production-dimension-final-client.log`, `production-dimension-client-server.log`. This is graphical initialization/UI/denial evidence, not a complete natural portal/realm playthrough.
