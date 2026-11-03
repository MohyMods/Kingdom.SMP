#!/usr/bin/env python3
"""Run actual pinned claim APIs on a fresh normal dedicated server, with embedded test peers."""
import json
import argparse
from pathlib import Path
import subprocess
import tempfile

parser=argparse.ArgumentParser()
parser.add_argument('--seed-aircraft-restore-world',type=Path,help='Copy a stopped verified private claim-server fixture for saved-aircraft restoration tests only')
parser.add_argument('--stage-cargo-preview',action='store_true',help='Stop the verified private cargo voyage before NPC unloading for ordinary-client QA')
parser.add_argument('--stage-flight-preview',action='store_true',help='Stop the verified private flight route before the Steward report for ordinary client QA')
parser.add_argument('--namespace',choices=['kingdom_smp_claim_tests','kingdom_smp_townhall_tests','kingdom_smp_commerce_tests','kingdom_smp_bank_tests','kingdom_smp_auction_tests','kingdom_smp_market_tests','kingdom_smp_crop_tests','kingdom_smp_profession_tests', 'kingdom_smp_farmer_tests','kingdom_smp_farmer_events_tests','kingdom_smp_bounty_tests','kingdom_smp_farmer_gear_tests','kingdom_smp_gear_shop_tests','kingdom_smp_farmer_loop_tests','kingdom_smp_hunt_tests','kingdom_smp_foundation_tests','kingdom_smp_foundation_resume_tests','kingdom_smp_hut_foundation_tests','kingdom_smp_wharf_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_tests','kingdom_smp_waystone_tests','kingdom_smp_quest_tests','kingdom_smp_space_tests','kingdom_smp_travel_tests','kingdom_smp_archive_tests'],default='kingdom_smp_claim_tests')
parser.add_argument('--mods', default='work/claim-test-mods')
parser.add_argument('--verify-wharf-restart', action='store_true')
parser.add_argument('--stage-aircraft-recovery-preview', action='store_true', help='Stop the private verified aircraft fixture with a consumed pending plan outside client chunk range for actual recovery UI checks')
parser.add_argument('--stage-space-preview', action='store_true', help='Stop the private space fixture after its verified Tier9 preparation for actual graphical checks')
args=parser.parse_args()
marker={'kingdom_smp_aircraft_fuel_tests':'AIRCRAFT_FUEL_PREPARED:','kingdom_smp_cargo_journey_tests':'CARGO_JOURNEY_PARTIAL_PREPARED:','kingdom_smp_cargo_receipt_tests':'CARGO_RESERVATION_PREPARED:','kingdom_smp_flight_tests':'FLIGHT_PARTIAL_PREPARED:','kingdom_smp_aircraft_repair_tests':'AIRCRAFT_REPAIR_PENDING_PREPARED:','kingdom_smp_aircraft_sales_tests':'AIRCRAFT_SALES_PREPARED:','kingdom_smp_space_tests':'SPACE_FLOOR_PREPARED:','kingdom_smp_aircraft_packet_tests':'AIRCRAFT_PACKETS_PREPARED:','kingdom_smp_aircraft_restore_tests':'AIRCRAFT_RESTORE_PREPARED:','kingdom_smp_aircraft_tests':'AIRCRAFT_PREPARED:','kingdom_smp_waystone_tests':'WAYSTONE_PREPARED:','kingdom_smp_quest_tests':'FTB_SETTLEMENT_PREPARED:','kingdom_smp_travel_tests':'TRAVEL_PREPARED:','kingdom_smp_archive_tests':'ARCHIVE_PREPARED:','kingdom_smp_wharf_tests':'WHARF_ASSERTIONS_PASSED:','kingdom_smp_hut_foundation_tests':'HUT_FOUNDATION_PREPARED:','kingdom_smp_foundation_resume_tests':'FOUNDATION_RESTART_PREPARED:','kingdom_smp_foundation_tests':'FOUNDATION_ASSERTIONS_PASSED:','kingdom_smp_hunt_tests':'HUNT_ASSERTIONS_PASSED:','kingdom_smp_farmer_loop_tests':'FARMER_FULL_LOOP_ASSERTIONS_PASSED:','kingdom_smp_gear_shop_tests':'FARMER_GEAR_SHOP_ASSERTIONS_PASSED:','kingdom_smp_farmer_gear_tests':'FARMER_GEAR_ASSERTIONS_PASSED:','kingdom_smp_bounty_tests':'BOUNTY_ASSERTIONS_PASSED:','kingdom_smp_farmer_events_tests':'FARMER_EVENTS_ASSERTIONS_PASSED:','kingdom_smp_farmer_tests':'FARMER_QUEST_ASSERTIONS_PASSED:','kingdom_smp_profession_tests':'PROFESSION_CONTRACT_ASSERTIONS_PASSED:','kingdom_smp_crop_tests':'CROP_RECOVERY_ASSERTIONS_PASSED:','kingdom_smp_townhall_tests':'TOWN_HALL_TEST_ASSERTIONS_PASSED:','kingdom_smp_commerce_tests':'COMMERCE_TEST_ASSERTIONS_PASSED:','kingdom_smp_claim_tests':'CLAIM_TEST_ASSERTIONS_PASSED:','kingdom_smp_bank_tests':'BANK_TEST_ASSERTIONS_PASSED:','kingdom_smp_market_tests':'MARKET_TEST_ASSERTIONS_PASSED:','kingdom_smp_auction_tests':'AUCTION_TEST_ASSERTIONS_PASSED:'}[args.namespace]
root = Path(__file__).resolve().parent.parent
subprocess.run(['python3', str(root / 'scripts/download-claim-test-mods.py')], check=True)
if args.namespace in ('kingdom_smp_aircraft_tests','kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests'):
    subprocess.run(['python3', str(root / 'scripts/download-aircraft-test-mods.py'), '--destination', args.mods], check=True)
