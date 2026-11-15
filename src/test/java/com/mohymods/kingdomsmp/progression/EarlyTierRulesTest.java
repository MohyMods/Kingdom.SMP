package com.mohymods.kingdomsmp.progression;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
class EarlyTierRulesTest {
 @TempDir Path directory;
 @Test void defaultsHaveNoPreNetherOrEndDeadlocksAndRemainPinnedWithoutRewriting()throws Exception {
  for(int tier=3;tier<=5;tier++) {
   var file=directory.resolve("tier"+tier+".json");var costs=EarlyTierRules.load(file,tier);assertEquals(8,costs.size());var bytes=Files.readAllBytes(file);assertEquals(costs,EarlyTierRules.load(file,tier));assertArrayEquals(bytes,Files.readAllBytes(file));
   assertTrue(costs.keySet().stream().noneMatch(k->k.contains("ender")||k.contains("chorus")||k.contains("shulker")));
   if(tier<5)assertTrue(costs.keySet().stream().noneMatch(k->k.contains("nether")||k.contains("blaze")||k.contains("quartz")||k.contains("magma")));
  }
  assertThrows(IllegalArgumentException.class,()->EarlyTierRules.defaults(6));
 }
 @Test void malformedOrChangedCostsCannotReinterpretSavedCredit()throws Exception {
  var file=directory.resolve("config.json");var costs=EarlyTierRules.load(file,3);var ledger=TierTwoLedger.open(directory.resolve("data"),costs);var who=UUID.randomUUID();var key=costs.keySet().iterator().next();ledger.prepare(UUID.randomUUID(),who,"Alice",key,1);assertTrue(ledger.recover(new TierTwoLedger.Receipt(ledger.id(),1)));
  var changed=new HashMap<>(costs);changed.put(key,costs.get(key)+1);assertThrows(java.io.IOException.class,()->TierTwoLedger.open(directory.resolve("data"),changed));
  var original=Files.readString(file);Files.writeString(file,original.replace("\"tier\": 3","\"tier\": 4"));var bytes=Files.readAllBytes(file);assertThrows(java.io.IOException.class,()->EarlyTierRules.load(file,3));assertArrayEquals(bytes,Files.readAllBytes(file));
 }
 @Test void separateStageReceiptsCannotReplayAndPreparedAbortSurvivesRestart()throws Exception {
  var costs=Map.of("minecraft:iron_ingot",2);var three=TierTwoLedger.open(directory.resolve("3"),costs);var four=TierTwoLedger.open(directory.resolve("4"),costs);var who=UUID.randomUUID();var op=UUID.randomUUID();three.prepare(op,who,"Alice","minecraft:iron_ingot",2);assertTrue(three.recover(new TierTwoLedger.Receipt(three.id(),1)));assertTrue(three.resourcesComplete());
  four.prepare(UUID.randomUUID(),who,"Alice","minecraft:iron_ingot",2);assertThrows(java.io.IOException.class,()->four.recover(new TierTwoLedger.Receipt(three.id(),1)));
  var reopened=TierTwoLedger.open(directory.resolve("4"),costs);assertFalse(reopened.recover(TierTwoLedger.Receipt.empty()));assertFalse(reopened.resourcesComplete());assertEquals(0,reopened.total("minecraft:iron_ingot"));assertThrows(IllegalArgumentException.class,()->three.prepare(op,who,"Alice","minecraft:iron_ingot",1));
 }
}
