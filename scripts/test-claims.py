#!/usr/bin/env python3
"""Run actual pinned claim APIs on a fresh normal dedicated server, with embedded test peers."""
import json
import argparse
from pathlib import Path
import subprocess
import tempfile

parser=argparse.ArgumentParser()
parser.add_argument('--mine-access-corrupt',action='store_true',help='One extra private stopped-save fault boot verifies native item/movement safety before matching restore')
parser.add_argument('--mine-access',action='store_true',help='Real retained mine access/native accepted descent and return/lighting/assessment')
parser.add_argument('--forest-reserve',action='store_true',help='Actual native retained community forest/funded Master service/final all24Lumberjack assessment')
parser.add_argument('--bee-reagents',action='store_true',help='Actual Alchemist supervised mixed resource combs/partial restart/canonical receipt')
parser.add_argument('--bee-jobs',action='store_true',help='Actual supervised native per-species comb production, partial restart and physical receipt')
parser.add_argument('--bee-apiary',action='store_true',help='Exact native bee AI pollination/hive production and approved woodland inspection')
parser.add_argument('--builder-civic',action='store_true',help='Actual new civic roads/native funded Master service and all24Builder final acceptance')
parser.add_argument('--builder-fittings',action='store_true',help='Actual exact Supplementaries native furnishing interactions after genuine Master room')
parser.add_argument('--seed-builder-world',type=Path,help='Copy only a stopped verified earned16Builder private fixture for Master acceptance')
parser.add_argument('--seed-master-forestry-world',type=Path,help='Copy a stopped private verified full16native Lumberjack fixture for Master forestry acceptance')
parser.add_argument('--seed-profession-world',type=Path,help='Copy a stopped verified private all-premises fixture for focused native production tests')
parser.add_argument('--seed-aircraft-restore-world',type=Path,help='Copy a stopped verified private claim-server fixture for saved-aircraft restoration tests only')
parser.add_argument('--stage-resident-name-preview',action='store_true',help='Stop after real purchased-plan naming for ordinary-client QA')
parser.add_argument('--resume-resident-name-construction',action='store_true',help='Resume a stopped verified private name-preview fixture through construction and restart')
parser.add_argument('--stage-cargo-preview',action='store_true',help='Stop the verified private cargo voyage before NPC unloading for ordinary-client QA')
parser.add_argument('--stage-scout-preview',action='store_true',help='Stop verified private reconnaissance before the Steward report for ordinary-client QA')
parser.add_argument('--stage-flight-preview',action='store_true',help='Stop the verified private flight route before the Steward report for ordinary client QA')
parser.add_argument('--namespace',choices=['kingdom_smp_builder_master_tests','kingdom_smp_chunk_unload_tests','kingdom_smp_alchemy_tests','kingdom_smp_master_forestry_tests','kingdom_smp_profession_production_tests','kingdom_smp_claim_tests','kingdom_smp_townhall_tests','kingdom_smp_commerce_tests','kingdom_smp_bank_tests','kingdom_smp_auction_tests','kingdom_smp_market_tests','kingdom_smp_crop_tests','kingdom_smp_profession_tests','kingdom_smp_commission_tests','kingdom_smp_woodland_tests','kingdom_smp_retained_tests','kingdom_smp_tree_tests','kingdom_smp_premises_tests', 'kingdom_smp_farmer_tests','kingdom_smp_farmer_events_tests','kingdom_smp_bounty_tests','kingdom_smp_farmer_gear_tests','kingdom_smp_gear_shop_tests','kingdom_smp_farmer_loop_tests','kingdom_smp_hunt_tests','kingdom_smp_foundation_tests','kingdom_smp_foundation_resume_tests','kingdom_smp_hut_foundation_tests','kingdom_smp_wharf_tests','kingdom_smp_resident_name_tests','kingdom_smp_title_tests','kingdom_smp_scout_tests','kingdom_smp_aircraft_fuel_tests','kingdom_smp_cargo_journey_tests','kingdom_smp_cargo_receipt_tests','kingdom_smp_flight_tests','kingdom_smp_aircraft_repair_tests','kingdom_smp_aircraft_sales_tests','kingdom_smp_aircraft_packet_tests','kingdom_smp_aircraft_restore_tests','kingdom_smp_aircraft_tests','kingdom_smp_waystone_tests','kingdom_smp_quest_tests','kingdom_smp_space_tests','kingdom_smp_travel_tests','kingdom_smp_archive_tests'],default='kingdom_smp_claim_tests')
parser.add_argument('--mods', default=None)
parser.add_argument('--alchemy-reagents',action='store_true',help='Exercise actual Mystical Agriculture planting/growth/harvest and delivery in private copies')
parser.add_argument('--mekanism-smelt',action='store_true',help='Actual exact Mekanism raw-osmium native primary processing/physical delivery')
parser.add_argument('--native-copper',action='store_true',help='Actual legacy coal/iron retention and native separate-area copper extraction')
parser.add_argument('--native-ore',action='store_true',help='Real decorated future Overworld native ore generation and successful personal extraction in a stopped private clone')
parser.add_argument('--encounters',action='store_true',help='Actual natural-origin hostile deaths and personal once-only profession evidence')
parser.add_argument('--expedition-supplies',action='store_true',help='Actual native food cooking/torch-compass crafting and permanent Adventurer delivery')
parser.add_argument('--create-workshop',action='store_true',help='Actual all7prerequisites and final native functional Engineer workshop assessment')
parser.add_argument('--create-transmission',action='store_true',help='Actual live ratio/reversal and staged native clutch output stop')
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
marker={'kingdom_smp_builder_master_tests':'BUILDER_MASTER_INTERIOR_NATIVE_PASSED:','kingdom_smp_chunk_unload_tests':'CHUNK_UNLOAD_NATIVE_PASSED:','kingdom_smp_alchemy_tests':'ALCHEMY_PENDING_PREPARED:','kingdom_smp_master_forestry_tests':'MASTER_FORESTRY_NATIVE_PASSED:','kingdom_smp_profession_production_tests':'PROFESSION_PRODUCTION_PASSED:','kingdom_smp_premises_tests':'PROFESSION_PREMISES_PREPARED:','kingdom_smp_resident_name_tests':'RESIDENT_NAME_PREPARED:','kingdom_smp_title_tests':'TITLE_SELECTION_PREPARED:','kingdom_smp_scout_tests':'SCOUT_PARTIAL_PREPARED:','kingdom_smp_aircraft_fuel_tests':'AIRCRAFT_FUEL_PREPARED:','kingdom_smp_cargo_journey_tests':'CARGO_JOURNEY_PARTIAL_PREPARED:','kingdom_smp_cargo_receipt_tests':'CARGO_RESERVATION_PREPARED:','kingdom_smp_flight_tests':'FLIGHT_PARTIAL_PREPARED:','kingdom_smp_aircraft_repair_tests':'AIRCRAFT_REPAIR_PENDING_PREPARED:','kingdom_smp_aircraft_sales_tests':'AIRCRAFT_SALES_PREPARED:','kingdom_smp_space_tests':'SPACE_FLOOR_PREPARED:','kingdom_smp_aircraft_packet_tests':'AIRCRAFT_PACKETS_PREPARED:','kingdom_smp_aircraft_restore_tests':'AIRCRAFT_RESTORE_PREPARED:','kingdom_smp_aircraft_tests':'AIRCRAFT_PREPARED:','kingdom_smp_waystone_tests':'WAYSTONE_PREPARED:','kingdom_smp_quest_tests':'FTB_SETTLEMENT_PREPARED:','kingdom_smp_travel_tests':'TRAVEL_PREPARED:','kingdom_smp_archive_tests':'ARCHIVE_PREPARED:','kingdom_smp_wharf_tests':'WHARF_ASSERTIONS_PASSED:','kingdom_smp_hut_foundation_tests':'HUT_FOUNDATION_PREPARED:','kingdom_smp_foundation_resume_tests':'FOUNDATION_RESTART_PREPARED:','kingdom_smp_foundation_tests':'FOUNDATION_ASSERTIONS_PASSED:','kingdom_smp_hunt_tests':'HUNT_ASSERTIONS_PASSED:','kingdom_smp_farmer_loop_tests':'FARMER_FULL_LOOP_ASSERTIONS_PASSED:','kingdom_smp_gear_shop_tests':'FARMER_GEAR_SHOP_ASSERTIONS_PASSED:','kingdom_smp_farmer_gear_tests':'FARMER_GEAR_ASSERTIONS_PASSED:','kingdom_smp_bounty_tests':'BOUNTY_ASSERTIONS_PASSED:','kingdom_smp_farmer_events_tests':'FARMER_EVENTS_ASSERTIONS_PASSED:','kingdom_smp_farmer_tests':'FARMER_QUEST_ASSERTIONS_PASSED:','kingdom_smp_tree_tests':'NATIVE_TREE_ASSERTIONS_PASSED:','kingdom_smp_retained_tests':'RETAINED_BUILDING_ASSERTIONS_PASSED:','kingdom_smp_woodland_tests':'PROFESSION_COMMISSION_ASSERTIONS_PASSED:','kingdom_smp_commission_tests':'PROFESSION_COMMISSION_ASSERTIONS_PASSED:','kingdom_smp_profession_tests':'PROFESSION_CONTRACT_ASSERTIONS_PASSED:','kingdom_smp_crop_tests':'CROP_RECOVERY_ASSERTIONS_PASSED:','kingdom_smp_townhall_tests':'TOWN_HALL_TEST_ASSERTIONS_PASSED:','kingdom_smp_commerce_tests':'COMMERCE_TEST_ASSERTIONS_PASSED:','kingdom_smp_claim_tests':'CLAIM_TEST_ASSERTIONS_PASSED:','kingdom_smp_bank_tests':'BANK_TEST_ASSERTIONS_PASSED:','kingdom_smp_market_tests':'MARKET_TEST_ASSERTIONS_PASSED:','kingdom_smp_auction_tests':'AUCTION_TEST_ASSERTIONS_PASSED:'}[args.namespace]
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
if args.seed_builder_world:
    import fcntl, shutil
    source=args.seed_builder_world.resolve()
    if args.seed_profession_world or args.seed_master_forestry_world or args.seed_aircraft_restore_world or args.namespace!='kingdom_smp_builder_master_tests' or source.parent!=(root/'work').resolve() or not source.name.startswith('claim-server.') or not (source/'claims-fixture/data/kingdomsmp_retained_buildings_v1.dat').is_file():
        raise SystemExit('Builder Master tests require a stopped private earned Builder fixture only')
    text=(source/'logs/latest.log').read_text()
    if not any(marker in text for marker in ['RETAINED_RESTART_ASSERTIONS_PASSED:','BUILDER_MASTER_INTERIOR_NATIVE_PASSED:','BUILDER_MASTER_INTERIOR_RESTART_PASSED:','SUPPLEMENTARIES_NATIVE_PASSED:','SUPPLEMENTARIES_RESTART_PASSED:','BUILDER_CIVIC_NATIVE_PASSED:','BUILDER_CIVIC_RESTART_PASSED:']):
        raise SystemExit('No actual completed native Builder source evidence')
    with (source/'claims-fixture/session.lock').open('r+b') as lock:
        fcntl.lockf(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        shutil.copytree(source,fixture,dirs_exist_ok=True)
        fcntl.lockf(lock,fcntl.LOCK_UN)
elif args.namespace=='kingdom_smp_builder_master_tests':
    raise SystemExit('A verified stopped private Builder source is mandatory')
if args.seed_master_forestry_world:
    import fcntl, shutil
    source=args.seed_master_forestry_world.resolve()
    if args.seed_profession_world or args.seed_aircraft_restore_world or args.namespace!='kingdom_smp_master_forestry_tests' or source.parent!=(root/'work').resolve() or not source.name.startswith('claim-server.'):
        raise SystemExit('Master forestry requires a stopped private fixture only')
    text=(source/'logs/latest.log').read_text()
    if args.bee_jobs and (source/'claims-fixture/apiary-job-partial.txt').is_file() and (source/'execution-1.log').is_file():
        text += (source/'execution-1.log').read_text()
        origin=source/'apiary-job-origin-proof.log'
        if origin.is_file():
            text += origin.read_text()
    if not any(marker in text for marker in (['APIARY_JOBS_RESTART_PASSED:','FOREST_RESERVE_RESTART_PASSED:'] if args.forest_reserve else ['APIARY_RESTART_PASSED:','APIARY_JOBS_PENDING_PASSED:'] if args.bee_jobs else ['NATIVE_TREE_RESTART_ASSERTIONS_PASSED:','MASTER_FORESTRY_NATIVE_PASSED:','MASTER_FORESTRY_RESTART_PASSED:'])):
        raise SystemExit('No actual completed native forestry source evidence')
    with (source/'claims-fixture/session.lock').open('r+b') as lock:
        fcntl.lockf(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        shutil.copytree(source,fixture,dirs_exist_ok=True)
        fcntl.lockf(lock,fcntl.LOCK_UN)
elif args.namespace=='kingdom_smp_master_forestry_tests':
    raise SystemExit('A verified private forestry source is mandatory')
if args.bee_jobs and (fixture/'claims-fixture/apiary-job-partial.txt').is_file() and (fixture/'execution-1.log').is_file() and not (fixture/'apiary-job-origin-proof.log').is_file():
    text=(fixture/'execution-1.log').read_text()
    if 'APIARY_JOBS_PENDING_PASSED:' in text:
        (fixture/'apiary-job-origin-proof.log').write_text(text)
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
if args.builder_civic:
    if args.namespace!='kingdom_smp_builder_master_tests' or not args.seed_builder_world or args.builder_fittings:
        raise SystemExit('Builder civic requires its actual stopped Master furnishing fixture only')
    init=fixture/'builder-civic.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testBuilderCivic','true' } } }\n")
    command += ['--init-script',str(init)]
