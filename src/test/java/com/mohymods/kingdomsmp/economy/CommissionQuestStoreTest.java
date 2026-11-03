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
}
