package com.mohymods.kingdomsmp.integration.ftbquests;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class KingdomQuestGoalTest {
    private static KingdomQuestSnapshot snapshot(int tier, Set<String> done, Set<String> bought, boolean donated, boolean traded) {
        return new KingdomQuestSnapshot(tier, done, bought, donated, traded);
    }

    @Test void unknownVersionsAndLookalikeTagsNeverBind() {
        for (var goal : KingdomQuestGoal.values()) assertEquals(goal, KingdomQuestGoal.parseTag(goal.tag()).orElseThrow());
        for (String tag : new String[]{"kingdom_smp:projection/v2/hut", "kingdom_smp:projection/v1/master_farmer", "kingdom_smp:projection/v1/HUT", "other:projection/v1/hut", "kingdom_smp:projection/v1/hut/extra"})
            assertTrue(KingdomQuestGoal.parseTag(tag).isEmpty());
        assertTrue(KingdomQuestGoal.parseTag(null).isEmpty());
    }

    @Test void freeAndUnknownPlansCannotSatisfyARealPaidPurchase() {
        var free = snapshot(2, Set.of("kingdom_smp:builders_hut", "kingdom_smp:royal_town_hall"),
                Set.of("kingdom_smp:builders_hut", "kingdom_smp:royal_town_hall", "kingdom_smp:unimplemented_building"), true, false);
        assertFalse(KingdomQuestGoal.PURCHASE.complete(free));
        assertFalse(KingdomQuestGoal.PUBLIC_BUILDING.complete(free));
        var actual = snapshot(2, Set.of(), Set.of("kingdom_smp:farmers_house"), true, false);
        assertTrue(KingdomQuestGoal.PURCHASE.complete(actual));
        assertFalse(KingdomQuestGoal.PUBLIC_BUILDING.complete(actual));
    }

    @Test void purchasedBuildingMustMatchTheCompletedEntitlement() {
        var mismatch = snapshot(2, Set.of("kingdom_smp:builders_workshop"), Set.of("kingdom_smp:farmers_house"), true, false);
        assertFalse(KingdomQuestGoal.PUBLIC_BUILDING.complete(mismatch));
        var complete = snapshot(2, Set.of("kingdom_smp:farmers_house"), Set.of("kingdom_smp:farmers_house"), true, false);
        assertTrue(KingdomQuestGoal.PUBLIC_BUILDING.complete(complete));
    }

    @Test void legacyHeadquartersAreRecognizedWithoutCallingThemATownHall() {
        assertTrue(KingdomQuestGoal.HEADQUARTERS.complete(snapshot(2, Set.of("kingdom_smp:founders_lodge"), Set.of(), true, false)));
        assertTrue(KingdomQuestGoal.HEADQUARTERS.complete(snapshot(2, Set.of("kingdom_smp:royal_town_hall"), Set.of(), true, false)));
        assertFalse(KingdomQuestGoal.HEADQUARTERS.complete(snapshot(1, Set.of("kingdom_smp:royal_town_hall"), Set.of(), false, false)));
    }

    @Test void bankAndPublicTradeRespectTheirTierEvenInAnAdminFixture() {
        var locked = snapshot(2, Set.of("kingdom_smp:royal_bank"), Set.of(), true, true);
        assertFalse(KingdomQuestGoal.BANK.complete(locked)); assertFalse(KingdomQuestGoal.PUBLIC_TRADE.complete(locked));
        var unlocked = snapshot(3, Set.of("kingdom_smp:royal_bank"), Set.of(), true, true);
        assertTrue(KingdomQuestGoal.BANK.complete(unlocked)); assertTrue(KingdomQuestGoal.PUBLIC_TRADE.complete(unlocked));
    }

    @Test void snapshotsCopyCollectionsAndRejectMalformedWorldFacts() {
        var buildings = new HashSet<>(Set.of("kingdom_smp:builders_hut"));
        var snap = snapshot(1, buildings, Set.of(), false, false); buildings.clear();
        assertTrue(KingdomQuestGoal.HUT.complete(snap));
        assertThrows(UnsupportedOperationException.class, () -> snap.completedBuildingIds().clear());
        assertThrows(IllegalArgumentException.class, () -> snapshot(0, Set.of(), Set.of(), false, false));
        assertThrows(IllegalArgumentException.class, () -> snapshot(13, Set.of(), Set.of(), false, false));
        assertThrows(IllegalArgumentException.class, () -> snapshot(1, Set.of("KINGDOM_SMP:hut"), Set.of(), false, false));
    }
    @Test void bountyPurchaseAndCompletionRespectItsRealTierWithoutChangingStableTaskIds() {
        var owned=Set.of("kingdom_smp:royal_bounty_hall");
        assertFalse(KingdomQuestGoal.PURCHASE.complete(snapshot(2,owned,owned,true,false)));
        assertFalse(KingdomQuestGoal.PUBLIC_BUILDING.complete(snapshot(2,owned,owned,true,false)));
        assertTrue(KingdomQuestGoal.PURCHASE.complete(snapshot(3,Set.of(),owned,true,false)));
        assertFalse(KingdomQuestGoal.PUBLIC_BUILDING.complete(snapshot(3,Set.of(),owned,true,false)));
        assertTrue(KingdomQuestGoal.PUBLIC_BUILDING.complete(snapshot(3,owned,owned,true,false)));
        assertEquals(0x4B534D5000001105L,KingdomQuestGoal.PURCHASE.taskId());
        assertFalse(KingdomQuestGoal.PURCHASE.complete(snapshot(2,Set.of(),Set.of("kingdom_smp:royal_bank"),true,false)));
    }
}
