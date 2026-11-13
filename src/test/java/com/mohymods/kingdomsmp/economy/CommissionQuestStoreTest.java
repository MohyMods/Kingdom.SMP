package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.profession.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CommissionQuestStoreTest {
    @TempDir Path directory;
    final UUID player=UUID.randomUUID();
    String quest(Profession role){return role.questId(ProfessionRank.APPRENTICE,7);}
    void deliver(EconomyStore store,Profession role,String cycle,long period)throws Exception{
        var item=new CompoundTag();item.putString("id",role==Profession.BUILDER?"minecraft:cobblestone":"minecraft:oak_log");item.putInt("count",1);
        var pending=store.prepareContract(UUID.randomUUID(),player,role.id(),cycle,period,64,80,2000,item);
        assertTrue(store.recoverItem(new EconomyStore.Receipt(store.id(),pending.sequence())));
    }
    EconomyStore enroll()throws Exception{
        var store=EconomyStore.open(directory);
        store.changeProfession(UUID.randomUUID(),player,"enroll","","builder");
        store.changeProfession(UUID.randomUUID(),player,"enroll","","lumberjack");return store;
    }
    @Test void threeDistinctPeriodsEarnExactlyOnePersonalAwardWithoutCoinsOrRankFabrication()throws Exception{
        var s=enroll();for(long day=100;day<103;day++)deliver(s,Profession.BUILDER,"daily",day);
        long revision=s.revision();var operation=UUID.randomUUID();
        s.completeCommissionQuest(operation,player,quest(Profession.BUILDER),3);
        assertEquals(revision+1,s.revision());assertEquals(100,s.professions(player).track(Profession.BUILDER).xp());
        assertEquals(0,s.professions(player).track(Profession.LUMBERJACK).xp());assertEquals(0,s.balance(player));assertEquals(3,s.mailbox(player).size());
        s.completeCommissionQuest(operation,player,quest(Profession.BUILDER),3);assertEquals(revision+1,s.revision());
        assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,quest(Profession.BUILDER),3));
        assertEquals(ProfessionRank.APPRENTICE,s.professions(player).rank(Profession.BUILDER,CommissionQuests.catalog()));
        s.archive(player,"Commission earned proof archive");var reopened=EconomyStore.open(directory);
        assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());assertEquals(3,reopened.mailbox(player).size());
    }
    @Test void foreignWeeklyMissingAndUnacknowledgedProofsCannotSatisfyDailyQuest()throws Exception{
        var s=enroll();for(long day=100;day<103;day++)deliver(s,Profession.LUMBERJACK,"daily",day);
        deliver(s,Profession.BUILDER,"weekly",100);deliver(s,Profession.BUILDER,"daily",100);
        long rev=s.revision();assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,quest(Profession.BUILDER),3));assertEquals(rev,s.revision());
        assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),UUID.randomUUID(),quest(Profession.LUMBERJACK),3));
        assertThrows(IllegalArgumentException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,"kingdom_smp:builder/a01",1));
        assertThrows(IllegalArgumentException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,quest(Profession.BUILDER),0));
        assertEquals(0,s.professions(player).track(Profession.BUILDER).xp());
    }
    @Test void inactiveRoleAndConflictingOperationReplayAreRejected()throws Exception{
        var s=enroll();for(long day=100;day<103;day++)deliver(s,Profession.BUILDER,"daily",day);
        var operation=UUID.randomUUID();s.completeCommissionQuest(operation,player,quest(Profession.BUILDER),3);
        assertThrows(IllegalArgumentException.class,()->s.completeCommissionQuest(operation,player,quest(Profession.LUMBERJACK),3));
        for(long day=100;day<103;day++)deliver(s,Profession.LUMBERJACK,"daily",day);
        s.adminSet(UUID.randomUUID(),player,player,15000,"Private inactive-proof fixture funding");
        s.changeProfession(UUID.randomUUID(),player,"switch","lumberjack","farmer");
        assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,quest(Profession.LUMBERJACK),3));
    }
    @Test void uncertainAfterRenameRestartsWithExactlyOneAward()throws Exception{
        var s=enroll();for(long day=100;day<103;day++)deliver(s,Profession.BUILDER,"daily",day);
        var operation=UUID.randomUUID();var faulty=EconomyStore.open(directory,(p,data)->{AtomicNbt.write(p,data);throw new IOException("Private after-rename quest fault");});
        assertThrows(IOException.class,()->faulty.completeCommissionQuest(operation,player,quest(Profession.BUILDER),3));
        var reopened=EconomyStore.open(directory);reopened.completeCommissionQuest(operation,player,quest(Profession.BUILDER),3);
        assertEquals(100,reopened.professions(player).track(Profession.BUILDER).xp());assertEquals(3,reopened.mailbox(player).size());
    }
    void earnedLumberjackApprenticeModel(EconomyStore s)throws Exception{
        // Pure canonical model fixture; actual native full-band earning is tested separately.
        for(int n=1;n<=4;n++){String id=Profession.LUMBERJACK.questId(ProfessionRank.APPRENTICE,n);int goal=TreeQuestService.get(id).goal();s.completeTreeQuest(UUID.randomUUID(),player,id,goal,goal);}
        var delivery=s.prepareProfessionDelivery(UUID.randomUUID(),player,CraftDeliveryQuests.TIMBER,"Unit fixture actual native adapter boundary",Map.of("minecraft:oak_planks",128,"minecraft:stripped_oak_log",32));s.recoverItem(new EconomyStore.Receipt(s.id(),delivery.sequence()));
        s.approveProfessionInspection(UUID.randomUUID(),UUID.randomUUID(),player,"kingdom_smp:lumberjack/a06","Unit inspected loaded forest fixture");
        for(long day=100;day<103;day++)deliver(s,Profession.LUMBERJACK,"daily",day);s.completeCommissionQuest(UUID.randomUUID(),player,quest(Profession.LUMBERJACK),3);
        s.approveProfessionInspection(UUID.randomUUID(),UUID.randomUUID(),player,"kingdom_smp:lumberjack/a08","Unit complete woodland fixture");assertEquals(800,s.professions(player).track(Profession.LUMBERJACK).xp());
    }
    void tieredWeekly(EconomyStore s,long period,int tier)throws Exception{
        var item=new CompoundTag();item.putString("id","minecraft:oak_log");item.putInt("count",1);var pending=s.prepareContract(UUID.randomUUID(),player,"lumberjack","weekly",period,256,80,2000,item,tier);assertTrue(s.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence())));
    }
    @Test void advancedCommissionRequiresMatchingBandCycleAndVerifiedTierWithoutReinterpretingLegacyHistory()throws Exception{
        var s=enroll();String advanced="kingdom_smp:lumberjack/j07";
        for(long week=100;week<102;week++)deliver(s,Profession.LUMBERJACK,"weekly",week);
        assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,advanced,2));
        earnedLumberjackApprenticeModel(s);
        for(long week=110;week<112;week++)deliver(s,Profession.LUMBERJACK,"weekly",week); // Legacy unknown tier cannot be guessed.
        tieredWeekly(s,120,2);tieredWeekly(s,121,2);
        assertEquals(0,s.commissionContractPeriods(player,advanced));assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,advanced,2));
        tieredWeekly(s,130,3);assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),player,advanced,2));tieredWeekly(s,131,3);
        int mails=s.mailbox(player).size();long spent=s.contractPoolSpent("weekly",131);var operation=UUID.randomUUID();s.completeCommissionQuest(operation,player,advanced,2);s.completeCommissionQuest(operation,player,advanced,2);
        assertEquals(1000,s.professions(player).track(Profession.LUMBERJACK).xp());assertEquals(mails,s.mailbox(player).size());assertEquals(spent,s.contractPoolSpent("weekly",131));assertEquals(0,s.balance(player));assertEquals(2,s.commissionContractPeriods(player,advanced));
        s.archive(player,"Mixed legacy and tiered commission history");var restored=EconomyStore.open(directory);assertEquals(s.professions(player),restored.professions(player));assertEquals(2,restored.commissionContractPeriods(player,advanced));assertThrows(IllegalStateException.class,()->restored.completeCommissionQuest(UUID.randomUUID(),player,advanced,2));
    }
    @Test void newTierReceiptRetainsOriginalCancellationAndAfterRenameRecoverySafety()throws Exception{
        var s=enroll();var item=new CompoundTag();item.putString("id","minecraft:cobblestone");item.putInt("count",1);
        assertThrows(IllegalArgumentException.class,()->s.prepareContract(UUID.randomUUID(),player,"builder","daily",1,64,80,2000,item,13));assertTrue(s.pendingItem().isEmpty());
        s.prepareContract(UUID.randomUUID(),player,"builder","daily",1,64,80,2000,item,2);assertFalse(s.recoverItem(EconomyStore.Receipt.empty()));assertTrue(s.mailbox(player).isEmpty());
        var pending=s.prepareContract(UUID.randomUUID(),player,"builder","daily",1,64,80,2000,item,2);var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());
        var faulty=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new IOException("Tiered receipt after rename");});assertThrows(IOException.class,()->faulty.recoverItem(receipt));var restored=EconomyStore.open(directory);assertFalse(restored.recoverItem(receipt));assertEquals(1,restored.commissionContractPeriods(player,quest(Profession.BUILDER)));assertEquals(1,restored.mailbox(player).size());assertEquals(80,restored.contractPoolSpent("daily",1));
    }

}