if args.namespace == 'kingdom_smp_space_tests':
    subprocess.run(['python3', str(root / 'scripts/download-space-test-mods.py'), '--destination', args.mods], check=True)
if args.namespace == 'kingdom_smp_waystone_tests':
    subprocess.run(['python3', str(root / 'scripts/download-waystone-test-mods.py'), '--destination', args.mods], check=True)
if args.namespace == 'kingdom_smp_quest_tests':
    subprocess.run(['python3', str(root / 'scripts/download-quest-test-mods.py'), '--destination', args.mods], check=True)
if args.namespace == 'kingdom_smp_travel_tests':
    subprocess.run(['python3', str(root / 'scripts/download-dimension-test-mods.py'), '--destination', args.mods], check=True)
fixture = Path(tempfile.mkdtemp(prefix='claim-server.', dir=root / 'work'))
if args.seed_aircraft_restore_world:
    import fcntl, shutil
    source = args.seed_aircraft_restore_world.resolve()
    if args.namespace not in ('kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests') or source.parent != (root / 'work').resolve() or not source.name.startswith('claim-server.') or not (source / 'claims-fixture/aircraft-runtime-proof.nbt').is_file():
        raise SystemExit('Restoration tests require a verified private claim-server fixture; refusing arbitrary user saves')
    if args.namespace in ('kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests') and not (source / 'claims-fixture/aircraft-sales-proof.nbt').is_file():
        raise SystemExit('Repair/flight tests require a stopped verified purchased-aircraft fixture, not the initial hangar-only world')
    with (source / 'claims-fixture/session.lock').open('r+b') as lock:
        fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        shutil.copytree(source,fixture,dirs_exist_ok=True)
        fcntl.lockf(lock, fcntl.LOCK_UN)
elif args.namespace in ('kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests'):
    raise SystemExit('Pass --seed-aircraft-restore-world with a stopped completed native Aerodrome fixture')

