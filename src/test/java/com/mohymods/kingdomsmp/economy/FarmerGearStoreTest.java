package com.mohymods.kingdomsmp.economy;
import com.mohymods.kingdomsmp.profession.*;
import com.mohymods.kingdomsmp.equipment.FarmerGear;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import net.minecraft.nbt.CompoundTag;
import java.nio.file.Path;
import java.io.IOException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class FarmerGearStoreTest {
 @TempDir Path dir;final UUID p=UUID.randomUUID();final FarmerGear gear=FarmerGear.FORTUNE_X_HOE;
 EconomyStore open()throws IOException{return EconomyStore.open(dir);}
 CompoundTag item(FarmerGear g){var data=new CompoundTag();data.putString("id",g.id());data.putInt("count",1);return data;}
 void rank(EconomyStore s,ProfessionRank target)throws Exception{s.changeProfession(UUID.randomUUID(),p,"enroll","","farmer");for(var rank:ProfessionRank.values()){if(rank==target&&target==ProfessionRank.JOURNEYMAN)break;for(int n=1;n<=8;n++){var id=FarmerQuests.id(rank,n);if(FarmerQuests.food(id)){var inputs=new HashMap<String,Integer>();FarmerDeliveries.family(id).forEach(i->inputs.put(i,1));var pending=s.prepareFarmerDelivery(UUID.randomUUID(),p,id,"explicit test-fixture food proof",inputs);s.recoverItem(new EconomyStore.Receipt(s.id(),pending.sequence()));}else if(FarmerQuests.inspection(id))s.approveFarmerInspection(UUID.randomUUID(),UUID.randomUUID(),p,id,"explicit test-fixture inspected farm");else s.completeFarmerQuest(UUID.randomUUID(),p,id,"explicit test-fixture event proof");}}}
 void fund(EconomyStore s,long coins)throws Exception{s.adminSet(UUID.randomUUID(),p,p,coins,"Equipment fixture funding");}
 @Test void moneyDeliveryAndSuccessfulCountAreAtomicPersistentAndRetrySafe()throws Exception{var s=open();rank(s,ProfessionRank.MASTER);fund(s,100000);var op=UUID.randomUUID();s.buyFarmerGear(op,p,gear.id(),25000,item(gear));s.buyFarmerGear(op,p,gear.id(),25000,item(gear));assertEquals(75000,s.balance(p));assertEquals(1,s.equipmentPurchases(p,gear.id()));assertTrue(s.shopDelivery(op));s.buyFarmerGear(UUID.randomUUID(),p,gear.id(),25000,item(gear));var restored=open();assertEquals(37500,restored.balance(p));assertEquals(62500,restored.equipmentCoinsSpent());assertEquals(2,restored.equipmentPurchases(p,gear.id()));assertEquals(2,restored.itemMailbox(p).size());assertThrows(IllegalStateException.class,()->restored.buyFarmerGear(UUID.randomUUID(),p,gear.id(),25000,item(gear)));assertEquals(2,restored.equipmentPurchases(p,gear.id()));}
 @Test void inactiveAndInsufficientRankCannotBuyButRetainTheirCounts()throws Exception{var s=open();rank(s,ProfessionRank.JOURNEYMAN);fund(s,100000);assertThrows(IllegalStateException.class,()->s.buyFarmerGear(UUID.randomUUID(),p,gear.id(),25000,item(gear)));s.buyFarmerGear(UUID.randomUUID(),p,FarmerGear.JOURNEYMAN_HOE.id(),5000,item(FarmerGear.JOURNEYMAN_HOE));s.changeProfession(UUID.randomUUID(),p,"switch","farmer","builder");assertThrows(IllegalStateException.class,()->s.buyFarmerGear(UUID.randomUUID(),p,FarmerGear.JOURNEYMAN_HOE.id(),5000,item(FarmerGear.JOURNEYMAN_HOE)));assertEquals(1,open().equipmentPurchases(p,FarmerGear.JOURNEYMAN_HOE.id()));}
 @Test void foreignItemOrReusedNonceDifferentTermsNeverSpend()throws Exception{var s=open();rank(s,ProfessionRank.MASTER);fund(s,100000);assertThrows(IllegalArgumentException.class,()->s.buyFarmerGear(UUID.randomUUID(),p,gear.id(),25000,item(FarmerGear.JOURNEYMAN_HOE)));assertEquals(100000,s.balance(p));UUID op=UUID.randomUUID();s.buyFarmerGear(op,p,gear.id(),25000,item(gear));assertThrows(IllegalArgumentException.class,()->s.buyFarmerGear(op,p,gear.id(),26000,item(gear)));}
 @Test void failedRenameRecoversOneDeliveryAndOnePriceIncrement()throws Exception{var s=open();rank(s,ProfessionRank.MASTER);fund(s,100000);var failing=EconomyStore.open(dir,(path,nbt)->{AtomicNbt.write(path,nbt);throw new IOException("after rename");});assertThrows(IOException.class,()->failing.buyFarmerGear(UUID.randomUUID(),p,gear.id(),25000,item(gear)));var restored=open();assertEquals(75000,restored.balance(p));assertEquals(1,restored.equipmentPurchases(p,gear.id()));assertEquals(1,restored.itemMailbox(p).size());}
}
