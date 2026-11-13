#!/usr/bin/env python3
"""Run actual pinned claim APIs on a fresh normal dedicated server, with embedded test peers."""
import json
import argparse
from pathlib import Path
import subprocess
import tempfile

parser=argparse.ArgumentParser()
parser.add_argument('--seed-master-forestry-world',type=Path,help='Copy a stopped private verified full16native Lumberjack fixture for Master forestry acceptance')
parser.add_argument('--seed-profession-world',type=Path,help='Copy a stopped verified private all-premises fixture for focused native production tests')
parser.add_argument('--seed-aircraft-restore-world',type=Path,help='Copy a stopped verified private claim-server fixture for saved-aircraft restoration tests only')
parser.add_argument('--stage-resident-name-preview',action='store_true',help='Stop after real purchased-plan naming for ordinary-client QA')
parser.add_argument('--resume-resident-name-construction',action='store_true',help='Resume a stopped verified private name-preview fixture through construction and restart')
parser.add_argument('--stage-cargo-preview',action='store_true',help='Stop the verified private cargo voyage before NPC unloading for ordinary-client QA')
parser.add_argument('--stage-scout-preview',action='store_true',help='Stop verified private reconnaissance before the Steward report for ordinary-client QA')
parser.add_argument('--stage-flight-preview',action='store_true',help='Stop the verified private flight route before the Steward report for ordinary client QA')
parser.add_argument('--namespace',choices=['kingdom_smp_chunk_unload_tests','kingdom_smp_alchemy_tests','kingdom_smp_master_forestry_tests','kingdom_smp_profession_production_tests','kingdom_smp_claim_tests','kingdom_smp_townhall_tests','kingdom_smp_commerce_tests','kingdom_smp_bank_tests','kingdom_smp_auction_tests','kingdom_smp_market_tests','kingdom_smp_crop_tests','kingdom_smp_profession_tests','kingdom_smp_commission_tests','kingdom_smp_woodland_tests','kingdom_smp_retained_tests','kingdom_smp_tree_tests','kingdom_smp_premises_tests', 'kingdom_smp_farmer_tests','kingdom_smp_farmer_events_tests','kingdom_smp_bounty_tests','kingdom_smp_farmer_gear_tests','kingdom_smp_gear_shop_tests','kingdom_smp_farmer_loop_tests','kingdom_smp_hunt_tests','kingdom_smp_foundation_tests','kingdom_smp_foundation_resume_tests','kingdom_smp_hut_foundation_tests','kingdom_smp_wharf_tests','kingdom_smp_resident_name_tests','kingdom_smp_title_tests','kingdom_smp_scout_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_tests','kingdom_smp_waystone_tests','kingdom_smp_quest_tests','kingdom_smp_space_tests','kingdom_smp_travel_tests','kingdom_smp_archive_tests'],default='kingdom_smp_claim_tests')
parser.add_argument('--mods', default=None)
parser.add_argument('--alchemy-reagents',action='store_true',help='Exercise actual Mystical Agriculture planting/growth/harvest and delivery in private copies')
parser.add_argument('--create-milling',action='store_true',help='Native traced player wheat/millstone mutations/persistent partial production')
parser.add_argument('--create-press',action='store_true',help='Native powered press/input drops/partial restart/physical delivery')
parser.add_argument('--create-drive',action='store_true',help='Native flowing waterwheel/shaft and confirmed current-claim inspection')
parser.add_argument('--create-craft',action='store_true',help='Native Create primary recipes and saved authenticated Engineer deliveries')
parser.add_argument('--library-field-guide',action='store_true',help='Real native dimension travel and saved personal Library field guide on a stopped completedJfixture')
parser.add_argument('--alchemy-mixed',action='store_true',help='Run actual grouped underwater and beneficial splash commissions from earned10Alchemy proofs')
parser.add_argument('--alchemy-restore',action='store_true',help='Verify a stopped private completed Alchemist fixture in one native restore boot')
parser.add_argument('--alchemy-journeyman',action='store_true',help='Test real Tier6native duration/strength brewing after earned full Alchemist Apprentice')
parser.add_argument('--merchant-production',action='store_true',help='Run real Bank, auction, supply and order Merchant objectives in a copied private profession world')
parser.add_argument('--smith-production',action='store_true',help='Add actual Blacksmith native production after the premises restart fixture')
parser.add_argument('--verify-wharf-restart', action='store_true')
parser.add_argument('--stage-aircraft-recovery-preview', action='store_true', help='Stop the private verified aircraft fixture with a consumed pending plan outside client chunk range for actual recovery UI checks')
parser.add_argument('--stage-space-preview', action='store_true', help='Stop the private space fixture after its verified Tier9 preparation for actual graphical checks')
args=parser.parse_args()
if sum([args.alchemy_restore,args.alchemy_journeyman,args.alchemy_mixed,args.alchemy_reagents])>1:
    parser.error("Choose restoration or new Journeyman production, not both")
