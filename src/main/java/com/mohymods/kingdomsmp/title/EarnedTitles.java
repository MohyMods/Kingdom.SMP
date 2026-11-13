package com.mohymods.kingdomsmp.title;

import com.mohymods.kingdomsmp.commerce.CommerceCatalog;
import com.mohymods.kingdomsmp.economy.EconomyStore;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Server projection of existing immutable first-buyer and preserved profession records. */
public final class EarnedTitles {
  public record Earned(String id, String label, String detail) {
    public Earned {
      TitleBook.validate(id);
      if (label == null
          || label.isBlank()
          || label.length() > 64
          || detail == null
          || detail.length() > 256) throw new IllegalArgumentException("Invalid title display");
    }
  }

  private EarnedTitles() {}

  public static List<Earned> project(
      UUID actor,
      Collection<EconomyStore.Purchase> purchases,
      Collection<CommerceCatalog.Building> buildings,
      ProfessionRoster roster,
      ProfessionQuestCatalog quests) {
    var result = new ArrayList<Earned>();
    for (var purchase : purchases) {
      if (!purchase.buyer().equals(actor)) continue;
      buildings.stream()
          .filter(b -> b.id().equals(purchase.building()))
          .findFirst()
          .ifPresent(
              b ->
                  result.add(
                      new Earned(
                          "kingdom_smp:founder/" + ResourceLocation.parse(b.id()).getPath(),
                          b.founder(),
                          "Permanent first-buyer recognition for "
                              + b.title()
                              + ". This title grants no operator, property or inventory"
                              + " permissions.")));
    }
    for (var profession : Profession.values()) {
      if (!roster.tracks().containsKey(profession)) continue;
      var rank = roster.rank(profession, quests);
      for (var earned : ProfessionRank.values())
        if (earned.ordinal() <= rank.ordinal())
          result.add(
              new Earned(
                  "kingdom_smp:profession/" + profession.id() + "/" + earned.id(),
                  word(earned.id()) + " " + word(profession.id()),
                  "Earned profession rank. "
                      + (roster.isActive(profession)
                          ? "Active profession; selecting a title does not alter its bonuses."
                          : "Inactive profession; rank recognition is retained and gameplay bonuses"
                              + " remain disabled.")));
    }
    return List.copyOf(result);
  }

  private static String word(String value) {
    return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
  }
}