if args.builder_fittings:
    if args.namespace!='kingdom_smp_builder_master_tests' or not args.seed_builder_world:
        raise SystemExit('Builder fittings requires its actual stopped Master room fixture')
    init=fixture/'builder-fittings.init.gradle'
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testBuilderFittings','true' } } }\n")
    command += ['--init-script',str(init)]
if args.mekanism_smelt:
    if args.namespace != "kingdom_smp_profession_production_tests" or any([args.native_copper,args.native_ore,args.expedition_supplies,args.create_workshop,args.create_transmission,args.create_milling,args.create_press,args.create_drive,args.create_craft,args.library_field_guide,args.merchant_production]):
        raise SystemExit("Mekanism smelting requires focused namespace and a single producer")
    init=fixture/"mekanism-smelt.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testMekanismSmelt','true' } } }\n")
    command += ["--init-script",str(init)]
if args.native_ore or args.native_copper:
    if args.native_ore and args.native_copper:
        raise SystemExit("Choose original ore production or later separate copper only")
    if args.namespace != "kingdom_smp_profession_production_tests" or any([args.mekanism_smelt,args.expedition_supplies,args.create_workshop,args.create_transmission,args.create_milling,args.create_press,args.create_drive,args.create_craft,args.library_field_guide,args.merchant_production]):
        raise SystemExit("Native ore requires focused namespace and a single producer")
    init=fixture/"native-ore.init.gradle"
    init.write_text("allprojects { afterEvaluate { def run = tasks.named('runServer', JavaExec); def prep = tasks.register('prepareNativeOreFixture',JavaExec) { dependsOn tasks.named('gameTestClasses'); classpath = files('build/moddev/artifacts/neoforge-21.1.265.jar') + files(file('build/moddev/serverLegacyClasspath.txt').readLines()) + sourceSets.gameTest.output; mainClass = 'com.mohymods.kingdomsmp.gametest.NativeOreFixturePrep'; args '"+str(fixture)+"' }; run.configure { dependsOn prep; systemProperty '"+("kingdom.testNativeCopper" if args.native_copper else "kingdom.testNativeOre")+"','true' } } }\n")
    command += ["--init-script",str(init)]
