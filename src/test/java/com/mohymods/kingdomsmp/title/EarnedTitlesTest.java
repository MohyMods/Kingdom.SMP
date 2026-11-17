package com.mohymods.kingdomsmp.title;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.commerce.CommerceCatalog;
import com.mohymods.kingdomsmp.economy.EconomyStore;
import com.mohymods.kingdomsmp.profession.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class EarnedTitlesTest {
  @Test
  void onlyCanonicalBuyerEarnsTheExistingFounderIdentity() {
    var buyer = UUID.randomUUID();
    var other = UUID.randomUUID();
    var b =
        new CommerceCatalog.Building(
            "kingdom_smp:royal_bank",
            "Royal Bank",
            3,
            2500,
            "Oswin Fairmark",
            "Founder of the Royal Bank");
    var purchase = new EconomyStore.Purchase(UUID.randomUUID(), buyer, "Buyer", b.id(), 2500);
    var found =
        EarnedTitles.project(
            buyer, List.of(purchase), List.of(b), ProfessionRoster.empty(), FarmerQuests.catalog());
    assertEquals("kingdom_smp:founder/royal_bank", found.getFirst().id());
    assertEquals(b.founder(), found.getFirst().label());
    assertTrue(
        EarnedTitles.project(
                other,
                List.of(purchase),
                List.of(b),
                ProfessionRoster.empty(),
                FarmerQuests.catalog())
            .isEmpty());
  }

  @Test
  void inactiveFarmerRetainsEveryEarnedTitleWithoutMasterOrActiveBenefits() {
    var quests = FarmerQuests.catalog();
    var roster = ProfessionRoster.empty().enroll(Profession.FARMER).roster();
    for (int i = 1; i <= 8; i++)
      roster =
          roster.completeQuest(
              Profession.FARMER,
              Profession.FARMER.questId(ProfessionRank.APPRENTICE, i),
              100,
              quests);
    roster = roster.deactivate(Profession.FARMER).roster();
    var titles = EarnedTitles.project(UUID.randomUUID(), List.of(), List.of(), roster, quests);
    assertEquals(
        List.of("Apprentice Farmer", "Journeyman Farmer"),
        titles.stream().map(EarnedTitles.Earned::label).toList());
    assertTrue(titles.stream().allMatch(t -> t.detail().contains("Inactive")));
    assertFalse(roster.perksEligible(Profession.FARMER, ProfessionRank.APPRENTICE, quests));
    assertEquals(800, roster.track(Profession.FARMER).xp());
  }

  @Test
  void missingProfessionHistoryAndUnknownHistoricalBuildingsNeverInventEntitlements() {
    var actor = UUID.randomUUID();
    var purchase =
        new EconomyStore.Purchase(
            UUID.randomUUID(), actor, "Buyer", "kingdom_smp:unknown_building", 100);
    assertTrue(
        EarnedTitles.project(
                actor,
                List.of(purchase),
                List.of(),
                ProfessionRoster.empty(),
                FarmerQuests.catalog())
            .isEmpty());
  }
}
