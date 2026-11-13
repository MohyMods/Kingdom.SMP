package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import java.nio.file.Path;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class AllProfessionCommissionTest {
 @TempDir Path directory;
 @Test void everyNewRoleUsesThreeEligibleDailyPeriodsAndKeepsFutureBandsClosed()throws Exception{
  var terms=ContractRules.load(directory.resolve("terms.json"));var s=EconomyStore.open(directory.resolve("data"));
  for(var role:Profession.values())if(!Set.of(Profession.FARMER,Profession.BUILDER,Profession.LUMBERJACK,Profession.BLACKSMITH).contains(role)){
   var p=UUID.randomUUID();s.changeProfession(UUID.randomUUID(),p,"enroll","",role.id());var id=role.questId(ProfessionRank.APPRENTICE,7);int tier=ApprovedProfessionContent.get(id).minimumTier();var t=terms.get(role.id()+"/daily");var item=new CompoundTag();item.putString("id",BuiltInRegistries.ITEM.getKey(t.item()).toString());item.putInt("count",1);
   for(int n=0;n<5;n++){var pending=n==0?s.prepareContract(UUID.randomUUID(),p,role.id(),"daily",100+n,t.quantity(),t.reward(),t.pool(),item):s.prepareContract(UUID.randomUUID(),p,role.id(),"daily",100+n,t.quantity(),t.reward(),t.pool(),item,n==1?tier-1:tier);var receipt=new EconomyStore.Receipt(s.id(),pending.sequence());assertTrue(s.recoverItem(receipt));assertFalse(s.recoverItem(receipt));if(n<4)assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),p,id,3));}
   assertEquals(3,s.commissionContractPeriods(p,id));var op=UUID.randomUUID();s.completeCommissionQuest(op,p,id,3);s.completeCommissionQuest(op,p,id,3);assertEquals(100,s.professions(p).track(role).xp());assertEquals(5,s.mailbox(p).size());assertEquals(0,s.balance(p));assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),p,id,3));assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),p,role.questId(ProfessionRank.JOURNEYMAN,7),2));assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),p,role.questId(ProfessionRank.MASTER,6),5));
  }var reopened=EconomyStore.open(directory.resolve("data"));assertEquals(s.revision(),reopened.revision());assertEquals(24,terms.all().size());assertEquals(33,CommissionQuests.ids().size());
 }
}