if args.mine_access_corrupt and not args.mine_access:
    raise SystemExit("Mine access corruption requires its guarded private fixture run")
if args.mine_access:
    if args.namespace != "kingdom_smp_profession_production_tests" or not args.seed_profession_world:
        raise SystemExit("Mine access requires a stopped verified original native Miner fixture")
    init=fixture/"mine-access.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testMineAccess','true' } } }\n")
    command += ["--init-script",str(init)]
if args.forest_reserve:
    if any([args.bee_jobs,args.bee_apiary,args.bee_reagents]) or args.namespace != "kingdom_smp_master_forestry_tests" or not args.seed_master_forestry_world:
        raise SystemExit("Forest reserve requires original native21Lumberjack/source working productive apiary only")
    init=fixture/"forest-reserve.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testForestReserve','true' } } }\n")
    command += ["--init-script",str(init)]
if args.bee_reagents:
    if args.namespace != "kingdom_smp_alchemy_tests" or not args.seed_profession_world:
        raise SystemExit("Bee reagents requires verified stopped original native Alchemist source")
    init=fixture/"bee-reagents.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testBeeReagents','true' } } }\n")
    command += ["--init-script",str(init)]
if args.bee_jobs:
    if args.bee_apiary or args.namespace != "kingdom_smp_master_forestry_tests" or not args.seed_master_forestry_world:
        raise SystemExit("Bee jobs require verified stopped working native woodland apiary")
    init=fixture/"bee-jobs.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testBeeJobs','true' } } }\n")
    command += ["--init-script",str(init)]
