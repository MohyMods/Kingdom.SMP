package com.mohymods.kingdomsmp.economy;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import net.minecraft.nbt.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ContractStoreTest {
 @TempDir Path dir;final UUID player=UUID.randomUUID();
 EconomyStore open()throws IOException{return EconomyStore.open(dir);}
 CompoundTag wheat(){var item=new CompoundTag();item.putString("id","minecraft:wheat");item.putInt("count",1);return item;}
 void enroll(EconomyStore s,UUID id)throws IOException{s.changeProfession(UUID.randomUUID(),id,"enroll","","farmer");}
 EconomyStore.PendingItem prepare(EconomyStore s,UUID p,String cycle,long day,long pool)throws IOException{return s.prepareContract(UUID.randomUUID(),p,"farmer",cycle,day,64,80,pool,wheat());}
 void commit(EconomyStore s,EconomyStore.PendingItem pending)throws IOException{assertTrue(s.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence())));}
 @Test void preparedDeliveryDoesNotGrantBeforeNativeReceipt()throws Exception{var s=open();enroll(s,player);var pending=prepare(s,player,"daily",10,2000);assertEquals(0,s.contractPoolSpent("daily",10));assertFalse(s.contractCompleted(player,"farmer","daily",10));assertTrue(s.mailbox(player).isEmpty());assertFalse(s.recoverItem(EconomyStore.Receipt.empty()));assertEquals(0,s.contractPoolSpent("daily",10));assertTrue(open().pendingItem().isEmpty());}
 @Test void nativeReceiptCommitsOnceToPersistentFundedMailAndPool()throws Exception{var s=open();enroll(s,player);var pending=prepare(s,player,"daily",10,2000);commit(s,pending);var r=open();assertTrue(r.contractCompleted(player,"farmer","daily",10));assertEquals(80,r.contractPoolSpent("daily",10));assertEquals(0,r.balance(player));assertEquals(80,r.mailbox(player).getFirst().amount());assertThrows(IllegalStateException.class,()->prepare(r,player,"daily",10,2000));r.claimCoins(player,pending.operation());r.claimCoins(player,pending.operation());assertEquals(80,r.balance(player));assertEquals(pending.sequence(),r.expectedReceipt(player));}
 @Test void daysWeeksAndSharedPoolDoNotResetOnReopen()throws Exception{var s=open();enroll(s,player);commit(s,prepare(s,player,"daily",10,160));UUID other=UUID.randomUUID();enroll(s,other);commit(s,prepare(s,other,"daily",10,160));UUID third=UUID.randomUUID();enroll(s,third);var r=open();assertThrows(IllegalStateException.class,()->prepare(r,third,"daily",10,160));commit(r,prepare(r,player,"daily",11,160));commit(r,prepare(r,player,"weekly",10,160));assertEquals(160,r.contractPoolSpent("daily",10));assertEquals(80,r.contractPoolSpent("daily",11));assertEquals(80,r.contractPoolSpent("weekly",10));}
 @Test void inactiveProfessionAndConcurrentNativeWorkRefuse()throws Exception{var s=open();assertThrows(IllegalStateException.class,()->prepare(s,player,"daily",10,2000));enroll(s,player);var pending=prepare(s,player,"daily",10,2000);assertThrows(IllegalStateException.class,()->s.changeProfession(UUID.randomUUID(),player,"enroll","","builder"));assertThrows(IllegalStateException.class,()->prepare(s,player,"weekly",1,10000));s.recoverItem(EconomyStore.Receipt.empty());assertEquals(1,s.professions(player).active().size());}
 @Test void postRenameFailureRecoversBothClaimAndRewardExactlyOnce()throws Exception{var s=open();enroll(s,player);var pending=prepare(s,player,"daily",10,2000);var fail=EconomyStore.open(dir,(file,data)->{AtomicNbt.write(file,data);throw new IOException("after rename");});assertThrows(IOException.class,()->fail.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence())));var restored=open();assertTrue(restored.pendingItem().isEmpty());assertTrue(restored.contractCompleted(player,"farmer","daily",10));assertEquals(1,restored.mailbox(player).size());assertEquals(80,restored.contractPoolSpent("daily",10));}
 @Test void foreignReceiptDoesNotGuessConsumption()throws Exception{var s=open();enroll(s,player);var pending=prepare(s,player,"daily",10,2000);assertThrows(IOException.class,()->s.recoverItem(new EconomyStore.Receipt(UUID.randomUUID(),pending.sequence())));assertTrue(s.pendingItem().isPresent());assertTrue(s.mailbox(player).isEmpty());assertEquals(0,s.contractPoolSpent("daily",10));}
}
