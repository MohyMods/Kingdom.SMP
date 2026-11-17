package com.mohymods.kingdomsmp.economy;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import org.junit.jupiter.api.Test;
/** Canonical projection model only; never substitutes for native full-band earning. */
class AlphaLibraryCommissionTest {
 @Test void alphaSixMasterServiceStillRequiresFiveDistinctCorrectBandCycleAndTierRecords()throws Exception {
  var projection=new ContractState();var player=UUID.randomUUID();
  var counter=EconomyStore.class.getDeclaredMethod("commissionPeriods",ContractState.class,UUID.class,String.class);counter.setAccessible(true);
  projection.professionPeriods.add(new ContractState.QuestPeriod(player,"librarian","master","daily",1,0));
  projection.professionPeriods.add(new ContractState.QuestPeriod(player,"librarian","master","daily",2,5));
  projection.professionPeriods.add(new ContractState.QuestPeriod(player,"librarian","journeyman","daily",3,6));
  projection.professionPeriods.add(new ContractState.QuestPeriod(player,"librarian","master","weekly",4,6));
  projection.professionPeriods.add(new ContractState.QuestPeriod(UUID.randomUUID(),"librarian","master","daily",5,6));
  assertEquals(0L,counter.invoke(null,projection,player,"kingdom_smp:librarian/m06"));
  for(int day=100;day<105;day++)projection.professionPeriods.add(new ContractState.QuestPeriod(player,"librarian","master","daily",day,6));
  projection.professionPeriods.add(new ContractState.QuestPeriod(player,"librarian","master","daily",100,6));
  assertEquals(5L,counter.invoke(null,projection,player,"kingdom_smp:librarian/m06"));
  assertEquals(5L,counter.invoke(null,projection.copy(),player,"kingdom_smp:librarian/m06"));
  projection.professionPeriods.add(new ContractState.QuestPeriod(player,"librarian","master","daily",200,8));
  assertEquals(6L,counter.invoke(null,projection,player,"kingdom_smp:librarian/m06"));
  assertEquals(8,ApprovedProfessionContent.get("kingdom_smp:librarian/m06").minimumTier());
  assertEquals(5,CommissionQuests.get("kingdom_smp:librarian/m06").goal());
  assertEquals(300,CommissionQuests.xp("kingdom_smp:librarian/m06"));
 }
}
