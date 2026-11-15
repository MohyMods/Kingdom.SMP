package com.mohymods.kingdomsmp.kingdom;
import static org.junit.jupiter.api.Assertions.*;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class AlphaScopeTest {
 @Test void sixSelectedProfessionsDoNotHideOrEraseTheOtherSix(){
  assertEquals(6,Arrays.stream(Profession.values()).filter(AlphaScope::selected).count());
  assertTrue(AlphaScope.selected(Profession.LIBRARIAN));assertFalse(AlphaScope.selected(Profession.ALCHEMIST));assertEquals(12,Profession.values().length);
 }
 @Test void historicalHigherTierNeverUnlocksFutureDimensionsButReturnRemainsAvailable(){
  assertTrue(KingdomTravelRules.allowed(12,"minecraft:the_end"));
  assertFalse(AlphaScope.destinationAvailable(12,"minecraft:the_end"));assertFalse(AlphaScope.destinationAvailable(12,"deeperdarker:otherside"));
  assertTrue(AlphaScope.destinationAvailable(12,"minecraft:overworld"));assertTrue(AlphaScope.destinationAvailable(6,"twilightforest:twilight_forest"));assertFalse(AlphaScope.destinationAvailable(5,"twilightforest:twilight_forest"));
  assertFalse(AlphaScope.serviceAvailable(9));assertFalse(AlphaScope.serviceAvailable(10));assertTrue(AlphaScope.serviceAvailable(6));
  assertFalse(AlphaScope.destinationAvailable(12,"ad_astra:moon"));assertTrue(AlphaScope.futureMessage().contains("Future Update"));
 }
 @Test void unfinishedMasterMetadataIsNotAnAwardOrAClaimOfCompletion(){
  assertTrue(AlphaScope.futureQuest("kingdom_smp:alchemist/m01"));assertFalse(AlphaScope.futureQuest("kingdom_smp:miner/m01"));assertFalse(AlphaScope.futureQuest(null));
  assertFalse(ProfessionQuestService.ready(null,"kingdom_smp:alchemist/m01"));assertThrows(IllegalStateException.class,()->ProfessionQuestService.complete(null,UUID.randomUUID(),"kingdom_smp:alchemist/m01"));
  assertEquals(288,ProfessionQuestService.presentationCatalog().definitions().size());
 }
}
