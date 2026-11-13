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
   int weaponSlot=(int)call(((List<?>)description.getClass().getMethod("getSlots",String.class).invoke(description,"weapon")).getFirst(),"index");
   int ammoSlot=(int)call(((List<?>)description.getClass().getMethod("getSlots",String.class).invoke(description,"inventory")).get(1),"index");
   ((Container)plane).setItem(weaponSlot,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("immersive_aircraft:heavy_crossbow"))));
   ((Container)plane).setItem(ammoSlot,new ItemStack(Items.ARROW,32));
   h.runAfterDelay(3,()->guard(h,()->{
    h.assertTrue((float)call(plane,"getFuelUtilization")>0&&((Container)plane).getItem(fuelSlot).getCount()<4,"Actual native ticks consume coal into native fuel; no injected engine/fuel value");
    send(p,"EnginePowerMessage",new Class<?>[]{float.class},.5f);
    h.assertTrue((float)call(plane,"getEngineTarget")==.5f,"Transformed native receiveServer accepts legitimate fuelled pilot throttle");
    var peer=ClaimRuntimeTests.vanillaPlayer(level,UUID.nameUUIDFromBytes("OfflinePlayer:PacketPeer".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"PacketPeer");
    inventory(h,p,peer,plane,pos);
    weapon(h,p,peer,plane,weaponSlot,ammoSlot);
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
 private static void weapon(GameTestHelper h,ServerPlayer pilot,ServerPlayer peer,Entity plane,int slot,int ammoSlot)throws Exception{
  var nativeAllowed=plane.getClass().getMethod("canFireWeapon",Entity.class,int.class,int.class);
  h.assertTrue(Boolean.TRUE.equals(nativeAllowed.invoke(plane,pilot,slot,0)),"Real equipped native crossbow and ammunition permit its actual controlling operator");
  var ammo=((Container)plane).getItem(ammoSlot);int beforeAmmo=ammo.getCount();
  var before=new HashSet<java.util.UUID>();h.getLevel().getEntities().getAll().forEach(entity->{if(entity instanceof net.minecraft.world.entity.projectile.AbstractArrow)before.add(entity.getUUID());});
  var fireTypes=new Class<?>[]{int.class,int.class,Vector3f.class};
  send(peer,"FireMessage",fireTypes,slot,0,new Vector3f(0,0,1));
  send(pilot,"FireMessage",fireTypes,slot,0,new Vector3f(1000,0,0));
  send(pilot,"FireMessage",fireTypes,slot,0,new Vector3f(1,1,1));
  h.assertTrue(((Container)plane).getItem(ammoSlot).getCount()==beforeAmmo,"Wrong operator and oversized native direction cannot consume ammunition");
  var afterInvalid=new ArrayList<net.minecraft.world.entity.projectile.AbstractArrow>();h.getLevel().getEntities().getAll().forEach(entity->{if(entity instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow&&!before.contains(arrow.getUUID()))afterInvalid.add(arrow);});
  h.assertTrue(afterInvalid.isEmpty(),"Rejected direction cannot move a muzzle or create a remote projectile");
  send(pilot,"FireMessage",fireTypes,slot,0,new Vector3f(0,0,1));
  var created=new ArrayList<net.minecraft.world.entity.projectile.AbstractArrow>();h.getLevel().getEntities().getAll().forEach(entity->{if(entity instanceof net.minecraft.world.entity.projectile.AbstractArrow arrow&&!before.contains(arrow.getUUID()))created.add(arrow);});
  h.assertTrue(!created.isEmpty()&&((Container)plane).getItem(ammoSlot).getCount()<beforeAmmo,"Actual legal native weapon fire creates projectiles and consumes real ammunition");
  for(var arrow:created){h.assertTrue(arrow.getOwner()==pilot&&arrow.distanceToSqr(plane)<100,"Native projectile retains the real operator and local muzzle origin for downstream claim protection");arrow.discard();}
 }
 private static void inventory(GameTestHelper h,ServerPlayer p,ServerPlayer peer,Entity plane,BlockPos pos)throws Exception{
  var level=h.getLevel();var description=call(plane,"getInventoryDescription");var getSlots=description.getClass().getMethod("getSlots",String.class);
  int cargo=(int)call(((List<?>)getSlots.invoke(description,"inventory")).getFirst(),"index");
  int upgrade=(int)call(((List<?>)getSlots.invoke(description,"upgrade")).getFirst(),"index");
  var cargoStack=new ItemStack(Items.DIAMOND,13);((Container)plane).setItem(cargo,cargoStack.copy());
  var equipment=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("immersive_aircraft:enhanced_propeller")));((Container)plane).setItem(upgrade,equipment.copy());
  DimensionTravelRuntimeTests.seed(p.server,5);drain(peer);
  send(peer,"InventoryRequest",new Class<?>[]{int.class},plane.getId());var messages=drain(peer);boolean visual=false;
  for(Object packet:messages)if(packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom&&custom.payload().getClass().getName().equals("immersive_aircraft.network.s2c.InventoryUpdateMessage")){
   var update=custom.payload();int slot=(int)call(update,"getIndex");h.assertTrue(slot<cargo,"Actual initial native public synchronization never transmits private cargo or ammunition");
   if(slot==upgrade){var stack=(ItemStack)update.getClass().getMethod("getStack",Entity.class).invoke(update,plane);h.assertTrue(ItemStack.isSameItemSameComponents(stack,equipment),"Public render equipment preserves the actual native item");visual=true;}
  }
  h.assertTrue(visual,"A non-owner may still receive visual upgrades below the flight unlock tier");
  var original=p.containerMenu;plane.getClass().getMethod("openInventory",ServerPlayer.class).invoke(plane,p);
  h.assertTrue(p.containerMenu==original,"Locked native initial menu opening refuses cargo disclosure");DimensionTravelRuntimeTests.seed(p.server,6);
  try{ClaimRuntimeTests.ftbClaim(p,pos);}catch(IllegalStateException existing){if(!existing.getMessage().endsWith("ALREADY_CLAIMED"))throw existing;}ClaimRuntimeTests.opacClaim(level,p.getUUID(),pos);
  drain(p);plane.getClass().getMethod("openInventory",ServerPlayer.class).invoke(plane,p);var menu=p.containerMenu;
  h.assertTrue(menu!=original&&menu.stillValid(p),"Real authorized native aircraft menu opens with current tier and both claim APIs");
  var initial=drain(p);boolean full=initial.stream().anyMatch(packet->packet instanceof net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket content&&content.getItems().stream().anyMatch(item->ItemStack.isSameItemSameComponents(item,cargoStack)&&item.getCount()==13));
  if(!full)com.mojang.logging.LogUtils.getLogger().error("Native cargo sync diagnostic slot={} container={} menu={} packets={}",cargo,((Container)plane).getItem(cargo),menu.getItems(),initial.stream().map(v->v.getClass().getName()).toList());
  h.assertTrue(full,"Authorized native full menu synchronization retains actual cargo contents");
  ClaimRuntimeTests.ftbUnclaim(level,pos);ClaimRuntimeTests.ftbClaim(peer,pos);
  h.assertTrue(!menu.stillValid(p),"An already-open native cargo menu rechecks independently revoked FTB rights");
  for(var kind:List.of(net.minecraft.world.inventory.ClickType.PICKUP,net.minecraft.world.inventory.ClickType.QUICK_MOVE,net.minecraft.world.inventory.ClickType.SWAP))click(p,cargo,kind);
  h.assertTrue(((Container)plane).getItem(cargo).getCount()==13&&menu.getCarried().isEmpty(),"Actual vanilla click packets after FTB revocation cannot extract, shift-transfer or swap cargo");
  ClaimRuntimeTests.ftbUnclaim(level,pos);ClaimRuntimeTests.ftbClaim(p,pos);ClaimRuntimeTests.opacUnclaim(level,pos);ClaimRuntimeTests.opacClaim(level,peer.getUUID(),pos);
  h.assertTrue(!menu.stillValid(p),"OPAC independently invalidates the same open native menu");click(p,cargo,net.minecraft.world.inventory.ClickType.PICKUP);
  h.assertTrue(((Container)plane).getItem(cargo).getCount()==13&&menu.getCarried().isEmpty(),"Current OPAC denial precedes native inventory mutation");
  ClaimRuntimeTests.opacUnclaim(level,pos);ClaimRuntimeTests.opacClaim(level,p.getUUID(),pos);
  DimensionTravelRuntimeTests.seed(p.server,5);click(p,cargo,net.minecraft.world.inventory.ClickType.QUICK_MOVE);
  h.assertTrue(!menu.stillValid(p)&&((Container)plane).getItem(cargo).getCount()==13,"Post-open tier revocation also refuses native cargo transfer");DimensionTravelRuntimeTests.seed(p.server,6);
  h.assertTrue(menu.stillValid(p),"Restored real rights revive the original menu without a new aircraft or copied cargo");
  click(p,cargo,net.minecraft.world.inventory.ClickType.PICKUP);
  h.assertTrue(((Container)plane).getItem(cargo).isEmpty()&&menu.getCarried().getCount()==13,"Actual authorized native cargo pickup remains functional");
  click(p,cargo,net.minecraft.world.inventory.ClickType.PICKUP);
  h.assertTrue(((Container)plane).getItem(cargo).getCount()==13&&menu.getCarried().isEmpty(),"Putting cargo back preserves exact count and empties cursor");p.closeContainer();
 }
 private static void click(ServerPlayer p,int slot,net.minecraft.world.inventory.ClickType kind){var menu=p.containerMenu;p.connection.handleContainerClick(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(menu.containerId,menu.getStateId(),slot,0,kind,ItemStack.EMPTY,new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));}
 private static List<Object> drain(ServerPlayer p){var channel=(io.netty.channel.embedded.EmbeddedChannel)p.connection.getConnection().channel();p.connection.getConnection().flushChannel();channel.runPendingTasks();var result=new ArrayList<Object>();Object packet;while((packet=channel.readOutbound())!=null){if(packet instanceof net.minecraft.network.protocol.game.ClientboundBundlePacket bundle)bundle.subPackets().forEach(result::add);else result.add(packet);}return result;}
 private static boolean floating(ServerPlayer p)throws Exception{var field=net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("clientVehicleIsFloating");field.setAccessible(true);return field.getBoolean(p.connection);}
 private static Object call(Object target,String name)throws Exception{return target.getClass().getMethod(name).invoke(target);}
 private static Object message(String name,Class<?>[] types,Object...args)throws Exception{return Class.forName("immersive_aircraft.network.c2s."+name).getConstructor(types).newInstance(args);}
 private static void send(ServerPlayer p,String name,Class<?>[] types,Object...args)throws Exception{var packet=message(name,types,args);packet.getClass().getMethod("receiveServer",ServerPlayer.class).invoke(packet,p);}
 @FunctionalInterface private interface Step{void run()throws Exception;}
 private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception e){com.mojang.logging.LogUtils.getLogger().error("Aircraft native packet assertion failed",e);h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