if (args.alchemy_restore or args.alchemy_journeyman or args.alchemy_mixed or args.alchemy_reagents) and args.namespace != "kingdom_smp_alchemy_tests":
    parser.error("Alchemist flags require their private Alchemist namespace")
if args.mods is None:
    args.mods = 'work/mystical-test-mods' if args.alchemy_reagents else 'work/aircraft-test-mods' if args.namespace in ('kingdom_smp_aircraft_tests','kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_resident_name_tests','kingdom_smp_title_tests','kingdom_smp_scout_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests') else 'work/claim-test-mods'
marker={'kingdom_smp_chunk_unload_tests':'CHUNK_UNLOAD_NATIVE_PASSED:','kingdom_smp_alchemy_tests':'ALCHEMY_PENDING_PREPARED:','kingdom_smp_master_forestry_tests':'MASTER_FORESTRY_NATIVE_PASSED:','kingdom_smp_profession_production_tests':'PROFESSION_PRODUCTION_PASSED:','kingdom_smp_premises_tests':'PROFESSION_PREMISES_PREPARED:','kingdom_smp_resident_name_tests':'RESIDENT_NAME_PREPARED:','kingdom_smp_title_tests':'TITLE_SELECTION_PREPARED:','kingdom_smp_scout_tests':'SCOUT_PARTIAL_PREPARED:','kingdom_smp_aircraft_fuel_tests':'AIRCRAFT_FUEL_PREPARED:','kingdom_smp_cargo_journey_tests':'CARGO_JOURNEY_PARTIAL_PREPARED:','kingdom_smp_cargo_receipt_tests':'CARGO_RESERVATION_PREPARED:','kingdom_smp_flight_tests':'FLIGHT_PARTIAL_PREPARED:','kingdom_smp_aircraft_repair_tests':'AIRCRAFT_REPAIR_PENDING_PREPARED:','kingdom_smp_aircraft_sales_tests':'AIRCRAFT_SALES_PREPARED:','kingdom_smp_space_tests':'SPACE_FLOOR_PREPARED:','kingdom_smp_aircraft_packet_tests':'AIRCRAFT_PACKETS_PREPARED:','kingdom_smp_aircraft_restore_tests':'AIRCRAFT_RESTORE_PREPARED:','kingdom_smp_aircraft_tests':'AIRCRAFT_PREPARED:','kingdom_smp_waystone_tests':'WAYSTONE_PREPARED:','kingdom_smp_quest_tests':'FTB_SETTLEMENT_PREPARED:','kingdom_smp_travel_tests':'TRAVEL_PREPARED:','kingdom_smp_archive_tests':'ARCHIVE_PREPARED:','kingdom_smp_wharf_tests':'WHARF_ASSERTIONS_PASSED:','kingdom_smp_hut_foundation_tests':'HUT_FOUNDATION_PREPARED:','kingdom_smp_foundation_resume_tests':'FOUNDATION_RESTART_PREPARED:','kingdom_smp_foundation_tests':'FOUNDATION_ASSERTIONS_PASSED:','kingdom_smp_hunt_tests':'HUNT_ASSERTIONS_PASSED:','kingdom_smp_farmer_loop_tests':'FARMER_FULL_LOOP_ASSERTIONS_PASSED:','kingdom_smp_gear_shop_tests':'FARMER_GEAR_SHOP_ASSERTIONS_PASSED:','kingdom_smp_farmer_gear_tests':'FARMER_GEAR_ASSERTIONS_PASSED:','kingdom_smp_bounty_tests':'BOUNTY_ASSERTIONS_PASSED:','kingdom_smp_farmer_events_tests':'FARMER_EVENTS_ASSERTIONS_PASSED:','kingdom_smp_farmer_tests':'FARMER_QUEST_ASSERTIONS_PASSED:','kingdom_smp_tree_tests':'NATIVE_TREE_ASSERTIONS_PASSED:','kingdom_smp_retained_tests':'RETAINED_BUILDING_ASSERTIONS_PASSED:','kingdom_smp_woodland_tests':'PROFESSION_COMMISSION_ASSERTIONS_PASSED:','kingdom_smp_commission_tests':'PROFESSION_COMMISSION_ASSERTIONS_PASSED:','kingdom_smp_profession_tests':'PROFESSION_CONTRACT_ASSERTIONS_PASSED:','kingdom_smp_crop_tests':'CROP_RECOVERY_ASSERTIONS_PASSED:','kingdom_smp_townhall_tests':'TOWN_HALL_TEST_ASSERTIONS_PASSED:','kingdom_smp_commerce_tests':'COMMERCE_TEST_ASSERTIONS_PASSED:','kingdom_smp_claim_tests':'CLAIM_TEST_ASSERTIONS_PASSED:','kingdom_smp_bank_tests':'BANK_TEST_ASSERTIONS_PASSED:','kingdom_smp_market_tests':'MARKET_TEST_ASSERTIONS_PASSED:','kingdom_smp_auction_tests':'AUCTION_TEST_ASSERTIONS_PASSED:'}[args.namespace]
root = Path(__file__).resolve().parent.parent
subprocess.run(['python3', str(root / 'scripts/download-claim-test-mods.py')], check=True)
if args.namespace in ('kingdom_smp_aircraft_tests','kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_resident_name_tests','kingdom_smp_title_tests','kingdom_smp_scout_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests'):
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
if args.seed_master_forestry_world:
    import fcntl, shutil
    source=args.seed_master_forestry_world.resolve()
    if args.seed_profession_world or args.seed_aircraft_restore_world or args.namespace!='kingdom_smp_master_forestry_tests' or source.parent!=(root/'work').resolve() or not source.name.startswith('claim-server.'):
        raise SystemExit('Master forestry requires a stopped private fixture only')
    text=(source/'logs/latest.log').read_text()
    if not any(marker in text for marker in ['NATIVE_TREE_RESTART_ASSERTIONS_PASSED:','MASTER_FORESTRY_NATIVE_PASSED:','MASTER_FORESTRY_RESTART_PASSED:']):
        raise SystemExit('No actual completed native forestry source evidence')
    with (source/'claims-fixture/session.lock').open('r+b') as lock:
        fcntl.lockf(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        shutil.copytree(source,fixture,dirs_exist_ok=True)
        fcntl.lockf(lock,fcntl.LOCK_UN)
elif args.namespace=='kingdom_smp_master_forestry_tests':
    raise SystemExit('A verified private forestry source is mandatory')
if args.seed_profession_world:
    import fcntl, shutil
    source = args.seed_profession_world.resolve()
    if args.seed_aircraft_restore_world or args.namespace not in ('kingdom_smp_profession_production_tests','kingdom_smp_alchemy_tests') or source.parent != (root / 'work').resolve() or not source.name.startswith('claim-server.') or not (source / 'claims-fixture/smith-repair-runtime-proof.nbt').is_file():
        raise SystemExit('Production tests require a stopped verified private profession fixture; refusing arbitrary user saves')
    if not (source / 'claims-fixture/profession-production-proof.nbt').is_file() and 'ALL_PROFESSION_CONTRACTS_PASSED:' not in (source / 'logs/latest.log').read_text():
        raise SystemExit('Source has no successful all-profession acceptance marker')
    with (source / 'claims-fixture/session.lock').open('r+b') as lock:
        fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        shutil.copytree(source,fixture,dirs_exist_ok=True)
        fcntl.lockf(lock, fcntl.LOCK_UN)
elif args.namespace in ('kingdom_smp_profession_production_tests','kingdom_smp_alchemy_tests'):
    raise SystemExit('Pass --seed-profession-world with a stopped verified private premises fixture')
if args.seed_aircraft_restore_world:
    import fcntl, shutil
    source = args.seed_aircraft_restore_world.resolve()
    if args.namespace not in ('kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_resident_name_tests','kingdom_smp_title_tests','kingdom_smp_scout_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests') or source.parent != (root / 'work').resolve() or not source.name.startswith('claim-server.') or not (source / 'claims-fixture/aircraft-runtime-proof.nbt').is_file():
        raise SystemExit('Restoration tests require a verified private claim-server fixture; refusing arbitrary user saves')
    if args.namespace in ('kingdom_smp_resident_name_tests','kingdom_smp_title_tests','kingdom_smp_scout_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests') and not (source / 'claims-fixture/aircraft-sales-proof.nbt').is_file():
        raise SystemExit('Repair/flight tests require a stopped verified purchased-aircraft fixture, not the initial hangar-only world')
    with (source / 'claims-fixture/session.lock').open('r+b') as lock:
        fcntl.lockf(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        shutil.copytree(source,fixture,dirs_exist_ok=True)
        fcntl.lockf(lock, fcntl.LOCK_UN)
elif args.namespace in ('kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_resident_name_tests','kingdom_smp_title_tests','kingdom_smp_scout_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests'):
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
if args.namespace == 'kingdom_smp_tree_tests':
    properties['generator-settings'] = json.dumps({'layers':[{'block':'minecraft:grass_block','height':1}], 'biome':'minecraft:forest', 'features':True, 'lakes':False},separators=(',',':'))
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
if args.alchemy_reagents:
    init=fixture/'alchemy-reagents.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testAlchemyReagents','true' } } }\n")
    command += ['--init-script',str(init)]
