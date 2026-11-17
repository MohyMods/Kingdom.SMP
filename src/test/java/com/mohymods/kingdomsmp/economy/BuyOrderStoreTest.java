package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class BuyOrderStoreTest {
    @TempDir Path directory;
    final UUID buyer=UUID.randomUUID(),seller=UUID.randomUUID(),other=UUID.randomUUID();
    EconomyStore open()throws IOException{return EconomyStore.open(directory);}
    CompoundTag item(int count){var t=new CompoundTag();t.putString("id","minecraft:iron_ingot");t.putInt("count",count);return t;}
    void fund(EconomyStore s,UUID who,long amount)throws IOException{s.adminSet(UUID.randomUUID(),other,who,amount,"Buy order test funding");}
    UUID order(EconomyStore s,int count,long price)throws IOException{UUID id=UUID.randomUUID();s.createOrder(id,buyer,"Buyer","Iron order",item(1),count,price,false,1000,100000);return id;}
    UUID fill(EconomyStore s,UUID id,int n)throws IOException{UUID op=UUID.randomUUID();var pending=s.prepareOrderFill(op,seller,id,n,item(n),2000);assertTrue(s.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence())));return op;}
    @Test void wholeFundingAndImmutableOrderSurviveReloadWithoutRewritingHistory()throws Exception{
        var s=open();fund(s,buyer,1000);var before=AtomicNbt.read(directory.resolve("kingdomsmp_economy.dat")).getList("history",net.minecraft.nbt.Tag.TAG_COMPOUND).copy();UUID id=order(s,64,10);var r=open();assertEquals(360,r.balance(buyer));assertEquals(640,r.order(id).orElseThrow().fundsHeld());
        var original=r.order(id).orElseThrow().item();original.putInt("count",64);assertEquals(1,r.order(id).orElseThrow().item().getInt("count"));assertEquals(before.getCompound(0),AtomicNbt.read(directory.resolve("kingdomsmp_economy.dat")).getList("history",net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0));
    }
    @Test void partialFillThenCancelReturnsOnlyUnspentFundingAndConservesMoney()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10),sale=fill(s,id,16);assertEquals(48,s.order(id).orElseThrow().remaining());assertEquals(480,s.order(id).orElseThrow().fundsHeld());assertEquals(152,s.mailbox(seller).getFirst().amount());assertEquals(8,s.taxRemoved());assertEquals(16,s.itemMailbox(buyer).getFirst().item().getInt("count"));
        UUID cancel=UUID.randomUUID();s.closeOrder(cancel,buyer,id,3000);assertFalse(s.order(id).orElseThrow().active());assertEquals(0,s.order(id).orElseThrow().fundsHeld());assertEquals(480,s.mailbox(buyer).getFirst().amount());s.claimCoins(buyer,cancel);s.claimCoins(seller,sale);assertEquals(1000,s.balance(buyer)+s.balance(seller)+s.taxRemoved());
        assertThrows(IllegalStateException.class,()->s.closeOrder(UUID.randomUUID(),buyer,id,3001));s.closeOrder(cancel,buyer,id,3000);assertEquals(1,s.itemMailbox(buyer).size());
    }
    @Test void completingSeveralFillsConsumesAllFundingWithoutAnotherBuyerDebit()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);fill(s,id,16);fill(s,id,48);assertEquals(360,s.balance(buyer));assertEquals(0,s.order(id).orElseThrow().remaining());assertFalse(s.order(id).orElseThrow().active());assertEquals(2,s.itemMailbox(buyer).size());assertEquals(608,s.mailbox(seller).stream().mapToLong(EconomyStore.Mail::amount).sum());assertEquals(32,s.taxRemoved());assertThrows(IllegalStateException.class,()->s.closeOrder(UUID.randomUUID(),buyer,id,3000));
    }
    @Test void preparationAndPreRemovalInterruptionDoNotChangeDemandOrPaySeller()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);s.prepareOrderFill(UUID.randomUUID(),seller,id,16,item(16),2000);var r=open();assertEquals(64,r.order(id).orElseThrow().remaining());assertTrue(r.itemMailbox(buyer).isEmpty());assertFalse(r.recoverItem(EconomyStore.Receipt.empty()));assertTrue(r.mailbox(seller).isEmpty());assertEquals(360,r.balance(buyer));
    }
    @Test void exactPostRemovalReceiptCommitsPartialFillOnceAfterRestart()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,32,10);var p=s.prepareOrderFill(UUID.randomUUID(),seller,id,16,item(16),2000);var r=open();assertTrue(r.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));assertFalse(r.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));assertEquals(16,open().order(id).orElseThrow().remaining());assertEquals(1,r.itemMailbox(buyer).size());assertEquals(p.sequence(),r.expectedReceipt(seller));
    }
    @Test void foreignReceiptBlocksRefundPaymentsAndOtherNativeMutations()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);var p=s.prepareOrderFill(UUID.randomUUID(),seller,id,16,item(16),2000);assertThrows(IOException.class,()->s.recoverItem(new EconomyStore.Receipt(UUID.randomUUID(),p.sequence())));assertThrows(IllegalStateException.class,()->s.closeOrder(UUID.randomUUID(),buyer,id,3000));assertThrows(IllegalStateException.class,()->fund(s,seller,100));assertThrows(IllegalStateException.class,()->s.prepareTransfer(UUID.randomUUID(),buyer,1));assertEquals(64,open().order(id).orElseThrow().remaining());
    }
    @Test void namedOrDifferentItemPayloadCannotFillAnExactTemplate()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);var wrong=item(16);wrong.putString("id","minecraft:gold_ingot");assertThrows(IllegalArgumentException.class,()->s.prepareOrderFill(UUID.randomUUID(),seller,id,16,wrong,2000));var named=item(16);var components=new CompoundTag();components.putString("minecraft:custom_name","\"Renamed\"");named.put("components",components);assertThrows(IllegalArgumentException.class,()->s.prepareOrderFill(UUID.randomUUID(),seller,id,16,named,2000));assertTrue(s.pendingItem().isEmpty());assertEquals(64,s.order(id).orElseThrow().remaining());
    }
    @Test void invalidCountsSelfFillAndDeadlineRefuseBeforePreparingRemoval()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,4,10);assertThrows(IllegalStateException.class,()->s.prepareOrderFill(UUID.randomUUID(),buyer,id,1,item(1),2000));assertThrows(IllegalStateException.class,()->s.prepareOrderFill(UUID.randomUUID(),seller,id,5,item(5),2000));assertThrows(IllegalStateException.class,()->s.prepareOrderFill(UUID.randomUUID(),seller,id,1,item(1),100000));assertThrows(IllegalArgumentException.class,()->s.prepareOrderFill(UUID.randomUUID(),seller,id,2,item(1),2000));assertTrue(s.pendingItem().isEmpty());
    }
    @Test void insufficientFundingAndEleventhActiveOrderNeverDebit()throws Exception{
        var s=open();fund(s,buyer,100);assertThrows(IllegalStateException.class,()->order(s,64,10));assertEquals(100,s.balance(buyer));for(int i=0;i<10;i++)order(s,1,1);assertThrows(IllegalStateException.class,()->order(s,1,1));assertEquals(90,s.balance(buyer));var closed=s.orders().getFirst();s.closeOrder(UUID.randomUUID(),buyer,closed.id(),2000);order(s,1,1);assertEquals(10,s.orders().stream().filter(EconomyStore.BuyOrder::active).count());
    }
    @Test void onlyOwnerCanCancelBeforeExpiryAndClockCannotPrecedeLastFill()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);fill(s,id,16);assertThrows(IllegalStateException.class,()->s.closeOrder(UUID.randomUUID(),seller,id,3000));assertThrows(IllegalStateException.class,()->s.closeOrder(UUID.randomUUID(),buyer,id,1999));s.closeOrder(UUID.randomUUID(),other,id,100000);assertEquals(480,s.mailbox(buyer).getFirst().amount());assertEquals(0,s.order(id).orElseThrow().fundsHeld());
    }
    @Test void createAndRefundReplayDoNotDebitOrCreateAnotherMail()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);s.createOrder(id,buyer,"Buyer","Iron order",item(1),64,10,false,1000,100000);assertEquals(360,s.balance(buyer));UUID refund=UUID.randomUUID();s.closeOrder(refund,buyer,id,2000);s.closeOrder(refund,buyer,id,2000);assertEquals(1,s.mailbox(buyer).size());assertEquals(640,s.mailbox(buyer).getFirst().amount());assertThrows(IllegalArgumentException.class,()->s.createOrder(id,buyer,"Buyer","Iron order",item(1),64,11,false,1000,100000));
    }
    @Test void competingSellersCannotConsumeTheLastUnitsTwice()throws Exception{
        var s=open();fund(s,buyer,100);UUID id=order(s,8,10);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);try{List<Future<Boolean>> results=new ArrayList<>();for(var who:List.of(seller,other))results.add(pool.submit(()->{start.await();try{var pending=s.prepareOrderFill(UUID.randomUUID(),who,id,8,item(8),2000);s.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence()));return true;}catch(IllegalStateException e){return false;}}));start.countDown();int accepted=0;for(var r:results)if(r.get())accepted++;assertEquals(1,accepted);assertFalse(s.order(id).orElseThrow().active());assertEquals(1,s.itemMailbox(buyer).size());assertEquals(76,s.mailbox(seller).stream().mapToLong(EconomyStore.Mail::amount).sum()+s.mailbox(other).stream().mapToLong(EconomyStore.Mail::amount).sum());}finally{pool.shutdownNow();}
    }
    @Test void beforeAndAfterCreateRenamePreserveWholeFundingAndOwnership()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=UUID.randomUUID();var before=EconomyStore.open(directory,(p,t)->{throw new IOException("Before create rename");});assertThrows(IOException.class,()->before.createOrder(id,buyer,"Buyer","Order",item(1),64,10,false,1000,100000));assertEquals(1000,open().balance(buyer));assertTrue(open().orders().isEmpty());var after=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After create rename");});assertThrows(IOException.class,()->after.createOrder(id,buyer,"Buyer","Order",item(1),64,10,false,1000,100000));var r=open();r.createOrder(id,buyer,"Buyer","Order",item(1),64,10,false,1000,100000);assertEquals(360,r.balance(buyer));assertEquals(1,r.orders().size());
    }
    @Test void interruptedNativeFillCommitNeverDuplicatesBuyerDelivery()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);var p=s.prepareOrderFill(UUID.randomUUID(),seller,id,16,item(16),2000);var failing=EconomyStore.open(directory,(f,t)->{AtomicNbt.write(f,t);throw new IOException("After fill rename");});assertThrows(IOException.class,()->failing.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));var r=open();assertEquals(48,r.order(id).orElseThrow().remaining());assertEquals(1,r.itemMailbox(buyer).size());assertEquals(152,r.mailbox(seller).getFirst().amount());assertFalse(r.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));
    }
    @Test void fullRecipientPursesDoNotBlockFillOrUnspentRefund()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);fund(s,buyer,EconomyStore.MAX_COINS);fund(s,seller,EconomyStore.MAX_COINS);fill(s,id,16);s.closeOrder(UUID.randomUUID(),buyer,id,3000);assertEquals(152,s.mailbox(seller).getFirst().amount());assertEquals(480,s.mailbox(buyer).getFirst().amount());assertThrows(IllegalArgumentException.class,()->s.claimCoins(buyer,s.mailbox(buyer).getFirst().id()));assertFalse(s.mailbox(buyer).getFirst().claimed());
    }
    @Test void taxOnEachPartialSaleIsExplicitAndMasterRateRemainsTenPercent()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=UUID.randomUUID();s.createOrder(id,buyer,"Buyer","Master test",item(1),10,10,true,1000,100000);fill(s,id,3);fill(s,id,7);assertEquals(10,s.taxRemoved());assertEquals(90,s.mailbox(seller).stream().mapToLong(EconomyStore.Mail::amount).sum());
    }
    @Test void displayPriorityIsHighestUnitPriceThenOldestThenStableId()throws Exception{
        var s=open();fund(s,buyer,1000);UUID low=order(s,1,5),old=order(s,1,10),later=UUID.randomUUID();s.createOrder(later,buyer,"Buyer","Later",item(1),1,10,false,1001,100000);var rows=s.orders();assertEquals(List.of(old,later,low),rows.stream().map(EconomyStore.BuyOrder::id).toList());assertEquals(rows,open().orders());
    }
    @Test void orderFillAndBankAndItemClaimsShareOneReceiptChain()throws Exception{
        var s=open();fund(s,buyer,1000);UUID id=order(s,64,10);fill(s,id,8);long prior=s.expectedReceipt(seller);var bank=s.prepareTransfer(UUID.randomUUID(),seller,1);assertEquals(prior,bank.previousReceipt());s.recoverTransfer(new EconomyStore.Receipt(s.id(),bank.sequence()));var fill=s.prepareOrderFill(UUID.randomUUID(),seller,id,8,item(8),3000);assertEquals(bank.sequence(),fill.previousReceipt());s.recoverItem(new EconomyStore.Receipt(s.id(),fill.sequence()));assertEquals(fill.sequence(),open().expectedReceipt(seller));
    }
}
