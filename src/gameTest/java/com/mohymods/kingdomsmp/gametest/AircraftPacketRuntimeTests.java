package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.integration.aircraft.*;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;
import org.joml.Vector3f;
import java.nio.file.*;
import java.util.*;

/** Real native receiveServer methods, transformed optional mixins, normal dedicated JVM restart. */
@GameTestHolder("kingdom_smp_aircraft_packet_tests") @PrefixGameTestTemplate(false)
public final class AircraftPacketRuntimeTests {
 @GameTest(template="empty",timeoutTicks=600)
 public static void nativeControlsRetainPermissionsAndRejectPoison(GameTestHelper h)throws Exception{
  var level=h.getLevel();var server=level.getServer();var path=server.getWorldPath(LevelResource.ROOT).resolve("aircraft-packet-proof.nbt");
  var project=ConstructionService.store(server).projects().stream().filter(p->p.blueprint().equals(RoyalAerodromeBlueprint.ID)&&p.phase()==ConstructionStore.Phase.COMPLETE).findFirst().orElseThrow();
  var p=ClaimRuntimeTests.vanillaPlayer(level,project.owner(),project.ownerName());
  CommerceService.verifyReceipt(p);long balance=CommerceService.store(server).balance(p.getUUID());
  if(Files.exists(path))h.assertTrue(AtomicNbt.read(path).getLong("balance")==balance,"Real restart retains exact canonical purse and receipt");
  var pos=project.origin().offset(40,1,13);
  for(var cell:BlockPos.betweenClosed(pos.offset(-8,-1,-8),pos.offset(8,-1,8)))level.setBlock(cell,Blocks.STONE.defaultBlockState(),18);
  p.teleportTo(level,pos.getX()+.5,pos.getY(),pos.getZ()-3,0,0);level.getChunkSource().move(p);
  h.runAfterDelay(80,()->guard(h,()->{
   h.assertTrue(!p.hasPermissions(2)&&!p.getAbilities().mayfly&&AircraftAccess.denial(server).isEmpty(),"Ordinary Survival pilot, actual completed Aerodrome/Tier6");
   var plane=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("immersive_aircraft:biplane")).create(level);
   h.assertTrue(plane!=null,"Native registered biplane");plane.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);level.addFreshEntity(plane);
   h.assertTrue(p.startRiding(plane,true),"Normal native mount authorization");
   var description=call(plane,"getInventoryDescription");var boilers=(List<?>)description.getClass().getMethod("getSlots",String.class).invoke(description,"boiler");
   h.assertTrue(!boilers.isEmpty(),"Native aircraft defines real fuel slots");
   int fuelSlot=(int)call(boilers.getFirst(),"index");((Container)plane).setItem(fuelSlot,new ItemStack(Items.COAL,4));
   h.runAfterDelay(3,()->guard(h,()->{
    h.assertTrue((float)call(plane,"getFuelUtilization")>0&&((Container)plane).getItem(fuelSlot).getCount()<4,"Actual native ticks consume coal into native fuel; no injected engine/fuel value");
    send(p,"EnginePowerMessage",new Class<?>[]{float.class},.5f);
    h.assertTrue((float)call(plane,"getEngineTarget")==.5f,"Transformed native receiveServer accepts legitimate fuelled pilot throttle");
    // Embedded peers acknowledge the real server teleport explicitly, like a client.
    var teleportId=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("awaitingTeleport");teleportId.setAccessible(true);
    p.connection.handleAcceptTeleportPacket(new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(teleportId.getInt(p.connection)));
    // Real vanilla movement handler, not a direct call to our predicate. An aircraft
    // above empty terrain must retain native powered lift with allow-flight=false.
    plane.moveTo(pos.getX()+.5,pos.getY()+20,pos.getZ()+.5);plane.setDeltaMovement(Vec3.ZERO);plane.setOnGround(false);plane.verticalCollisionBelow=false;
    p.connection.tick();p.connection.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(plane));
    h.assertTrue(!server.isFlightAllowed()&&!floating(p)&&AircraftAccess.nativeLift(p),"Actual native vehicle movement does not flag an eligible aircraft as illegal flight");
    long refused=AircraftPacketGuard.rejected(p);
    for(float value:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-1,2})send(p,"EnginePowerMessage",new Class<?>[]{float.class},value);
    h.assertTrue((float)call(plane,"getEngineTarget")==.5f&&AircraftPacketGuard.rejected(p)==refused+4,"Real optional injection refuses poisoned and overdriven throttle without changing engine");
    var inputArgs=new Class<?>[]{int.class,float.class,float.class,float.class};
    send(p,"PilotInputMessage",inputArgs,plane.getId(),2f,-2f,.25f);
    var input=call(plane,"getPilotInput");
    h.assertTrue((float)call(input,"x")==1&&(float)call(input,"y")==-1&&(float)call(input,"z")==.25f,"Finite sensitivity remains accepted with native clamping");
    send(p,"PilotInputMessage",inputArgs,plane.getId()+1000,0f,0f,0f);
    send(p,"PilotInputMessage",inputArgs,plane.getId(),Float.NaN,0f,0f);
    h.assertTrue(input.equals(call(plane,"getPilotInput")),"Native target identity and nonfinite input checks retain original input");
    float health=(float)call(plane,"getHealth");
    for(float damage:new float[]{Float.NaN,Float.POSITIVE_INFINITY,-5})send(p,"CollisionMessage",new Class<?>[]{float.class},damage);
    h.assertTrue((float)call(plane,"getHealth")==health,"Malformed native collision payload cannot poison or heal aircraft");
    send(p,"FireMessage",new Class<?>[]{int.class,int.class,Vector3f.class},Integer.MAX_VALUE,-1,new Vector3f(Float.NaN,0,0));
    send(p,"FireMessage",new Class<?>[]{int.class,int.class,Vector3f.class},Integer.MAX_VALUE,-1,new Vector3f(0,0,0));
    send(p,"SeatChangeMessage",new Class<?>[]{int.class,int.class},plane.getId(),Integer.MAX_VALUE);
    h.assertTrue(p.getVehicle()==plane&&plane.getControllingPassenger()==p,"Native invalid weapon/seat identifiers retain their own permission checks and leave pilot seated");
    var enginePacket=message("EnginePowerMessage",new Class<?>[]{float.class},1f);
    var offThread=new java.util.concurrent.atomic.AtomicBoolean(true);var thread=new Thread(()->offThread.set(AircraftPacketGuard.permit(p,enginePacket)));thread.start();thread.join();
    h.assertTrue(!offThread.get(),"Native boundary refuses an off-thread call without touching world state");
    // A forged sender cannot replace the native controlling pilot, even with eligible access.
    var peer=ClaimRuntimeTests.vanillaPlayer(level,UUID.nameUUIDFromBytes("OfflinePlayer:PacketPeer".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"PacketPeer");
    send(peer,"EnginePowerMessage",new Class<?>[]{float.class},1f);
    h.assertTrue((float)call(plane,"getEngineTarget")==.5f,"Unseated authenticated peer cannot control another player's aircraft");
    DimensionTravelRuntimeTests.seed(server,5);
    plane.verticalCollisionBelow=false;plane.setOnGround(false);p.connection.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(plane));
    h.assertTrue(!AircraftAccess.nativeLift(p)&&floating(p),"Locked aircraft cannot reuse the narrow native lift policy");
    send(p,"EnginePowerMessage",new Class<?>[]{float.class},1f);
    h.assertTrue((float)call(plane,"getEngineTarget")==.5f,"Current tier rechecked on native control before the periodic rider guard");
    var key=Class.forName("immersive_aircraft.network.c2s.CommandMessage$Key");Object dismount=Arrays.stream(key.getEnumConstants()).filter(v->v.toString().equals("DISMOUNT")).findFirst().orElseThrow();
    send(p,"CommandMessage",new Class<?>[]{key,Vec3.class},dismount,new Vec3(Double.NaN,Double.POSITIVE_INFINITY,Double.MAX_VALUE));
    h.assertTrue(!p.isPassenger()&&Double.isFinite(plane.getDeltaMovement().length())&&plane.getDeltaMovement().length()<=AircraftPacketRules.MAX_DISMOUNT_SPEED+1e-9,"Actual native DISMOUNT remains usable while locked and bounds poisoned velocity");
    DimensionTravelRuntimeTests.seed(server,6);
    h.assertTrue(p.startRiding(plane,true),"Existing aircraft can be reboarded after restoring legitimate eligibility");
    var ordinary=new Vec3(.25,.1,-.5);send(p,"CommandMessage",new Class<?>[]{key,Vec3.class},dismount,ordinary);
    h.assertTrue(!p.isPassenger()&&plane.getDeltaMovement().equals(ordinary),"Legitimate native dismount momentum is unchanged");
    plane.discard();
    var boat=new net.minecraft.world.entity.vehicle.Boat(level,pos.getX()+.5,pos.getY()+20,pos.getZ()+.5);level.addFreshEntity(boat);
    h.assertTrue(p.startRiding(boat,true),"Ordinary vanilla vehicle control fixture mounts");boat.setDeltaMovement(Vec3.ZERO);boat.setOnGround(false);boat.verticalCollisionBelow=false;
    p.connection.tick();p.connection.handleMoveVehicle(new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(boat));
    h.assertTrue(!AircraftAccess.nativeLift(p)&&floating(p),"Vanilla boat still triggers the original airborne vehicle predicate");p.stopRiding();boat.discard();
    p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()-3);level.getChunkSource().move(p);
    h.assertTrue(CommerceService.store(server).balance(p.getUUID())==balance&&!p.getAbilities().mayfly,"No financial mutation or flight ability grant");
    boolean restart=Files.exists(path);var proof=new CompoundTag();proof.putLong("balance",balance);AtomicNbt.write(path,proof);
    h.succeed();com.mojang.logging.LogUtils.getLogger().info(restart?"AIRCRAFT_PACKETS_RESTART_VERIFIED: actual second JVM, same purse/project, native fuel and valid controls, malformed input/target/seat/engine/damage/fire/dismount and tier checks":"AIRCRAFT_PACKETS_PREPARED: actual native C2S methods transformed; real coal consumption and valid throttle, poison/overdrive blocked, native permissions and safe dismount retained");
   }));
  }));
 }
 private static boolean floating(ServerPlayer p)throws Exception{var field=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("clientVehicleIsFloating");field.setAccessible(true);return field.getBoolean(p.connection);}
 private static Object call(Object target,String name)throws Exception{return target.getClass().getMethod(name).invoke(target);}
 private static Object message(String name,Class<?>[] types,Object...args)throws Exception{return Class.forName("immersive_aircraft.network.c2s."+name).getConstructor(types).newInstance(args);}
 private static void send(ServerPlayer p,String name,Class<?>[] types,Object...args)throws Exception{var packet=message(name,types,args);packet.getClass().getMethod("receiveServer",ServerPlayer.class).invoke(packet,p);}
 @FunctionalInterface private interface Step{void run()throws Exception;}
 private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){com.mojang.logging.LogUtils.getLogger().error("Aircraft native packet assertion failed",e);h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
