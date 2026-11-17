package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class ExpeditionSuppliesTest {
 @Test void mixedCookedFamiliesRemainOneCategoryWhileEquipmentCannotReplaceFood(){
  assertEquals(Map.of("cooked_food",16,"minecraft:torch",16,"minecraft:compass",1),CraftDeliveryQuests.totals(ExpeditionSupplies.QUEST,Map.of("minecraft:cooked_beef",8,"minecraft:baked_potato",8,"minecraft:torch",16,"minecraft:compass",1)));
  for(String item:List.of("minecraft:beef","minecraft:bread","minecraft:rotten_flesh","minecraft:poisonous_potato","client:cooked_beef"))assertThrows(IllegalArgumentException.class,()->CraftDeliveryQuests.totals(ExpeditionSupplies.QUEST,Map.of(item,16)));
 }
 @Test void approvedObjectiveUsesNativeCookingOnlyWithOriginalTierAndCanonicalDelivery(){
  var d=CraftDeliveryQuests.get(ExpeditionSupplies.QUEST);assertEquals(3,d.minimumTier());assertEquals(100,d.xp());assertEquals(Set.of(ApprovedProfessionContent.Validation.MAKE,ApprovedProfessionContent.Validation.DEL),d.validation());assertEquals(ExpeditionSupplies.METRIC,ExpeditionSupplies.metric(ExpeditionSupplies.FOOD));assertEquals(8,ExpeditionSupplies.RAW.size());assertTrue(ExpeditionSupplies.metrics().entrySet().stream().filter(e->ExpeditionSupplies.RAW.containsKey(e.getKey())).allMatch(e->e.getValue().startsWith("smelt_")));
 }
}
