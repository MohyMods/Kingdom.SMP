package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.integration.aircraft.FlightBook;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FlightEconomyTest {
  @TempDir Path directory;
  final UUID pilot = UUID.randomUUID(),
      crew = UUID.randomUUID(),
      registration = UUID.randomUUID(),
      origin = UUID.randomUUID(),
      target = UUID.randomUUID();

  EconomyStore active() throws Exception {
    var s = EconomyStore.open(directory);
    s.adminSet(UUID.randomUUID(), crew, pilot, 30000, "Private dispatch test");
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

  UUID id(EconomyStore s, UUID actor, long day) {
    return FlightBook.missionId(s.id(), actor, day);
  }

  void award(EconomyStore s, UUID actor, long day, long budget) throws Exception {
    s.awardFlight(
        id(s, actor, day), actor, day, 500, budget, registration, 0, 128, 128, 8, origin, target);
  }

  @Test
  void onceOnlyMailAndFinitePoolSurviveRestartClaimAndArchive() throws Exception {
    var s = active();
    award(s, pilot, 100, 500);
    award(s, pilot, 100, 500);
    assertEquals(10000, s.balance(pilot));
    assertEquals(500, s.flightSpent(100));
    assertEquals(1, s.mailbox(pilot).size());
    s.claimCoins(pilot, id(s, pilot, 100));
    s = EconomyStore.open(directory);
    assertEquals(10500, s.balance(pilot));
    assertEquals(500, s.flightSpent(100));
    s.archive(crew, "Private dispatch archive");
    assertEquals(500, EconomyStore.open(directory).flightSpent(100));
  }

  @Test
  void sharingRevocationBudgetAndDailyCutoffAreCanonical() throws Exception {
    var s = active();
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, true);
    award(s, pilot, 100, 500);
    assertThrows(IllegalStateException.class, () -> award(s, crew, 100, 500));
    assertThrows(IllegalStateException.class, () -> award(s, crew, 100, 1000));
    award(s, crew, 101, 1000);
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, false);
    assertThrows(IllegalStateException.class, () -> award(s, crew, 102, 1000));
    assertThrows(IllegalStateException.class, () -> award(s, pilot, 99, 1000));
    assertEquals(500, s.flightSpent(101));
  }

  @Test
  void missingProofChangedGenerationAndDifferentWinnerIdentityCannotPay() throws Exception {
    var s = active();
    assertThrows(
        IllegalStateException.class,
        () ->
            s.awardFlight(
                UUID.randomUUID(),
                pilot,
                100,
                500,
                5000,
                registration,
                1,
                128,
                128,
                8,
                origin,
                target));
    assertThrows(
        IllegalStateException.class,
        () ->
            s.awardFlight(
                UUID.randomUUID(),
                pilot,
                100,
                500,
                5000,
                registration,
                0,
                128,
                127,
                8,
                origin,
                target));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            s.awardFlight(
                UUID.randomUUID(),
                pilot,
                100,
                500,
                5000,
                registration,
                0,
                128,
                128,
                8,
                origin,
                origin));
    award(s, pilot, 100, 5000);
    assertThrows(
        IllegalStateException.class,
        () ->
            s.awardFlight(
                UUID.randomUUID(),
                pilot,
                100,
                500,
                5000,
                registration,
                0,
                128,
                128,
                8,
                origin,
                target));
    assertEquals(500, s.flightSpent(100));
  }

  @Test
  void uncertainJournalWriteRecoversOneRewardWithoutAnotherMint() throws Exception {
    active();
    var failed =
        EconomyStore.open(
            directory,
            (path, data) -> {
              com.mohymods.kingdomsmp.persistence.AtomicNbt.write(path, data);
              throw new java.io.IOException("Private after-rename dispatch fault");
            });
    assertThrows(java.io.IOException.class, () -> award(failed, pilot, 100, 500));
    assertTrue(failed.quarantined());
    var loaded = EconomyStore.open(directory);
    award(loaded, pilot, 100, 500);
    assertEquals(500, loaded.flightSpent(100));
    assertEquals(1, loaded.mailbox(pilot).size());
    assertFalse(loaded.mailbox(pilot).getFirst().claimed());
  }

  @Test
  void competingEligiblePilotsCannotOverdrawTheLastReward() throws Exception {
    var s = active();
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, true);
    var start = new java.util.concurrent.CountDownLatch(1);
    var paid = new java.util.concurrent.atomic.AtomicInteger();
    var blocked = new java.util.concurrent.atomic.AtomicInteger();
    var unexpected = new java.util.concurrent.atomic.AtomicReference<Throwable>();
    var threads = new ArrayList<Thread>();
    for (var actor : List.of(pilot, crew)) {
      var thread =
          new Thread(
              () -> {
                try {
                  start.await();
                  award(s, actor, 100, 500);
                  paid.incrementAndGet();
                } catch (IllegalStateException expected) {
                  blocked.incrementAndGet();
                } catch (Throwable failure) {
                  unexpected.set(failure);
                }
              });
      threads.add(thread);
      thread.start();
    }
    start.countDown();
    for (var thread : threads) thread.join();
    assertNull(unexpected.get());
    assertEquals(1, paid.get());
    assertEquals(1, blocked.get());
    assertEquals(500, EconomyStore.open(directory).flightSpent(100));
  }
}
