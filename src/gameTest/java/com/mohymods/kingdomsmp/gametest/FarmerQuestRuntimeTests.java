package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.nio.file.*;
import java.util.*;

@GameTestHolder("kingdom_smp_farmer_tests") @PrefixGameTestTemplate(false)
public final class FarmerQuestRuntimeTests {
 @GameTest(template="empty",timeoutTicks=6000)
 public static void actualHoeSeedsHarvestAndCanonicalQuest(GameTestHelper h)throws Exception{
  CommerceRuntimeTests.completeCommerceLoop(h,()->guard(h,()->begin(h)));
 }
 private static void begin(GameTestHelper h)throws Exception{
  var server=h.getLevel().getServer();var hall=ConstructionService.lodge(server);var p=server.getPlayerList().getPlayer(hall.owner());var npc=(com.mohymods.kingdomsmp.npc.ReeveEntity)h.getLevel().getEntity(hall.npc());near(p,npc);CommerceService.openResident(p,npc);
  var q=ProfessionService.review(p,"learn:farmer");ProfessionService.confirm(p,q.nonce());
  h.assertTrue(ProfessionService.entries(p).stream().anyMatch(row->row.id().equals("quest:"+FarmerQuests.id(ProfessionRank.APPRENTICE,1))&&row.state().equals("IN_PROGRESS"))&&ProfessionService.entries(p).stream().anyMatch(row->row.id().equals("quest:"+FarmerQuests.id(ProfessionRank.JOURNEYMAN,1))&&row.state().equals("LOCKED")),"Royal interface distinguishes available unfinished objectives from genuinely gated future bands");
  var rules=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-farmer-quests.json");FarmerQuestRules.get(server);String json=Files.readString(rules);Files.writeString(rules,json.replace("\"a01\": 32","\"a01\": 2").replace("\"a02\": 128","\"a02\": 2"));FarmerQuestRules.clear(server);
  h.assertTrue(FarmerQuestRules.get(server).goal("a01")==2&&FarmerQuestRules.get(server).goal("a02")==2,"Native fixture uses the real configurable quantity protocol");
  var level=h.getLevel();var soil=h.absolutePos(new BlockPos(-12,2,-12));
  for(int n=0;n<3;n++){var cell=soil.east(n);level.setBlock(cell,Blocks.DIRT.defaultBlockState(),3);for(int y=1;y<4;y++)level.setBlock(cell.above(y),Blocks.AIR.defaultBlockState(),3);}
  p.moveTo(soil.getX()+1.5,soil.getY()+1,soil.getZ()+2.5);p.getInventory().clearContent();p.getInventory().selected=0;p.getInventory().setItem(0,new ItemStack(Items.IRON_HOE));
  use(p,soil);use(p,soil.east());
  h.runAfterDelay(3,()->guard(h,()->{
   h.assertTrue(FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"soil")==2,"Actual hoe interactions, deferred successful block state and distinct positions count");
   level.setBlock(soil,Blocks.DIRT.defaultBlockState(),3);use(p,soil);
   p.getInventory().setItem(0,new ItemStack(Items.WHEAT_SEEDS,2));use(p,soil);use(p,soil.east());
   h.assertTrue(p.getInventory().getItem(0).isEmpty(),"Native planting permanently consumes exactly two real seeds");
   h.runAfterDelay(3,()->guard(h,()->{
    h.assertTrue(FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"soil")==2,"Retilling the same soil does not manufacture a third unique cell");
    for(int n=0;n<3;n++){var cell=soil.east(n);level.setBlock(cell,Blocks.FARMLAND.defaultBlockState(),3);level.setBlock(cell.above(),((CropBlock)Blocks.WHEAT).getStateForAge(7),3);}
    p.getInventory().setItem(0,ItemStack.EMPTY);
    for(int n=0;n<3;n++)h.assertTrue(p.gameMode.destroyBlock(soil.east(n).above()),"Native protected block destruction succeeds in the allowed fixture area");
    h.runAfterDelay(3,()->guard(h,()->{
     h.assertTrue(FarmerQuestService.count(p,ProfessionRank.APPRENTICE,"wheat")==2,"Only two genuine planted lifecycles count; unplanted mature copied crop does not");
     h.assertTrue(FarmerQuestService.count(p,ProfessionRank.JOURNEYMAN,"wheat")==0,"No future-band credit");
     var data=FarmerSavedData.get(server);var reopened=FarmerSavedData.load(data.save(new net.minecraft.nbt.CompoundTag(),server.registryAccess()),server.registryAccess());h.assertTrue(reopened.evidence().count(p.getUUID(),ProfessionRank.APPRENTICE,"wheat")==2,"Crop provenance and objective evidence round-trip without invented progress");
     near(p,npc);CommerceService.openResident(p,npc);
     for(int n=1;n<=2;n++){String id=FarmerQuests.id(ProfessionRank.APPRENTICE,n);var review=ProfessionService.review(p,"quest:"+id);ProfessionService.confirm(p,review.nonce());}
     var farmer=ConstructionService.store(server).projects().stream().filter(v->v.blueprint().equals(ProfessionBlueprints.FARMER)).findFirst().orElseThrow();var farmerNpc=h.getLevel().getEntity(farmer.npc());near(p,farmerNpc);CommerceService.openResident(p,(com.mohymods.kingdomsmp.npc.ReeveEntity)farmerNpc);
     p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(Items.BREAD,32));var food=ProfessionService.review(p,"food:"+FarmerQuests.id(ProfessionRank.APPRENTICE,7));p.getInventory().getItem(0).shrink(1);boolean stale=false;try{ProfessionService.confirm(p,food.nonce());}catch(IllegalStateException expected){stale=true;}h.assertTrue(stale&&p.getInventory().getItem(0).getCount()==31,"Changed food inventory rejects delivery before mutation");p.getInventory().setItem(0,new ItemStack(Items.BREAD,32));food=ProfessionService.review(p,"food:"+FarmerQuests.id(ProfessionRank.APPRENTICE,7));ProfessionService.confirm(p,food.nonce());h.assertTrue(p.getInventory().getItem(0).isEmpty(),"Actual quest bread consumed through native checkpoint and canonical receipt");
     var store=CommerceService.store(server);h.assertTrue(store.professions(p.getUUID()).track(Profession.FARMER).xp()==300,"Royal profession review commits two event proofs and one consumed-food proof");
     boolean refused=false;try{ProfessionService.review(p,"quest:"+FarmerQuests.id(ProfessionRank.APPRENTICE,2));}catch(IllegalStateException expected){refused=true;}h.assertTrue(refused,"Duplicate quest confirmation cannot award twice");
     var dispatcher=server.getCommands().getDispatcher();String inspect="kingdom admin farmer inspect "+p.getGameProfile().getName()+" kingdom_smp:farmer/a08 "+soil.getX()+" "+soil.getY()+" "+soil.getZ()+" Verified personal crops water access and paths";var stranger=server.getPlayerList().getPlayers().stream().filter(v->!v.getUUID().equals(p.getUUID())).findFirst().orElseThrow();boolean unauthorized=false;try{dispatcher.execute(inspect,stranger.createCommandSourceStack().withPermission(0));}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){unauthorized=true;}h.assertTrue(unauthorized,"Ordinary command source cannot access administrator quest inspection");
     for(int n=0;n<8;n++){var cell=soil.offset(n%4,0,n/4);h.getLevel().setBlock(cell,Blocks.FARMLAND.defaultBlockState(),3);h.getLevel().setBlock(cell.above(),n<4?((CropBlock)Blocks.WHEAT).getStateForAge(1):Blocks.AIR.defaultBlockState(),3);}h.getLevel().setBlock(soil.offset(-1,0,0),Blocks.WATER.defaultBlockState(),3);h.getLevel().setBlock(soil.offset(0,0,-1),Blocks.DIRT_PATH.defaultBlockState(),3);p.moveTo(soil.getX()+1.5,soil.getY()+1,soil.getZ()+2.5);server.getPlayerList().op(p.getGameProfile());
     h.assertTrue(dispatcher.execute(inspect,p.createCommandSourceStack())==1,"Actual administrator review inspects physical farm and current property permissions");h.assertTrue(!store.professions(p.getUUID()).track(Profession.FARMER).completedQuestIds().contains(FarmerQuests.id(ProfessionRank.APPRENTICE,8)),"Inspection preview alone awards no progression");
     String confirmInspection="kingdom admin farmer confirm "+FarmerInspectionService.currentReview(p).orElseThrow();h.assertTrue(dispatcher.execute(confirmInspection,p.createCommandSourceStack())==1,"Exact current inspection is confirmed through real permission-checked command");h.assertTrue(dispatcher.execute(confirmInspection,p.createCommandSourceStack())==0,"Replayed administrator confirmation awards nothing");h.assertTrue(store.professions(p.getUUID()).track(Profession.FARMER).xp()==400,"Actual farm review has one separately attributed canonical XP award");
     var persisted=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(persisted.professions(p.getUUID()).equals(store.professions(p.getUUID()))&&persisted.professions(p.getUUID()).rank(Profession.FARMER,FarmerQuests.catalog())==ProfessionRank.APPRENTICE,"Personal XP/proofs reopen; two objectives cannot unlock Journeyman");
     com.mojang.logging.LogUtils.getLogger().info("FARMER_QUEST_ASSERTIONS_PASSED: real hoe, distinct cells, native seed use, provenance harvest, copied mature refusal, no future-band credit, reviewed canonical XP, stale food refusal, actual bread delivery, replay and reload");h.succeed();
    }));
   }));
  }));
 }
 private static void near(ServerPlayer p,net.minecraft.world.entity.Entity npc){
  var level=p.serverLevel();for(var pos:BlockPos.betweenClosed(npc.blockPosition().offset(-2,0,-2),npc.blockPosition().offset(2,0,2))){if(!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir()||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP))continue;p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);if(p.hasLineOfSight(npc)&&p.distanceToSqr(npc)<25)return;}throw new IllegalStateException("No valid nearby resident interaction position");
 }
 private static void use(ServerPlayer p,BlockPos pos){p.gameMode.useItemOn(p,p.serverLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));}
 @FunctionalInterface private interface Step {void run()throws Exception;}
 private static void guard(GameTestHelper h,Step step){try{step.run();}catch(Exception failure){h.runAfterDelay(1,()->h.fail(failure.toString()));}}
}