if args.bee_apiary:
    if args.namespace != "kingdom_smp_master_forestry_tests" or not args.seed_master_forestry_world:
        raise SystemExit("Bee apiary requires verified stopped native forestry source")
    init=fixture/"bee-apiary.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testBeeApiary','true' } } }\n")
    command += ["--init-script",str(init)]
if args.encounters:
    if args.namespace != "kingdom_smp_profession_production_tests" or any([args.native_ore,args.native_copper,args.mekanism_smelt,args.expedition_supplies,args.create_workshop,args.create_transmission,args.create_milling,args.create_press,args.create_drive,args.create_craft,args.library_field_guide,args.merchant_production]):
        raise SystemExit("Encounters requires focused namespace and a single producer")
    init=fixture/"encounters.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testEncounters','true' } } }\n")
    command += ["--init-script",str(init)]
if args.expedition_supplies:
    if args.namespace != "kingdom_smp_profession_production_tests" or any([args.create_workshop,args.create_transmission,args.create_milling,args.create_press,args.create_drive,args.create_craft,args.library_field_guide,args.merchant_production]):
        raise SystemExit("Expedition supplies requires focused namespace and a single producer")
    init=fixture/"expedition-supplies.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testExpeditionSupplies','true' } } }\n")
    command += ["--init-script",str(init)]
