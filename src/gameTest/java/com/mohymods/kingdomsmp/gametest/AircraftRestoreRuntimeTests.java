package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

/** A copied, genuinely completed fixture, with native PlayerList/RootVehicle across seven real JVMs. */
@GameTestHolder("kingdom_smp_aircraft_restore_tests") @PrefixGameTestTemplate(false)
public final class AircraftRestoreRuntimeTests {
 @GameTest(template="empty",timeoutTicks=1200)
 public static void savedRiderKeepsAircraftAndCurrentPermissions(GameTestHelper h)throws Exception{
  var level=h.getLevel();var server=level.getServer();var path=server.getWorldPath(LevelResource.ROOT).resolve("aircraft-restore-proof.nbt");
  var project=ConstructionService.store(server).projects().stream().filter(p->p.blueprint().equals(RoyalAerodromeBlueprint.ID)&&p.phase()==ConstructionStore.Phase.COMPLETE).findFirst().orElseThrow();
  var proof=Files.exists(path)?AtomicNbt.read(path):new CompoundTag();int phase=proof.getInt("phase");
  if(phase==2)DimensionTravelRuntimeTests.seed(server,5);
  if(phase==6){level.getWorldBorder().setCenter(proof.getDouble("borderX"),proof.getDouble("borderZ"));level.getWorldBorder().setSize(proof.getDouble("borderSize"));DimensionTravelRuntimeTests.seed(server,6);}
  ServerPlayer peer=null;
  if(phase==3||phase==4){
   peer=ClaimRuntimeTests.vanillaPlayer(level,UUID.nameUUIDFromBytes("OfflinePlayer:RecoveryPeer".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"RecoveryPeer");
   var pos=BlockPos.of(proof.getLong("pos"));
   if(phase==3){ClaimRuntimeTests.ftbUnclaim(level,pos);ClaimRuntimeTests.ftbClaim(peer,pos);}
   else{ClaimRuntimeTests.opacUnclaim(level,pos);ClaimRuntimeTests.opacClaim(level,peer.getUUID(),pos);}
  }
  // No direct attach here: a real saved RootVehicle must reattach inside vanilla placeNewPlayer.
  var p=ClaimRuntimeTests.vanillaPlayer(level,project.owner(),project.ownerName());
  h.assertTrue(!p.hasPermissions(2)&&!p.getAbilities().mayfly&&!p.isNoGravity(),"Ordinary player retains original abilities and no operator/flight bypass");
  CommerceService.verifyReceipt(p);long balance=CommerceService.store(server).balance(p.getUUID());
  if(phase>0)h.assertTrue(balance==proof.getLong("balance"),"All restarts preserve the original canonical purse and inventory receipt");
  if(phase==1||phase==6){
   h.assertTrue(p.isPassenger()&&p.getVehicle().getUUID().equals(proof.getUUID("aircraft")),"Actual vanilla saved vehicle reattached, without direct test mounting or regeneration");
   h.assertTrue(!AircraftAccess.savedVehicleRestore(p,p.getVehicle()),"Narrow native restoration authority is cleared before ordinary play resumes");
   if(phase==6){h.assertTrue(!p.getPersistentData().getBoolean("KingdomAircraftSafeLanding"),"Verified restored access clears the persisted no-ground recovery marker");h.succeed();log("AIRCRAFT_RESTORE_RECOVERED: actual seventh JVM restores original saved plane/rider after no-ground refusal, current permissions, exact purse and no flight grant");return;}
   proof.putInt("phase",2);AtomicNbt.write(path,proof);h.succeed();log("AIRCRAFT_RESTORE_ALLOWED: actual second JVM retains original mounted aircraft, purse, capabilities and cleared restoration scope");return;
  }
  if(phase==2||phase==3||phase==4){
   h.assertTrue(!p.isPassenger(),"Locked/revoked saved riding is refused after vanilla cleanup, not by deleting the aircraft");
   h.assertTrue(p.fallDistance==0&&level.getBlockState(p.blockPosition().below()).isFaceSturdy(level,p.blockPosition().below(),net.minecraft.core.Direction.UP),"Denied saved rider is already on verified safe ground before accepting play packets");
   h.assertTrue(!p.getPersistentData().getBoolean("KingdomAircraftSafeLanding"),"Successful safe landing clears its server recovery marker");
  }
  if(phase==5)h.assertTrue(!p.isPassenger()&&p.fallDistance==0,"World-saved unmounted aircraft does not relocate or endanger the safely landed player on another restart");
  var pos=phase==0?project.origin().offset(40,1,13):BlockPos.of(proof.getLong("pos"));
  // A physical private inspection pad keeps the real native player safe while normal player
  // chunk tracking loads the saved aircraft. It touches no project/aircraft state or claim policy.
  var inspection=pos.offset(-5,-1,-5);
  for(var cell:BlockPos.betweenClosed(inspection.offset(-1,0,-1),inspection.offset(1,0,1)))level.setBlock(cell,Blocks.STONE.defaultBlockState(),18);
  p.teleportTo(level,inspection.getX()+.5,inspection.getY()+1,inspection.getZ()+.5,0,0);
  // Embedded peers do not acknowledge the actual teleport/movement packet; drive the
  // vanilla chunk-tracking handover which a real connected client performs.
  level.getChunkSource().move(p);
  var peerFinal=peer;
  h.runAfterDelay(80,()->guard(h,()->{
   Entity aircraft;
   if(phase==0){
    try{ClaimRuntimeTests.ftbClaim(p,pos);}catch(IllegalStateException existing){if(!existing.getMessage().endsWith("ALREADY_CLAIMED"))throw existing;}ClaimRuntimeTests.opacClaim(level,p.getUUID(),pos);
    aircraft=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("immersive_aircraft:biplane")).create(level);h.assertTrue(aircraft!=null,"Actual native biplane exists");aircraft.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);aircraft.setNoGravity(true);level.addFreshEntity(aircraft);
    proof.putUUID("aircraft",aircraft.getUUID());proof.putLong("pos",pos.asLong());proof.putLong("balance",balance);
   }else{
    aircraft=level.getEntity(proof.getUUID("aircraft"));h.assertTrue(aircraft!=null&&aircraft.isAlive(),"Exact original aircraft survived the real restart/denied restoration: "+proof.getUUID("aircraft"));
    h.assertTrue(level.getEntitiesOfClass(Entity.class,aircraft.getBoundingBox().inflate(8),AircraftAccess::aircraft).stream().filter(e->e.getUUID().equals(proof.getUUID("aircraft"))).count()==1,"One original aircraft, no replacement or duplicate UUID");
   }
   if(phase==2||phase==3||phase==4){
    h.assertTrue(!p.startRiding(aircraft,true)&&!p.isPassenger(),"Ordinary native force boarding cannot reuse the completed login's restoration exemption");
    if(phase==2)DimensionTravelRuntimeTests.seed(server,6);
    if(phase==3){ClaimRuntimeTests.ftbUnclaim(level,pos);ClaimRuntimeTests.ftbClaim(p,pos);}
    if(phase==4){ClaimRuntimeTests.opacUnclaim(level,pos);ClaimRuntimeTests.opacClaim(level,p.getUUID(),pos);}
   }
   h.assertTrue(AircraftAccess.denial(server).isEmpty()&&p.startRiding(aircraft,true),"Restoring legitimate eligibility/claims permits the existing original aircraft without another purchase");
   if(phase==4){
    // Genuine midair revocation: native ticks must safely recover the rider, preserve the
    // original aircraft and avoid a permanent chunk ticket or flight-ability grant.
    var air=pos.above(40);aircraft.moveTo(air.getX()+.5,air.getY(),air.getZ()+.5);p.moveTo(air.getX()+.5,air.getY(),air.getZ()+.5);proof.putLong("pos",air.asLong());DimensionTravelRuntimeTests.seed(server,5);
    h.runAfterDelay(25,()->guard(h,()->{
     h.assertTrue(!p.isPassenger()&&aircraft.isAlive()&&p.fallDistance==0&&!p.getAbilities().mayfly&&!p.isNoGravity(),"Actual tick revocation over void rescues the ordinary player without destroying aircraft or granting flight");
     h.assertTrue(level.getBlockState(p.blockPosition().below()).isFaceSturdy(level,p.blockPosition().below(),net.minecraft.core.Direction.UP),"Remote Hut fallback lands on real supported clear ground");
     proof.putInt("phase",5);AtomicNbt.write(path,proof);h.succeed();log("AIRCRAFT_RESTORE_OPAC: actual fifth JVM rejects current OPAC policy, preserves original aircraft; real midair tick revocation safely lands player at existing Hut");
    }));return;
   }
   if(phase==5){
    // No safe terrain inside an explicitly narrowed private QA border. The real tick
    // guard must disconnect without mutating its live player-list iteration or losing
    // the native RootVehicle. No alternative aircraft or financial grant is created.
    proof.putDouble("borderX",level.getWorldBorder().getCenterX());proof.putDouble("borderZ",level.getWorldBorder().getCenterZ());proof.putDouble("borderSize",level.getWorldBorder().getSize());
    level.getWorldBorder().setCenter(aircraft.getX(),aircraft.getZ());level.getWorldBorder().setSize(3);DimensionTravelRuntimeTests.seed(server,5);
    h.runAfterDelay(25,()->guard(h,()->{
     h.assertTrue(p.hasDisconnected()&&p.getPersistentData().getBoolean("KingdomAircraftSafeLanding"),"No-ground refusal safely disconnects and retains recovery state; no server tick CME");
     var saved=AtomicNbt.read(server.getWorldPath(LevelResource.ROOT).resolve("playerdata").resolve(p.getUUID()+".dat"));
     h.assertTrue(saved.getCompound("RootVehicle").getUUID("Attach").equals(proof.getUUID("aircraft"))&&saved.getCompound("NeoForgeData").getBoolean("KingdomAircraftSafeLanding"),"Exact original native aircraft and recovery marker are durably preserved in the player's real save");
     proof.putInt("phase",6);AtomicNbt.write(path,proof);h.succeed();log("AIRCRAFT_RESTORE_PRESERVED: actual sixth JVM retains world-saved aircraft after safe ejection; real no-ground tick disconnect preserves its native RootVehicle and marker without a player-list crash");
    }));return;
   }
   proof.putInt("phase",phase+1);AtomicNbt.write(path,proof);h.succeed();
   log(phase==0?"AIRCRAFT_RESTORE_PREPARED: real vanilla rider and native RootVehicle saved for actual JVM restart":phase==2?"AIRCRAFT_RESTORE_LOCKED: actual third JVM safely denies seeded Tier5, preserves aircraft and removes one-call restore exemption": "AIRCRAFT_RESTORE_FTB: actual fourth JVM rechecks independently revoked FTB policy without discarding saved aircraft");
  }));
  if(phase==5)DimensionTravelRuntimeTests.seed(server,6);
 }
 private static void log(String message){com.mojang.logging.LogUtils.getLogger().info(message);}
 @FunctionalInterface private interface Step{void run()throws Exception;}
 private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){com.mojang.logging.LogUtils.getLogger().error("Aircraft restoration assertion failed",e);h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
