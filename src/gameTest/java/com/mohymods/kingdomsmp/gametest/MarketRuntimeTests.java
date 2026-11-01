package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Actual Tier 2 counters, finite shop stock, native mail claims and retained Tier 3 auction gate. */
@GameTestHolder("kingdom_smp_market_tests") @PrefixGameTestTemplate(false)
public final class MarketRuntimeTests {
    private static long requests;
    private MarketRuntimeTests(){ }
    @GameTest(template="empty",timeoutTicks=6000)
    public static void tierTwoMarket(GameTestHelper h)throws Exception{
        CommerceRuntimeTests.completeCommerceLoop(h,()->{try{market(h);}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}});
    }
    private static void market(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();var hall=ConstructionService.lodge(server);var p=server.getPlayerList().getPlayer(hall.owner());var other=server.getPlayerList().getPlayers().stream().filter(v->!v.getUUID().equals(p.getUUID())).findFirst().orElseThrow();
        var b=BlueprintRegistry.get(hall.blueprint(),hall.blueprintVersion());var home=hall.origin().offset(b.rotate(b.home(),hall.rotation()));
        near(p,home,0);near(other,home,-1);p.getInventory().clearContent();other.getInventory().clearContent();var s=CommerceService.store(server);
        h.assertTrue(com.mohymods.kingdomsmp.contribution.ContributionService.kingdom(server).attainedTier()==2,"Market is actually tested before Tier 3/Bank");
        s.adminSet(UUID.randomUUID(),other.getUUID(),p.getUUID(),2000,"Isolated market purchasing fixture");
        var moved=review(p,1,"shop:iron",8);p.moveTo(home.getX()+100,home.getY(),home.getZ());refused(h,()->confirm(p,moved,1,"shop:iron"),"Counter distance rechecked before stock/payment");near(p,home,0);
        h.assertTrue(s.balance(p.getUUID())==2000&&s.shopSold(MarketService.day(),MarketService.key("iron"))==0,"Denied purchase creates no debit, stock or item delivery");
        var first=review(p,1,"shop:iron",8);confirm(p,first,1,"shop:iron");refused(h,()->confirm(p,first,1,"shop:iron"),"Replayed confirmation cannot purchase twice");
        h.assertTrue(s.balance(p.getUUID())==1872&&s.shopSold(MarketService.day(),MarketService.key("iron"))==8&&count(p,Items.IRON_INGOT)==0,"Purchase debits128 and creates exact private mail without direct inventory mutation");
        var mail=s.itemMailbox(p.getUUID()).stream().filter(m->!m.claimed()).findFirst().orElseThrow();h.assertTrue(s.shopDelivery(mail.id()),"Mail origin is canonical shop operation");
        var claim=review(p,2,mail.id().toString(),1);confirm(p,claim,2,mail.id().toString());
        h.assertTrue(count(p,Items.IRON_INGOT)==8&&s.itemMailbox(p.getUUID()).stream().allMatch(EconomyStore.ItemMail::claimed),"Shop delivery claims exactly once at real Tier 2 Steward");CommerceService.verifyReceipt(p);
        // Sell the purchased stack: even at the best buy rate, it cannot recover its cost.
        var resale=review(p,1,"iron",8);confirm(p,resale,1,"iron");h.assertTrue(count(p,Items.IRON_INGOT)==0&&s.balance(p.getUUID())==1936,"NPC roundtrip loses64; no positive-arbitrage loop");
        confirm(p,review(p,1,"shop:iron",8),1,"shop:iron");h.assertTrue(s.balance(p.getUUID())==1784,"Next eight stock units use19 each, not the initial16");
        var a=review(p,1,"shop:iron",64);var race=review(other,1,"shop:iron",64);confirm(p,a,1,"shop:iron");
        refused(h,()->confirm(other,race,1,"shop:iron"),"Competing buyer cannot consume already reserved final stock");refused(h,()->review(other,1,"shop:iron",1),"Exhausted finite stock rejects without payment");
        h.assertTrue(s.balance(p.getUUID())==1416&&s.shopSold(MarketService.day(),MarketService.key("iron"))==32,"Accepted final16 include both22 and24 bands; only32 units were sold");
        var pendingMail=s.itemMailbox(p.getUUID()).stream().filter(m->!m.claimed()).findFirst().orElseThrow();for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        refused(h,()->review(p,2,pendingMail.id().toString(),1),"Full inventory refuses shop delivery rather than dropping or duplicating it");p.getInventory().clearContent();
        for(var m:s.itemMailbox(p.getUUID()).stream().filter(v->!v.claimed()).toList())confirm(p,review(p,2,m.id().toString(),1),2,m.id().toString());
        h.assertTrue(count(p,Items.IRON_INGOT)==24,"All remaining exact shop deliveries claimed with native receipts");
        // Host-only escrow fixture deliberately has the same NPC-looking title; its origin still requires Bank.
        var item=(CompoundTag)new ItemStack(Items.IRON_INGOT).save(p.registryAccess());UUID listing=UUID.randomUUID(),offline=UUID.randomUUID();long now=System.currentTimeMillis();
        var prepared=s.prepareListing(listing,offline,"Fixture","NPC shop: Iron ingots",10,false,now,now+86400000,item);s.recoverItem(new EconomyStore.Receipt(s.id(),prepared.sequence()));UUID purchase=UUID.randomUUID();s.buyListing(purchase,p.getUUID(),listing,now+1);
        h.assertTrue(s.itemMailbox(p.getUUID()).stream().anyMatch(m->m.id().equals(purchase)&&!m.claimed()),"Gate fixture is an actual owned unclaimed auction delivery");
        h.assertTrue(!s.shopDelivery(purchase),"Identical display title does not forge canonical shop origin");refused(h,()->review(p,2,purchase.toString(),1),"Ordinary auction mail remains Bank Tier 3 gated at Tier 2");
        var restored=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));
        h.assertTrue(restored.shopSold(MarketService.day(),MarketService.key("iron"))==32&&restored.balance(p.getUUID())==1406&&restored.shopCoinsSpent()==648,"Saved stock, exact debit and shop coin sink survive independent reopen");
        h.assertTrue(CommerceService.snapshot(p,++requests,1,"").entries().stream().anyMatch(e->e.id().equals("shop:iron")&&e.state().equals("OUT_OF_STOCK")),"Exhausted shop row visibly locks");
        h.succeed();com.mojang.logging.LogUtils.getLogger().info("MARKET_TEST_ASSERTIONS_PASSED: full preceding Hut/Hall/Farmer loop; actual Tier2 shop/mail, exact marginal prices, finite stock contention, no arbitrage, distance/replay/full-inventory refusals, native receipt reload, retained Bank-gated auction origin");
    }
    private static void near(ServerPlayer p,net.minecraft.core.BlockPos home,int dx){p.moveTo(home.getX()+.5+dx,home.getY(),home.getZ()-2.5);}
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static CommerceRequest request(int action,int tab,String id,int count,UUID nonce){return new CommerceRequest(++requests,action,tab,id,count,nonce);}
    private static CommerceView review(ServerPlayer p,int tab,String id,int count){CommerceService.handle(p,request(1,tab,id,count,CommerceRequest.NONE));var v=CommerceService.snapshot(p,++requests,tab,"");if(!v.reviewing())throw new IllegalStateException("Missing market review: "+v.notice());return v;}
    private static void confirm(ServerPlayer p,CommerceView v,int tab,String id){CommerceService.handle(p,request(2,tab,id,64,v.review()));}
    private static void refused(GameTestHelper h,Runnable action,String message){boolean denied=false;try{action.run();}catch(IllegalStateException|IllegalArgumentException expected){denied=true;}h.assertTrue(denied,message);}
}
