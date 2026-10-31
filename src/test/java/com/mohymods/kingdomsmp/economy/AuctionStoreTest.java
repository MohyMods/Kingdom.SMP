package com.mohymods.kingdomsmp.economy;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AuctionStoreTest {
    @TempDir Path directory;
    final UUID seller=UUID.randomUUID(),buyer=UUID.randomUUID(),other=UUID.randomUUID();
    EconomyStore open()throws IOException{return EconomyStore.open(directory);}
    CompoundTag stack(){var t=new CompoundTag();t.putString("id","minecraft:diamond");t.putInt("count",3);var c=new CompoundTag();c.putString("minecraft:custom_name","\"Treasury gems\"");t.put("components",c);return t;}
    void fund(EconomyStore s,UUID player,long n)throws IOException{s.adminSet(UUID.randomUUID(),other,player,n,"Auction test setup");}
    UUID list(EconomyStore s,long price,boolean master)throws IOException{UUID id=UUID.randomUUID();var p=s.prepareListing(id,seller,"Seller","Treasury gems",price,master,1000,100000,stack());assertTrue(s.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));return id;}
    void buy(EconomyStore s,UUID id,UUID op)throws IOException{s.buyListing(op,buyer,id,2000);}

    @Test void nativeEscrowCommitsOnlyAfterMatchingSavedReceiptAndSurvivesRestart()throws Exception{
        var s=open();var item=stack();var p=s.prepareListing(UUID.randomUUID(),seller,"Seller","Gems",100,false,1000,5000,item);item.putInt("count",64);
        assertTrue(s.listings().isEmpty());assertEquals(0,s.expectedReceipt(seller));assertEquals(p,open().pendingItem().orElseThrow());
        var restored=open();assertTrue(restored.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));assertEquals(3,restored.listings().getFirst().item().getInt("count"));
        assertEquals(p.sequence(),restored.expectedReceipt(seller));assertFalse(restored.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));
        assertEquals(1,open().listings().size());assertTrue(open().pendingItem().isEmpty());
    }
    @Test void preNativeInterruptionCancelsWithoutEscrowOrReceipt()throws Exception{
        var s=open();s.prepareListing(UUID.randomUUID(),seller,"Seller","Gems",100,false,1000,5000,stack());
        var restored=open();assertFalse(restored.recoverItem(EconomyStore.Receipt.empty()));assertEquals(0,restored.revision());assertTrue(restored.listings().isEmpty());
    }
    @Test void mismatchedNativeReceiptRetainsPendingAndBlocksMoney()throws Exception{
        var s=open();var p=s.prepareListing(UUID.randomUUID(),seller,"Seller","Gems",100,false,1000,5000,stack());
        assertThrows(IOException.class,()->s.recoverItem(new EconomyStore.Receipt(UUID.randomUUID(),p.sequence())));
        assertThrows(IllegalStateException.class,()->fund(s,buyer,1000));assertThrows(IllegalStateException.class,()->s.queueCoins(UUID.randomUUID(),buyer,10,"Blocked"));
        assertThrows(IllegalStateException.class,()->s.prepareTransfer(UUID.randomUUID(),buyer,1));
        assertThrows(IllegalStateException.class,()->s.prepareSale(UUID.randomUUID(),buyer,"minecraft:wheat",1,1,0,100,100,100));
        assertEquals(p,open().pendingItem().orElseThrow());
    }
    @Test void fixedPurchaseAtomicallyDebitsCreatesFundedProceedsAndExactItemMail()throws Exception{
        var s=open();fund(s,buyer,200);UUID id=list(s,101,false),op=UUID.randomUUID();buy(s,id,op);
        assertEquals(99,s.balance(buyer));assertEquals(0,s.balance(seller));assertFalse(s.listing(id).orElseThrow().active());
        assertEquals(95,s.mailbox(seller).getFirst().amount());assertEquals(6,s.taxRemoved());assertEquals(stack(),s.itemMailbox(buyer).getFirst().item());
        assertTrue(s.itemMailbox(seller).isEmpty());assertEquals(op,s.itemMailbox(buyer).getFirst().id());
        var restored=open();restored.buyListing(op,buyer,id,2000);assertEquals(99,restored.balance(buyer));assertEquals(1,restored.itemMailbox(buyer).size());
        restored.claimCoins(seller,op);assertEquals(95,restored.balance(seller));assertEquals(200,restored.balance(buyer)+restored.balance(seller)+restored.taxRemoved());
    }
    @Test void masterTaxUsesTenPercentAndTinyPriceBurnIsConservative()throws Exception{
        var s=open();fund(s,buyer,200);UUID id=list(s,101,true),op=UUID.randomUUID();buy(s,id,op);
        assertEquals(11,s.taxRemoved());assertEquals(90,s.mailbox(seller).getFirst().amount());
        UUID tiny=list(s,1,false);buy(s,tiny,UUID.randomUUID());assertEquals(12,s.taxRemoved());assertEquals(1,s.mailbox(seller).size());assertEquals(2,s.itemMailbox(buyer).size());
        assertEquals(450_000_000_000L,EconomyStore.tax(EconomyStore.MAX_COINS,false));assertEquals(900_000_000_000L,EconomyStore.tax(EconomyStore.MAX_COINS,true));
    }
    @Test void competingBuyersGetExactlyOneItemAndOneDebit()throws Exception{
        var s=open();fund(s,buyer,200);fund(s,other,200);UUID id=list(s,100,false);var gate=new CountDownLatch(1);
        try(var workers=Executors.newFixedThreadPool(2)){
            List<Future<Boolean>> results=new ArrayList<>();for(var actor:List.of(buyer,other))results.add(workers.submit(()->{gate.await();try{s.buyListing(UUID.randomUUID(),actor,id,2000);return true;}catch(IllegalStateException sold){return false;}}));
            gate.countDown();int accepted=0;for(var result:results)if(result.get())accepted++;assertEquals(1,accepted);
        }
        assertEquals(300,s.balance(buyer)+s.balance(other));assertEquals(1,s.itemMailbox(buyer).size()+s.itemMailbox(other).size());assertEquals(95,s.mailbox(seller).getFirst().amount());assertEquals(5,s.taxRemoved());
        assertEquals(300,open().balance(buyer)+open().balance(other));
    }
    @Test void unavailableInsufficientSelfAndExpiredBuysNeverCharge()throws Exception{
        var s=open();UUID id=list(s,100,false);long rev=s.revision();
        assertThrows(IllegalStateException.class,()->s.buyListing(UUID.randomUUID(),buyer,id,2000));
        assertThrows(IllegalStateException.class,()->s.buyListing(UUID.randomUUID(),seller,id,2000));
        assertEquals(rev,s.revision());fund(s,buyer,200);rev=s.revision();
        assertThrows(IllegalStateException.class,()->s.buyListing(UUID.randomUUID(),buyer,id,100000));assertEquals(rev,s.revision());assertEquals(200,s.balance(buyer));
        assertTrue(s.listing(id).orElseThrow().active());assertTrue(s.itemMailbox(buyer).isEmpty());
    }
    @Test void sellerCancellationReturnsOneExactStackWithoutRefundMint()throws Exception{
        var s=open();UUID id=list(s,100,false),op=UUID.randomUUID();
        assertThrows(IllegalStateException.class,()->s.returnListing(UUID.randomUUID(),other,id,2000));
        s.returnListing(op,seller,id,2000);assertEquals(1,s.itemMailbox(seller).size());assertEquals(stack(),s.itemMailbox(seller).getFirst().item());assertEquals(0,s.balance(seller));
        var restored=open();restored.returnListing(op,seller,id,2000);assertEquals(1,restored.itemMailbox(seller).size());
        assertThrows(IllegalStateException.class,()->restored.returnListing(UUID.randomUUID(),seller,id,2000));assertFalse(restored.listing(id).orElseThrow().active());
    }
    @Test void expiryReturnMayRunOfflineAndNeverReopensListing()throws Exception{
        var s=open();UUID id=list(s,100,false),op=UUID.randomUUID();s.returnListing(op,other,id,100000);
        var restored=open();assertEquals(1,restored.itemMailbox(seller).size());assertFalse(restored.listing(id).orElseThrow().active());
        assertThrows(IllegalStateException.class,()->restored.buyListing(UUID.randomUUID(),buyer,id,99999));
    }
    @Test void itemClaimIsOwnerBoundNativeAndExactlyOnceAfterReload()throws Exception{
        var s=open();fund(s,buyer,200);UUID id=list(s,100,false),mail=UUID.randomUUID();buy(s,id,mail);
        assertThrows(IllegalStateException.class,()->s.prepareItemClaim(UUID.randomUUID(),other,mail));
        var p=s.prepareItemClaim(UUID.randomUUID(),buyer,mail);assertFalse(s.itemMailbox(buyer).getFirst().claimed());
        var restored=open();assertTrue(restored.recoverItem(new EconomyStore.Receipt(restored.id(),p.sequence())));assertTrue(restored.itemMailbox(buyer).getFirst().claimed());
        assertEquals(p.sequence(),restored.expectedReceipt(buyer));assertFalse(restored.recoverItem(new EconomyStore.Receipt(restored.id(),p.sequence())));
        assertThrows(IllegalStateException.class,()->restored.prepareItemClaim(UUID.randomUUID(),buyer,mail));assertEquals(100,restored.balance(buyer));
    }
    @Test void itemDeliveryPreparationCanCancelBeforeSaveWithoutLosingMail()throws Exception{
        var s=open();UUID id=list(s,100,false),mail=UUID.randomUUID();s.returnListing(mail,seller,id,2000);s.prepareItemClaim(UUID.randomUUID(),seller,mail);
        var restored=open();long previous=restored.expectedReceipt(seller);assertFalse(restored.recoverItem(new EconomyStore.Receipt(restored.id(),previous)));assertFalse(restored.itemMailbox(seller).getFirst().claimed());
        assertTrue(restored.pendingItem().isEmpty());assertEquals(0,restored.balance(seller));
    }
    @Test void returnedPayloadCopiesCannotMutateCanonicalEscrowOrMail()throws Exception{
        var s=open();UUID id=list(s,100,false);s.listing(id).orElseThrow().item().putInt("count",64);assertEquals(3,s.listing(id).orElseThrow().item().getInt("count"));
        UUID mail=UUID.randomUUID();s.returnListing(mail,seller,id,2000);s.itemMailbox(seller).getFirst().item().putInt("count",64);assertEquals(3,open().itemMailbox(seller).getFirst().item().getInt("count"));
        var p=s.prepareItemClaim(UUID.randomUUID(),seller,mail);p.payload().putString("kind","ADMIN_SET");assertEquals(p,open().pendingItem().orElseThrow());
    }
    @Test void oversizedInvalidAndEmptyStacksNeverPrepareOrWrite()throws Exception{
        var s=open();var bad=stack();bad.putInt("count",65);assertThrows(IllegalArgumentException.class,()->s.prepareListing(UUID.randomUUID(),seller,"Seller","Bad",100,false,0,1,bad));
        bad.putInt("count",0);assertThrows(IllegalArgumentException.class,()->s.prepareListing(UUID.randomUUID(),seller,"Seller","Bad",100,false,0,1,bad));
        bad.putInt("count",1);bad.putString("id","minecraft:air");assertThrows(IllegalArgumentException.class,()->s.prepareListing(UUID.randomUUID(),seller,"Seller","Bad",100,false,0,1,bad));
        var huge=stack();huge.putByteArray("unknown",new byte[65536]);assertThrows(IllegalArgumentException.class,()->s.prepareListing(UUID.randomUUID(),seller,"Seller","Huge",100,false,0,1,huge));assertEquals(0,s.revision());assertTrue(s.pendingItem().isEmpty());
    }
    @Test void failedPurchaseBeforeOrAfterRenameRecoversWholeSaleOnly()throws Exception{
        var s=open();fund(s,buyer,200);UUID id=list(s,100,false),op=UUID.randomUUID();
        var before=EconomyStore.open(directory,(p,t)->{throw new IOException("Before rename");});assertThrows(IOException.class,()->before.buyListing(op,buyer,id,2000));assertTrue(before.quarantined());
        assertEquals(200,open().balance(buyer));assertTrue(open().listing(id).orElseThrow().active());assertTrue(open().itemMailbox(buyer).isEmpty());
        var after=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After rename");});assertThrows(IOException.class,()->after.buyListing(op,buyer,id,2000));
        var restored=open();restored.buyListing(op,buyer,id,2000);assertEquals(100,restored.balance(buyer));assertEquals(95,restored.mailbox(seller).getFirst().amount());assertEquals(1,restored.itemMailbox(buyer).size());
    }
    @Test void failedItemPreparationAfterRenameHasRecoverablePreNativeRecord()throws Exception{
        open();var failing=EconomyStore.open(directory,(p,t)->{AtomicNbt.write(p,t);throw new IOException("After preparation");});
        assertThrows(IOException.class,()->failing.prepareListing(UUID.randomUUID(),seller,"Seller","Gems",100,false,1000,5000,stack()));assertTrue(failing.quarantined());
        var restored=open();assertTrue(restored.pendingItem().isPresent());assertFalse(restored.recoverItem(EconomyStore.Receipt.empty()));assertTrue(restored.listings().isEmpty());
    }
    @Test void failedEscrowCommitAfterRenameCannotDuplicateDeposit()throws Exception{
        var s=open();var p=s.prepareListing(UUID.randomUUID(),seller,"Seller","Gems",100,false,1000,5000,stack());var failing=EconomyStore.open(directory,(f,t)->{AtomicNbt.write(f,t);throw new IOException("After commit");});
        assertThrows(IOException.class,()->failing.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));var restored=open();assertEquals(1,restored.listings().size());assertTrue(restored.pendingItem().isEmpty());assertEquals(p.sequence(),restored.expectedReceipt(seller));
    }
    @Test void failedMailClaimCommitAfterRenameCannotDeliverAgain()throws Exception{
        var s=open();UUID id=list(s,100,false),mail=UUID.randomUUID();s.returnListing(mail,seller,id,2000);var p=s.prepareItemClaim(UUID.randomUUID(),seller,mail);
        var failing=EconomyStore.open(directory,(f,t)->{AtomicNbt.write(f,t);throw new IOException("After claim");});assertThrows(IOException.class,()->failing.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));
        var restored=open();assertTrue(restored.itemMailbox(seller).getFirst().claimed());assertTrue(restored.pendingItem().isEmpty());assertThrows(IllegalStateException.class,()->restored.prepareItemClaim(UUID.randomUUID(),seller,mail));
    }
    @Test void oldPayloadFreeJournalRemainsByteCompatibleUntilNewAction()throws Exception{
        var s=open();fund(s,buyer,1000);UUID identity=s.id();var file=directory.resolve("kingdomsmp_economy.dat");var before=AtomicNbt.read(file);assertFalse(before.getList("history",net.minecraft.nbt.Tag.TAG_COMPOUND).getCompound(0).contains("payload"));
        var restored=open();assertEquals(identity,restored.id());assertEquals(1000,restored.balance(buyer));assertEquals(before,AtomicNbt.read(file));
        list(restored,100,false);restored=open();assertEquals(identity,restored.id());assertEquals(1000,restored.balance(buyer));assertEquals(1,restored.listings().size());assertEquals(2,restored.revision());
    }
    @Test void nativeItemsAndSalesAndBankUseOneReceiptChain()throws Exception{
        var s=open();UUID id=list(s,100,false);long first=s.expectedReceipt(seller);var bank=s.prepareTransfer(UUID.randomUUID(),seller,100);assertEquals(first,bank.previousReceipt());s.recoverTransfer(new EconomyStore.Receipt(s.id(),bank.sequence()));
        UUID mail=UUID.randomUUID();s.returnListing(mail,seller,id,2000);var claim=s.prepareItemClaim(UUID.randomUUID(),seller,mail);assertEquals(bank.sequence(),claim.previousReceipt());s.recoverItem(new EconomyStore.Receipt(s.id(),claim.sequence()));
        var sale=s.prepareSale(UUID.randomUUID(),seller,"minecraft:wheat",1,1,0,100,100,100);assertEquals(claim.sequence(),sale.previousReceipt());s.recoverSale(new EconomyStore.Receipt(s.id(),sale.sequence()));assertEquals(101,open().balance(seller));
    }
    @Test void purchaseOperationCannotBeReusedWithAnotherActorOrListing()throws Exception{
        var s=open();fund(s,buyer,500);UUID id=list(s,100,false),op=UUID.randomUUID();buy(s,id,op);UUID next=list(s,100,false);
        assertThrows(IllegalArgumentException.class,()->s.buyListing(op,buyer,next,2000));assertThrows(IllegalArgumentException.class,()->s.buyListing(op,other,id,2000));assertEquals(400,s.balance(buyer));assertTrue(s.listing(next).orElseThrow().active());
    }
    @Test void aggregateAdmissionLimitPreservesExistingBuyReturnAndClaimRecovery()throws Exception{
        var s=open();fund(s,buyer,1000);var heavy=stack();heavy.putByteArray("payload",new byte[62000]);UUID first=null;int accepted=0;boolean refused=false;
        for(int i=0;i<200;i++){
            UUID id=UUID.randomUUID();try{var p=s.prepareListing(id,seller,"Seller","Bounded escrow",100,false,1000,100000,heavy);s.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence()));if(first==null)first=id;accepted++;}
            catch(IllegalStateException capacity){refused=true;break;}
        }
        assertTrue(refused);assertTrue(accepted>100&&accepted<150);assertFalse(s.quarantined());assertTrue(s.pendingItem().isEmpty());
        var restored=open();assertEquals(accepted,restored.listings().size());UUID purchase=UUID.randomUUID();restored.buyListing(purchase,buyer,first,2000);
        var claim=restored.prepareItemClaim(UUID.randomUUID(),buyer,purchase);restored.recoverItem(new EconomyStore.Receipt(restored.id(),claim.sequence()));restored.claimCoins(seller,purchase);
        UUID second=restored.listings().stream().filter(EconomyStore.Listing::active).findFirst().orElseThrow().id();UUID returned=UUID.randomUUID();restored.returnListing(returned,seller,second,2000);
        var recovery=restored.prepareItemClaim(UUID.randomUUID(),seller,returned);restored.recoverItem(new EconomyStore.Receipt(restored.id(),recovery.sequence()));
        assertEquals(95,restored.balance(seller));assertTrue(restored.itemMailbox(buyer).getFirst().claimed());assertFalse(restored.quarantined());assertEquals(accepted,open().listings().size());
    }

}
