package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
class CreatePressDeliveryTest {
 @Test void plainSupportedSheetCategoriesMixButOldUnrelatedMaterialsDoNot(){
  var id=CreatePressService.QUEST;assertEquals(Map.of("sheets",32),CraftDeliveryQuests.defaults(id));assertEquals(Map.of("sheets",32),CraftDeliveryQuests.totals(id,Map.of("create:iron_sheet",16,"create:copper_sheet",8,"create:golden_sheet",8)));
  assertThrows(IllegalArgumentException.class,()->CraftDeliveryQuests.totals(id,Map.of("minecraft:iron_ingot",32)));
  assertThrows(IllegalArgumentException.class,()->CraftDeliveryQuests.totals(id,Map.of("create:iron_sheet",0)));
  assertThrows(IllegalArgumentException.class,()->CraftDeliveryQuests.totals(id,Map.of("create:iron_sheet",2304,"create:copper_sheet",1)));
 }
 @Test void exactNativeImpactIdentitiesAreBoundedAndNeverRecreditedAfterRestore(){
  var player=UUID.randomUUID();String source="worldpress/"+UUID.randomUUID();var metrics=Set.of(CreatePressService.METRIC);var data=new ProfessionEvidence(Profession.ENGINEER,metrics);assertTrue(data.record(player,ProfessionRank.APPRENTICE,CreatePressService.METRIC,1,source));var restored=ProfessionEvidence.load(data.save(),Profession.ENGINEER,metrics);
  assertFalse(restored.record(player,ProfessionRank.APPRENTICE,CreatePressService.METRIC,1,source));assertEquals(1,restored.count(player,ProfessionRank.APPRENTICE,CreatePressService.METRIC));assertEquals(0,restored.count(player,ProfessionRank.JOURNEYMAN,CreatePressService.METRIC));
 }
}
