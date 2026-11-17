package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.integration.aircraft.FlightBook;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ScoutEconomyTest {
  @TempDir Path directory;
  final UUID pilot = UUID.randomUUID(), crew = UUID.randomUUID(), registration = UUID.randomUUID();

  EconomyStore active() throws Exception {
    var s = EconomyStore.open(directory);
    s.adminSet(UUID.randomUUID(), crew, pilot, 30000, "Private reconnaissance funds");
    var item = new CompoundTag();
    item.putString("id", "immersive_aircraft:biplane");
    item.putInt("count", 1);
    s.buyAircraft(registration, pilot, "immersive_aircraft:biplane", 20000, item);
    var body = new CompoundTag();
    body.putString("id", "immersive_aircraft:biplane");
    body.putUUID("UUID", UUID.randomUUID());
    var receipt =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), pilot, registration, body, "minecraft:overworld");
    s.recoverItem(new EconomyStore.Receipt(s.id(), receipt.sequence()));
    s.activateAircraft(UUID.randomUUID(), pilot, registration);
    return s;
  }

  List<ScoutState.Region> regions(int first) {
    return List.of(
        new ScoutState.Region(first, 0, "minecraft:plains"),
        new ScoutState.Region(first + 1, 0, "minecraft:forest"));
  }

  UUID id(EconomyStore s, UUID actor, long day) {
    return FlightBook.scoutMissionId(s.id(), actor, day);
  }

  void award(EconomyStore s, UUID actor, long day, List<ScoutState.Region> observations)
      throws Exception {
    s.awardScout(
        id(s, actor, day),
        actor,
        day,
        300,
        600,
        registration,
        0,
        2,
        256,
        512,
        512,
        8,
        0,
        0,
        observations);
  }

  @Test
  void sharedRecognitionAndOnceOnlyMailSurviveClaimArchiveAndRestart() throws Exception {
    var s = active();
    award(s, pilot, 100, regions(20));
    award(s, pilot, 100, regions(20));
    assertEquals(2, s.surveyedRegions());
    assertEquals(300, s.scoutSpent(100));
    assertEquals(pilot, s.surveyRecognition(regions(20).getFirst().key()).orElseThrow().pilot());
    s.claimCoins(pilot, id(s, pilot, 100));
    s.archive(crew, "Private survey history");
    s = EconomyStore.open(directory);
    assertEquals(10300, s.balance(pilot));
    assertEquals(2, s.surveyedRegions());
    assertTrue(s.scoutAward(pilot, 100).isPresent());
  }

  @Test
  void overlappingReportsCannotPayTwiceAndUnmappedRegionsCanStillBeReported() throws Exception {
    var s = active();
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, true);
    award(s, pilot, 100, regions(20));
    assertThrows(IllegalStateException.class, () -> award(s, crew, 100, regions(20)));
    assertEquals(300, s.scoutSpent(100));
    award(s, crew, 100, regions(22));
    assertEquals(600, s.scoutSpent(100));
    assertEquals(4, s.surveyedRegions());
    assertThrows(IllegalArgumentException.class, () -> award(s, pilot, 100, regions(24)));
    assertEquals(4, s.surveyedRegions());
  }

  @Test
  void insufficientTravelNearbyDuplicatesAndRevokedAccessNeverEarnRecognition() throws Exception {
    var s = active();
    assertThrows(
        IllegalStateException.class,
        () ->
            s.awardScout(
                UUID.randomUUID(),
                pilot,
                100,
                300,
                600,
                registration,
                0,
                2,
                256,
                512,
                511,
                8,
                0,
                0,
                regions(20)));
    assertThrows(IllegalStateException.class, () -> award(s, pilot, 100, regions(0)));
    var repeated = List.of(regions(20).getFirst(), regions(20).getFirst());
    assertThrows(IllegalArgumentException.class, () -> award(s, pilot, 100, repeated));
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, true);
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, false);
    assertThrows(IllegalStateException.class, () -> award(s, crew, 100, regions(20)));
    assertEquals(0, s.surveyedRegions());
    assertTrue(s.mailbox(pilot).isEmpty());
  }

  @Test
  void uncertainAfterRenameCommitRecoversRecognitionAndMailOnlyOnce() throws Exception {
    active();
    var failed =
        EconomyStore.open(
            directory,
            (path, data) -> {
              AtomicNbt.write(path, data);
              throw new java.io.IOException("Private survey after-rename fault");
            });
    assertThrows(java.io.IOException.class, () -> award(failed, pilot, 100, regions(20)));
    assertTrue(failed.quarantined());
    var s = EconomyStore.open(directory);
    award(s, pilot, 100, regions(20));
    assertEquals(2, s.surveyedRegions());
    assertEquals(300, s.scoutSpent(100));
    assertEquals(1, s.mailbox(pilot).size());
  }

  @Test
  void newDaysNeverPayForAlreadyMappedRegionsAndBackwardsClockCannotReopenQuota() throws Exception {
    var s = active();
    award(s, pilot, 100, regions(20));
    assertThrows(IllegalStateException.class, () -> award(s, pilot, 101, regions(20)));
    award(s, pilot, 101, regions(22));
    assertThrows(IllegalStateException.class, () -> award(s, pilot, 99, regions(24)));
    assertEquals(4, s.surveyedRegions());
  }

  @Test
  void exhaustedPinnedPoolNeverRecognizesOrPaysAThirdReport() throws Exception {
    var s = active();
    var third = UUID.randomUUID();
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, true);
    s.shareAircraft(UUID.randomUUID(), pilot, registration, third, true);
    award(s, pilot, 100, regions(20));
    award(s, crew, 100, regions(22));
    assertThrows(IllegalStateException.class, () -> award(s, third, 100, regions(24)));
    assertThrows(
        IllegalStateException.class,
        () ->
            s.awardScout(
                id(s, third, 100),
                third,
                100,
                300,
                900,
                registration,
                0,
                2,
                256,
                512,
                512,
                8,
                0,
                0,
                regions(24)));
    assertEquals(600, s.scoutSpent(100));
    assertEquals(4, s.surveyedRegions());
    assertFalse(s.surveyed(regions(24).getFirst().key()));
    assertTrue(s.mailbox(third).isEmpty());
  }

  @Test
  void failureBeforeJournalWriteLeavesRegionsAndRewardAvailableAfterRecovery() throws Exception {
    active();
    var failed =
        EconomyStore.open(
            directory,
            (path, data) -> {
              throw new java.io.IOException("Private survey before-write fault");
            });
    assertThrows(java.io.IOException.class, () -> award(failed, pilot, 100, regions(20)));
    assertTrue(failed.quarantined());
    var recovered = EconomyStore.open(directory);
    assertEquals(0, recovered.surveyedRegions());
    assertEquals(0, recovered.scoutSpent(100));
    assertTrue(recovered.mailbox(pilot).isEmpty());
    award(recovered, pilot, 100, regions(20));
    assertEquals(2, recovered.surveyedRegions());
    assertEquals(1, recovered.mailbox(pilot).size());
  }
}