properties = {
    'server-ip': '127.0.0.1', 'server-port': '0', 'online-mode': 'false',
    'level-name': 'claims-fixture', 'level-type': 'minecraft:flat',
    'generator-settings': json.dumps({'layers': [{'block': 'minecraft:air', 'height': 1}],
        'biome': 'minecraft:plains', 'features': False, 'lakes': False}, separators=(',', ':')),
    'spawn-protection': '0', 'view-distance': '3', 'simulation-distance': '4',
    'sync-chunk-writes': 'true', 'max-players': '10', 'difficulty': 'peaceful',
    'generate-structures': 'false',
}
(fixture / 'server.properties').write_text(''.join(f'{k}={v}\n' for k, v in properties.items()))
(fixture / 'eula.txt').write_text('eula=true\n')
if args.namespace == 'kingdom_smp_aircraft_tests':
    # Only this private test world: exercise mixed FTB edit-whitelisted/non-whitelisted cells.
    pack = fixture / 'claims-fixture/datapacks/aircraft-claim-fixture'
    tag = pack / 'data/ftbchunks/tags/block/edit_whitelist.json'
    tag.parent.mkdir(parents=True)
    (pack / 'pack.mcmeta').write_text(json.dumps({'pack': {'pack_format': 48, 'description': 'Private aircraft claim regression fixture'}}))
    tag.write_text(json.dumps({'values': ['minecraft:emerald_block']}))

command = [str(root / 'scripts/gradle-cloud.sh'), 'runServer',
    f'-PkingdomServerRunDir={fixture}', f'-PkingdomClaimTestMods={args.mods}',
    f'-PkingdomTestNamespace={args.namespace}', '--console=plain']
if args.stage_cargo_preview:
    if args.namespace != 'kingdom_smp_cargo_journey_tests':
        raise SystemExit('--stage-cargo-preview requires the private cargo journey namespace')
    init = fixture / 'cargo-review-preview.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testStageCargoReview', 'true' } } }\n")
    command += ['--init-script',str(init)]
if args.stage_flight_preview:
    if args.namespace != 'kingdom_smp_flight_tests':
        raise SystemExit('--stage-flight-preview requires the native flight namespace')
    init = fixture / 'flight-review-preview.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testStageFlightReview', 'true' } } }\n")
    command += ['--init-script',str(init)]
if args.stage_aircraft_recovery_preview:
    if args.namespace != 'kingdom_smp_aircraft_sales_tests':
        raise SystemExit('--stage-aircraft-recovery-preview requires the native aircraft sales namespace')
    init = fixture / 'aircraft-recovery-preview.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testAircraftRecoveryPreview', 'true' } } }\n")
    command.extend(['-I', str(init)])
print(f'Isolated claim fixture: {fixture}', flush=True)
restart_markers = {
    'kingdom_smp_aircraft_fuel_tests': ['AIRCRAFT_FUEL_PREPARED:', 'AIRCRAFT_FUEL_RESTART_VERIFIED:'],
    'kingdom_smp_cargo_journey_tests': ['CARGO_JOURNEY_PARTIAL_PREPARED:', 'CARGO_JOURNEY_ACK_FAULT_PREPARED:', 'CARGO_JOURNEY_REWARD_PREPARED:', 'CARGO_JOURNEY_RESTART_VERIFIED:'],
    'kingdom_smp_cargo_receipt_tests': ['CARGO_RESERVATION_PREPARED:', 'CARGO_NATIVE_DEBIT_PREPARED:', 'CARGO_WORLD_ROLLBACK_PREPARED:', 'CARGO_RECEIPT_MISMATCH_BLOCKED:'],
    'kingdom_smp_flight_tests': ['FLIGHT_PARTIAL_PREPARED:', 'FLIGHT_REWARD_PREPARED:', 'FLIGHT_RESTART_VERIFIED:'],
    'kingdom_smp_aircraft_repair_tests': ['AIRCRAFT_REPAIR_PENDING_PREPARED:', 'AIRCRAFT_REPAIR_RECEIPT_PREPARED:', 'AIRCRAFT_REPAIR_RECEIPT_VERIFIED:'],
    'kingdom_smp_aircraft_sales_tests': ['AIRCRAFT_SALES_PREPARED:', 'AIRCRAFT_SALES_RESTART_VERIFIED:', 'AIRCRAFT_SALES_PENDING_PREPARED:', 'AIRCRAFT_SALES_ACTIVE_VERIFIED:'],
    'kingdom_smp_space_tests': ['SPACE_FLOOR_PREPARED:', 'SPACE_COUNTDOWN_PREPARED:', 'SPACE_FLOOR_RESTART_VERIFIED:'],
    'kingdom_smp_aircraft_packet_tests': ['AIRCRAFT_PACKETS_PREPARED:', 'AIRCRAFT_PACKETS_RESTART_VERIFIED:'],
    'kingdom_smp_aircraft_restore_tests': ['AIRCRAFT_RESTORE_PREPARED:', 'AIRCRAFT_RESTORE_ALLOWED:', 'AIRCRAFT_RESTORE_LOCKED:', 'AIRCRAFT_RESTORE_FTB:', 'AIRCRAFT_RESTORE_OPAC:', 'AIRCRAFT_RESTORE_PRESERVED:', 'AIRCRAFT_RESTORE_RECOVERED:'],
    'kingdom_smp_aircraft_tests': ['AIRCRAFT_PREPARED:', 'AIRCRAFT_CONSTRUCTED:', 'AIRCRAFT_RESTART_VERIFIED:'],
    'kingdom_smp_waystone_tests': ['WAYSTONE_PREPARED:', 'WAYSTONE_RESTART_VERIFIED:'],
    'kingdom_smp_quest_tests': ['FTB_SETTLEMENT_PREPARED:', 'FTB_SETTLEMENT_RESTART_VERIFIED:'],
    'kingdom_smp_travel_tests': ['TRAVEL_PREPARED:', 'TRAVEL_RESTART_VERIFIED:'],
    'kingdom_smp_archive_tests': ['ARCHIVE_PREPARED:', 'ARCHIVE_RESUMED:', 'ARCHIVE_VERIFIED:'],
    'kingdom_smp_foundation_resume_tests': ['FOUNDATION_RESTART_PREPARED:', 'FOUNDATION_RESTART_RESUMED:', 'FOUNDATION_RESTART_VERIFIED:'],
    'kingdom_smp_hut_foundation_tests': ['HUT_FOUNDATION_PREPARED:', 'HUT_FOUNDATION_RESUMED:', 'HUT_FOUNDATION_VERIFIED:'],
}
markers = restart_markers.get(args.namespace, [marker])
if args.stage_cargo_preview:
    markers = ['CARGO_JOURNEY_PARTIAL_PREPARED:', 'CARGO_JOURNEY_REVIEW_PREPARED:']
