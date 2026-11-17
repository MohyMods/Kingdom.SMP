# Royal reconnaissance

Corin offers reconnaissance from the existing Royal Contracts screen. A real registered powered aircraft records previously unrecognized Overworld chunk regions and their actual biome IDs. The original aircraft must land near the profile's headquarters before its existing Steward accepts the report. No coordinates, travel proof or biome IDs come from the client.

Implementation defaults are six regions (16 by16blocks), centers at least256blocks from the recorded airfield,512airborne blocks,300private-Mailbox coins and a3000shared UTC-day pool. These bounded configurable defaults do not finalize long-term balance, introduce profession objectives or grant XP/unlocks. Server policy is `serverconfig/kingdomsmp-air-scout.json` schema1. Tested new-world defaults are enabled; existing explicit false policies are preserved. Explicit administrator disables are retained.

Canonical AIRCRAFT_SCOUT_REWARD commits shared region recognition, once-per-pilot/day entitlement, pinned finite daily spending and private coin mail together. Previously recognized regions cannot pay another pilot or a later day. Competing reports and stale reviews are rechecked against the canonical revision. The separate economy-bound identity-paired evidence book retains pinned terms, original registration/generation/body, server observations, travel and landing. Teleports reset unfinished proof. Native control, current ownership/shared crew, Tier6/intact Aerodrome, current Steward and aircraft claim access remain required. No permanent loading tickets, terrain changes, physical cargo consumption or aircraft replacement are added.

Acceptance in progress; not a1.0or full-modpack certification. Actual logs are preserved, including the first embedded-peer keepalive timeout, the accidental default test-mod directory contamination, and a real omitted final Steward access check discovered by native permission-revocation testing. The latter is corrected through the shared arrival validator used for review and confirmation. No player-facing enabled default preceded these tests.

Run on a stopped locked private purchased-aircraft fixture only:

```sh
JAVA_HOME="$PWD/work/toolchains/jdk-21.0.13+9" python3 scripts/test-claims.py \
  --namespace kingdom_smp_scout_tests --mods work/aircraft-test-mods \
  --seed-aircraft-restore-world work/claim-server.bqwf3p_o
```

The script refuses arbitrary user saves. `--stage-scout-preview` stops the private three-JVM sequence after the second boot's genuine unpaid arrival for ordinary graphical report checks; it does not activate any production bypass.

Executed acceptance:

- Full build469JUnit tests pass with zero failures/errors/skips: `work/production-air-scout-acceptance-build.log`. Tests cover shared overlap races, exact replay, pinned/exhausted daily pools, current crew, insufficient travel/supporting data, same-region later-day refusal, backwards clocks, archive/claim/reload, before-write and after-rename failures, identity-pair recovery, typed region data and explicit malformed/disabled policies.
- `work/production-air-scout-native-4.log`: three actual dedicated JVMs pass. Original paid native frame, real coal burn/throttle and ordinary vanilla movement packets record partial travel and at least three actual biome-tagged regions; mounted restart retains those observations. Return/landing, current Steward interaction, independently revoked FTB/OPAC at acceptance AND final confirmation, stale network replay refusal, exactly one300coin private claim and thirdboot exact six canonical regions/paid spending/purse pass. The longer embedded peer answers genuine vanilla keepalive challenges; flight policies and timeouts remain unchanged.
- `work/production-air-scout-review-native.log`: two additional JVMs pass with the new explicit-disabled tick guard, Royal detail hints and driver default isolated aircraft directory. Stops at real unpaid arrival for normal-client QA. Source `work/claim-server.2510snlb` remains private/stopped; a locked copy became `work/production-air-scout-client-server`25601/RCON25611.
- Ordinary Survival client performs normal `/kingdom commerce open`, Royal Contracts review/confirmation, then normal Mailbox claim. Purse38000→38300; shared recognized regions0→6; paid pool0→300; all three original registrations remain. Actual960×640review and claimed-mail images inspected: `air-scout-real-review-final-960.png`, `air-scout-real-mailbox-960.png`, `air-scout-real-claimed-960.png`. The first review expired while inspected and safely issued no reward; it was reviewed again normally. An initial click selected an existing bank plan rather than a coin delivery; no construction or payment changed.
- Real ordinary client AND dedicated restart preserve38300purse, six recognized regions,300paid, completed/rewarded evidence with22original observed candidate regions and868airborne blocks, all original registrations and no mayfly/flying/invulnerability/OP grants. `air-scout-real-restart-inspect.txt`, `air-scout-real-restart-stop.txt` and actual `air-scout-real-restart-purse-1280.png` retained. Restart window uses1280×800; its image shows the persisted purse on Buy Orders after a coordinate click used the old960layout. Do not label it a960image or a scouting-page rendering check. Both runs saved/stopped cleanly.

This uses the controlled Tier6/completed-building/purchased-frame fixture, seeded coal/initial takeoff and a private air-only landing pad. The route is genuinely recorded through native movement handlers, but is not a natural graphical flight, ten-player load test or full277-entry pack certification. No profession XP, later-tier cost activation, new building or binary release is claimed.

Final enabled-default source build `work/production-air-scout-enabled-build.log` passes469JUnit. Optional-aircraft-absent real legacy/FTB/OPAC regression `work/production-air-scout-core-legacy-regression.log` passes with native Hut, progressive revocation/resume/offline pause and journal reload. No repeat testing is needed without a relevant change.