if args.alchemy_mixed:
    init=fixture/'alchemy-mixed.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testAlchemyMixed','true' } } }\n")
    command += ['--init-script',str(init)]
if args.alchemy_journeyman:
    if args.namespace != 'kingdom_smp_alchemy_tests':
        raise SystemExit('--alchemy-journeyman requires the native Alchemist namespace')
    init=fixture/'alchemy-journeyman.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testAlchemyJourneyman','true' } } }\n")
    command += ['--init-script',str(init)]
if args.create_milling:
    if args.namespace != "kingdom_smp_profession_production_tests" or args.create_press or args.create_drive or args.create_craft or args.library_field_guide or args.merchant_production:
        raise SystemExit("Create milling requires focused namespace and a single producer")
    init=fixture/"create-milling.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testCreateMilling','true' } } }\n")
    command += ["--init-script",str(init)]
if args.create_press:
    if args.namespace != "kingdom_smp_profession_production_tests" or args.create_drive or args.create_craft or args.library_field_guide or args.merchant_production:
        raise SystemExit("Create press requires focused namespace and a single producer")
    init=fixture/"create-press.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testCreatePress','true' } } }\n")
    command += ["--init-script",str(init)]
if args.create_drive:
    if args.namespace != "kingdom_smp_profession_production_tests" or args.create_craft or args.library_field_guide or args.merchant_production:
        raise SystemExit("Create drive requires focused namespace and a single producer")
    init=fixture/"create-drive.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testCreateDrive','true' } } }\n")
    command += ["--init-script",str(init)]
