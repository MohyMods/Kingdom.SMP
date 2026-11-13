package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("kingdom_smp_aircraft_tests") @PrefixGameTestTemplate(false)
public final class AircraftRuntimeTests {
 private static long requests;
 @GameTest(template="empty",timeoutTicks=18000)
 public static void actualAerodromeAndAircraftBoundary(GameTestHelper h)throws Exception{
  System.setProperty("kingdom.testDeterministicPeers","true");
  var path=h.getLevel().getServer().getWorldPath(LevelResource.ROOT).resolve("aircraft-runtime-proof.nbt");
  if(Files.exists(path)){resume(h,path,AtomicNbt.read(path));return;}
  UUID[] active={null};boolean[] done={false};int[] last={-1};long[] stagnant={0};
  h.onEachTick(()->{if(active[0]==null||done[0])return;guard(h,()->{
   var s=ConstructionService.store(h.getLevel().getServer());var v=s.get(active[0]);
   if(v.placed()<75){ConstructionService.step(h.getLevel().getServer(),v.id());return;}
   done[0]=true;h.assertTrue(v.phase()==ConstructionStore.Phase.BUILDING&&!AircraftAccess.aerodromeComplete(h.getLevel().getServer()),"Partial progressive construction does not unlock flight");
   var p=h.getLevel().getServer().getPlayerList().getPlayer(v.owner());var proof=new CompoundTag();proof.putUUID("project",v.id());proof.putUUID("owner",p.getUUID());proof.putString("name",p.getGameProfile().getName());proof.putInt("phase",0);proof.putInt("placed",v.placed());proof.putLong("balance",CommerceService.store(p.server).balance(p.getUUID()));AtomicNbt.write(path,proof);
   h.succeed();com.mojang.logging.LogUtils.getLogger().info("AIRCRAFT_PREPARED: earned settlement and Bank, explicit Tier5/6 boundary and funds seeds, sole paid rotated protected Aerodrome, partial native construction; flight remains locked");
  });});
  BankRuntimeTests.completeBankLoop(h,()->guard(h,()->{
   var level=h.getLevel();var server=level.getServer();var hall=ConstructionService.lodge(server);var p=server.getPlayerList().getPlayer(hall.owner());
   var bank=ConstructionService.store(server).projects().stream().filter(v->v.blueprint().equals(RoyalBankBlueprint.ID)).findFirst().orElseThrow();near(p,level.getEntity(bank.npc()));CommerceService.openResident(p,(ReeveEntity)level.getEntity(bank.npc()));
   var money=CommerceService.store(server);money.adminSet(UUID.randomUUID(),new UUID(0,0),p.getUUID(),15000,"Explicit isolated Aerodrome transaction funds fixture");
   DimensionTravelRuntimeTests.seed(server,5);long before=money.balance(p.getUUID());boolean refused=false;try{review(p,RoyalAerodromeBlueprint.ID.toString());}catch(IllegalStateException expected){refused=true;}
   h.assertTrue(refused&&money.balance(p.getUUID())==before&&money.purchase(RoyalAerodromeBlueprint.ID.toString()).isEmpty(),"Tier5 cannot buy or pay for Aerodrome");
   h.assertTrue(!AircraftAccess.denial(server).isEmpty(),"Tier5 use locked");DimensionTravelRuntimeTests.seed(server,6);h.assertTrue(!AircraftAccess.denial(server).isEmpty(),"Tier6 alone does not unlock flight");
   var craft=craft(h,p.blockPosition().offset(0,1,8));h.assertTrue(!p.startRiding(craft,true)&&!p.isPassenger(),"Native force boarding cannot bypass missing Aerodrome");craft.discard();
   var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse("immersive_aircraft:biplane"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
   h.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND)).isCanceled()&&p.getMainHandItem().getCount()==1,"Early aircraft deployment refuses use without consuming aircraft item");
   p.getInventory().clearContent();confirm(p,review(p,RoyalAerodromeBlueprint.ID.toString()));
   boolean twice=false;try{review(p,RoyalAerodromeBlueprint.ID.toString());}catch(IllegalStateException expected){twice=true;}h.assertTrue(twice,"Only one legitimate first buyer");
   var project=ConstructionService.store(server).reward("market:"+RoyalAerodromeBlueprint.ID).orElseThrow();ConstructionService.select(p,project.id());ConstructionService.confirm(p,ConstructionService.review(p,1).nonce(),2);h.assertTrue(p.getMainHandItem().is(KingdomBlueprints.AERODROME.get()),"Authenticated recoverable physical Aerodrome plan");
   var b=BlueprintRegistry.get(project.blueprint(),1);var rotation=Rotation.CLOCKWISE_90;var site=hall.origin().offset(80,0,40);
   for(var pos:BlockPos.betweenClosed(site.offset(-3,-1,-4),site.offset(b.width(rotation)+3,-1,b.depth(rotation)+3)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
   for(int x=site.getX()>>4;x<=(site.getX()+b.width(rotation)-1)>>4;x++)for(int z=(site.getZ()-1)>>4;z<=(site.getZ()+b.depth(rotation)-1)>>4;z++){var pos=new BlockPos(x*16,site.getY(),z*16);ClaimRuntimeTests.ftbClaim(p,pos);ClaimRuntimeTests.opacClaim(level,p.getUUID(),pos);}
   p.moveTo(site.getX()-2,site.getY()+1,site.getZ());
   if(Boolean.getBoolean("kingdom.testAerodromePreviewOnly")){h.succeed();com.mojang.logging.LogUtils.getLogger().info("AIRCRAFT_PREVIEW_READY: sole authentic paid plan held by ordinary owner; real staged floor and owner claims, no construction redemption");return;}
   var preview=ConstructionService.preview(p,site,rotation);h.assertTrue(preview.denial().isEmpty(),"Actual protected rotated full hangar placement: "+preview.denial());
   ConstructionService.confirm(p,ConstructionService.view(p,1,false,"").review(),4);ConstructionService.confirm(p,ConstructionService.review(p,7).nonce(),8);var post=b.workPost(site,rotation);level.getEntity(BuilderHutSavedData.get(server).hut().orElseThrow().npcId()).moveTo(post.getX()+.5,post.getY(),post.getZ()+.5);active[0]=project.id();
  }));
 }
 private static void resume(GameTestHelper h,Path path,CompoundTag proof)throws Exception{
  var server=h.getLevel().getServer();var level=h.getLevel();var store=ConstructionService.store(server);var project=store.get(proof.getUUID("project"));
  var p=ClaimRuntimeTests.player(level,proof.getUUID("owner"),proof.getString("name"));var b=BlueprintRegistry.get(project.blueprint(),1);var home=project.origin().offset(b.rotate(b.home(),project.rotation()));p.moveTo(home.getX()+.5,home.getY(),home.getZ()-2.5);
  h.assertTrue(CommerceService.store(server).balance(p.getUUID())==proof.getLong("balance"),"Paid purse and original owner survive real restart");CommerceService.verifyReceipt(p);
  boolean[] done={false};int[] last={-1};long[] stagnant={0};
  h.onEachTick(()->{if(done[0]||h.getTick()<80)return;guard(h,()->{
   var current=store.get(project.id());
   if(current.placed()!=last[0]){last[0]=current.placed();stagnant[0]=h.getTick();}
   if(h.getTick()-stagnant[0]>200)h.fail("Aerodrome stalled at "+current.placed()+"/"+ConstructionFoundations.blocks(current).size()+": "+ConstructionService.activity(current.id()));
   if(proof.getInt("phase")==0){
    h.assertTrue(current.placed()>=proof.getInt("placed"),"Saved prefix never regresses or reissues entitlement");
    if(current.phase()==ConstructionStore.Phase.BUILDING){for(int i=0;i<12&&store.get(project.id()).phase()==ConstructionStore.Phase.BUILDING;i++)ConstructionService.step(server,project.id());return;}
    done[0]=true;finishVerification(h,p,store.get(project.id()),path,proof,false);
   }else{
    done[0]=true;h.assertTrue(current.phase()==ConstructionStore.Phase.COMPLETE&&current.npc().equals(proof.getUUID("npc"))&&current.placed()==proof.getInt("placed"),"Completed project and exactly same resident identity survive second actual restart");finishVerification(h,p,current,path,proof,true);
   }
  });});
 }

