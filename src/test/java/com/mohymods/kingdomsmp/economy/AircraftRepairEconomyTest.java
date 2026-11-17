package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AircraftRepairEconomyTest {
  @TempDir Path directory;
  final UUID owner = UUID.randomUUID(), other = UUID.randomUUID(), registration = UUID.randomUUID();

  EconomyStore open() throws IOException {
    return EconomyStore.open(directory);
  }

  EconomyStore active() throws Exception {
    var s = open();
    s.adminSet(UUID.randomUUID(), other, owner, 30000, "Private repair test funds");
    var item = new CompoundTag();
    item.putString("id", "immersive_aircraft:biplane");
    item.putInt("count", 1);
    s.buyAircraft(registration, owner, "immersive_aircraft:biplane", 20000, item);
    var body = new CompoundTag();
    body.putString("id", "immersive_aircraft:biplane");
    body.putUUID("UUID", UUID.randomUUID());
    var receipt =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), owner, registration, body, "minecraft:overworld");
    s.recoverItem(new EconomyStore.Receipt(s.id(), receipt.sequence()));
    s.activateAircraft(UUID.randomUUID(), owner, registration);
    return s;
  }

  @Test
  void paidReservationSurvivesRestartAndBecomesAChargeOnlyAfterDelivery() throws Exception {
    var s = active();
    UUID ticket = UUID.randomUUID();
    s.buyAircraftRepair(ticket, owner, registration, .5f, 2000);
    assertEquals(9000, s.balance(owner));
    assertEquals(1000, s.aircraftRepairEscrow());
    assertEquals(20000, s.shopCoinsSpent());
    s = open();
    assertEquals(ticket, s.aircraftRepair(registration).orElseThrow().id());
    UUID ack = UUID.randomUUID();
    s.settleAircraftRepair(ack, owner, ticket, false);
    s.settleAircraftRepair(ack, owner, ticket, false);
    s = open();
    assertEquals(9000, s.balance(owner));
    assertEquals(0, s.aircraftRepairEscrow());
    assertEquals(21000, s.shopCoinsSpent());
    assertEquals(ticket, s.deliveredAircraftRepair(registration).orElseThrow());
    assertTrue(s.aircraftRepairs().isEmpty());
    s.archive(other, "Private repair history archive");
    assertEquals(21000, open().shopCoinsSpent());
  }

  @Test
  void canonicalRefundIsOwnerBoundAndReturnsTheExactReservationOnce() throws Exception {
    var s = active();
    UUID ticket = UUID.randomUUID();
    s.buyAircraftRepair(ticket, owner, registration, .7f, 2000);
    assertEquals(9400, s.balance(owner));
    assertThrows(
        IllegalStateException.class,
        () -> s.settleAircraftRepair(UUID.randomUUID(), other, ticket, true));
    UUID refund = UUID.randomUUID();
    s.settleAircraftRepair(refund, owner, ticket, true);
    s.settleAircraftRepair(refund, owner, ticket, true);
    assertEquals(10000, open().balance(owner));
    assertEquals(20000, open().shopCoinsSpent());
    assertEquals(0, open().aircraftRepairEscrow());
    assertThrows(
        NoSuchElementException.class,
        () -> s.settleAircraftRepair(UUID.randomUUID(), owner, ticket, false));
  }

  @Test
  void competingRepairAndIneligibleActorNeverDoubleReserveOrDebit() throws Exception {
    var s = active();
    s.shareAircraft(UUID.randomUUID(), owner, registration, other, true);
    assertThrows(
        IllegalStateException.class,
        () -> s.buyAircraftRepair(UUID.randomUUID(), other, registration, .5f, 2000));
    UUID ticket = UUID.randomUUID();
    s.buyAircraftRepair(ticket, owner, registration, .5f, 2000);
    s.buyAircraftRepair(ticket, owner, registration, .5f, 2000);
    assertThrows(
        IllegalStateException.class,
        () -> s.buyAircraftRepair(UUID.randomUUID(), owner, registration, .5f, 2000));
    assertEquals(9000, s.balance(owner));
    assertEquals(1, open().aircraftRepairs().size());
  }

  @Test
  void uncertainReservationAndRefundAfterRenameRecoverWithoutAnExtraPayment() throws Exception {
    active();
    UUID ticket = UUID.randomUUID();
    var failing =
        EconomyStore.open(
            directory,
            (path, tag) -> {
              AtomicNbt.write(path, tag);
              throw new IOException("Private after-rename fault");
            });
    assertThrows(
        IOException.class, () -> failing.buyAircraftRepair(ticket, owner, registration, .5f, 2000));
    assertTrue(failing.quarantined());
    var s = open();
    assertEquals(9000, s.balance(owner));
    assertEquals(1000, s.aircraftRepairEscrow());
    var refund = UUID.randomUUID();
    var failedRefund =
        EconomyStore.open(
            directory,
            (path, tag) -> {
              AtomicNbt.write(path, tag);
              throw new IOException("Private refund after-rename fault");
            });
    assertThrows(
        IOException.class, () -> failedRefund.settleAircraftRepair(refund, owner, ticket, true));
    s = open();
    s.settleAircraftRepair(refund, owner, ticket, true);
    assertEquals(10000, s.balance(owner));
    assertTrue(s.aircraftRepairs().isEmpty());
  }

  @Test
  void repairPricesRejectDeadFullAndPoisonedHealthAndRoundDeterministically() {
    assertEquals(600, AircraftState.repairPrice(.7f, 2000));
    assertEquals(1334, AircraftState.repairPrice(.33333334f, 2000));
    assertEquals(1, AircraftState.repairPrice(.99999f, 100));
    for (float value :
        new float[] {0, 1, -1, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY})
      assertThrows(IllegalArgumentException.class, () -> AircraftState.repairPrice(value, 2000));
  }

  @Test
  void deliveredReceiptIsArchivedAndRetiredOnlyWithItsLegitimateAirframeGeneration()
      throws Exception {
    var s = active();
    UUID paid = UUID.randomUUID();
    s.buyAircraftRepair(paid, owner, registration, .5f, 2000);
    s.settleAircraftRepair(UUID.randomUUID(), owner, paid, false);
    UUID canceled = UUID.randomUUID();
    s.buyAircraftRepair(canceled, owner, registration, .5f, 2000);
    s.settleAircraftRepair(UUID.randomUUID(), owner, canceled, true);
    assertEquals(paid, open().deliveredAircraftRepair(registration).orElseThrow());
    s.archive(other, "Private native service receipt archive");
    assertEquals(paid, open().deliveredAircraftRepair(registration).orElseThrow());
    var item = new CompoundTag();
    item.putString("id", "immersive_aircraft:biplane");
    item.putInt("count", 1);
    s.packAircraft(UUID.randomUUID(), owner, registration, item);
    assertTrue(open().deliveredAircraftRepair(registration).isEmpty());
    assertEquals(1, open().aircraft(registration).orElseThrow().generation());
  }
}
