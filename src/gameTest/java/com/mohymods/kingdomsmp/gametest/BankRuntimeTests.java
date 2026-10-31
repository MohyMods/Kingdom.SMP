package com.mohymods.kingdomsmp.gametest;
import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.contribution.ContributionService;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.*;
import com.mohymods.kingdomsmp.progression.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
@GameTestHolder("kingdom_smp_bank_tests") @PrefixGameTestTemplate(false)
public final class BankRuntimeTests {
 private static long requests;
 @GameTest(template="empty",timeoutTicks=6000)
 public static void realTierThreeBank(GameTestHelper h)throws Exception{
  CommerceRuntimeTests.completeCommerceLoop(h,()->{try{advanceAndBuild(h);}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}});
 }
 private static void advanceAndBuild(GameTestHelper h)throws Exception{
  var level=h.getLevel();var server=level.getServer();var hall=ConstructionService.lodge(server);var player=server.getPlayerList().getPlayer(hall.owner());
  var other=server.getPlayerList().getPlayers().stream().filter(p->p!=player).findFirst().orElseThrow();
  h.assertTrue(ContributionService.kingdom(server).attainedTier()==2,"Real transition starts at Tier2, no forced tier");
  for(var r:TierTwoService.Resource.values()){
   int count=TierTwoService.ledger(server).requirements().get(r.id);
   while(count>0){int batch=Math.min(2304,count);give(player,r.item==null?Items.OAK_LOG:r.item,batch);var nonce=TierTwoService.handleReview(player,r.id,batch);TierTwoService.confirm(player,nonce);count-=batch;}
  }
  h.assertTrue(ContributionService.kingdom(server).attainedTier()==2,"Resources alone do not bypass road approval");
  boolean denied=false;try{server.getCommands().getDispatcher().execute("kingdom admin milestone road confirm ordinary-player",player.createCommandSourceStack());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}
  h.assertTrue(denied,"Ordinary player cannot approve own road through command");
  server.getCommands().getDispatcher().execute("kingdom admin milestone road confirm Isolated inspected fixture road",server.createCommandSourceStack());
  h.assertTrue(ContributionService.kingdom(server).attainedTier()==3&&TierTwoService.ledger(server).complete(),"All eight consumed resources plus audited approval reach real Tier3");
  h.assertTrue(ConstructionService.store(server).projects().size()==3,"Tier3 recognition does not grant another contributor blueprint");
  var hb=BlueprintRegistry.get(hall.blueprint(),1);var home=hall.origin().offset(hb.home());near(player,home);
  var money=CommerceService.store(server);money.adminSet(UUID.randomUUID(),new UUID(0,0),player.getUUID(),7000,"Explicit Bank financial test fixture");
  confirm(player,review(player,0,RoyalBankBlueprint.ID.toString(),1),0);
  var project=ConstructionService.store(server).reward("market:"+RoyalBankBlueprint.ID).orElseThrow();ConstructionService.select(player,project.id());
  var claim=ConstructionService.review(player,1);ConstructionService.confirm(player,claim.nonce(),2);
  var b=BlueprintRegistry.get(project.blueprint(),1);var site=hall.origin().offset(-32,0,40);
  for(var pos:BlockPos.betweenClosed(site.offset(-3,-1,-4),site.offset(b.width()+3,-1,b.depth()+3)))level.setBlock(pos,Blocks.STONE.defaultBlockState(),18);
  for(int x=site.getX()>>4;x<=(site.getX()+b.width()-1)>>4;x++)for(int z=(site.getZ()-1)>>4;z<=(site.getZ()+b.depth()-1)>>4;z++){var pos=new BlockPos(x*16,site.getY(),z*16);ClaimRuntimeTests.ftbClaim(player,pos);ClaimRuntimeTests.opacClaim(level,player.getUUID(),pos);}
  player.moveTo(site.getX()-2,site.getY()+1,site.getZ());
  var preview=ConstructionService.preview(player,site,Rotation.NONE);h.assertTrue(preview.denial().isEmpty(),"Bank site validated by both actual claims: "+preview.denial());
  var placement=ConstructionService.view(player,1,false,"");ConstructionService.confirm(player,placement.review(),4);var start=ConstructionService.review(player,7);ConstructionService.confirm(player,start.nonce(),8);
  ConstructionService.closeReview(player);h.assertTrue(ConstructionService.current(player).id().equals(project.id()),"After context loss, K Construction resumes the owned active project");
  var hut=BuilderHutSavedData.get(server).hut().orElseThrow();var builder=level.getEntity(hut.npcId());var post=b.workPost(site,Rotation.NONE);builder.moveTo(post.getX()+.5,post.getY(),post.getZ()+.5);
  boolean[] done={false};h.onEachTick(()->{
   if(done[0])return;
   try{
    var store=ConstructionService.store(server);for(int i=0;i<2&&store.get(project.id()).phase()==ConstructionStore.Phase.BUILDING;i++)ConstructionService.step(server,project.id());
    if(store.get(project.id()).phase()!=ConstructionStore.Phase.COMPLETE)return;done[0]=true;
    h.runAfterDelay(20,()->{try{banking(h,player,other,store.get(project.id()));}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}});
   }catch(Exception e){done[0]=true;h.fail(e.toString());}
  });
 }
 private static void banking(GameTestHelper h,ServerPlayer p,ServerPlayer other,ConstructionStore.Project bank)throws Exception{
  var level=h.getLevel();var b=BlueprintRegistry.get(bank.blueprint(),1);h.assertTrue(ConstructionService.structureIntact(level,bank),"Bank retains all actual required blocks");
  for(var block:b.blocks()){var pos=bank.origin().offset(block.offset());h.assertTrue(level.getBlockState(pos).canSurvive(level,pos),"Supported Bank furnishing: "+pos);}
  var bars=bank.origin().offset(10,1,8);level.setBlock(bars,level.getBlockState(bars).setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST,true),3);
  h.assertTrue(ConstructionService.structureIntact(level,bank),"Normal iron-bar neighbor connections do not disable Bank services");
  var home=bank.origin().offset(b.home());near(p,home);other.moveTo(home.getX()-1.5,home.getY(),home.getZ()-2.5);
  var store=CommerceService.store(p.server);long before=store.balance(p.getUUID());
  for(var coin:CurrencyItems.Denomination.values())confirm(p,review(p,3,coin.id(),1),3);
  long physical=0;for(int i=0;i<36;i++)physical+=CurrencyItems.value(p.getInventory().getItem(i));
  h.assertTrue(physical==1111&&store.balance(p.getUUID())==before-1111,"Four authentic coin denominations withdraw with exact native debit");
  var deposit=review(p,3,"deposit",1);confirm(p,deposit,3);h.assertTrue(store.balance(p.getUUID())==before,"Deposit restores exact value, no roundtrip arbitrage");
  boolean replay=false;try{confirm(p,deposit,3);}catch(IllegalStateException expected){replay=true;}h.assertTrue(replay&&store.balance(p.getUUID())==before,"Replayed deposit cannot mint money");
  long recipient=store.balance(other.getUUID());confirm(p,review(p,3,"transfer:"+other.getUUID(),23),3);
  h.assertTrue(store.balance(p.getUUID())==before-23&&store.balance(other.getUUID())==recipient+23,"Actual nearby Bank payment is atomic on both accounts");
  give(p,Items.COBBLESTONE,2304);boolean full=false;try{review(p,3,"copper",1);}catch(IllegalStateException expected){full=true;}h.assertTrue(full,"Full native inventory cannot receive duplicate/drop-prone coin withdrawal");p.getInventory().clearContent();
  p.containerMenu.setCarried(new ItemStack(Items.COBBLESTONE));boolean cursor=false;try{review(p,3,"copper",1);}catch(IllegalStateException expected){cursor=true;}h.assertTrue(cursor,"A nonempty inventory cursor cannot cross a native coin checkpoint");p.containerMenu.setCarried(ItemStack.EMPTY);
  near(p,home);var distant=review(p,3,"gold",1);p.moveTo(home.getX()+100,home.getY(),home.getZ());boolean range=false;try{confirm(p,distant,3);}catch(IllegalStateException expected){range=true;}h.assertTrue(range,"Bank confirmations recheck actual position");
  near(p,home);CommerceService.verifyReceipt(p);var reopened=EconomyStore.open(p.server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data"));
  h.assertTrue(reopened.balance(p.getUUID())==store.balance(p.getUUID())&&reopened.purchases().size()==2,"Native Bank records reload without extra entitlement or money");
  h.succeed();com.mojang.logging.LogUtils.getLogger().info("BANK_TEST_ASSERTIONS_PASSED: real eight-resource Tier3 transition, road permissions, bought and progressive Bank, physical four-coin withdrawal/deposit, native receipt, no replay/arbitrage, in-person atomic transfer, capacity/distance denials, persistence");
 }
 private static void near(ServerPlayer p,BlockPos home){p.moveTo(home.getX()+.5,home.getY(),home.getZ()-2.5);}
 private static void give(ServerPlayer p,Item item,int count){p.getInventory().clearContent();for(int i=0;count>0;i++){int n=Math.min(count,64);p.getInventory().setItem(i,new ItemStack(item,n));count-=n;}}
 private static CommerceView review(ServerPlayer p,int tab,String id,int qty){return CommerceService.handle(p,new CommerceRequest(++requests,1,tab,id,qty,CommerceRequest.NONE));}
 private static void confirm(ServerPlayer p,CommerceView view,int tab){if(!view.reviewing())throw new IllegalStateException(view.notice());CommerceService.handle(p,new CommerceRequest(++requests,2,tab,"",1,view.review()));}
}
