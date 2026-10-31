package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.persistence.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

/** Real preceding gameplay plus recoverable escrow, component delivery and contested purchase. */
@GameTestHolder("kingdom_smp_auction_tests") @PrefixGameTestTemplate(false)
public final class AuctionRuntimeTests {
    private static long requests;
    @GameTest(template="empty",timeoutTicks=7000)
    public static void fixedPriceLoop(GameTestHelper h)throws Exception{
        BankRuntimeTests.completeBankLoop(h,()->{try{auction(h);}catch(Exception e){h.runAfterDelay(1,()->h.fail(e.toString()));}});
    }
    private static void auction(GameTestHelper h)throws Exception{
        var server=h.getLevel().getServer();var bank=ConstructionService.store(server).projects().stream().filter(p->p.blueprint().equals(RoyalBankBlueprint.ID)).findFirst().orElseThrow();
        var home=bank.origin().offset(BlueprintRegistry.get(bank.blueprint(),1).home());
        var seller=server.getPlayerList().getPlayer(bank.owner());var buyer=server.getPlayerList().getPlayers().stream().filter(p->p!=seller).findFirst().orElseThrow();
        var third=ClaimRuntimeTests.player(h.getLevel(),"AuctionThird");
        for(var p:List.of(seller,buyer,third)){p.moveTo(home.getX()+.5,home.getY(),home.getZ()-2.5);p.getInventory().clearContent();}
        var banker=(com.mohymods.kingdomsmp.npc.ReeveEntity)h.getLevel().getEntity(bank.npc());
        for(var p:List.of(seller,buyer,third))CommerceService.openResident(p,banker);
        var s=CommerceService.store(server);for(var p:List.of(buyer,third))s.adminSet(UUID.randomUUID(),new UUID(0,0),p.getUUID(),500,"Explicit auction buyer fixture");
        h.assertTrue(!seller.hasPermissions(2)&&!buyer.hasPermissions(2)&&!third.hasPermissions(2),"Native auction peers cannot bypass permissions");
        seller.getInventory().setItem(seller.getInventory().selected,new ItemStack(Items.DIAMOND,16));
        var stale=review(seller,5,"sell",8,100);seller.getMainHandItem().shrink(1);
        refused(h,()->confirm(seller,stale,5),"Stale held inventory refuses before escrow");
        h.assertTrue(s.listings().isEmpty()&&seller.getMainHandItem().getCount()==15,"Stale review has no escrow or extra consumption");
        seller.getInventory().setItem(seller.getInventory().selected,new ItemStack(Items.DIAMOND,16));
        var listed=review(seller,5,"sell",16,100);confirm(seller,listed,5);
        h.assertTrue(seller.getMainHandItem().isEmpty()&&s.listings().size()==1,"Actual native stack removed only after escrow preparation");
        var l=s.listings().getFirst();var a=review(buyer,5,"buy:"+l.id(),1,0);var b=review(third,5,"buy:"+l.id(),1,0);
        confirm(buyer,a,5);refused(h,()->confirm(third,b,5),"Competing last-listing buyer cannot pay twice");refused(h,()->confirm(buyer,a,5),"Repeated purchase confirmation is not a second debit");
        h.assertTrue(s.balance(buyer.getUUID())==400&&s.balance(third.getUUID())==500&&!s.listing(l.id()).orElseThrow().active(),"One first buyer charged, listing sold once");
        var proceeds=s.mailbox(seller.getUUID()).stream().filter(m->m.kind()==EconomyStore.MailKind.COINS&&!m.claimed()).findFirst().orElseThrow();
        h.assertTrue(proceeds.amount()==95&&s.taxRemoved()==5,"Exact sale conserves 100 as 95 proceeds and 5 tax");
        var delivery=s.itemMailbox(buyer.getUUID()).getFirst();refused(h,()->review(third,2,delivery.id().toString(),1,0),"Foreign item mail cannot be claimed");
        for(int i=0;i<36;i++)buyer.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        refused(h,()->review(buyer,2,delivery.id().toString(),1,0),"Full inventory refuses whole delivery with no drops");buyer.getInventory().clearContent();
        var receipt=review(buyer,2,delivery.id().toString(),1,0);confirm(buyer,receipt,2);refused(h,()->confirm(buyer,receipt,2),"Replayed item claim cannot duplicate native items");
        h.assertTrue(count(buyer,Items.DIAMOND)==16&&s.itemMailbox(buyer.getUUID()).getFirst().claimed(),"Exactly sixteen diamonds delivered once");
        CommerceService.handle(seller,new CommerceRequest(++requests,4,5,"",1,CommerceRequest.NONE));
        var coin=review(seller,2,proceeds.id().toString(),1,0);long prior=s.balance(seller.getUUID());confirm(seller,coin,2);
        h.assertTrue(s.balance(seller.getUUID())==prior+95,"Funded seller proceeds claimed once");

        var sword=new ItemStack(Items.DIAMOND_SWORD);sword.set(DataComponents.CUSTOM_NAME,Component.literal("Escrow heirloom"));sword.setDamageValue(17);
        seller.getInventory().setItem(seller.getInventory().selected,sword.copy());confirm(seller,review(seller,5,"sell",1,50),5);
        var named=s.listings().stream().filter(EconomyStore.Listing::active).findFirst().orElseThrow();confirm(seller,review(seller,5,"return:"+named.id(),1,0),5);
        var returned=s.itemMailbox(seller.getUUID()).stream().filter(m->!m.claimed()).findFirst().orElseThrow();confirm(seller,review(seller,2,returned.id().toString(),1,0),2);
        h.assertTrue(ItemStack.matches(sword,seller.getMainHandItem()),"Exact custom name and damage survive listing, cancellation and native delivery");
        refused(h,()->{seller.getInventory().setItem(seller.getInventory().selected,new ItemStack(Items.SHULKER_BOX));review(seller,5,"sell",1,10);},"Nested inventory item is explicitly unsupported");
        seller.getInventory().clearContent();seller.getInventory().setItem(seller.getInventory().selected,new ItemStack(Items.EMERALD));var distance=review(seller,5,"sell",1,10);seller.moveTo(home.getX()+100,home.getY(),home.getZ());refused(h,()->confirm(seller,distance,5),"Escrow confirm rechecks actual Bank distance");seller.moveTo(home.getX()+.5,home.getY(),home.getZ()-2.5);
        var reload=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));
        h.assertTrue(reload.listings().size()==2&&reload.taxRemoved()==5&&reload.balance(buyer.getUUID())==400&&reload.itemMailbox(buyer.getUUID()).getFirst().claimed(),"Native restart projection retains sold/cancelled listing, balance, tax and claimed delivery");
        for(var p:List.of(seller,buyer))CommerceService.verifyReceipt(p);
        h.succeed();com.mojang.logging.LogUtils.getLogger().info("AUCTION_TEST_ASSERTIONS_PASSED: full preceding Bank loop, native escrow, first-buyer contention, one debit, exact 5% tax/proceeds, private/full-inventory/replay guards, component-preserving return, unsupported items, distance and journal reload");
    }
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static CommerceView review(ServerPlayer p,int tab,String id,int qty,long amount){return CommerceService.handle(p,new CommerceRequest(++requests,1,tab,id,qty,CommerceRequest.NONE,amount));}
    private static void confirm(ServerPlayer p,CommerceView v,int tab){if(!v.reviewing())throw new IllegalStateException(v.notice());CommerceService.handle(p,new CommerceRequest(++requests,2,tab,"",1,v.review()));}
    @FunctionalInterface private interface Attempt{void run()throws Exception;}
    private static void refused(GameTestHelper h,Attempt a,String detail)throws Exception{boolean no=false;try{a.run();}catch(IllegalArgumentException|IllegalStateException e){no=true;}h.assertTrue(no,detail);}
}
