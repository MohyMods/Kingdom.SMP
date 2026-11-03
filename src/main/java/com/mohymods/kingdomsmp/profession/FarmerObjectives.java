package com.mohymods.kingdomsmp.profession;

import static com.mohymods.kingdomsmp.profession.QuestObjective.*;

import java.util.*;

/**
 * D034 objective equivalence table. Existing IDs, quantities, evidence and payout protocols remain.
 */
public final class FarmerObjectives {
  private static final Map<String, QuestObjective> RULES =
      Map.ofEntries(
          Map.entry("a01", event(each("a01", "soil"))),
          Map.entry("a02", event(each("a02", "wheat"))),
          Map.entry("a03", event(each("a03", "carrot"))),
          Map.entry("a04", event(each("a04", "potato"))),
          Map.entry("a05", event(sum("a05", "cow", "sheep", "chicken", "pig"))),
          Map.entry("a06", event(each("a06", "bread"), each("a06", "consumed/minecraft:bread"))),
          Map.entry("a07", manual(Kind.CONSUMED_DELIVERY)),
          Map.entry("a08", manual(Kind.INSPECTION)),
          Map.entry("j01", event(each("j01", "beetroot", "wheat"))),
          Map.entry("j02", event(each("j02", "carrot", "potato"))),
          Map.entry("j03", event(sum("j03", "melon", "pumpkin"))),
          Map.entry("j04", event(each("j04", "cow", "sheep", "chicken"))),
          Map.entry("j05", manual(Kind.CONSUMED_DELIVERY)),
          Map.entry("j06", manual(Kind.CONSUMED_DELIVERY)),
          Map.entry("j07", event(each("j07", "contracts/daily"))),
          Map.entry("j08", manual(Kind.INSPECTION)),
          Map.entry(
              "m01",
              event(
                  sum("m01", "wheat", "carrot", "potato", "beetroot"),
                  each("m01_category", "wheat", "carrot", "potato", "beetroot"))),
          Map.entry(
              "m02",
              event(
                  sum("m02", "cow", "sheep", "chicken", "pig"),
                  each("m02_category", "cow", "sheep", "chicken", "pig"))),
          Map.entry("m03", manual(Kind.CONSUMED_DELIVERY)),
          Map.entry("m04", event(each("m04", "contracts/weekly"))),
          Map.entry("m05", event(each("m05", "community/master_food"))),
          Map.entry("m06", event(each("m06", "replant"))),
          Map.entry("m07", manual(Kind.INSPECTION)),
          Map.entry("m08", manual(Kind.INSPECTION)));

  private FarmerObjectives() {}

  public static QuestObjective rule(String id) {
    var definition =
        FarmerQuests.catalog()
            .definition(id)
            .orElseThrow(() -> new IllegalArgumentException("Unknown approved Farmer objective"));
    return RULES.get(
        definition.rank().questPrefix() + String.format(Locale.ROOT, "%02d", definition.ordinal()));
  }
}