if args.stage_flight_preview:
    markers = ['FLIGHT_PARTIAL_PREPARED:', 'FLIGHT_REVIEW_PREPARED:']
if args.stage_aircraft_recovery_preview:
    markers = markers[:3]
if args.stage_space_preview:
    if args.namespace != "kingdom_smp_space_tests":
        raise SystemExit("--stage-space-preview requires the native space namespace")
    markers = markers[:1]
if args.verify_wharf_restart:
    if args.namespace != 'kingdom_smp_wharf_tests':
        raise SystemExit('--verify-wharf-restart requires kingdom_smp_wharf_tests')
    markers = [marker, 'WHARF_RESTART_VERIFIED:']
for cycle, marker in enumerate(markers):
    started = passed = stopped = failed = False
    with (fixture / ('execution.log' if len(markers)==1 else f'execution-{cycle+1}.log')).open('w') as log:
        process = subprocess.Popen(command, cwd=root, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT, text=True, bufsize=1)
        try:
            for line in process.stdout:
                print(line, end='', flush=True)
                log.write(line); log.flush()
                if 'Done (' in line and not started:
                    started = True
                    test_origin = '-128 80 -128' if args.seed_aircraft_restore_world or len(markers)>1 and cycle>0 else '0 80 0'
                    process.stdin.write(f'execute positioned {test_origin} run test runall\n'); process.stdin.flush()
                if marker in line:
                    passed = True
                if ('[minecraft/LogTestReporter]' in line and ' failed at ' in line) or 'Game test server crashed' in line or 'No tests found' in line or 'net.minecraft.gametest.framework.GameTestAssertException:' in line:
                    failed = True
                if (passed or failed) and not stopped:
                    stopped = True
                    process.stdin.write('save-all flush\nstop\n'); process.stdin.flush()
            result = process.wait()
        finally:
            if process.poll() is None:
                process.stdin.write('stop\n'); process.stdin.flush()
                process.wait(timeout=30)
    if result or not passed or failed:
        raise SystemExit(f'Claim fixture failed: exit={result}, passed={passed}, failed={failed}; {fixture}')
print(args.namespace + f' SUITE_PASSED: {len(markers)} normal dedicated startup(s), assertions, flush and clean stop(s)', flush=True)
