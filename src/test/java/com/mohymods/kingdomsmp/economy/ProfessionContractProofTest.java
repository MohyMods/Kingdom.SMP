package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import com.mohymods.kingdomsmp.profession.*;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class ProfessionContractProofTest {
 @TempDir Path directory;final UUID player=UUID.randomUUID();
 CompoundTag item(){var t=new CompoundTag();t.putString("id","minecraft:cobblestone");t.putInt("count",1);return t;}
 void deliver(EconomyStore s,Profession role,long day)throws Exception{var p=s.prepareContract(UUID.randomUUID(),player,role.id(),"daily",day,64,80,2000,item());assertTrue(s.recoverItem(new EconomyStore.Receipt(s.id(),p.sequence())));}
 @Test void completedPeriodsRemainPersonalProfessionScopedAndDistinctAcrossArchiveReload()throws Exception{var s=EconomyStore.open(directory);for(var role:List.of("builder","lumberjack","farmer"))s.changeProfession(UUID.randomUUID(),player,"enroll","",role);deliver(s,Profession.BUILDER,100);deliver(s,Profession.LUMBERJACK,100);assertEquals(1,s.professionContractPeriods(player,Profession.BUILDER,ProfessionRank.APPRENTICE,"daily"));assertEquals(1,s.professionContractPeriods(player,Profession.LUMBERJACK,ProfessionRank.APPRENTICE,"daily"));assertEquals(0,s.farmerContractPeriods(player,ProfessionRank.APPRENTICE,"daily"));assertEquals(0,s.professionContractPeriods(UUID.randomUUID(),Profession.BUILDER,ProfessionRank.APPRENTICE,"daily"));assertEquals(0,s.professionContractPeriods(player,Profession.BUILDER,ProfessionRank.JOURNEYMAN,"daily"));s.archive(player,"Private contract proof archive");s=EconomyStore.open(directory);assertEquals(1,s.professionContractPeriods(player,Profession.BUILDER,ProfessionRank.APPRENTICE,"daily"));assertEquals(160,s.contractPoolSpent("daily",100));assertEquals(2,s.mailbox(player).size());}
 @Test void canceledNativePreparationNeverProducesProgressOrPayment()throws Exception{var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","builder");s.prepareContract(UUID.randomUUID(),player,"builder","daily",100,64,80,2000,item());assertFalse(s.recoverItem(EconomyStore.Receipt.empty()));var restored=EconomyStore.open(directory);assertEquals(0,restored.professionContractPeriods(player,Profession.BUILDER,ProfessionRank.APPRENTICE,"daily"));assertEquals(0,restored.contractPoolSpent("daily",100));assertTrue(restored.mailbox(player).isEmpty());}
 @Test void uncertainAfterRenameRecoversOneNativeProofAndOneRewardOnly()throws Exception{var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),player,"enroll","","builder");var pending=s.prepareContract(UUID.randomUUID(),player,"builder","daily",100,64,80,2000,item());var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());var failed=EconomyStore.open(directory,(p,d)->{AtomicNbt.write(p,d);throw new java.io.IOException("Private after-rename contract proof fault");});assertThrows(java.io.IOException.class,()->failed.recoverItem(receipt));var restored=EconomyStore.open(directory);assertFalse(restored.recoverItem(receipt));assertEquals(1,restored.professionContractPeriods(player,Profession.BUILDER,ProfessionRank.APPRENTICE,"daily"));assertEquals(80,restored.contractPoolSpent("daily",100));assertEquals(1,restored.mailbox(player).size());}
}