if args.create_workshop:
    if args.namespace != "kingdom_smp_profession_production_tests" or args.create_transmission or args.create_milling or args.create_press or args.create_drive or args.create_craft or args.library_field_guide or args.merchant_production:
        raise SystemExit("Create workshop requires focused namespace and a single producer")
    init=fixture/"create-workshop.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testCreateWorkshop','true' } } }\n")
    command += ["--init-script",str(init)]
if args.create_transmission:
    if args.namespace != "kingdom_smp_profession_production_tests" or args.create_milling or args.create_press or args.create_drive or args.create_craft or args.library_field_guide or args.merchant_production:
        raise SystemExit("Create transmission requires focused namespace and a single producer")
    init=fixture/"create-transmission.init.gradle"
    init.write_text("allprojects { afterEvaluate { tasks.named('runServer', JavaExec).configure { systemProperty 'kingdom.testCreateTransmission','true' } } }\n")
    command += ["--init-script",str(init)]
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
if args.bee_jobs or args.bee_reagents:
    print('Private Productive Bees QA timing: extra native hive ticks only; actual bee AI/forage/full quantities/materials unchanged',flush=True)
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
if args.create_transmission:
    markers=["CREATE_TRANSMISSION_NATIVE_PASSED:","CREATE_TRANSMISSION_RESTART_PASSED:"]
if args.create_workshop:
    markers=["CREATE_WORKSHOP_NATIVE_PASSED:","CREATE_WORKSHOP_RESTART_PASSED:"]
