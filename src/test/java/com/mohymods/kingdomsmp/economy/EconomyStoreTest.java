package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class EconomyStoreTest {
    @TempDir Path directory;
    final UUID player=UUID.randomUUID(),other=UUID.randomUUID(),admin=UUID.randomUUID();
    EconomyStore open()throws IOException{return EconomyStore.open(directory);}
    void fund(EconomyStore s,UUID p,long amount)throws IOException{s.adminSet(UUID.randomUUID(),admin,p,amount,"Isolated test balance");}
    EconomyStore.PendingSale prepare(EconomyStore s,UUID op,long period)throws IOException{return s.prepareSale(op,player,"minecraft:wheat",64,64,period,20_000,1000,2000);}

    @Test void purchaseCommitsDebitSoldStockAndMailboxTogetherAndReloads()throws Exception{
        var s=open();fund(s,player,1500);UUID op=UUID.randomUUID();
        var p=s.purchase(op,player,"Alice","kingdom_smp:royal_bank",1000);
        assertEquals(500,s.balance(player));assertEquals(1,s.purchases().size());
        assertEquals(p.operation(),s.mailbox(player).getFirst().id());assertEquals(EconomyStore.MailKind.BLUEPRINT,s.mailbox(player).getFirst().kind());
        s=open();assertEquals(p,s.purchase(op,player,"Alice","kingdom_smp:royal_bank",1000));assertEquals(500,s.balance(player));
        assertEquals(2,s.revision());assertEquals(1,s.mailbox(player).size());
    }
    @Test void insufficientAndSoldOutPurchasesDoNotChargeOrWrite()throws Exception{
        var s=open();fund(s,player,999);long before=s.revision();
        assertThrows(IllegalStateException.class,()->s.purchase(UUID.randomUUID(),player,"Alice","kingdom_smp:royal_bank",1000));
        assertEquals(before,s.revision());assertEquals(999,s.balance(player));assertTrue(s.purchases().isEmpty());assertTrue(s.mailbox(player).isEmpty());
        fund(s,other,2000);s.purchase(UUID.randomUUID(),other,"Bob","kingdom_smp:royal_bank",1000);before=s.revision();
        assertThrows(IllegalStateException.class,()->s.purchase(UUID.randomUUID(),player,"Alice","kingdom_smp:royal_bank",500));
        assertEquals(before,s.revision());assertEquals(999,s.balance(player));
    }
    @Test void simultaneousFirstBuyerRaceHasOneWinnerAndOneDebit()throws Exception{
        var s=open();fund(s,player,2000);fund(s,other,2000);var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            List<Future<Boolean>> results=new ArrayList<>();
            for(UUID id:List.of(player,other))results.add(pool.submit(()->{gate.await();try{s.purchase(UUID.randomUUID(),id,"Founder","kingdom_smp:royal_bank",1000);return true;}catch(IllegalStateException sold){return false;}}));
            gate.countDown();assertEquals(1,results.stream().map(f->{try{return f.get();}catch(Exception e){throw new RuntimeException(e);}}).filter(Boolean::booleanValue).count());
        }
        assertEquals(3000,s.balance(player)+s.balance(other));assertEquals(1,s.purchases().size());assertEquals(1,s.mailbox(player).size()+s.mailbox(other).size());
        assertEquals(3000,open().balance(player)+open().balance(other));
    }
    @Test void operationIdentityCannotBeRepurposedAfterRestart()throws Exception{
        var s=open();fund(s,player,2000);UUID op=UUID.randomUUID();s.purchase(op,player,"Alice","kingdom_smp:royal_bank",1000);
        var restored=open();assertThrows(IllegalArgumentException.class,()->restored.purchase(op,player,"Alice","kingdom_smp:royal_bank",1100));
        assertThrows(IllegalArgumentException.class,()->restored.queueCoins(op,player,1000,"Forged retry"));assertEquals(1000,restored.balance(player));
    }
    @Test void coinMailboxEnqueueAndClaimArePersistentAndOwnerBound()throws Exception{
        var s=open();UUID op=UUID.randomUUID();var entry=s.queueCoins(op,player,123,"Contract reward");
        assertEquals(0,s.balance(player));assertEquals(entry,s.queueCoins(op,player,123,"Contract reward"));
        assertThrows(IllegalArgumentException.class,()->s.claimCoins(other,op));assertFalse(s.mailbox(player).getFirst().claimed());
        assertTrue(s.claimCoins(player,op).claimed());long revision=s.revision();var restored=open();assertEquals(123,restored.balance(player));
        assertTrue(restored.claimCoins(player,op).claimed());assertEquals(revision,restored.revision());assertEquals(123,restored.balance(player));
    }
    @Test void uncertainPurchaseBeforeRenameQuarantinesWithoutCharging()throws Exception{
        var s=open();fund(s,player,2000);var failing=EconomyStore.open(directory,(p,t)->{throw new IOException("Before rename");});
        assertThrows(IOException.class,()->failing.purchase(UUID.randomUUID(),player,"Alice","kingdom_smp:royal_bank",1000));assertTrue(failing.quarantined());
        assertThrows(IllegalStateException.class,()->failing.queueCoins(UUID.randomUUID(),player,10,"Retry forbidden"));
        var restored=open();assertEquals(2000,restored.balance(player));assertTrue(restored.purchases().isEmpty());assertTrue(restored.mailbox(player).isEmpty());
    }
    @Test void uncertainPurchaseAfterRenameRecoversExactlyOneEntitlementAndDebit()throws Exception{
        var s=open();fund(s,player,2000);UUID op=UUID.randomUUID();
        var failing=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After rename");});
        assertThrows(IOException.class,()->failing.purchase(op,player,"Alice","kingdom_smp:royal_bank",1000));assertTrue(failing.quarantined());
        var restored=open();restored.purchase(op,player,"Alice","kingdom_smp:royal_bank",1000);
        assertEquals(1000,restored.balance(player));assertEquals(1,restored.purchases().size());assertEquals(1,restored.mailbox(player).size());
    }
    @Test void uncertainMailboxClaimCannotDoubleCredit()throws Exception{
        var s=open();var entry=s.queueCoins(UUID.randomUUID(),player,100,"Quest reward");
        var failing=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After mailbox commit");});
        assertThrows(IOException.class,()->failing.claimCoins(player,entry.id()));
        var restored=open();assertTrue(restored.claimCoins(player,entry.id()).claimed());assertEquals(100,restored.balance(player));assertEquals(2,restored.revision());
    }
    @Test void preparedSaleLocksMoneyUntilDurableNativeReceiptCommits()throws Exception{
        var s=open();fund(s,player,1000);UUID op=UUID.randomUUID();var sale=prepare(s,op,123);
        assertEquals(1000,s.balance(player));assertEquals(0,s.usage(123,"minecraft:wheat",player).sharedSpent());
        assertThrows(IllegalStateException.class,()->s.purchase(UUID.randomUUID(),player,"Alice","kingdom_smp:royal_bank",500));
        assertThrows(IllegalStateException.class,()->s.adminSet(UUID.randomUUID(),admin,player,0,"Concurrent change"));
        var restored=open();assertEquals(sale,restored.pendingSale().orElseThrow());
        assertTrue(restored.recoverSale(new EconomyStore.Receipt(restored.id(),sale.sequence())));assertEquals(1064,restored.balance(player));
        assertEquals(sale.sequence(),restored.expectedReceipt(player));assertEquals(new EconomyStore.Usage(64,64,64),restored.usage(123,"minecraft:wheat",player));
        assertFalse(open().recoverSale(new EconomyStore.Receipt(restored.id(),sale.sequence())));assertEquals(1064,open().balance(player));
        assertThrows(IllegalArgumentException.class,()->prepare(restored,op,123));
    }
    @Test void interruptedSaleBeforeNativeSaveCancelsWithoutMoneyOrQuota()throws Exception{
        var s=open();prepare(s,UUID.randomUUID(),123);s=open();assertFalse(s.recoverSale(EconomyStore.Receipt.empty()));
        assertEquals(0,s.balance(player));assertTrue(s.pendingSale().isEmpty());assertEquals(0,s.revision());assertEquals(0,s.usage(123,"minecraft:wheat",player).resourceQuantity());
    }
    @Test void receiptMismatchDoesNotGuessOrDropPendingOperation()throws Exception{
        var s=open();var sale=prepare(s,UUID.randomUUID(),123);
        assertThrows(IOException.class,()->s.recoverSale(new EconomyStore.Receipt(UUID.randomUUID(),sale.sequence())));
        assertEquals(sale,open().pendingSale().orElseThrow());assertEquals(0,open().balance(player));
    }
    @Test void uncertainSaleCommitAfterRenameCreditsOnlyOnce()throws Exception{
        var s=open();var sale=prepare(s,UUID.randomUUID(),123);UUID store=s.id();
        var failing=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After sale credit");});
        assertThrows(IOException.class,()->failing.recoverSale(new EconomyStore.Receipt(store,sale.sequence())));
        var restored=open();assertEquals(64,restored.balance(player));assertTrue(restored.pendingSale().isEmpty());assertEquals(sale.sequence(),restored.expectedReceipt(player));
    }
    @Test void dailyLimitsUseOneSharedSpendResourceCountAndPersonalCoinAllowance()throws Exception{
        var s=open();var p=prepare(s,UUID.randomUUID(),123);s.recoverSale(new EconomyStore.Receipt(s.id(),p.sequence()));
        assertThrows(IllegalStateException.class,()->s.prepareSale(UUID.randomUUID(),other,"minecraft:iron_ingot",1,10,123,70,100,2000));
        assertThrows(IllegalStateException.class,()->s.prepareSale(UUID.randomUUID(),other,"minecraft:wheat",40,10,123,1000,100,2000));
        assertThrows(IllegalStateException.class,()->s.prepareSale(UUID.randomUUID(),player,"minecraft:iron_ingot",1,10,123,1000,100,70));
        assertThrows(IllegalArgumentException.class,()->prepare(s,UUID.randomUUID(),122));
        assertEquals(new EconomyStore.Usage(0,0,0),s.usage(124,"minecraft:wheat",player));
        var next=prepare(s,UUID.randomUUID(),124);assertEquals(p.sequence(),next.previousReceipt());
    }
    @Test void boundsAndOverflowNeverConsumeMailOrStock()throws Exception{
        var s=open();fund(s,player,EconomyStore.MAX_COINS);var mail=s.queueCoins(UUID.randomUUID(),player,1,"Capacity test");
        assertThrows(IllegalArgumentException.class,()->s.claimCoins(player,mail.id()));assertFalse(s.mailbox(player).getFirst().claimed());
        assertThrows(IllegalArgumentException.class,()->prepare(s,UUID.randomUUID(),123));assertTrue(s.pendingSale().isEmpty());
        assertThrows(IllegalArgumentException.class,()->s.adminSet(UUID.randomUUID(),admin,player,-1,"Invalid"));
        assertThrows(IllegalArgumentException.class,()->s.purchase(UUID.randomUUID(),player,"Alice","kingdom_smp:royal_bank",499));
        assertThrows(IllegalArgumentException.class,()->s.purchase(UUID.randomUUID(),player,"Alice","kingdom_smp:royal_bank",15001));assertTrue(s.purchases().isEmpty());
    }
    @Test void corruptFutureMissingAndMismatchedCheckpointsNeverInitializeNewMoney()throws Exception{
        var s=open();fund(s,player,1000);Path file=directory.resolve("kingdomsmp_economy.dat"),seal=directory.resolve("kingdomsmp_economy.created");
        var good=AtomicNbt.read(file);var future=good.copy();future.putInt("schema",99);AtomicNbt.write(file,future);assertThrows(IOException.class,this::open);
        AtomicNbt.write(file,good);var bad=AtomicNbt.read(seal);bad.putUUID("identity",UUID.randomUUID());AtomicNbt.write(seal,bad);assertThrows(IOException.class,this::open);
        Files.delete(file);assertThrows(IOException.class,this::open);
    }
    @Test void duplicateOperationOnDiskIsRejectedInsteadOfReplayedAsNewMoney()throws Exception{
        var s=open();fund(s,player,1000);Path file=directory.resolve("kingdomsmp_economy.dat");var tag=AtomicNbt.read(file);var history=tag.getList("history",Tag.TAG_COMPOUND);
        var duplicate=history.getCompound(0).copy();duplicate.putLong("sequence",2);history.add(duplicate);tag.putLong("revision",2);AtomicNbt.write(file,tag);assertThrows(IOException.class,this::open);
    }
    @Test void bankWithdrawalReservesBalanceAndCommitsOnlyMatchingNativeReceipt()throws Exception{
        var s=open();fund(s,player,1200);UUID op=UUID.randomUUID();var transfer=s.prepareTransfer(op,player,-1000);
        assertEquals(1200,s.balance(player));assertEquals(0,s.expectedReceipt(player));
        assertThrows(IllegalStateException.class,()->s.purchase(UUID.randomUUID(),player,"Alice","kingdom_smp:royal_bank",500));
        assertThrows(IllegalStateException.class,()->s.queueCoins(UUID.randomUUID(),other,1,"While reserved"));
        assertThrows(IllegalStateException.class,()->prepare(s,UUID.randomUUID(),123));
        var restored=open();assertEquals(transfer,restored.pendingTransfer().orElseThrow());
        assertTrue(restored.recoverTransfer(new EconomyStore.Receipt(restored.id(),transfer.sequence())));
        assertEquals(200,restored.balance(player));assertEquals(transfer.sequence(),restored.expectedReceipt(player));
        assertTrue(restored.pendingTransfer().isEmpty());assertFalse(restored.recoverTransfer(new EconomyStore.Receipt(restored.id(),transfer.sequence())));
        assertEquals(200,open().balance(player));assertThrows(IllegalArgumentException.class,()->restored.prepareTransfer(op,player,-100));
    }
    @Test void bankDepositAndSaleShareOneOrderedNativeReceiptChain()throws Exception{
        var s=open();var sale=prepare(s,UUID.randomUUID(),123);s.recoverSale(new EconomyStore.Receipt(s.id(),sale.sequence()));
        var deposit=s.prepareTransfer(UUID.randomUUID(),player,1011);
        assertEquals(sale.sequence(),deposit.previousReceipt());assertEquals(64,s.balance(player));
        assertTrue(s.recoverTransfer(new EconomyStore.Receipt(s.id(),deposit.sequence())));assertEquals(1075,s.balance(player));
        var second=prepare(s,UUID.randomUUID(),123);assertEquals(deposit.sequence(),second.previousReceipt());
        assertTrue(s.recoverSale(new EconomyStore.Receipt(s.id(),second.sequence())));assertEquals(1139,open().balance(player));
        assertEquals(new EconomyStore.Usage(128,128,128),open().usage(123,"minecraft:wheat",player));
    }
    @Test void bankPreparationBeforeNativeSaveCanCancelWithoutCoinLoss()throws Exception{
        var s=open();fund(s,player,1000);s.prepareTransfer(UUID.randomUUID(),player,-500);
        var restored=open();assertFalse(restored.recoverTransfer(EconomyStore.Receipt.empty()));
        assertEquals(1000,restored.balance(player));assertTrue(restored.pendingTransfer().isEmpty());assertEquals(1,restored.revision());
        var sale=prepare(restored,UUID.randomUUID(),123);restored.recoverSale(new EconomyStore.Receipt(restored.id(),sale.sequence()));
        restored.prepareTransfer(UUID.randomUUID(),player,250);restored=open();
        assertFalse(restored.recoverTransfer(new EconomyStore.Receipt(restored.id(),sale.sequence())));
        assertEquals(1064,restored.balance(player));
    }
    @Test void bankWrongReceiptPreservesPendingForInspection()throws Exception{
        var s=open();var transfer=s.prepareTransfer(UUID.randomUUID(),player,100);
        assertThrows(IOException.class,()->s.recoverTransfer(new EconomyStore.Receipt(UUID.randomUUID(),transfer.sequence())));
        assertThrows(IOException.class,()->s.recoverTransfer(new EconomyStore.Receipt(s.id(),transfer.sequence()+1)));
        assertEquals(transfer,open().pendingTransfer().orElseThrow());assertEquals(0,open().balance(player));
    }
    @Test void bankRejectsInsufficientOrOverflowingFundsWithoutPreparing()throws Exception{
        var s=open();assertThrows(IllegalArgumentException.class,()->s.prepareTransfer(UUID.randomUUID(),player,-1));
        assertThrows(IllegalArgumentException.class,()->s.prepareTransfer(UUID.randomUUID(),player,0));
        assertThrows(IllegalArgumentException.class,()->s.prepareTransfer(UUID.randomUUID(),player,Long.MIN_VALUE));
        fund(s,player,EconomyStore.MAX_COINS);long before=s.revision();
        assertThrows(IllegalArgumentException.class,()->s.prepareTransfer(UUID.randomUUID(),player,1));
        assertEquals(before,s.revision());assertTrue(s.pendingTransfer().isEmpty());assertEquals(EconomyStore.MAX_COINS,s.balance(player));
    }
    @Test void saleReservationAlsoBlocksBankConversions()throws Exception{
        var s=open();prepare(s,UUID.randomUUID(),123);
        assertThrows(IllegalStateException.class,()->s.prepareTransfer(UUID.randomUUID(),player,100));
        assertTrue(s.pendingTransfer().isEmpty());assertTrue(open().pendingSale().isPresent());
    }
    @Test void uncertainBankPreparationRetainsOneRecoverablePendingRecord()throws Exception{
        var s=open();fund(s,player,500);var failing=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After bank prepare");});
        assertThrows(IOException.class,()->failing.prepareTransfer(UUID.randomUUID(),player,-100));assertTrue(failing.quarantined());
        var restored=open();assertTrue(restored.pendingTransfer().isPresent());assertEquals(500,restored.balance(player));
        assertFalse(restored.recoverTransfer(EconomyStore.Receipt.empty()));assertEquals(500,restored.balance(player));
    }
    @Test void uncertainBankCommitCannotDebitOrCreditTwice()throws Exception{
        var s=open();fund(s,player,500);var transfer=s.prepareTransfer(UUID.randomUUID(),player,-100);
        var failing=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After bank debit");});
        assertThrows(IOException.class,()->failing.recoverTransfer(new EconomyStore.Receipt(s.id(),transfer.sequence())));
        var restored=open();assertEquals(400,restored.balance(player));assertTrue(restored.pendingTransfer().isEmpty());
        assertFalse(restored.recoverTransfer(new EconomyStore.Receipt(restored.id(),transfer.sequence())));assertEquals(400,open().balance(player));
        var deposit=restored.prepareTransfer(UUID.randomUUID(),player,200);
        var failedDeposit=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After bank credit");});
        assertThrows(IOException.class,()->failedDeposit.recoverTransfer(new EconomyStore.Receipt(restored.id(),deposit.sequence())));
        assertEquals(600,open().balance(player));assertTrue(open().pendingTransfer().isEmpty());
    }

    @Test void inPersonPaymentIsAtomicAndIdempotentAcrossRestart()throws Exception{
        var s=open();fund(s,player,1000);UUID op=UUID.randomUUID();s.transferCoins(op,player,other,400);
        assertEquals(600,s.balance(player));assertEquals(400,s.balance(other));assertEquals(0,s.expectedReceipt(player));
        var restored=open();restored.transferCoins(op,player,other,400);assertEquals(600,restored.balance(player));assertEquals(400,restored.balance(other));
        assertThrows(IllegalArgumentException.class,()->restored.transferCoins(op,player,other,401));
        assertThrows(IllegalArgumentException.class,()->restored.transferCoins(UUID.randomUUID(),player,player,1));
    }
    @Test void concurrentPaymentsCannotOverdrawTheSender()throws Exception{
        var s=open();fund(s,player,1000);UUID third=UUID.randomUUID();var start=new CountDownLatch(1);
        try(var workers=Executors.newFixedThreadPool(2)){
            List<Future<Boolean>> outcomes=new ArrayList<>();for(var target:List.of(other,third))outcomes.add(workers.submit(()->{start.await();try{s.transferCoins(UUID.randomUUID(),player,target,700);return true;}catch(IllegalStateException insufficient){return false;}}));
            start.countDown();int accepted=0;for(var future:outcomes)if(future.get())accepted++;assertEquals(1,accepted);
        }
        assertEquals(300,s.balance(player));assertEquals(700,s.balance(other)+s.balance(third));assertEquals(1000,open().balance(player)+open().balance(other)+open().balance(third));
    }
    @Test void uncertainAccountTransferAlwaysRecoversWholePaymentOrNothing()throws Exception{
        var s=open();fund(s,player,1000);UUID before=UUID.randomUUID();
        var failBefore=EconomyStore.open(directory,(p,t)->{throw new IOException("Before payment rename");});
        assertThrows(IOException.class,()->failBefore.transferCoins(before,player,other,300));assertEquals(1000,open().balance(player));assertEquals(0,open().balance(other));
        UUID after=UUID.randomUUID();var failAfter=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After payment rename");});
        assertThrows(IOException.class,()->failAfter.transferCoins(after,player,other,300));var restored=open();
        assertEquals(700,restored.balance(player));assertEquals(300,restored.balance(other));restored.transferCoins(after,player,other,300);assertEquals(700,restored.balance(player));
    }
    @Test void paymentRejectsOverflowAndCannotRaceNativeReservation()throws Exception{
        var s=open();fund(s,player,1000);fund(s,other,EconomyStore.MAX_COINS);long revision=s.revision();
        assertThrows(IllegalArgumentException.class,()->s.transferCoins(UUID.randomUUID(),player,other,1));assertEquals(revision,s.revision());assertEquals(1000,s.balance(player));
        s.prepareTransfer(UUID.randomUUID(),player,-500);
        assertThrows(IllegalStateException.class,()->s.transferCoins(UUID.randomUUID(),player,UUID.randomUUID(),1));
        assertEquals(1000,s.balance(player));
    }

}
