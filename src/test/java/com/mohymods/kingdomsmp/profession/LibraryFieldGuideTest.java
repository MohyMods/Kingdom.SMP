package com.mohymods.kingdomsmp.profession;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
class LibraryFieldGuideTest {
 @Test void notesMustReferencePersonalNativeSavedVisitNotAssertionsInText(){
  var p=UUID.randomUUID();var other=UUID.randomUUID();var data=new ProfessionEvidence(Profession.LIBRARIAN,Set.of(LibraryFieldGuide.METRIC));
  String dim="aether:the_aether",biome="aether:skyroot_forest",text="My field observations\ndimension="+dim+" biome="+biome;
  assertThrows(IllegalStateException.class,()->LibraryFieldGuide.validate(text,(d,b)->data.seen(p,LibraryFieldGuide.METRIC,LibraryFieldGuide.identity(d,b))));
  assertTrue(data.record(p,ProfessionRank.JOURNEYMAN,LibraryFieldGuide.METRIC,1,LibraryFieldGuide.identity(dim,biome)));
  var restored=ProfessionEvidence.load(data.save(),Profession.LIBRARIAN,Set.of(LibraryFieldGuide.METRIC));
  assertEquals(64,LibraryFieldGuide.validate(text,(d,b)->restored.seen(p,LibraryFieldGuide.METRIC,LibraryFieldGuide.identity(d,b))).length());
  assertThrows(IllegalStateException.class,()->LibraryFieldGuide.validate(text,(d,b)->restored.seen(other,LibraryFieldGuide.METRIC,LibraryFieldGuide.identity(d,b))));
  assertThrows(IllegalStateException.class,()->LibraryFieldGuide.validate(text.replace(biome,"aether:skyroot_grove"),(d,b)->restored.seen(p,LibraryFieldGuide.METRIC,LibraryFieldGuide.identity(d,b))));
 }
 @Test void noArbitraryDimensionsDuplicatedDeclarationsOrUnboundedNotes(){
  for(String text:List.of("I went there", "dimension=minecraft:the_end biome=minecraft:the_end","dimension=aether:the_aether biome=bad","dimension=aether:the_aether biome=aether:skyroot_forest\ndimension=aether:the_aether biome=aether:skyroot_forest","x".repeat(32769)))assertThrows(IllegalStateException.class,()->LibraryFieldGuide.validate(text,(d,b)->true));
  assertTrue(InspectionQuests.contains(LibraryFieldGuide.QUEST));assertEquals(200,InspectionQuests.get(LibraryFieldGuide.QUEST).xp());
 }
}
