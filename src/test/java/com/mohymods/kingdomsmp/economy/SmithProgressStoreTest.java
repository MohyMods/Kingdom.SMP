package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.util.*;
import java.nio.file.*;
import java.io.IOException;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
class SmithProgressStoreTest {
 @TempDir Path directory;UUID p=UUID.randomUUID();
 EconomyStore enrolled()throws Exception{var s=EconomyStore.open(directory);s.changeProfession(UUID.randomUUID(),p,"enroll","","blacksmith");return s;}
 void firstFive(EconomyStore s)throws Exception{for(String id:List.of(CraftDeliveryQuests.FUEL,CraftDeliveryQuests.IRON,CraftDeliveryQuests.RODS,CraftDeliveryQuests.TOOLS,CraftDeliveryQuests.SHIELDS)){var inputs=id.equals(CraftDeliveryQuests.FUEL)?Map.of("minecraft:coal",64):CraftDeliveryQuests.defaults(id);var pending=s.prepareProfessionDelivery(UUID.randomUUID(),p,id,"Pure canonical model adapter fixture",inputs);s.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence()));}}
 void contract(EconomyStore s,long day,int tier)throws Exception{var item=new CompoundTag();item.putString("id","minecraft:iron_ingot");item.putInt("count",1);var delivery=tier==0?s.prepareContract(UUID.randomUUID(),p,"blacksmith","daily",day,16,160,2000,item):s.prepareContract(UUID.randomUUID(),p,"blacksmith","daily",day,16,160,2000,item,tier);s.recoverItem(new EconomyStore.Receipt(s.id(),delivery.sequence()));}
 @Test void exactThreeTierPinnedPeriodsAwardOnceAndCannotReuseUnknownOrEarlyHistory()throws Exception{var s=enrolled();contract(s,10,0);contract(s,11,2);assertEquals(0,s.commissionContractPeriods(p,"kingdom_smp:blacksmith/a07"));contract(s,12,3);contract(s,13,3);assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),p,"kingdom_smp:blacksmith/a07",3));contract(s,14,3);s.completeCommissionQuest(UUID.randomUUID(),p,"kingdom_smp:blacksmith/a07",3);assertEquals(100,s.professions(p).track(Profession.BLACKSMITH).xp());assertEquals(5,s.mailbox(p).size());assertEquals(0,s.balance(p));assertThrows(IllegalStateException.class,()->s.completeCommissionQuest(UUID.randomUUID(),p,"kingdom_smp:blacksmith/a07",3));s.archive(p,"Native smith commission model audit");assertEquals(3,EconomyStore.open(directory).commissionContractPeriods(p,"kingdom_smp:blacksmith/a07"));}
 @Test void repairCannotUseInspectionAuthorityOrSkipToolsAndAfterRenameNeverDuplicatesXp()throws Exception{var s=enrolled();assertThrows(IllegalStateException.class,()->s.completeRepairQuest(UUID.randomUUID(),p,4,4));assertThrows(IllegalArgumentException.class,()->s.approveProfessionInspection(UUID.randomUUID(),UUID.randomUUID(),p,RepairService.QUEST,"Wrong repair authority"));firstFive(s);assertThrows(IllegalArgumentException.class,()->s.completeRepairQuest(UUID.randomUUID(),p,4,3));var operation=UUID.randomUUID();var bad=EconomyStore.open(directory,(file,data)->{AtomicNbt.write(file,data);throw new IOException("Native repair after rename");});assertThrows(IOException.class,()->bad.completeRepairQuest(operation,p,4,4));var restored=EconomyStore.open(directory);restored.completeRepairQuest(operation,p,4,4);assertEquals(600,restored.professions(p).track(Profession.BLACKSMITH).xp());assertThrows(IllegalStateException.class,()->restored.completeRepairQuest(UUID.randomUUID(),p,4,4));assertTrue(restored.mailbox(p).isEmpty());assertEquals(0,restored.balance(p));}
}
