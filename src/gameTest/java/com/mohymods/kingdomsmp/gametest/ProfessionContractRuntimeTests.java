package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
@GameTestHolder("kingdom_smp_profession_tests") @PrefixGameTestTemplate(false)
public final class ProfessionContractRuntimeTests {
 private static long requests;
 @GameTest(template="empty",timeoutTicks=6000)
 public static void actualEnrollmentPerkAndDeliveries(GameTestHelper h)throws Exception{CommerceRuntimeTests.completeCommerceLoop(h,()->{try{roles(h);}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}});}
 private static void roles(GameTestHelper h)throws Exception{
  var server=h.getLevel().getServer();var hall=ConstructionService.lodge(server);var p=server.getPlayerList().getPlayer(hall.owner());var home=hall.origin().offset(BlueprintRegistry.get(hall.blueprint(),1).home());p.moveTo(home.getX()+.5,home.getY(),home.getZ()-2.5);var s=CommerceService.store(server);long money=s.balance(p.getUUID());
  var steward=(com.mohymods.kingdomsmp.npc.ReeveEntity)h.getLevel().getEntity(hall.npc());near(p,steward);CommerceService.openResident(p,steward);
  confirm(p,review(p,7,"learn:farmer",1),7,"learn:farmer");h.assertTrue(s.professions(p.getUUID()).isActive(Profession.FARMER)&&s.balance(p.getUUID())==money,"Actual enrollment into first slot is persisted and free");
  h.assertTrue(ProfessionService.allowance(p,"wheat",2000,20000)==2500&&ProfessionService.allowance(p,"stone",2000,20000)==2000,"Only matching active profession changes allowance; per-unit rates unchanged");
  boolean blocked=false;try{review(p,7,"learn:miner",1);}catch(IllegalStateException expected){blocked=true;}h.assertTrue(blocked,"No unbuilt profession enrollment through manipulated selected ID");
  var farm=ConstructionService.store(server).projects().stream().filter(v->v.blueprint().equals(ProfessionBlueprints.FARMER)).findFirst().orElseThrow();var fh=farm.origin().offset(BlueprintRegistry.get(farm.blueprint(),1).home());p.moveTo(fh.getX()+.5,fh.getY(),fh.getZ()-1.5);
  var resident=(com.mohymods.kingdomsmp.npc.ReeveEntity)h.getLevel().getEntity(farm.npc());near(p,resident);CommerceService.openResident(p,resident);
  give(p,Items.WHEAT,64);var stale=review(p,8,"farmer/daily",1);p.getInventory().getItem(0).shrink(1);blocked=false;try{confirm(p,stale,8,"farmer/daily");}catch(IllegalStateException expected){blocked=true;}h.assertTrue(blocked&&count(p,Items.WHEAT)==63&&!s.contractCompleted(p.getUUID(),"farmer","daily",ContractRules.period("daily")),"Changed native inventory consumes/awards nothing");
  give(p,Items.WHEAT,64);var delivery=review(p,8,"farmer/daily",1);confirm(p,delivery,8,"farmer/daily");h.assertTrue(count(p,Items.WHEAT)==0&&s.contractPoolSpent("daily",ContractRules.period("daily"))==80,"Exactly64actualwheat consumed with durable80coin pool debit");
  blocked=false;try{confirm(p,delivery,8,"farmer/daily");}catch(IllegalStateException expected){blocked=true;}h.assertTrue(blocked,"Duplicate contract click cannot issue another reward");
  blocked=false;try{review(p,8,"farmer/daily",1);}catch(IllegalStateException expected){blocked=true;}h.assertTrue(blocked,"Fresh nonce cannot repeat the same profession/day");
  give(p,Items.WHEAT,512);confirm(p,review(p,8,"farmer/weekly",1),8,"farmer/weekly");h.assertTrue(count(p,Items.WHEAT)==0&&s.contractPoolSpent("weekly",ContractRules.period("weekly"))==800,"Weekly delivery uses exact multi-stack inventory removal and separate persistent pool");
  for(var mail:s.mailbox(p.getUUID()).stream().filter(v->v.kind()==EconomyStore.MailKind.COINS&&!v.claimed()).toList())confirm(p,review(p,2,mail.id().toString(),1),2,mail.id().toString());h.assertTrue(s.balance(p.getUUID())==money+880,"Both funded rewards claimed once through the actual mailbox");CommerceService.verifyReceipt(p);
  var reopened=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(reopened.professions(p.getUUID()).equals(s.professions(p.getUUID()))&&reopened.contractCompleted(p.getUUID(),"farmer","daily",ContractRules.period("daily"))&&reopened.balance(p.getUUID())==money+880,"Roster, native receipt completion, pools and rewards survive authoritative reopen");
  h.assertTrue(s.professions(p.getUUID()).rank(Profession.FARMER,ProfessionService.quests())==ProfessionRank.APPRENTICE,"Economic contracts cannot fabricate unapproved progression/Master rank");h.succeed();com.mojang.logging.LogUtils.getLogger().info("PROFESSION_CONTRACT_ASSERTIONS_PASSED: actual enrollment, matching allowance, locked premises, stale inventory/replay refusals,64and512native deliveries,880fundedmail, retained roster/pools/reopen, no fake rank");
 }
 private static void near(ServerPlayer p,net.minecraft.world.entity.Entity npc){
  var level=p.serverLevel();for(var pos:net.minecraft.core.BlockPos.betweenClosed(npc.blockPosition().offset(-2,0,-2),npc.blockPosition().offset(2,0,2))){if(!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir()||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))continue;p.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5);if(p.hasLineOfSight(npc)&&p.distanceToSqr(npc)<25)return;}throw new IllegalStateException("No valid nearby resident interaction position");
 }
 private static void give(ServerPlayer p,Item item,int count){p.getInventory().clearContent();for(int slot=0;count>0;slot++){int n=Math.min(count,64);p.getInventory().setItem(slot,new ItemStack(item,n));count-=n;}}
 private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
 private static CommerceView review(ServerPlayer p,int tab,String id,int count){CommerceService.handle(p,new CommerceRequest(++requests,1,tab,id,count,CommerceRequest.NONE));var view=CommerceService.snapshot(p,++requests,tab,"");if(!view.reviewing())throw new IllegalStateException("No review: "+view.notice());return view;}
 private static void confirm(ServerPlayer p,CommerceView view,int tab,String id){CommerceService.handle(p,new CommerceRequest(++requests,2,tab,id,1,view.review()));}
}