if args.create_craft:
    if args.namespace != "kingdom_smp_profession_production_tests" or args.library_field_guide or args.merchant_production:
        raise SystemExit("Create craft requires the focused namespace and a single producer")
    init=fixture/"create-craft.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testCreateCraft','true' } } }\n")
    command += ["--init-script",str(init)]
if args.library_field_guide:
    if args.namespace != 'kingdom_smp_profession_production_tests' or args.merchant_production:
        raise SystemExit('--library-field-guide needs the focused namespace without another producer')
    init=fixture/'library-field-guide.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testLibraryFieldGuide','true' } } }\n")
    command += ['--init-script',str(init)]
if args.merchant_production:
    if args.namespace != 'kingdom_smp_profession_production_tests':
        raise SystemExit('--merchant-production requires the focused private profession namespace')
    init = fixture / 'merchant-production.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testMerchantProduction', 'true' } } }\n")
    command += ['--init-script', str(init)]
if args.smith_production:
    if args.namespace != 'kingdom_smp_premises_tests':
        raise SystemExit('--smith-production requires the premises namespace')
    command.append('-PkingdomSmithProduction=true')
if args.stage_cargo_preview:
    if args.namespace != 'kingdom_smp_cargo_journey_tests':
        raise SystemExit('--stage-cargo-preview requires the private cargo journey namespace')
    init = fixture / 'cargo-review-preview.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testStageCargoReview', 'true' } } }\n")
    command += ['--init-script',str(init)]
