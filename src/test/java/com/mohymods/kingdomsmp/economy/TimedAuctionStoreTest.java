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

class TimedAuctionStoreTest {
    @TempDir Path directory;
    final UUID seller=UUID.randomUUID(),first=UUID.randomUUID(),second=UUID.randomUUID();
    final long created=1000,end=created+EconomyStore.TIMED_DURATION;
    EconomyStore open()throws IOException{return EconomyStore.open(directory);}
    void fund(EconomyStore s,UUID p,long amount)throws IOException{s.adminSet(UUID.randomUUID(),seller,p,amount,"Timed auction test funding");}
    CompoundTag item(){var t=new CompoundTag();t.putString("id","minecraft:emerald");t.putInt("count",8);return t;}
    UUID list(EconomyStore s,long price,boolean master)throws IOException{
        UUID id=UUID.randomUUID();var p=s.prepareTimedListing(id,seller,"Seller","Eight emeralds",price,master,created,end,item());
        assertTrue(s.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));return id;
    }
    @Test void startsWithExactlyTwentyFourHoursAndPreservesNativeReceipt()throws Exception{
        var s=open();assertThrows(IllegalArgumentException.class,()->s.prepareTimedListing(UUID.randomUUID(),seller,"Seller","Bad",100,false,created,end-1,item()));
        var id=list(s,100,false);var l=open().listing(id).orElseThrow();assertTrue(l.timed());assertNull(l.bidder());assertEquals(end,l.expires());assertEquals(1,open().expectedReceipt(seller));assertEquals(100,EconomyStore.minimumBid(l));
    }
    @Test void pendingTimedEscrowCommitsOrCancelsFromExactNativeReceipt()throws Exception{
        var s=open();var p=s.prepareTimedListing(UUID.randomUUID(),seller,"Seller","Emeralds",100,false,created,end,item());
        var restore=open();assertTrue(restore.pendingItem().isPresent());assertFalse(restore.recoverItem(EconomyStore.Receipt.empty()));assertTrue(restore.listings().isEmpty());
        p=restore.prepareTimedListing(UUID.randomUUID(),seller,"Seller","Emeralds",100,false,created,end,item());var post=new EconomyStore.Receipt(s.id(),p.sequence());restore=open();assertTrue(restore.recoverItem(post));assertEquals(1,restore.listings().size());
    }
    @Test void bidFullyFundsAndOutbidRefundIsPrivateFundedMail()throws Exception{
        var s=open();fund(s,first,1000);fund(s,second,1000);var id=list(s,100,false);
        s.bidListing(UUID.randomUUID(),first,id,100,created+1);UUID bid=UUID.randomUUID();s.bidListing(bid,second,id,105,created+2);
        assertEquals(900,s.balance(first));assertEquals(895,s.balance(second));assertEquals(100,s.mailbox(first).getFirst().amount());assertEquals(first,s.mailbox(first).getFirst().owner());assertEquals(second,s.listing(id).orElseThrow().bidder());
        s.claimCoins(first,bid);s.claimCoins(first,bid);assertEquals(1000,s.balance(first));assertEquals(895,open().balance(second));
    }
    @Test void leadingBidderRaiseDebitsOnlyDifferenceAndDoesNotCreateRefund()throws Exception{
        var s=open();fund(s,first,200);var id=list(s,100,false);s.bidListing(UUID.randomUUID(),first,id,100,created+1);s.bidListing(UUID.randomUUID(),first,id,105,created+2);
        assertEquals(95,s.balance(first));assertTrue(s.mailbox(first).isEmpty());assertEquals(105,open().listing(id).orElseThrow().bid());
    }
    @Test void minimumIncreaseRoundsUpAndTinyBidsAlwaysIncrease()throws Exception{
        var s=open();fund(s,first,1000);var id=list(s,1,false);s.bidListing(UUID.randomUUID(),first,id,1,created+1);assertEquals(2,EconomyStore.minimumBid(s.listing(id).orElseThrow()));
        s.bidListing(UUID.randomUUID(),first,id,101,created+2);assertEquals(107,EconomyStore.minimumBid(s.listing(id).orElseThrow()));
        assertThrows(IllegalStateException.class,()->s.bidListing(UUID.randomUUID(),first,id,106,created+3));assertEquals(899,s.balance(first));
    }
    @Test void rollingAntiSnipeExtendsFromTheActualLateBid()throws Exception{
        var s=open();fund(s,first,1000);fund(s,second,1000);var id=list(s,100,false);
        s.bidListing(UUID.randomUUID(),first,id,100,end-150_000);assertEquals(end,s.listing(id).orElseThrow().expires());
        s.bidListing(UUID.randomUUID(),second,id,105,end-30_000);assertEquals(end+90_000,s.listing(id).orElseThrow().expires());
        s.bidListing(UUID.randomUUID(),first,id,111,end+80_000);assertEquals(end+200_000,open().listing(id).orElseThrow().expires());
    }
    @Test void sellerCannotBidAndNobodyCanBuyTimedOrCancelFundedAuction()throws Exception{
        var s=open();fund(s,seller,1000);fund(s,first,1000);var id=list(s,100,false);
        assertThrows(IllegalStateException.class,()->s.bidListing(UUID.randomUUID(),seller,id,100,created+1));assertThrows(IllegalStateException.class,()->s.buyListing(UUID.randomUUID(),first,id,created+1));
        s.bidListing(UUID.randomUUID(),first,id,100,created+1);assertThrows(IllegalStateException.class,()->s.returnListing(UUID.randomUUID(),seller,id,end));assertEquals(900,s.balance(first));
    }
    @Test void deadlineAndClockRollbackRejectWithoutTakingMoney()throws Exception{
        var s=open();fund(s,first,1000);fund(s,second,1000);var id=list(s,100,false);s.bidListing(UUID.randomUUID(),first,id,100,created+50);
        assertThrows(IllegalStateException.class,()->s.bidListing(UUID.randomUUID(),second,id,105,created+49));assertThrows(IllegalStateException.class,()->s.bidListing(UUID.randomUUID(),second,id,105,end));assertEquals(1000,s.balance(second));
        assertThrows(IllegalStateException.class,()->s.settleTimed(UUID.randomUUID(),seller,id,end-1));
    }
    @Test void expiredWinnerSettlesOnceWithoutAnotherDebitAndRetainsApprovedTax()throws Exception{
        var s=open();fund(s,first,1000);var id=list(s,101,false);s.bidListing(UUID.randomUUID(),first,id,101,created+1);UUID op=UUID.randomUUID();s.settleTimed(op,seller,id,end);s.settleTimed(op,seller,id,end);
        assertEquals(899,s.balance(first));assertEquals(95,s.mailbox(seller).getFirst().amount());assertEquals(6,s.taxRemoved());assertEquals(8,s.itemMailbox(first).getFirst().item().getInt("count"));assertFalse(s.listing(id).orElseThrow().active());
        var restored=open();assertEquals(6,restored.taxRemoved());assertEquals(95,restored.mailbox(seller).getFirst().amount());assertThrows(IllegalStateException.class,()->restored.settleTimed(UUID.randomUUID(),seller,id,end));
    }
    @Test void masterSettlementStillRemovesTenPercentNotAbolishingTax()throws Exception{
        var s=open();fund(s,first,1000);var id=list(s,100,true);s.bidListing(UUID.randomUUID(),first,id,100,created+1);s.settleTimed(UUID.randomUUID(),seller,id,end);
        assertEquals(90,s.mailbox(seller).getFirst().amount());assertEquals(10,s.taxRemoved());assertEquals(900,s.balance(first));
    }
    @Test void noBidCancellationAndExpiryReturnItemsWithNoCoinOrTax()throws Exception{
        var s=open();var a=list(s,100,false);var b=list(s,100,false);s.returnListing(UUID.randomUUID(),seller,a,created+1);s.returnListing(UUID.randomUUID(),first,b,end);
        assertEquals(2,s.itemMailbox(seller).size());assertEquals(0,s.taxRemoved());assertTrue(s.mailbox(seller).isEmpty());
    }
    @Test void insufficientBidCannotDisplaceWinnerOrCreateRefund()throws Exception{
        var s=open();fund(s,first,100);fund(s,second,104);var id=list(s,100,false);s.bidListing(UUID.randomUUID(),first,id,100,created+1);
        assertThrows(IllegalStateException.class,()->s.bidListing(UUID.randomUUID(),second,id,105,created+2));assertEquals(first,s.listing(id).orElseThrow().bidder());assertTrue(s.mailbox(first).isEmpty());assertEquals(104,s.balance(second));
    }
    @Test void bidReplayDoesNotDebitOrRefundTwice()throws Exception{
        var s=open();fund(s,first,1000);fund(s,second,1000);var id=list(s,100,false);s.bidListing(UUID.randomUUID(),first,id,100,created+1);UUID op=UUID.randomUUID();s.bidListing(op,second,id,105,created+2);s.bidListing(op,second,id,105,created+2);
        assertEquals(895,s.balance(second));assertEquals(1,s.mailbox(first).size());assertThrows(IllegalArgumentException.class,()->s.bidListing(op,second,id,111,created+2));
    }
    @Test void contestedSameMinimumOnlyOneBidderFundsTheLead()throws Exception{
        var s=open();fund(s,first,1000);fund(s,second,1000);var id=list(s,100,false);var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{List<Future<Boolean>> results=new ArrayList<>();for(UUID p:List.of(first,second))results.add(pool.submit(()->{start.await();try{s.bidListing(UUID.randomUUID(),p,id,100,created+1);return true;}catch(IllegalStateException e){return false;}}));start.countDown();int accepted=0;for(var f:results)if(f.get())accepted++;assertEquals(1,accepted);assertEquals(1900,s.balance(first)+s.balance(second));}finally{pool.shutdownNow();}
    }
    @Test void crashBeforeOrAfterBidRenamePreservesWholeFundingAndRefund()throws Exception{
        var s=open();fund(s,first,1000);fund(s,second,1000);var id=list(s,100,false);s.bidListing(UUID.randomUUID(),first,id,100,created+1);UUID op=UUID.randomUUID();
        var before=EconomyStore.open(directory,(p,t)->{throw new IOException("Before bid rename");});assertThrows(IOException.class,()->before.bidListing(op,second,id,105,created+2));assertEquals(1000,open().balance(second));assertEquals(first,open().listing(id).orElseThrow().bidder());
        var after=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After bid rename");});assertThrows(IOException.class,()->after.bidListing(op,second,id,105,created+2));var r=open();r.bidListing(op,second,id,105,created+2);assertEquals(895,r.balance(second));assertEquals(100,r.mailbox(first).getFirst().amount());
    }
    @Test void interruptedSettlementCreatesAllDeliveriesOrNoneAndReplaysSafely()throws Exception{
        var s=open();fund(s,first,1000);var id=list(s,100,false);s.bidListing(UUID.randomUUID(),first,id,100,created+1);UUID op=UUID.randomUUID();
        var before=EconomyStore.open(directory,(p,t)->{throw new IOException("Before settlement rename");});assertThrows(IOException.class,()->before.settleTimed(op,seller,id,end));assertTrue(open().listing(id).orElseThrow().active());assertTrue(open().itemMailbox(first).isEmpty());
        var after=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After settlement rename");});assertThrows(IOException.class,()->after.settleTimed(op,seller,id,end));var r=open();r.settleTimed(op,seller,id,end);assertEquals(95,r.mailbox(seller).getFirst().amount());assertEquals(1,r.itemMailbox(first).size());assertEquals(900,r.balance(first));assertEquals(5,r.taxRemoved());
    }
    @Test void refundAndSettlementDoNotFailWhenRecipientPurseIsAlreadyFull()throws Exception{
        var s=open();fund(s,first,1000);fund(s,second,1000);var id=list(s,100,false);s.bidListing(UUID.randomUUID(),first,id,100,created+1);fund(s,first,EconomyStore.MAX_COINS);fund(s,seller,EconomyStore.MAX_COINS);
        s.bidListing(UUID.randomUUID(),second,id,105,created+2);s.settleTimed(UUID.randomUUID(),seller,id,end);assertEquals(100,s.mailbox(first).getFirst().amount());assertEquals(99,s.mailbox(seller).getFirst().amount());assertEquals(1,s.itemMailbox(second).size());
        assertThrows(IllegalArgumentException.class,()->s.claimCoins(first,s.mailbox(first).getFirst().id()));assertFalse(s.mailbox(first).getFirst().claimed());
    }
}