if args.mine_access:
    markers=(["MINE_ACCESS_PENDING_PASSED:","MINE_ACCESS_CORRUPT_PASSED:","MINE_ACCESS_NATIVE_PASSED:","MINE_ACCESS_RESTART_PASSED:"] if args.mine_access_corrupt else ["MINE_ACCESS_PENDING_PASSED:","MINE_ACCESS_NATIVE_PASSED:","MINE_ACCESS_RESTART_PASSED:"])
if args.forest_reserve:
    markers=["FOREST_RESERVE_NATIVE_PASSED:","FOREST_RESERVE_RESTART_PASSED:"]
if args.bee_reagents:
    markers=(["APIARY_REAGENTS_NATIVE_PASSED:","APIARY_REAGENTS_RESTART_PASSED:"] if (fixture/"claims-fixture/apiary-reagents-partial.txt").is_file() else ["APIARY_REAGENTS_PENDING_PASSED:","APIARY_REAGENTS_NATIVE_PASSED:","APIARY_REAGENTS_RESTART_PASSED:"])
if args.bee_jobs:
    markers=(["APIARY_JOBS_NATIVE_PASSED:","APIARY_JOBS_RESTART_PASSED:"] if (fixture/"claims-fixture/apiary-job-partial.txt").is_file() else ["APIARY_JOBS_PENDING_PASSED:","APIARY_JOBS_NATIVE_PASSED:","APIARY_JOBS_RESTART_PASSED:"])
if args.bee_apiary:
    markers=["APIARY_NATIVE_PASSED:","APIARY_RESTART_PASSED:"]
if args.encounters:
    markers=["ENCOUNTER_NATIVE_PASSED:","ENCOUNTER_RESTART_PASSED:"]
if args.expedition_supplies:
    markers=["EXPEDITION_SUPPLIES_NATIVE_PASSED:","EXPEDITION_SUPPLIES_RESTART_PASSED:"]
if args.native_ore:
    markers=["NATIVE_ORE_RESTART_PASSED:"] if (fixture/"claims-fixture/native-ore-runtime-proof.nbt").is_file() else ["NATIVE_ORE_NATIVE_PASSED:","NATIVE_ORE_RESTART_PASSED:"]
if args.mekanism_smelt:
    markers=["MEKANISM_SMELT_NATIVE_PASSED:","MEKANISM_SMELT_RESTART_PASSED:"]
if args.native_copper:
    markers=["NATIVE_COPPER_RESTART_PASSED:"] if (fixture/"claims-fixture/native-copper-runtime-proof.nbt").is_file() else ["NATIVE_COPPER_NATIVE_PASSED:","NATIVE_COPPER_RESTART_PASSED:"]
if args.namespace=="kingdom_smp_builder_master_tests":
    markers=["BUILDER_MASTER_INTERIOR_NATIVE_PASSED:","BUILDER_MASTER_INTERIOR_RESTART_PASSED:"]
if args.builder_fittings:
    markers=["SUPPLEMENTARIES_NATIVE_PASSED:","SUPPLEMENTARIES_RESTART_PASSED:"]
if args.builder_civic:
    markers=["BUILDER_CIVIC_NATIVE_PASSED:","BUILDER_CIVIC_RESTART_PASSED:"]
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
                    test_origin = '-128 80 -128' if args.seed_builder_world or args.seed_aircraft_restore_world or args.seed_profession_world or args.seed_master_forestry_world or len(markers)>1 and cycle>0 else '0 80 0'
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
    if args.mine_access_corrupt:
        import shutil
        access_file=fixture/'claims-fixture/data/kingdomsmp_mine_access_v1.dat'
        access_backup=fixture/'mine-access-source-backup.dat'
        access_fault=fixture/'claims-fixture/mine-access-corrupt.txt'
        if result or not passed or failed:
            if access_backup.is_file() and access_fault.exists():
                shutil.copy2(access_backup,access_file)
                access_fault.unlink()
        elif cycle==0:
            shutil.copy2(access_file,access_backup)
            access_file.write_text('Intentional private stopped-world malformed evidence fixture')
            access_fault.write_text('Only generated private save; normal native item/movement transactions must remain safe')
        elif cycle==1:
            shutil.copy2(access_backup,access_file)
            access_fault.unlink()
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