if args.stage_resident_name_preview or args.resume_resident_name_construction:
    if args.namespace != 'kingdom_smp_resident_name_tests' or args.stage_resident_name_preview and args.resume_resident_name_construction:
        raise SystemExit('Resident-name staging/resume requires its private namespace and exactly one mode')
    if args.resume_resident_name_construction:
        if not (fixture / 'claims-fixture/resident-name-proof.nbt').is_file():
            raise SystemExit('Resident-name resume requires its previously verified private naming proof')
        init = fixture / 'resident-name-resume.init.gradle'
        init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testResumeResidentName', 'true' } } }\n")
        command += ['--init-script',str(init)]
if args.stage_scout_preview:
    if args.namespace != 'kingdom_smp_scout_tests':
        raise SystemExit('--stage-scout-preview requires the private reconnaissance namespace')
    init = fixture / 'scout-review-preview.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testStageScoutReview', 'true' } } }\n")
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
    'kingdom_smp_resident_name_tests': ['RESIDENT_NAME_PREPARED:', 'RESIDENT_NAME_CONSTRUCTED:', 'RESIDENT_NAME_RESTART_VERIFIED:'],
    'kingdom_smp_title_tests': ['TITLE_SELECTION_PREPARED:', 'TITLE_RESTART_VERIFIED:'],
    'kingdom_smp_scout_tests': ['SCOUT_PARTIAL_PREPARED:', 'SCOUT_REWARD_PREPARED:', 'SCOUT_RESTART_VERIFIED:'],
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
if args.namespace=="kingdom_smp_chunk_unload_tests":
    markers=["CHUNK_UNLOAD_NATIVE_PASSED:","CHUNK_UNLOAD_RESTART_PASSED:"]
if args.stage_resident_name_preview:
    markers = markers[:1]
if args.resume_resident_name_construction:
    markers = markers[1:]
if args.stage_cargo_preview:
    markers = ['CARGO_JOURNEY_PARTIAL_PREPARED:', 'CARGO_JOURNEY_REVIEW_PREPARED:']
if args.stage_scout_preview:
    markers = ['SCOUT_PARTIAL_PREPARED:', 'SCOUT_REVIEW_PREPARED:']
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
if args.namespace == 'kingdom_smp_master_forestry_tests':
    markers = ['MASTER_FORESTRY_RESTART_PASSED:'] if args.seed_master_forestry_world and 'original19earned3300XP' in (args.seed_master_forestry_world.resolve()/'logs/latest.log').read_text() else [marker,'MASTER_FORESTRY_RESTART_PASSED:']
