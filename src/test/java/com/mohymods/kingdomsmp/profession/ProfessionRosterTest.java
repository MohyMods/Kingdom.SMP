package com.mohymods.kingdomsmp.profession;

import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProfessionRosterTest {
    private ProfessionQuestCatalog catalog() {
        // Isolated approval fixtures only. No production quest content is enabled by these IDs.
        List<ProfessionQuestCatalog.Definition> definitions = new ArrayList<>();
        for (var profession : Profession.values()) for (var rank : ProfessionRank.values()) for (int i = 1; i <= 8; i++) {
            definitions.add(new ProfessionQuestCatalog.Definition(profession.questId(rank, i), profession, rank, i,
                    i == 1 ? Set.of() : Set.of(profession.questId(rank, i - 1))));
        }
        return ProfessionQuestCatalog.approved(definitions);
    }
    private ProfessionRoster completeBand(ProfessionRoster roster, Profession profession, ProfessionRank rank, ProfessionQuestCatalog catalog) {
        for (int i = 1; i <= 8; i++) roster = roster.completeQuest(profession, profession.questId(rank, i), 100, catalog);
        return roster;
    }
    private ProfessionRoster three() {
        return ProfessionRoster.empty().enroll(Profession.FARMER).roster().enroll(Profession.BUILDER).roster().enroll(Profession.LUMBERJACK).roster();
    }
    @Test void exactRosterAndKnownBuildingIdentitiesAreStable() {
        assertEquals(12, Profession.values().length);
        Set<String> ids = new HashSet<>();Set<String> buildings = new HashSet<>();
        for (var profession : Profession.values()) {assertSame(profession, Profession.byId(profession.id()));assertTrue(ids.add(profession.id()));assertTrue(buildings.add(profession.buildingId()));}
        assertEquals("kingdom_smp:farmers_house", Profession.FARMER.buildingId());
        assertEquals("kingdom_smp:builders_workshop", Profession.BUILDER.buildingId());
        assertEquals("kingdom_smp:lumberjacks_lodge", Profession.LUMBERJACK.buildingId());
        assertThrows(IllegalArgumentException.class, () -> Profession.byId("Farmer"));
        assertThrows(IllegalArgumentException.class, () -> Profession.byId("kingdom_smp:farmer"));
        assertThrows(IllegalArgumentException.class, () -> Profession.byId("new_profession"));
        assertSame(ProfessionRank.JOURNEYMAN, ProfessionRank.byId("journeyman"));
        assertThrows(IllegalArgumentException.class, () -> ProfessionRank.byId("grandmaster"));
    }
    @Test void firstThreeNewSelectionsAreFreeAndFourthRequiresExplicitSwitch() {
        var initial = ProfessionRoster.empty();var farmer = initial.enroll(Profession.FARMER);
        assertEquals(0, farmer.fee());assertTrue(farmer.changed());assertTrue(initial.active().isEmpty());
        var roster = three();assertEquals(3, roster.active().size());assertEquals(3, roster.tracks().size());
        assertThrows(IllegalStateException.class, () -> roster.enroll(Profession.MINER));
        assertEquals(3, roster.active().size());assertFalse(roster.tracks().containsKey(Profession.MINER));
    }
    @Test void paidSwitchRetainsOldProgressAndChangesOnlyTheRequestedActiveProfession() {
        var catalog = catalog();var roster = three().completeQuest(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.APPRENTICE, 1), 75, catalog);
        var switched = roster.switchProfession(Profession.FARMER, Profession.MINER);
        assertEquals(15_000, switched.fee());assertTrue(switched.changed());
        assertTrue(roster.isActive(Profession.FARMER));assertFalse(switched.roster().isActive(Profession.FARMER));
        assertTrue(switched.roster().isActive(Profession.MINER));assertTrue(switched.roster().isActive(Profession.BUILDER));
        assertTrue(switched.roster().isActive(Profession.LUMBERJACK));assertEquals(75, switched.roster().track(Profession.FARMER).xp());
        assertEquals(roster.track(Profession.FARMER), switched.roster().track(Profession.FARMER));
        assertEquals(4, switched.roster().tracks().size());assertEquals(3, switched.roster().active().size());
    }
    @Test void noOpRequestsDoNotChargeOrProduceANewRoster() {
        var roster = three();var enroll = roster.enroll(Profession.FARMER);var same = roster.switchProfession(Profession.FARMER, Profession.FARMER);
        assertSame(roster, enroll.roster());assertFalse(enroll.changed());assertEquals(0, enroll.fee());
        assertSame(roster, same.roster());assertFalse(same.changed());assertEquals(0, same.fee());
        assertThrows(IllegalStateException.class, () -> roster.switchProfession(Profession.MINER, Profession.FARMER));
        assertThrows(IllegalStateException.class, () -> roster.switchProfession(Profession.FARMER, Profession.BUILDER));
    }
    @Test void deactivationCannotCreateAFreeFourthProfessionOrFreeReactivation() {
        var roster = three().deactivate(Profession.FARMER).roster();
        assertEquals(3, roster.tracks().size());assertEquals(2, roster.active().size());
        var reactivate = roster.enroll(Profession.FARMER);assertEquals(15_000, reactivate.fee());
        var fourth = roster.enroll(Profession.MINER);assertEquals(15_000, fourth.fee());
        assertEquals(4, fourth.roster().tracks().size());assertEquals(3, fourth.roster().active().size());
        assertFalse(roster.deactivate(Profession.FARMER).changed());
    }
    @Test void returnedCollectionsAndInputTrackDataRemainImmutable() {
        var roster = three();assertThrows(UnsupportedOperationException.class, () -> roster.active().clear());
        assertThrows(UnsupportedOperationException.class, () -> roster.tracks().clear());
        Set<String> source = new HashSet<>();source.add(Profession.FARMER.questId(ProfessionRank.APPRENTICE, 1));
        var track = new ProfessionRoster.Track(5, source);source.clear();assertEquals(1, track.completedQuestIds().size());
        assertThrows(UnsupportedOperationException.class, () -> track.completedQuestIds().clear());
        var saved = roster.save();saved.getList("tracks", Tag.TAG_COMPOUND).getCompound(0).putLong("xp", 999);
        assertEquals(0, roster.track(Profession.FARMER).xp());
    }
    @Test void disabledCatalogueBlocksActualCompletionAndAdvancement() {
        var roster = ProfessionRoster.empty().enroll(Profession.FARMER).roster();var disabled = ProfessionQuestCatalog.disabled();
        assertFalse(disabled.enabled());assertTrue(disabled.definitions().isEmpty());
        assertThrows(IllegalStateException.class, () -> roster.completeQuest(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.APPRENTICE, 1), 100, disabled));
        assertEquals(ProfessionRank.APPRENTICE, roster.rank(Profession.FARMER, disabled));
        assertFalse(roster.perksEligible(Profession.FARMER, ProfessionRank.MASTER, disabled));
    }
    @Test void rankRequiresApprovedQuestProofsAndMasterTrialsCannotDeadlock() {
        var catalog = catalog();var roster = ProfessionRoster.empty().enroll(Profession.FARMER).roster();
        assertEquals(ProfessionRank.APPRENTICE, roster.rank(Profession.FARMER, catalog));
        assertFalse(catalog.mayAttempt(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.JOURNEYMAN, 1), Set.of()));
        assertFalse(catalog.mayAttempt(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.MASTER, 1), Set.of()));
        roster = completeBand(roster, Profession.FARMER, ProfessionRank.APPRENTICE, catalog);
        assertEquals(ProfessionRank.JOURNEYMAN, roster.rank(Profession.FARMER, catalog));
        roster = completeBand(roster, Profession.FARMER, ProfessionRank.JOURNEYMAN, catalog);
        assertEquals(ProfessionRank.JOURNEYMAN, roster.rank(Profession.FARMER, catalog));
        assertTrue(catalog.mayAttempt(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.MASTER, 1), roster.track(Profession.FARMER).completedQuestIds()));
        roster = completeBand(roster, Profession.FARMER, ProfessionRank.MASTER, catalog);
        assertEquals(ProfessionRank.MASTER, roster.rank(Profession.FARMER, catalog));assertEquals(24, roster.track(Profession.FARMER).completedQuestIds().size());
        assertEquals(2400, roster.track(Profession.FARMER).xp());assertTrue(roster.perksEligible(Profession.FARMER, ProfessionRank.MASTER, catalog));
    }
    @Test void rawXpAndIncompleteCatalogueCannotGrantAMasterRank() {
        var catalog = catalog();var roster = ProfessionRoster.empty().enroll(Profession.FARMER).roster()
                .completeQuest(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.APPRENTICE, 1), ProfessionRoster.MAX_XP, catalog);
        assertEquals(ProfessionRank.APPRENTICE, roster.rank(Profession.FARMER, catalog));
        var full = completeBand(ProfessionRoster.empty().enroll(Profession.FARMER).roster(), Profession.FARMER, ProfessionRank.APPRENTICE, catalog);
        var partial = ProfessionQuestCatalog.approved(List.of(catalog.definition(Profession.FARMER.questId(ProfessionRank.APPRENTICE, 1)).orElseThrow()));
        assertEquals(ProfessionRank.APPRENTICE, full.rank(Profession.FARMER, partial));
    }
    @Test void duplicateCompletionDoesNotAwardXpAgainAndPrerequisitesAreEnforced() {
        var catalog = catalog();var roster = ProfessionRoster.empty().enroll(Profession.FARMER).roster();
        String first = Profession.FARMER.questId(ProfessionRank.APPRENTICE, 1), second = Profession.FARMER.questId(ProfessionRank.APPRENTICE, 2);
        assertThrows(IllegalStateException.class, () -> roster.completeQuest(Profession.FARMER, second, 100, catalog));
        var credited = roster.completeQuest(Profession.FARMER, first, 100, catalog);
        assertSame(credited, credited.completeQuest(Profession.FARMER, first, 100, catalog));assertEquals(100, credited.track(Profession.FARMER).xp());
        assertThrows(IllegalArgumentException.class, () -> roster.completeQuest(Profession.FARMER, "kingdom_smp:miner/a01", 100, catalog));
        assertThrows(IllegalArgumentException.class, () -> roster.completeQuest(Profession.FARMER, "not-a-quest", 100, catalog));
    }
    @Test void inactiveProgressAndRankSurviveSwitchButPerksAndQuestCreditStop() {
        var catalog = catalog();var farmer = completeBand(ProfessionRoster.empty().enroll(Profession.FARMER).roster(), Profession.FARMER, ProfessionRank.APPRENTICE, catalog);
        var inactive = farmer.switchProfession(Profession.FARMER, Profession.MINER).roster();
        assertEquals(ProfessionRank.JOURNEYMAN, inactive.rank(Profession.FARMER, catalog));assertEquals(800, inactive.track(Profession.FARMER).xp());
        assertFalse(inactive.perksEligible(Profession.FARMER, ProfessionRank.APPRENTICE, catalog));
        assertThrows(IllegalStateException.class, () -> inactive.completeQuest(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.JOURNEYMAN, 1), 100, catalog));
        var restored = ProfessionRoster.load(inactive.save());assertEquals(inactive, restored);
        var activeAgain = restored.enroll(Profession.FARMER);assertEquals(15_000, activeAgain.fee());
        assertTrue(activeAgain.roster().perksEligible(Profession.FARMER, ProfessionRank.JOURNEYMAN, catalog));assertEquals(farmer.track(Profession.FARMER), activeAgain.roster().track(Profession.FARMER));
    }
    @Test void allTwelveTracksRoundTripInDeterministicBoundedState() {
        var roster = ProfessionRoster.empty().enroll(Profession.FARMER).roster();var current = Profession.FARMER;
        for (var profession : Profession.values()) if (profession != current && !roster.tracks().containsKey(profession)) {
            roster = roster.switchProfession(current, profession).roster();current = profession;
        }
        assertEquals(12, roster.tracks().size());assertEquals(1, roster.active().size());
        var saved = roster.save();var restored = ProfessionRoster.load(saved);assertEquals(roster, restored);assertEquals(saved, restored.save());assertEquals(roster.hashCode(), restored.hashCode());
    }
    @Test void stateRejectsWrongSchemaUnknownFieldsDuplicateTracksAndTooManyActive() {
        var original = three().save();var wrong = original.copy();wrong.putInt("schema", 2);assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(wrong));
        var unknown = original.copy();unknown.putString("secretMaster", "true");assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(unknown));
        var duplicate = original.copy();var rows = duplicate.getList("tracks", Tag.TAG_COMPOUND);rows.add(rows.getCompound(0).copy());assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(duplicate));
        var fourth = three().switchProfession(Profession.FARMER, Profession.MINER).roster().save();
        for (var row : fourth.getList("tracks", Tag.TAG_COMPOUND)) ((CompoundTag) row).putBoolean("active", true);
        assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(fourth));
        var malformed = original.copy();malformed.getList("tracks", Tag.TAG_COMPOUND).getCompound(0).putString("id", "unknown");assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(malformed));
    }
    @Test void stateRejectsForeignDuplicateUnknownAndOversizedQuestLists() {
        for (String id : new String[]{"kingdom_smp:miner/a01", "kingdom_smp:farmer/a09", "farmer/a01", ""}) {
            var data = ProfessionRoster.empty().enroll(Profession.FARMER).roster().save();var quests = new ListTag();quests.add(StringTag.valueOf(id));
            data.getList("tracks", Tag.TAG_COMPOUND).getCompound(0).put("quests", quests);assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(data));
        }
        var duplicate = ProfessionRoster.empty().enroll(Profession.FARMER).roster().save();var quests = new ListTag();
        quests.add(StringTag.valueOf("kingdom_smp:farmer/a01"));quests.add(StringTag.valueOf("kingdom_smp:farmer/a01"));
        duplicate.getList("tracks", Tag.TAG_COMPOUND).getCompound(0).put("quests", quests);assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(duplicate));
        var oversized = duplicate.copy();var many = new ListTag();for (int i = 0; i < 25; i++) many.add(StringTag.valueOf("kingdom_smp:farmer/a01"));
        oversized.getList("tracks", Tag.TAG_COMPOUND).getCompound(0).put("quests", many);assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(oversized));
    }
    @Test void stateAndTransitionsRejectMalformedTypesAndXpOverflow() {
        var data = ProfessionRoster.empty().enroll(Profession.FARMER).roster().save();
        for (long xp : new long[]{-1, ProfessionRoster.MAX_XP + 1, Long.MAX_VALUE}) {
            var malformed = data.copy();malformed.getList("tracks", Tag.TAG_COMPOUND).getCompound(0).putLong("xp", xp);assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(malformed));
        }
        var wrongType = data.copy();wrongType.getList("tracks", Tag.TAG_COMPOUND).getCompound(0).putInt("xp", 1);assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(wrongType));
        var invalidFlag = data.copy();invalidFlag.getList("tracks", Tag.TAG_COMPOUND).getCompound(0).putByte("active", (byte) 2);assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(invalidFlag));
        var wrongList = data.copy();var strings = new ListTag();strings.add(StringTag.valueOf("farmer"));wrongList.put("tracks", strings);assertThrows(IllegalArgumentException.class, () -> ProfessionRoster.load(wrongList));
        assertThrows(IllegalArgumentException.class, () -> new ProfessionRoster.Transition(ProfessionRoster.empty(), 1, true));
        assertThrows(IllegalArgumentException.class, () -> new ProfessionRoster.Transition(ProfessionRoster.empty(), 15_000, false));
        var catalog = catalog();var fullXp = ProfessionRoster.empty().enroll(Profession.FARMER).roster().completeQuest(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.APPRENTICE, 1), ProfessionRoster.MAX_XP, catalog);
        assertThrows(IllegalArgumentException.class, () -> fullXp.completeQuest(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.APPRENTICE, 2), 1, catalog));
        assertThrows(IllegalArgumentException.class, () -> fullXp.completeQuest(Profession.FARMER, Profession.FARMER.questId(ProfessionRank.APPRENTICE, 2), -1, catalog));
    }
    @Test void catalogueRejectsDuplicateForeignMissingAndForwardPrerequisites() {
        var first = new ProfessionQuestCatalog.Definition("kingdom_smp:farmer/a01", Profession.FARMER, ProfessionRank.APPRENTICE, 1, Set.of());
        assertThrows(IllegalArgumentException.class, () -> ProfessionQuestCatalog.approved(List.of(first, first)));
        assertThrows(IllegalArgumentException.class, () -> new ProfessionQuestCatalog.Definition("kingdom_smp:farmer/a01", Profession.MINER, ProfessionRank.APPRENTICE, 1, Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new ProfessionQuestCatalog.Definition("kingdom_smp:farmer/a01", Profession.FARMER, ProfessionRank.APPRENTICE, 1, Set.of("kingdom_smp:miner/a01")));
        var missing = new ProfessionQuestCatalog.Definition("kingdom_smp:farmer/a02", Profession.FARMER, ProfessionRank.APPRENTICE, 2, Set.of("kingdom_smp:farmer/a01"));
        assertThrows(IllegalArgumentException.class, () -> ProfessionQuestCatalog.approved(List.of(missing)));
        var forward = new ProfessionQuestCatalog.Definition("kingdom_smp:farmer/a01", Profession.FARMER, ProfessionRank.APPRENTICE, 1, Set.of("kingdom_smp:farmer/a02"));
        var second = new ProfessionQuestCatalog.Definition("kingdom_smp:farmer/a02", Profession.FARMER, ProfessionRank.APPRENTICE, 2, Set.of());
        assertThrows(IllegalArgumentException.class, () -> ProfessionQuestCatalog.approved(List.of(forward, second)));
    }
}
