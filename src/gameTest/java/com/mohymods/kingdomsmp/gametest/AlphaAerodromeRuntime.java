package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.integration.aircraft.AircraftAccess;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.progression.*;
import com.mojang.logging.LogUtils;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.nio.file.*;
import java.util.*;
/** Earned Tier6 fixture only; never writes tier/quest/rank/source counters. */
final class AlphaAerodromeRuntime {
 static long requests;
 static void run(GameTestHelper h)throws Exception {
  var server=h.getLevel().getServer();AircraftAccess.require();h.assertTrue(ContributionService.kingdom(server).attainedTier()==6&&TierTwoService.ledger(server).complete(),"Requires the actual full consumed Tier1–6 fixture");for(int t=3;t<=5;t++)h.assertTrue(EarlyTierService.ledger(server,t).resourcesComplete(),"All eight native quantities at each prior stage");
  var hall=ConstructionService.lodge(server);var p=ClaimRuntimeTests.player(h.getLevel(),hall.owner(),hall.ownerName());var store=ConstructionService.store(server);
  for(var project:store.projects()){var bounds=ConstructionService.bounds(project);for(int x=((int)bounds.minX)>>4;x<=((int)bounds.maxX)>>4;x++)for(int z=((int)bounds.minZ)>>4;z<=((int)bounds.maxZ)>>4;z++)h.getLevel().getChunk(x,z);var definition=BlueprintRegistry.get(project.blueprint(),project.blueprintVersion());h.getLevel().getChunkAt(definition.workPost(project.origin(),project.rotation()));}
  var path=server.getWorldPath(LevelResource.ROOT).resolve("alpha-aerodrome-test.nbt");
  boolean[] initialized={false};
  h.runAfterDelay(40,()->guard(h,()->{
   if(initialized[0])return;initialized[0]=true;
   EarlyTierService.verifyAll(p);var existing=store.reward("market:"+RoyalAerodromeBlueprint.ID);
   if(existing.isPresent()){
    var project=existing.orElseThrow();if(project.phase()==ConstructionStore.Phase.COMPLETE){verify(h,p,project);h.assertTrue(AtomicNbt.read(path).getLong("balance")==CommerceService.store(server).balance(p.getUUID()),"Original paid purse restores with completed building");LogUtils.getLogger().info("ALPHA_AERODROME_RESTART_PASSED: earned Tier6/complete original hangar/sole Corin/native aircraft use/paid purse/native receipts restore; no forced tier");h.succeed();return;}
    h.assertTrue(project.phase()==ConstructionStore.Phase.BUILDING&&project.placed()>=75,"Actual saved partial construction resumes");build(h,p,project,path,false);return;
   }
   h.assertTrue(!AircraftAccess.denial(server).isEmpty(),"Genuinely earned Tier6 alone still refuses aircraft");var craft=craft(h,p.blockPosition().offset(0,1,8));h.assertTrue(!p.startRiding(craft,true),"Actual native forced boarding refuses missing hangar");craft.discard();
   p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("immersive_aircraft:biplane"))));h.assertTrue(NeoForge.EVENT_BUS.post(new PlayerInteractEvent.RightClickItem(p,InteractionHand.MAIN_HAND)).isCanceled()&&p.getMainHandItem().getCount()==1,"Native item deployment refuses without consuming the plane");p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
   var bank=store.projects().stream().filter(v->v.blueprint().equals(RoyalBankBlueprint.ID)).findFirst().orElseThrow();near(p,h.getLevel().getEntity(bank.npc()));CommerceService.openResident(p,(ReeveEntity)h.getLevel().getEntity(bank.npc()));
   var money=CommerceService.store(server);money.adminSet(UUID.randomUUID(),new UUID(0,0),p.getUUID(),15000,"Explicit private Aerodrome financial fixture; kingdom tiers remain earned");
   var review=CommerceService.handle(p,new CommerceRequest(++requests,1,0,RoyalAerodromeBlueprint.ID.toString(),1,CommerceRequest.NONE));h.assertTrue(review.reviewing(),"Actual bank Architect purchase review: "+review.notice());CommerceService.handle(p,new CommerceRequest(++requests,2,0,"",1,review.review()));var project=store.reward("market:"+RoyalAerodromeBlueprint.ID).orElseThrow();
   ConstructionService.select(p,project.id());ConstructionService.confirm(p,ConstructionService.review(p,1).nonce(),2);h.assertTrue(p.getMainHandItem().is(KingdomBlueprints.AERODROME.get()),"Authentic paid physical plan");
   var b=BlueprintRegistry.get(project.blueprint(),1);var site=hall.origin().offset(80,0,40);var rotation=Rotation.CLOCKWISE_90;
   for(var pos:BlockPos.betweenClosed(site.offset(-3,-1,-4),site.offset(b.width(rotation)+3,-1,b.depth(rotation)+3)))h.getLevel().setBlock(pos,Blocks.STONE.defaultBlockState(),18);
   for(int x=site.getX()>>4;x<=(site.getX()+b.width(rotation)-1)>>4;x++)for(int z=(site.getZ()-1)>>4;z<=(site.getZ()+b.depth(rotation)-1)>>4;z++){var pos=new BlockPos(x*16,site.getY(),z*16);ClaimRuntimeTests.ftbClaim(p,pos);ClaimRuntimeTests.opacClaim(h.getLevel(),p.getUUID(),pos);}
   p.moveTo(site.getX()-2,site.getY()+1,site.getZ());var preview=ConstructionService.preview(p,site,rotation);h.assertTrue(preview.denial().isEmpty(),"Authenticated rotated complete footprint: "+preview.denial());ConstructionService.confirm(p,ConstructionService.view(p,1,false,"").review(),4);ConstructionService.confirm(p,ConstructionService.review(p,7).nonce(),8);build(h,p,store.get(project.id()),path,true);
  }));
 }
 static void build(GameTestHelper h,ServerPlayer p,ConstructionStore.Project project,Path path,boolean partial){
  var server=h.getLevel().getServer();var store=ConstructionService.store(server);var b=BlueprintRegistry.get(project.blueprint(),1);var post=b.workPost(project.origin(),project.rotation());var builder=h.getLevel().getEntity(BuilderHutSavedData.get(server).hut().orElseThrow().npcId());builder.moveTo(post.getX()+.5,post.getY(),post.getZ()+.5);p.moveTo(post.getX()+2,post.getY(),post.getZ());boolean[] done={false};
  h.onEachTick(()->{if(done[0])return;guard(h,()->{
   for(int i=0;i<16&&store.get(project.id()).phase()==ConstructionStore.Phase.BUILDING;i++)ConstructionService.step(server,project.id());var current=store.get(project.id());
   h.assertTrue(current.phase()==ConstructionStore.Phase.BUILDING||current.phase()==ConstructionStore.Phase.COMPLETE,"Actual protected construction continues: "+ConstructionService.activity(current.id()));
   if(partial&&current.placed()>=75){h.assertTrue(!AircraftAccess.aerodromeComplete(server),"Partial hangar grants no aircraft access");save(path,CommerceService.store(server).balance(p.getUUID()));done[0]=true;LogUtils.getLogger().info("ALPHA_AERODROME_PARTIAL_PASSED: earned Tier6/actual no-hangar mount+deployment denial/unique paid plan/rotated protected progressive original Aldric; private funds/terrain/time fixture only");h.succeed();}
   else if(current.phase()==ConstructionStore.Phase.COMPLETE){verify(h,p,current);save(path,CommerceService.store(server).balance(p.getUUID()));done[0]=true;LogUtils.getLogger().info("ALPHA_AERODROME_COMPLETE_PASSED: actual partial restart/original prefix/progressive furnished hangar/sole Corin/native boarding/loaded-damage refusal/exact repair/no forced tier");h.succeed();}
  });});
 }
 static void verify(GameTestHelper h,ServerPlayer p,ConstructionStore.Project project)throws Exception {
  var level=h.getLevel();h.assertTrue(ConstructionService.structureIntact(level,project)&&AircraftAccess.denial(p.server).isEmpty(),"Earned Tier6 and complete intact hangar enable actual aircraft");var npc=level.getEntity(project.npc());h.assertTrue(npc instanceof ReeveEntity&&npc.getName().getString().equals("Corin Windlass")&&ConstructionService.registeredReeve((ReeveEntity)npc),"Original permanent Flight Engineer");
  near(p,npc);var craft=craft(h,project.origin().offset(5,1,5));h.assertTrue(p.startRiding(craft,true)&&p.getVehicle()==craft,"Actual native biplane boarding now succeeds");p.stopRiding();
  var b=BlueprintRegistry.get(project.blueprint(),project.blueprintVersion());var block=b.blocks(project.rotation()).getFirst();var pos=project.origin().offset(block.offset());var old=level.getBlockState(pos);level.removeBlock(pos,false);h.assertTrue(!AircraftAccess.aerodromeComplete(p.server)&&!p.startRiding(craft,true),"Real loaded hangar damage revokes boarding without a tier bypass");level.setBlock(pos,old,18);h.assertTrue(AircraftAccess.aerodromeComplete(p.server),"Exact legitimate repair restores original hangar eligibility");craft.discard();
 }
 static Entity craft(GameTestHelper h,BlockPos pos){var entity=BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("immersive_aircraft:biplane")).create(h.getLevel());h.assertTrue(entity!=null&&AircraftAccess.aircraft(entity),"Exact installed native biplane class");entity.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);entity.setNoGravity(true);h.getLevel().addFreshEntity(entity);return entity;}
 static void near(ServerPlayer p,Entity npc){p.moveTo(npc.getX()-.5,npc.getY(),npc.getZ()-1.5);}
 static void save(Path path,long balance)throws Exception{var tag=new CompoundTag();tag.putLong("balance",balance);AtomicNbt.write(path,tag);}
 @FunctionalInterface interface Action{void run()throws Exception;}static void guard(GameTestHelper h,Action a){try{a.run();}catch(Exception e){h.fail(e.toString());}}
}
