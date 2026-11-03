package com.mohymods.kingdomsmp.profession;

import static com.mohymods.kingdomsmp.profession.QuestObjective.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class QuestObjectiveTest {
  @Test
  void sumsAndPerCategoryMinimaPreventSingleResourceDominance() {
    var rule = event(sum("total", "iron", "copper"), each("category", "iron", "copper"));
    var goals = Map.of("total", 100L, "category", 20L);
    assertFalse(rule.ready(k -> k.equals("iron") ? 100 : 0, k -> goals.get(k)));
    assertTrue(rule.ready(k -> k.equals("iron") ? 80 : 20, k -> goals.get(k)));
  }

  @Test
  void consumedOutputProofCannotBeReplacedWithRepeatedCraftEvents() {
    var rule = FarmerObjectives.rule(FarmerQuests.id(ProfessionRank.APPRENTICE, 6));
    assertFalse(rule.ready(k -> k.equals("bread") ? 1000 : 0, k -> 32));
    assertTrue(rule.ready(k -> 32, k -> 32));
  }

  @Test
  void allApprovedFarmerSlotsKeepTheirExistingManualOrEventClassification() {
    assertEquals(24, FarmerQuests.catalog().definitions().size());
    for (var d : FarmerQuests.catalog().definitions().values()) {
      var rule = FarmerObjectives.rule(d.id());
      assertEquals(FarmerQuests.food(d.id()), rule.kind() == Kind.CONSUMED_DELIVERY);
      assertEquals(FarmerQuests.inspection(d.id()), rule.kind() == Kind.INSPECTION);
      assertEquals(
          !FarmerQuests.food(d.id()) && !FarmerQuests.inspection(d.id()),
          rule.ready(k -> 1_000_000, k -> 1));
    }
  }

  @Test
  void manualWorkCannotAutoGrantAndMalformedEmptyRulesFailClosed() {
    assertFalse(manual(Kind.INSPECTION).ready(k -> Long.MAX_VALUE, k -> 1));
    assertFalse(manual(Kind.CONSUMED_DELIVERY).ready(k -> Long.MAX_VALUE, k -> 1));
    assertThrows(IllegalArgumentException.class, () -> event());
    assertThrows(
        IllegalArgumentException.class,
        () -> new Criterion(Aggregation.SUM, List.of("ore", "ore"), "goal"));
    assertThrows(
        IllegalArgumentException.class, () -> event(each("goal", "counter")).ready(k -> 1, k -> 0));
    assertThrows(
        IllegalStateException.class, () -> event(each("goal", "counter")).ready(k -> -1, k -> 1));
  }

  @Test
  void saturatingTotalsNeverWrapToAFalseNegativeOrGrant() {
    assertTrue(event(sum("goal", "a", "b")).ready(k -> Long.MAX_VALUE, k -> 1_000_000_000L));
    assertFalse(event(sum("goal", "a", "b")).ready(k -> 1, k -> 3));
  }

  @Test
  void evidenceAndGoalsForSeparateObjectivesDoNotLeakIntoOtherBands() {
    var births = FarmerObjectives.rule(FarmerQuests.id(ProfessionRank.JOURNEYMAN, 4));
    assertFalse(births.ready(k -> k.equals("pig") ? 999 : 0, k -> 8));
    assertTrue(births.ready(k -> 8, k -> 8));
    var diverse = FarmerObjectives.rule(FarmerQuests.id(ProfessionRank.MASTER, 2));
    assertFalse(diverse.ready(k -> k.equals("cow") ? 16 : 0, k -> k.endsWith("category") ? 2 : 16));
  }
}