 private static void finishVerification(GameTestHelper h,ServerPlayer p,ConstructionStore.Project project,Path path,CompoundTag proof,boolean restart)throws Exception{
  verify(h,p,project);var level=h.getLevel();var vehicle=craft(h,project.origin().offset(5,1,5));h.assertTrue(p.startRiding(vehicle,true),"Ordinary existing rider begins with valid access");var damaged=project.origin();var original=level.getBlockState(damaged);level.setBlock(damaged,Blocks.AIR.defaultBlockState(),18);
  h.runAfterDelay(25,()->guard(h,()->{
   h.assertTrue(!p.isPassenger()&&vehicle.isAlive(),"Actual scheduled server ticks revoke flight eligibility without deleting aircraft");level.setBlock(damaged,original,18);vehicle.discard();near(p,level.getEntity(project.npc()));
   if(!restart){var saved=proof.copy();saved.putInt("phase",1);saved.putUUID("npc",project.npc());saved.putInt("placed",project.placed());AtomicNbt.write(path,saved);}
   h.succeed();com.mojang.logging.LogUtils.getLogger().info(restart?"AIRCRAFT_RESTART_VERIFIED: actual third JVM, same paid entitlement/project/resident/purse, intact structure, native boarding, claim revocations and continued eligibility":"AIRCRAFT_CONSTRUCTED: actual restart resumed immutable prefix, full original hangar/apron/office, one Flight Engineer, native deployment/mount and full-footprint claims, damage/repair, real tick revocation and Tier6 AND completion gate; explicitly accelerated physical work");
  }));
 }
 private static void verify(GameTestHelper h,ServerPlayer p,ConstructionStore.Project project)throws Exception{
  var level=h.getLevel();var server=level.getServer();h.assertTrue(ConstructionService.structureIntact(level,project),"Complete furnished hangar is intact");var b=BlueprintRegistry.get(project.blueprint(),1);
  long reviewStarted=System.nanoTime();h.assertTrue(com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p,project.origin(),project.origin().offset(b.width(project.rotation())-1,b.height()-1,b.depth(project.rotation())-1))==null,"Whole loaded hangar footprint remains authorized");com.mojang.logging.LogUtils.getLogger().info("AERODROME_CLAIM_REVIEW_MS {}",(System.nanoTime()-reviewStarted)/1_000_000);
  for(var c:b.blocks(project.rotation())){var pos=project.origin().offset(c.offset());h.assertTrue(level.getBlockState(pos).canSurvive(level,pos),"Supported actual hangar furnishing: "+pos+" "+c.state());}
  var npc=level.getEntity(project.npc());h.assertTrue(npc instanceof ReeveEntity&&ConstructionService.registeredReeve((ReeveEntity)npc)&&npc.getName().getString().equals("Corin Windlass"),"One permanent authenticated Flight Engineer");
  h.assertTrue(level.getEntitiesOfClass(ReeveEntity.class,ConstructionService.bounds(project)).stream().filter(n->n.buildingId().equals(project.id())).count()==1,"No duplicate Flight Engineer");
  near(p,npc);CommerceService.openResident(p,(ReeveEntity)npc);h.assertTrue(CommerceService.counterProject(p).id().equals(project.id()),"Actual claim-protected resident opens genuine kingdom counter");
  var craft=craft(h,project.origin().offset(5,1,5));h.assertTrue(AircraftAccess.denial(server).isEmpty()&&p.startRiding(craft,true)&&p.getVehicle()==craft,"Tier6 AND completed Aerodrome permit actual native boarding");p.stopRiding();
  DimensionTravelRuntimeTests.seed(server,5);h.assertTrue(!p.startRiding(craft,true),"Completed Aerodrome alone cannot bypass Tier6");DimensionTravelRuntimeTests.seed(server,6);
  var damaged=project.origin();var state=level.getBlockState(damaged);level.setBlock(damaged,Blocks.AIR.defaultBlockState(),18);h.assertTrue(!p.startRiding(craft,true)&&!AircraftAccess.denial(server).isEmpty(),"Loaded damage suspends boarding without deleting aircraft or records");level.setBlock(damaged,state,18);
  h.assertTrue(p.startRiding(craft,true),"Repair restores existing access without new payment/plan");p.stopRiding();
  var veto=new java.util.function.Consumer<EntityMountEvent>(){public void accept(EntityMountEvent e){if(e.isMounting()&&e.getEntityBeingMounted()==craft)e.setCanceled(true);}};NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST,false,EntityMountEvent.class,veto);try{h.assertTrue(!p.startRiding(craft,true),"Allowed aircraft gate preserves another mod's mounting veto");}finally{NeoForge.EVENT_BUS.unregister(veto);}
  craft.discard();deploymentClaims(h,p,project);near(p,npc);h.assertTrue(server.getCommands().getDispatcher().execute("kingdom admin travel inspect",server.createCommandSourceStack())==1,"Real administrator aircraft diagnostics");
 }

 private static void deploymentClaims(GameTestHelper h,ServerPlayer p,ConstructionStore.Project project)throws Exception{
  var level=h.getLevel();var server=level.getServer();
  for(var item:BuiltInRegistries.ITEM)if(AircraftAccess.aircraftItem(new ItemStack(item)))h.assertTrue(BuiltInRegistries.ENTITY_TYPE.containsKey(BuiltInRegistries.ITEM.getKey(item)),"Every actual native aircraft item maps to its verified registered dimensions");
  int boundary=((project.origin().getX()+64)>>4)<<4,z=project.origin().getZ()+64,y=project.origin().getY();
  for(var pos:BlockPos.betweenClosed(new BlockPos(boundary-6,y-1,z-6),new BlockPos(boundary+6,y-1,z+6)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
  for(int x=(boundary-6)>>4;x<=(boundary+6)>>4;x++)for(int cz=(z-6)>>4;cz<=(z+6)>>4;cz++){var pos=new BlockPos(x*16,y,cz*16);try{ClaimRuntimeTests.ftbClaim(p,pos);}catch(IllegalStateException existing){if(!existing.getMessage().endsWith("ALREADY_CLAIMED"))throw existing;}ClaimRuntimeTests.opacClaim(level,p.getUUID(),pos);}
  p.moveTo(boundary-.25,y,z-3,0,40);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("immersive_aircraft:biplane"))));
  var hit=p.pick(p.blockInteractionRange(),0,false);h.assertTrue(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&hit.getLocation().x<boundary,"Server ray targets owner's chunk beside a claim boundary");
  h.assertTrue(!NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND)).isCanceled(),"Own claimed full footprint permits native deployment review");
  var other=server.getPlayerList().getPlayers().stream().filter(v->v!=p).findFirst().orElseGet(()->{try{return ClaimRuntimeTests.player(level,"AirClaimPeer");}catch(Exception e){throw new RuntimeException(e);}});
  var foreign=new BlockPos(boundary,y,z);ClaimRuntimeTests.ftbUnclaim(level,foreign);ClaimRuntimeTests.ftbClaim(other,foreign);
  h.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND)).isCanceled()&&p.getMainHandItem().getCount()==1,"FTB independently refuses aircraft footprint crossing an unauthorized chunk before consumption");
  var aircraft=craft(h,foreign);h.assertTrue(!p.startRiding(aircraft,true),"Direct native forced boarding retains actual FTB entity policy");
  var white=foreign.offset(2,0,0);var privateCell=white.east();level.setBlock(white,Blocks.EMERALD_BLOCK.defaultBlockState(),18);level.setBlock(privateCell,Blocks.STONE.defaultBlockState(),18);
  h.assertTrue(com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p,white,white)==null,"Real fixture datapack whitelist permits that exact block");
  h.assertTrue(com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p,white,privateCell)!=null,"Same-chunk edit-whitelisted first cell cannot authorize its private neighbor");level.setBlock(white,Blocks.AIR.defaultBlockState(),18);level.setBlock(privateCell,Blocks.AIR.defaultBlockState(),18);

  ClaimRuntimeTests.ftbUnclaim(level,foreign);ClaimRuntimeTests.ftbClaim(p,foreign);ClaimRuntimeTests.opacUnclaim(level,foreign);ClaimRuntimeTests.opacClaim(level,other.getUUID(),foreign);
  h.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND)).isCanceled()&&p.getMainHandItem().getCount()==1,"OPAC independently refuses aircraft footprint crossing without consuming plan or aircraft");
  h.assertTrue(!p.startRiding(aircraft,true),"Direct native forced boarding retains actual OPAC entity policy");
  // Configured static-fake identities use OPAC's position-sensitive branch; compare against
  // direct exact native per-cell decisions in a small mixed-claim volume, then restore policy.
  var opacRoot=Class.forName("xaero.pac.common.server.api.OpenPACServerAPI");var api=opacRoot.getMethod("get",net.minecraft.server.MinecraftServer.class).invoke(null,server);var protection=opacRoot.getMethod("getChunkProtection").invoke(api);
  var idsField=Class.forName("xaero.pac.common.server.claims.protection.ChunkProtection").getDeclaredField("staticFakePlayerIds");idsField.setAccessible(true);@SuppressWarnings("unchecked")var ids=(Set<UUID>)idsField.get(protection);boolean added=ids.add(p.getUUID());
  try{
   var from=new BlockPos(boundary-1,y,z);var to=new BlockPos(boundary+1,y+1,z+1);boolean expected=false;
   var query=Class.forName("xaero.pac.common.server.claims.protection.api.IChunkProtectionAPI").getMethod("onEntityPlaceBlock",Entity.class,net.minecraft.server.level.ServerLevel.class,BlockPos.class);
   for(var pos:BlockPos.betweenClosed(from,to))if(Boolean.TRUE.equals(query.invoke(protection,p,level,pos))){expected=true;break;}
   h.assertTrue((com.mohymods.kingdomsmp.placement.PlacementProtection.check(level,p,from,to)!=null)==expected,"Configured static-identity fallback exactly matches native per-cell OPAC policy");
  }finally{if(added)ids.remove(p.getUUID());}
  ClaimRuntimeTests.opacUnclaim(level,foreign);ClaimRuntimeTests.opacClaim(level,p.getUUID(),foreign);
  h.assertTrue(p.startRiding(aircraft,true),"Restored native entity permissions allow existing aircraft without replacement");p.stopRiding();aircraft.discard();p.getInventory().clearContent();
 }
 private static Entity craft(GameTestHelper h,BlockPos pos){var type=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("immersive_aircraft:biplane"));var entity=type.create(h.getLevel());h.assertTrue(entity!=null&&AircraftAccess.aircraft(entity),"Verified actual registered biplane class");entity.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);entity.setNoGravity(true);h.getLevel().addFreshEntity(entity);return entity;}
 private static CommerceView review(ServerPlayer p,String id){var v=CommerceService.handle(p,new CommerceRequest(++requests,1,0,id,1,CommerceRequest.NONE));if(!v.reviewing())throw new IllegalStateException(v.notice());return v;}
 private static void confirm(ServerPlayer p,CommerceView v){CommerceService.handle(p,new CommerceRequest(++requests,2,0,"",1,v.review()));}
 private static void near(ServerPlayer p,Entity npc){p.moveTo(npc.getX()-.5,npc.getY(),npc.getZ()-1.5);}
 @FunctionalInterface private interface Step{void run()throws Exception;}
 private static void guard(GameTestHelper h,Step s){try{s.run();}catch(Exception e){com.mojang.logging.LogUtils.getLogger().error("Aircraft native assertion failed",e);h.runAfterDelay(1,()->h.fail(e.toString()));}}
}
