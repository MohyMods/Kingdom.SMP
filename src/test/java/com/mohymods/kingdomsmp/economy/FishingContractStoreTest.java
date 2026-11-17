package com.mohymods.kingdomsmp.economy;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FishingContractStoreTest {
 @TempDir Path dir;final UUID player=UUID.randomUUID(),other=UUID.randomUUID();final String fish="kingdom_smp:fishing_contract/bluegill";
 EconomyStore open()throws IOException{return EconomyStore.open(dir);}
 EconomyStore.PendingItem prepare(EconomyStore s,UUID id,int count)throws Exception{return s.prepareCommunityDelivery(id,player,fish,12,"aquaculture:bluegill",count,100,4,1600);}
 void commit(EconomyStore s,UUID id,int count)throws Exception{var pending=prepare(s,id,count);assertTrue(s.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence())));}
 @Test void interruptedNativeRemovalHasNoIndependentFishReward()throws Exception{
  var s=open();prepare(s,UUID.randomUUID(),64);assertTrue(s.mailbox(player).isEmpty());assertEquals(0,s.fishingContractSpent(12));var reopened=open();assertFalse(reopened.recoverItem(EconomyStore.Receipt.empty()));assertTrue(reopened.communityProject(fish,12).isEmpty());
 }
 @Test void finiteSharedGoalPaysPrivateMailAndDoesNotUseFoodBudgetOrFarmerCredit()throws Exception{
  var s=open();commit(s,UUID.randomUUID(),64);commit(s,UUID.randomUUID(),36);var reopened=open();var project=reopened.communityProject(fish,12).orElseThrow();assertTrue(project.complete());assertEquals(100,project.contributors().get(player).total());assertEquals(0,project.contributors().get(player).master());assertEquals(0,reopened.communityMasterProofs(player));assertEquals(400,reopened.fishingContractSpent(12));assertEquals(0,reopened.communitySpent(12));assertEquals(0,reopened.balance(player));assertEquals(400,reopened.mailbox(player).stream().mapToLong(EconomyStore.Mail::amount).sum());assertThrows(IllegalStateException.class,()->prepare(reopened,UUID.randomUUID(),1));
  var food=reopened.prepareCommunityDelivery(UUID.randomUUID(),other,"kingdom_smp:community/grain",12,"minecraft:wheat",64,100,1,100);assertTrue(reopened.recoverItem(new EconomyStore.Receipt(reopened.id(),food.sequence())));assertEquals(64,reopened.communitySpent(12));assertEquals(400,reopened.fishingContractSpent(12));
 }
 @Test void alteredTermsUnknownFishAndRaisedWeeklyPoolFailWithoutPendingInventory()throws Exception{
  var s=open();commit(s,UUID.randomUUID(),64);assertThrows(IllegalStateException.class,()->s.prepareCommunityDelivery(UUID.randomUUID(),player,fish,12,"aquaculture:bluegill",1,101,4,1600));assertThrows(IllegalStateException.class,()->s.prepareCommunityDelivery(UUID.randomUUID(),other,"kingdom_smp:fishing_contract/catfish",12,"aquaculture:catfish",1,100,9,2000));assertThrows(IllegalArgumentException.class,()->s.prepareCommunityDelivery(UUID.randomUUID(),player,fish,12,"minecraft:cod",1,100,4,1600));assertThrows(IllegalArgumentException.class,()->s.prepareCommunityDelivery(UUID.randomUUID(),player,"kingdom_smp:fishing_contract/forged",12,"aquaculture:bluegill",1,100,4,1600));assertTrue(s.pendingItem().isEmpty());assertEquals(256,s.fishingContractSpent(12));
 }
 @Test void postRenameFailureReopensOneRewardAndRejectsReplay()throws Exception{
  var s=open();var operation=UUID.randomUUID();var pending=prepare(s,operation,64);var failing=EconomyStore.open(dir,(path,nbt)->{AtomicNbt.write(path,nbt);throw new IOException("after atomic rename");});assertThrows(IOException.class,()->failing.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence())));var reopened=open();assertEquals(64,reopened.communityProject(fish,12).orElseThrow().delivered());assertEquals(1,reopened.mailbox(player).size());assertFalse(reopened.recoverItem(new EconomyStore.Receipt(reopened.id(),pending.sequence())));assertThrows(IllegalArgumentException.class,()->prepare(reopened,operation,1));assertEquals(64,reopened.communityProject(fish,12).orElseThrow().delivered());assertEquals(1,reopened.mailbox(player).size());
 }
}
