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
 @Test void onlyValidatedLibraryAlphaVariantsMoveToSix(){
  assertEquals(8,ApprovedProfessionContent.get("kingdom_smp:librarian/m06").minimumTier());
  assertEquals(6,AlphaScope.questTier("kingdom_smp:librarian/m06"));assertFalse(AlphaScope.futureQuest("kingdom_smp:librarian/m06"));
  assertEquals(5,AlphaScope.questTier("kingdom_smp:blacksmith/m06"));assertEquals(8,AlphaScope.questTier("kingdom_smp:alchemist/m06"));
  for(var id:java.util.List.of("kingdom_smp:librarian/m01","kingdom_smp:librarian/m02","kingdom_smp:librarian/m03","kingdom_smp:librarian/m04","kingdom_smp:librarian/m05","kingdom_smp:librarian/m07","kingdom_smp:librarian/m08")){assertEquals(8,ApprovedProfessionContent.get(id).minimumTier());assertEquals(6,AlphaScope.questTier(id));assertFalse(AlphaScope.futureQuest(id));}
  assertTrue(AlphaScope.futureQuest("kingdom_smp:mage/m08"));
 }
 @Test void unfinishedMasterMetadataIsNotAnAwardOrAClaimOfCompletion(){
  assertTrue(AlphaScope.futureQuest("kingdom_smp:alchemist/m01"));assertFalse(AlphaScope.futureQuest("kingdom_smp:miner/m01"));assertFalse(AlphaScope.futureQuest(null));
  assertFalse(ProfessionQuestService.ready(null,"kingdom_smp:alchemist/m01"));assertThrows(IllegalStateException.class,()->ProfessionQuestService.complete(null,UUID.randomUUID(),"kingdom_smp:alchemist/m01"));
  assertEquals(288,ProfessionQuestService.presentationCatalog().definitions().size());
 }
}