if args.namespace == 'kingdom_smp_alchemy_tests':
    markers = ['REAGENT_CROP_PENDING_PASSED:','REAGENT_CROP_NATIVE_PASSED:','REAGENT_CROP_RESTART_PASSED:'] if args.alchemy_reagents else ['ALCHEMY_MIXED_NATIVE_PASSED:','ALCHEMY_MIXED_RESTART_PASSED:'] if args.alchemy_mixed else ['ALCHEMY_RESTART_PASSED:'] if args.alchemy_restore else ['ALCHEMY_JOURNEYMAN_NATIVE_PASSED:','ALCHEMY_JOURNEYMAN_RESTART_PASSED:'] if args.alchemy_journeyman else [marker,'ALCHEMY_APPRENTICE_NATIVE_PASSED:','ALCHEMY_RESTART_PASSED:']
if args.namespace == 'kingdom_smp_townhall_tests':
    markers = [marker, 'TOWN_HALL_RESTART_VERIFIED:']
if args.namespace == 'kingdom_smp_tree_tests':
    markers = ['NATIVE_TREE_PREPARED:', 'NATIVE_TREE_RESTART_ASSERTIONS_PASSED:']
if args.namespace == 'kingdom_smp_premises_tests':
    markers = ['PROFESSION_PREMISES_PREPARED:', 'SMITH_REPAIR_PREPARED:', 'SMITH_APPRENTICE_RESTART_PASSED:'] if args.smith_production else ['PROFESSION_PREMISES_PREPARED:', 'PROFESSION_PREMISES_RESTART_PASSED:']
if args.namespace == 'kingdom_smp_retained_tests':
    markers = [marker, 'RETAINED_CORRUPT_EVENT_ASSERTIONS_PASSED:', 'RETAINED_RESTART_ASSERTIONS_PASSED:']
    import shutil
    retained_file = fixture / 'claims-fixture/data/kingdomsmp_retained_buildings_v1.dat'
    retained_backup = fixture / 'retained-evidence-matching-backup.dat'
    corrupt_marker = fixture / 'claims-fixture/retained-corrupt-fixture'
if args.library_field_guide:
    markers=["LIBRARY_FIELD_GUIDE_NATIVE_PASSED:","LIBRARY_FIELD_GUIDE_RESTART_PASSED:"]
if args.create_craft:
    markers=["CREATE_CRAFT_NATIVE_PASSED:","CREATE_CRAFT_RESTART_PASSED:"]
if args.create_drive:
    markers=["CREATE_DRIVE_NATIVE_PASSED:","CREATE_DRIVE_RESTART_PASSED:"]
if args.create_press:
    markers=["CREATE_PRESS_PENDING_PASSED:","CREATE_PRESS_NATIVE_PASSED:","CREATE_PRESS_RESTART_PASSED:"]
if args.create_milling:
    markers=["CREATE_MILLING_PENDING_PASSED:","CREATE_MILLING_NATIVE_PASSED:","CREATE_MILLING_RESTART_PASSED:"]
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
                    test_origin = '-128 80 -128' if args.seed_aircraft_restore_world or args.seed_profession_world or args.seed_master_forestry_world or len(markers)>1 and cycle>0 else '0 80 0'
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
    if args.namespace == 'kingdom_smp_retained_tests':
        if result or not passed or failed:
            if retained_backup.is_file() and corrupt_marker.exists():
                shutil.copy2(retained_backup, retained_file)
                corrupt_marker.unlink()
        elif cycle == 0:
            shutil.copy2(retained_file, retained_backup)
            retained_file.write_bytes(b'Intentional private stopped-world malformed evidence fixture')
            corrupt_marker.write_text('Only this generated private QA save; native item transaction must remain safe')
        elif cycle == 1:
            shutil.copy2(retained_backup, retained_file)
            corrupt_marker.unlink()
    if result or not passed or failed:
        raise SystemExit(f'Claim fixture failed: exit={result}, passed={passed}, failed={failed}; {fixture}')
print(args.namespace + f' SUITE_PASSED: {len(markers)} normal dedicated startup(s), assertions, flush and clean stop(s)', flush=True)
