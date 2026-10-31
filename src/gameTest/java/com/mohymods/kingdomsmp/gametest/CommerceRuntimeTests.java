package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.CommerceService;
import com.mohymods.kingdomsmp.economy.EconomyStore;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.npc.*;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Actual services, inventory checkpoints, first-buyer contention and purchased construction. */
@GameTestHolder("kingdom_smp_commerce_tests")
@PrefixGameTestTemplate(false)
public final class CommerceRuntimeTests {
    private static long requests;
    private CommerceRuntimeTests() { }
    @GameTest(template="empty",timeoutTicks=4000)
    public static void completeCommerceLoop(GameTestHelper h)throws Exception{
        completeCommerceLoop(h,()->{h.succeed();com.mojang.logging.LogUtils.getLogger().info("COMMERCE_TEST_ASSERTIONS_PASSED: real earning, exclusive purchase, mailbox, purchased construction and resident");});
    }
    public static void completeCommerceLoop(GameTestHelper h,Runnable completion)throws Exception{
        TownHallRuntimeTests.newWorldLoop(h,()->{try{servicesAndPurchasedBuilding(h,completion);}catch(Exception e){h.runAfterDelay(1,()->h.fail(message(e)));}});
    }
    private static void servicesAndPurchasedBuilding(GameTestHelper h,Runnable completion)throws Exception{
        var level=h.getLevel();var server=level.getServer();var hall=ConstructionService.lodge(server);
        var winner=server.getPlayerList().getPlayer(hall.owner());
        var other=server.getPlayerList().getPlayers().stream().filter(p->!p.getUUID().equals(hall.owner())).findFirst().orElseThrow();
        h.assertTrue(winner!=null&&!winner.hasPermissions(2)&&!other.hasPermissions(2),"Actual native commerce peers have no operator bypass");
        var definition=BlueprintRegistry.get(hall.blueprint(),hall.blueprintVersion());
        var counter=hall.origin().offset(definition.rotate(definition.home(),hall.rotation()));
        near(winner,counter,0);near(other,counter,-1);
        var money=CommerceService.store(server);
        h.assertTrue(money.balance(winner.getUUID())==0,"Early income starts with no fabricated wallet balance");

        // A quote authorizes neither a changed inventory nor a moved player.
        give(winner,Items.WHEAT,64);var changed=review(winner,1,"wheat",64);
        winner.getInventory().getItem(0).shrink(1);
        refused(h,()->confirm(winner,changed,1,"wheat",64),"Changed inventory invalidates reviewed sale");
        h.assertTrue(count(winner,Items.WHEAT)==63&&money.balance(winner.getUUID())==0,"Stale review consumes no items and credits no coins");
        give(winner,Items.WHEAT,64);var first=review(winner,1,"wheat",64);confirm(winner,first,1,"wheat",64);
        h.assertTrue(count(winner,Items.WHEAT)==0&&money.balance(winner.getUUID())==64,"Actual sixty-four wheat consumed for server-derived sixty-four coins");
        verifyNativeInventory(h,winner,money);
        give(winner,Items.WHEAT,1024);var income=review(winner,1,"wheat",1024);confirm(winner,income,1,"wheat",1024);
        h.assertTrue(money.balance(winner.getUUID())==1088&&count(winner,Items.WHEAT)==0,"Pre-Bank supplies orders produce spendable early currency");
        give(winner,Items.WHEAT,16);var distance=review(winner,1,"wheat",16);
        winner.moveTo(counter.getX()+100,counter.getY(),counter.getZ());
        refused(h,()->confirm(winner,distance,1,"wheat",16),"Leaving actual Steward counter invalidates transaction");
        h.assertTrue(count(winner,Items.WHEAT)==16&&money.balance(winner.getUUID())==1088,"Out-of-range confirmation has no financial or inventory effects");
        near(winner,counter,0);winner.getInventory().clearContent();

        String building="kingdom_smp:farmers_house";
        h.assertTrue(CommerceService.catalog(server).building(building).price()==1000,"Isolated default schema-1 Farmer listing costs 1000");
        // Second peer funding is explicit test setup, not evidence of player-earned currency.
        money.adminSet(UUID.randomUUID(),server.getSingleplayerProfile()==null?other.getUUID():server.getSingleplayerProfile().getId(),other.getUUID(),2000,"Isolated second-buyer race fixture");
        var firstBuyer=review(winner,0,building,1);var secondBuyer=review(other,0,building,1);
        confirm(winner,firstBuyer,0,building,1);
        refused(h,()->confirm(other,secondBuyer,0,building,1),"Second first-buyer quote cannot overwrite sold stock");
        refused(h,()->confirm(winner,firstBuyer,0,building,1),"Replayed confirmation cannot charge again");
        h.assertTrue(money.balance(winner.getUUID())==88&&money.balance(other.getUUID())==2000,"Only successful first buyer is charged");
        var sale=money.purchase(building).orElseThrow();
        h.assertTrue(sale.buyer().equals(winner.getUUID())&&money.purchases().size()==1,"One durable primary entitlement and founder");
        var delivery=money.mailbox(winner.getUUID()).stream().filter(m->m.kind()==EconomyStore.MailKind.BLUEPRINT).findFirst().orElseThrow();
        h.assertTrue(delivery.id().equals(sale.operation())&&delivery.building().equals(building),"Private durable mailbox points to the actual winning purchase");
        CommerceService.handle(winner,request(3,2,delivery.id().toString(),1,CommerceRequest.NONE));
        var project=ConstructionService.current(winner);
        h.assertTrue(project.owner().equals(winner.getUUID())&&project.blueprint().toString().equals(building),"Mailbox manages authentic purchased construction entitlement");

        var coinMail=money.queueCoins(UUID.randomUUID(),winner.getUUID(),23,"Isolated mailbox receipt fixture");
        var mailReview=review(winner,2,coinMail.id().toString(),1);confirm(winner,mailReview,2,coinMail.id().toString(),1);
        h.assertTrue(money.balance(winner.getUUID())==111,"Coin delivery credits exact amount once");
        refused(h,()->confirm(winner,mailReview,2,coinMail.id().toString(),1),"Mailbox confirmation replay is refused");
        h.assertTrue(money.claimCoins(winner.getUUID(),coinMail.id()).claimed()&&money.balance(winner.getUUID())==111,"Durable claimed mail cannot duplicate proceeds");

        // Recoverable plan generations confer no extra building rights.
        ConstructionService.select(winner,project.id());
        var claim=ConstructionService.review(winner,1);ConstructionService.confirm(winner,claim.nonce(),2);
        var oldPlan=winner.getMainHandItem().copy();var oldToken=ConstructionService.store(server).get(project.id()).token();
        var recovery=ConstructionService.review(winner,1);ConstructionService.confirm(winner,recovery.nonce(),2);
        h.assertTrue(!oldToken.equals(ConstructionService.store(server).get(project.id()).token()),"Lost-plan recovery rotates permanent authority token");
        var current=winner.getMainHandItem().copy();winner.getInventory().setItem(winner.getInventory().selected,oldPlan);
        h.assertTrue(!ConstructionService.heldPlan(winner,ConstructionService.store(server).get(project.id())),"Copied earlier-generation purchase plan is stale");
        winner.getInventory().setItem(winner.getInventory().selected,current);
        h.assertTrue(ConstructionService.heldPlan(winner,ConstructionService.store(server).get(project.id())),"Recovered current physical plan authenticates");

        var b=BlueprintRegistry.get(project.blueprint(),project.blueprintVersion());var site=hall.origin().offset(0,0,40);
        for(var p:BlockPos.betweenClosed(hall.origin().offset(-5,-1,20),site.offset(b.width()+5,-1,b.depth()+5)))level.setBlock(p,Blocks.STONE.defaultBlockState(),18);
        for(int cx=site.getX()>>4;cx<=(site.getX()+b.width()-1)>>4;cx++)for(int cz=(site.getZ()-1)>>4;cz<=(site.getZ()+b.depth()-1)>>4;cz++){
            var point=new BlockPos(cx*16,site.getY(),cz*16);ClaimRuntimeTests.ftbClaim(winner,point);ClaimRuntimeTests.opacClaim(level,winner.getUUID(),point);
        }
        winner.moveTo(site.getX()-2,site.getY()+1,site.getZ());
        for(var rotation:Rotation.values())h.assertTrue(b.blocks(rotation).size()==b.blocks().size(),"Purchased hologram rotation preserves every physical building block");
        var preview=ConstructionService.preview(winner,site,Rotation.NONE);h.assertTrue(preview.denial().isEmpty(),"Real purchased site passes complete claim-aware validation: "+preview.denial());
        var placement=ConstructionService.view(winner,1,false,"");ConstructionService.confirm(winner,placement.review(),4);
        var begin=ConstructionService.review(winner,7);ConstructionService.confirm(winner,begin.nonce(),8);
        var nativeHut=BuilderHutSavedData.get(server).hut().orElseThrow();var builder=level.getEntity(nativeHut.npcId());
        var post=b.workPost(site,Rotation.NONE);builder.moveTo(post.getX()+.5,post.getY(),post.getZ()+.5);
        boolean[] complete={false};
        h.onEachTick(()->{
            if(complete[0])return;
            try{
                var store=ConstructionService.store(server);
                for(int i=0;i<2&&store.get(project.id()).phase()==ConstructionStore.Phase.BUILDING;i++)ConstructionService.step(server,project.id());
                var saved=store.get(project.id());if(saved.phase()!=ConstructionStore.Phase.COMPLETE)return;
                complete[0]=true;
                h.runAfterDelay(20,()->{
                    try{
                        h.assertTrue(ConstructionService.structureIntact(level,store.get(project.id())),"Purchased furnished Farmer building remains intact after native neighbor updates");
                        for(var block:b.blocks())h.assertTrue(level.getBlockState(site.offset(block.offset())).canSurvive(level,site.offset(block.offset())),"Every purchased furnishing has valid support");
                        var resident=level.getEntity(saved.npc());
                        h.assertTrue(resident instanceof ReeveEntity&&resident.getType()==KingdomNpcs.RESIDENT.get()&&resident.getName().getString().equals(BlueprintRegistry.defaultResident(saved.blueprint())),"One permanent named profession resident with correct identity type");
                        h.assertTrue(level.getEntities(KingdomNpcs.RESIDENT.get(),e->true).size()==1&&level.getEntities(KingdomNpcs.STEWARD.get(),e->true).size()==1&&level.getEntities(KingdomNpcs.REEVE.get(),e->true).isEmpty(),"Purchased resident does not replace Elara or create legacy Mira");
                        var data=server.getWorldPath(LevelResource.ROOT).resolve("data");var restored=EconomyStore.open(data);
                        h.assertTrue(restored.balance(winner.getUUID())==111&&restored.purchase(building).orElseThrow().equals(sale),"Native store reload preserves exact balance, sold stock, buyer and mailbox");
                        h.assertTrue(ConstructionStore.open(data).get(project.id()).equals(store.get(project.id())),"Purchased construction and resident UUID survive independent journal reopen");
                        server.tell(new net.minecraft.server.TickTask(server.getTickCount()+1,completion));
                    }catch(Exception e){h.fail(message(e));}
                });
            }catch(Exception e){complete[0]=true;h.fail(message(e));}
        });
    }
    private static void verifyNativeInventory(GameTestHelper h,ServerPlayer player,EconomyStore money)throws Exception{
        CommerceService.verifyReceipt(player);
        var disk=AtomicNbt.read(player.server.getWorldPath(LevelResource.PLAYER_DATA_DIR).resolve(player.getStringUUID()+".dat"));
        h.assertTrue(disk.hasUUID("UUID")&&disk.getUUID("UUID").equals(player.getUUID())&&disk.getList("Inventory",Tag.TAG_COMPOUND).equals(player.getInventory().save(new ListTag())),"Actual native player file durably matches post-sale inventory");
        h.assertTrue(money.expectedReceipt(player.getUUID())>0,"Accepted native sale has permanent player receipt sequence");
    }
    private static void near(ServerPlayer player,BlockPos counter,int offset){player.moveTo(counter.getX()+.5+offset,counter.getY(),counter.getZ()-2.5);}
    private static void give(ServerPlayer player,Item item,int count){player.getInventory().clearContent();for(int slot=0;count>0;slot++){int n=Math.min(64,count);player.getInventory().setItem(slot,new ItemStack(item,n));count-=n;}}
    private static int count(ServerPlayer player,Item item){int n=0;for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(item))n+=player.getInventory().getItem(i).getCount();return n;}
    private static CommerceRequest request(int action,int tab,String id,int quantity,UUID review){return new CommerceRequest(++requests,action,tab,id,quantity,review);}
    private static CommerceView review(ServerPlayer player,int tab,String id,int quantity){CommerceService.handle(player,request(1,tab,id,quantity,CommerceRequest.NONE));return CommerceService.snapshot(player,++requests,tab,"");}
    private static void confirm(ServerPlayer player,CommerceView view,int tab,String id,int quantity){if(!view.reviewing())throw new IllegalStateException("Server failed to return a commerce review: "+view.notice());CommerceService.handle(player,request(2,tab,id,quantity,view.review()));}
    private static void refused(GameTestHelper h,Runnable run,String reason){boolean denied=false;try{run.run();}catch(IllegalArgumentException|IllegalStateException expected){denied=true;}h.assertTrue(denied,reason);}
    private static String message(Exception e){return e.getMessage()==null?e.toString():e.getMessage();}
}
