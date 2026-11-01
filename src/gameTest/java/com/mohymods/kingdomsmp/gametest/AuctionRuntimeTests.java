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
        timed(h,seller,buyer,third);
        buyOrders(h,seller,buyer,third);
        h.succeed();com.mojang.logging.LogUtils.getLogger().info("AUCTION_TEST_ASSERTIONS_PASSED: full preceding settlement/Bank/fixed/timed loop plus funded buy orders, real partial inventory removals, competing sellers, exact item/proceeds mail, unspent cancellation/expiry, sandbox permissions/audit, 101-row visibility and native receipt reload");
    }
    private static void buyOrders(GameTestHelper h,ServerPlayer seller,ServerPlayer buyer,ServerPlayer other)throws Exception{
        var server=seller.server;var s=CommerceService.store(server);long sellerBefore=s.balance(seller.getUUID()),taxBefore=s.taxRemoved();
        s.adminSet(UUID.randomUUID(),CommerceRequest.NONE,buyer.getUUID(),2000,"Explicit native buy-order QA funding");
        buyer.getInventory().clearContent();buyer.getInventory().setItem(buyer.getInventory().selected,new ItemStack(Items.IRON_INGOT));
        confirm(buyer,review(buyer,6,"create",64,10),6);
        var order=s.orders().stream().filter(EconomyStore.BuyOrder::active).findFirst().orElseThrow();
        h.assertTrue(count(buyer,Items.IRON_INGOT)==1&&s.balance(buyer.getUUID())==1360&&order.fundsHeld()==640,"Order creation preserves sample and reserves all 640 actual coins");
        seller.getInventory().clearContent();seller.getInventory().setItem(seller.getInventory().selected,new ItemStack(Items.IRON_INGOT,32));
        var stale=review(seller,6,"fill:"+order.id(),16,0);seller.getMainHandItem().shrink(1);refused(h,()->confirm(seller,stale,6),"Changed inventory refuses before native partial removal");
        h.assertTrue(s.order(order.id()).orElseThrow().remaining()==64&&count(seller,Items.IRON_INGOT)==31,"Stale fill has no demand or inventory effects");
        seller.getInventory().setItem(seller.getInventory().selected,new ItemStack(Items.IRON_INGOT,32));
        confirm(seller,review(seller,6,"fill:"+order.id(),16,0),6);
        h.assertTrue(count(seller,Items.IRON_INGOT)==16&&s.order(order.id()).orElseThrow().remaining()==48&&s.balance(buyer.getUUID())==1360,"Only accepted sixteen items removed; buyer is not charged again");
        other.getInventory().clearContent();other.getInventory().setItem(other.getInventory().selected,new ItemStack(Items.IRON_INGOT,8));
        var competitor=review(other,6,"fill:"+order.id(),48,0);confirm(seller,review(seller,6,"fill:"+order.id(),16,0),6);
        refused(h,()->confirm(other,competitor,6),"Competing stale seller review cannot consume against changed demand");
        h.assertTrue(count(other,Items.IRON_INGOT)==8,"Losing stale seller retains every item");
        confirm(other,review(other,6,"fill:"+order.id(),16,0),6);
        h.assertTrue(s.order(order.id()).orElseThrow().remaining()==24&&count(other,Items.IRON_INGOT)==0,"Partial fill clamps to the eight real available items");
        confirm(buyer,review(buyer,6,"cancel:"+order.id(),1,0),6);
        var refund=s.mailbox(buyer.getUUID()).stream().filter(m->!m.claimed()&&m.description().equals("Unspent buy order funding")).findFirst().orElseThrow();
        h.assertTrue(refund.amount()==240&&!s.order(order.id()).orElseThrow().active(),"Cancellation returns only 24 unfilled units' funding");
        confirm(buyer,review(buyer,2,refund.id().toString(),1,0),2);
        // Another partially filled order uses the real gated admin expiry path.
        buyer.getInventory().setItem(buyer.getInventory().selected,new ItemStack(Items.GLASS));
        confirm(buyer,review(buyer,6,"create",16,10),6);var expiry=s.orders().stream().filter(EconomyStore.BuyOrder::active).findFirst().orElseThrow();
        seller.getInventory().setItem(seller.getInventory().selected,new ItemStack(Items.GLASS,4));confirm(seller,review(seller,6,"fill:"+expiry.id(),4,0),6);
        String command="kingdom admin orders expire-test "+expiry.id()+" confirm Native partial-order deadline audit";
        var marker=server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-sandbox.enabled");java.nio.file.Files.deleteIfExists(marker);
        h.assertTrue(server.getCommands().getDispatcher().execute(command,server.createCommandSourceStack())==0&&s.order(expiry.id()).orElseThrow().active(),"Order expiry test requires explicit isolated marker");
        java.nio.file.Files.writeString(marker,"Isolated native order fixture");boolean denied=false;try{server.getCommands().getDispatcher().execute(command,buyer.createCommandSourceStack());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException expected){denied=true;}h.assertTrue(denied,"Ordinary player cannot accelerate order expiry");
        h.assertTrue(server.getCommands().getDispatcher().execute(command,server.createCommandSourceStack())==1,"Confirmed isolated partial expiry succeeds");
        var expiredRefund=s.mailbox(buyer.getUUID()).stream().filter(m->!m.claimed()&&m.description().equals("Unspent buy order funding")).findFirst().orElseThrow();h.assertTrue(expiredRefund.amount()==120,"Expiry refunds unspent twelve units, not the original sixteen");
        confirm(buyer,review(buyer,2,expiredRefund.id().toString(),1,0),2);
        var items=s.itemMailbox(buyer.getUUID()).stream().filter(m->!m.claimed()).toList();
        for(int i=0;i<36;i++)buyer.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));refused(h,()->review(buyer,2,items.getFirst().id().toString(),1,0),"Full inventory keeps buy-order item mail safely unclaimed");buyer.getInventory().clearContent();buyer.getInventory().setItem(0,new ItemStack(Items.GLASS));
        for(var item:items)confirm(buyer,review(buyer,2,item.id().toString(),1,0),2);
        for(var p:List.of(seller,other))for(var coins:s.mailbox(p.getUUID()).stream().filter(m->!m.claimed()&&m.kind()==EconomyStore.MailKind.COINS).toList())confirm(p,review(p,2,coins.id().toString(),1,0),2);
        h.assertTrue(count(buyer,Items.IRON_INGOT)==40&&count(buyer,Items.GLASS)==5&&s.balance(buyer.getUUID())==1560,"Buyer receives exactly forty iron and four glass, plus its unchanged sample, and only unspent refunds");
        h.assertTrue(s.taxRemoved()==taxBefore+22,"Each actual partial sale removes its exact five-percent rounded tax");
        CommerceService.verifyReceipt(buyer);CommerceService.verifyReceipt(seller);CommerceService.verifyReceipt(other);
        var disk=EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));h.assertTrue(disk.balance(buyer.getUUID())==1560&&disk.orders().size()==2&&disk.orders().stream().noneMatch(EconomyStore.BuyOrder::active),"Actual native filled/refunded orders reload without lost funding");
        var history=AtomicNbt.read(server.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_economy.dat")).getList("history",net.minecraft.nbt.Tag.TAG_COMPOUND);boolean audit=false;for(int i=0;i<history.size();i++)if(history.getCompound(i).getCompound("payload").getString("sandboxReason").equals("Native partial-order deadline audit"))audit=true;h.assertTrue(audit,"Order expiry reason persists canonically");
        // Explicit off-line catalogue fixture proves the full bounded network projection is visible, not silently truncated.
        long now=System.currentTimeMillis();var template=(net.minecraft.nbt.CompoundTag)new ItemStack(Items.WHEAT).save(server.registryAccess());
        for(int i=0;i<100;i++){UUID owner=UUID.randomUUID();s.adminSet(UUID.randomUUID(),CommerceRequest.NONE,owner,1,"Explicit catalogue visibility fixture");s.createOrder(UUID.randomUUID(),owner,"Fixture"+i,"Wheat order",template,1,1,false,now,now+3_600_000);}
        var view=CommerceService.snapshot(buyer,9999,6,"");h.assertTrue(view.entries().size()==101,"One hundred real funded orders plus create action survive bounded projection for client pagination");
    }
    private static void timed(GameTestHelper h,ServerPlayer seller,ServerPlayer buyer,ServerPlayer other)throws Exception{
        var s=CommerceService.store(seller.server);long sellerBefore=s.balance(seller.getUUID()),buyerBefore=s.balance(buyer.getUUID()),otherBefore=s.balance(other.getUUID()),taxBefore=s.taxRemoved();
        seller.getInventory().setItem(seller.getInventory().selected,new ItemStack(Items.EMERALD,8));
        confirm(seller,review(seller,5,"sell_timed",8,100),5);
        var listing=s.listings().stream().filter(v->v.active()&&v.timed()).findFirst().orElseThrow();
        h.assertTrue(seller.getMainHandItem().isEmpty()&&listing.expires()-listing.created()==86_400_000,"Actual timed inventory escrow is removed with a native receipt and exact 24-hour deadline");
        var first=review(buyer,5,"bid:"+listing.id(),1,100);confirm(buyer,first,5);
        h.assertTrue(s.balance(buyer.getUUID())==buyerBefore-100&&s.listing(listing.id()).orElseThrow().bidder().equals(buyer.getUUID()),"Reviewed bid fully funds a real leading account");
        refused(h,()->review(seller,5,"return:"+listing.id(),1,0),"Seller cannot cancel after an actual funded bid");
        refused(h,()->review(other,5,"bid:"+listing.id(),1,104),"Server refuses less than five-percent next bid");
        confirm(other,review(other,5,"bid:"+listing.id(),1,105),5);
        var refund=s.mailbox(buyer.getUUID()).stream().filter(m->!m.claimed()&&m.description().equals("Outbid auction refund")).findFirst().orElseThrow();
        h.assertTrue(s.balance(buyer.getUUID())==buyerBefore-100&&refund.amount()==100,"Outbid coins become a funded private mail entitlement");
        confirm(buyer,review(buyer,2,refund.id().toString(),1,0),2);
        h.assertTrue(s.balance(buyer.getUUID())==buyerBefore,"Outbid native peer claims refund exactly once");
        var raise=review(other,5,"bid:"+listing.id(),1,111);confirm(other,raise,5);refused(h,()->confirm(other,raise,5),"Replayed leading raise cannot debit twice");
        h.assertTrue(s.balance(other.getUUID())==otherBefore-111,"Current winner's raise debits only six additional coins");
        var disk=EconomyStore.open(seller.server.getWorldPath(LevelResource.ROOT).resolve("data"));
        h.assertTrue(disk.listing(listing.id()).orElseThrow().bid()==111&&disk.balance(other.getUUID())==otherBefore-111,"Journal reload retains exact funded winner before expiry");
        // Real guarded admin testing route; marker is created only in this isolated native fixture.
        String command="kingdom admin auctions settle-test "+listing.id()+" confirm Native isolated deadline test";
        h.assertTrue(seller.server.getCommands().getDispatcher().execute(command,seller.server.createCommandSourceStack())==0&&s.listing(listing.id()).orElseThrow().active(),"Sandbox deadline command is disabled without its world marker");
        var marker=seller.server.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-sandbox.enabled");java.nio.file.Files.createDirectories(marker.getParent());java.nio.file.Files.writeString(marker,"Isolated native GameTest only");
        boolean ordinary=false;try{seller.server.getCommands().getDispatcher().execute(command,seller.createCommandSourceStack());}catch(com.mojang.brigadier.exceptions.CommandSyntaxException denied){ordinary=true;}
        h.assertTrue(ordinary,"Ordinary player cannot accelerate a funded auction even with sandbox marker");
        h.assertTrue(seller.server.getCommands().getDispatcher().execute(command,seller.server.createCommandSourceStack())==1,"Level-four reviewed sandbox settlement succeeds");
        var history=AtomicNbt.read(seller.server.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_economy.dat")).getList("history",net.minecraft.nbt.Tag.TAG_COMPOUND);
        h.assertTrue(history.getCompound(history.size()-1).getCompound("payload").getString("sandboxReason").equals("Native isolated deadline test"),"Sandbox reason persists inside canonical settlement history");
        h.assertTrue(!s.listing(listing.id()).orElseThrow().active()&&s.balance(other.getUUID())==otherBefore-111,"Deadline settlement performs no second winner debit");
        var proceeds=s.mailbox(seller.getUUID()).stream().filter(m->!m.claimed()&&m.description().equals("Timed auction proceeds")).findFirst().orElseThrow();
        h.assertTrue(proceeds.amount()==105&&s.taxRemoved()==taxBefore+6,"111 funded coins settle as 105 proceeds and six removed tax");
        confirm(seller,review(seller,2,proceeds.id().toString(),1,0),2);
        var mail=s.itemMailbox(other.getUUID()).stream().filter(m->!m.claimed()).findFirst().orElseThrow();confirm(other,review(other,2,mail.id().toString(),1,0),2);
        h.assertTrue(count(other,Items.EMERALD)==8&&s.balance(seller.getUUID())==sellerBefore+105,"Actual winner receives exactly eight escrowed emeralds and seller105 coins");
        AuctionService.maintain(seller.server,listing.expires()+1);
        h.assertTrue(s.itemMailbox(other.getUUID()).stream().allMatch(EconomyStore.ItemMail::claimed)&&count(other,Items.EMERALD)==8,"Repeated expiry maintenance does not duplicate winner items");
        CommerceService.verifyReceipt(other);CommerceService.verifyReceipt(seller);
    }
    private static int count(ServerPlayer p,Item item){int n=0;for(int i=0;i<36;i++)if(p.getInventory().getItem(i).is(item))n+=p.getInventory().getItem(i).getCount();return n;}
    private static CommerceView review(ServerPlayer p,int tab,String id,int qty,long amount){return CommerceService.handle(p,new CommerceRequest(++requests,1,tab,id,qty,CommerceRequest.NONE,amount));}
    private static void confirm(ServerPlayer p,CommerceView v,int tab){if(!v.reviewing())throw new IllegalStateException(v.notice());CommerceService.handle(p,new CommerceRequest(++requests,2,tab,"",1,v.review()));}
    @FunctionalInterface private interface Attempt{void run()throws Exception;}
    private static void refused(GameTestHelper h,Attempt a,String detail)throws Exception{boolean no=false;try{a.run();}catch(IllegalArgumentException|IllegalStateException e){no=true;}h.assertTrue(no,detail);}
}
