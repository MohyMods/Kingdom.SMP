package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CargoEconomyTest {
  @TempDir Path directory;
  final UUID pilot = UUID.randomUUID(),
      crew = UUID.randomUUID(),
      registration = UUID.randomUUID(),
      entity = UUID.randomUUID();

  EconomyStore active() throws Exception {
    var s = EconomyStore.open(directory);
    s.adminSet(UUID.randomUUID(), crew, pilot, 30000, "Private cargo protocol test");
    var item = new CompoundTag();
    item.putString("id", "immersive_aircraft:biplane");
    item.putInt("count", 1);
    s.buyAircraft(registration, pilot, "immersive_aircraft:biplane", 20000, item);
    var body = new CompoundTag();
    body.putString("id", "immersive_aircraft:biplane");
    body.putUUID("UUID", entity);
    var receipt =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), pilot, registration, body, "minecraft:overworld");
    s.recoverItem(new EconomyStore.Receipt(s.id(), receipt.sequence()));
    s.activateAircraft(UUID.randomUUID(), pilot, registration);
    return s;
  }

  CargoState.Terms terms(UUID actor) {
    return new CargoState.Terms(
        UUID.randomUUID(),
        actor,
        100,
        "minecraft:wheat",
        64,
        128,
        500,
        500,
        UUID.randomUUID(),
        UUID.randomUUID());
  }

  CargoState.Reservation reservation(CargoState.Terms t) {
    return new CargoState.Reservation(
        UUID.randomUUID(),
        t.id(),
        registration,
        entity,
        0,
        List.of(new CargoState.Slot(8, 40, 40), new CargoState.Slot(9, 40, 24)));
  }

  @Test
  void reservedBudgetAndOneMailRecoverAcrossRestartClaimAndArchive() throws Exception {
    var s = active();
    var terms = terms(pilot);
    s.acceptCargo(terms);
    s.acceptCargo(terms);
    var ticket = reservation(terms);
    s.reserveCargo(pilot, ticket, 128, 8);
    s.reserveCargo(pilot, ticket, 128, 8);
    assertEquals(10000, s.balance(pilot));
    assertEquals(500, s.cargoHeld(100));
    assertEquals(0, s.cargoPaid(100));
    s = EconomyStore.open(directory);
    assertEquals(ticket, s.pendingCargo(registration).orElseThrow());
    UUID ack = UUID.randomUUID();
    s.settleCargo(ack, pilot, ticket.id(), true);
    s.settleCargo(ack, pilot, ticket.id(), true);
    assertEquals(500, s.cargoPaid(100));
    assertEquals(0, s.cargoHeld(100));
    assertEquals(ticket.id(), s.deliveredCargoReceipt(registration).orElseThrow());
    s.claimCoins(pilot, ticket.id());
    s.claimCoins(pilot, ticket.id());
    s.archive(crew, "Private cargo archive");
    s = EconomyStore.open(directory);
    assertEquals(10500, s.balance(pilot));
    assertEquals(CargoState.Stage.DELIVERED, s.cargo(pilot, 100).orElseThrow().stage());
    assertTrue(s.pendingCargo().isEmpty());
  }

  @Test
  void canonicalCancelReleasesBudgetWithoutCreditingCoinsAndCanBeRetried() throws Exception {
    var s = active();
    var terms = terms(pilot);
    s.acceptCargo(terms);
    var ticket = reservation(terms);
    s.reserveCargo(pilot, ticket, 128, 8);
    assertThrows(
        IllegalStateException.class,
        () -> s.settleCargo(UUID.randomUUID(), crew, ticket.id(), false));
    UUID cancel = UUID.randomUUID();
    s.settleCargo(cancel, pilot, ticket.id(), false);
    s.settleCargo(cancel, pilot, ticket.id(), false);
    assertEquals(0, s.cargoHeld(100));
    assertEquals(0, s.cargoPaid(100));
    assertTrue(s.mailbox(pilot).isEmpty());
    var retry = reservation(terms);
    s.reserveCargo(pilot, retry, 128, 8);
    assertEquals(500, EconomyStore.open(directory).cargoHeld(100));
  }

  @Test
  void crewRevocationWrongBodyGenerationInsufficientProofAndWrongSlotTotalRefuseReservation()
      throws Exception {
    var s = active();
    var t = terms(crew);
    s.acceptCargo(t);
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, true);
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, false);
    assertThrows(IllegalStateException.class, () -> s.reserveCargo(crew, reservation(t), 128, 8));
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, true);
    var ticket = reservation(t);
    assertThrows(
        IllegalStateException.class,
        () ->
            s.reserveCargo(
                crew,
                new CargoState.Reservation(
                    ticket.id(), t.id(), registration, UUID.randomUUID(), 0, ticket.slots()),
                128,
                8));
    assertThrows(
        IllegalStateException.class,
        () ->
            s.reserveCargo(
                crew,
                new CargoState.Reservation(
                    ticket.id(), t.id(), registration, entity, 1, ticket.slots()),
                128,
                8));
    assertThrows(IllegalStateException.class, () -> s.reserveCargo(crew, ticket, 127, 8));
    assertThrows(IllegalStateException.class, () -> s.reserveCargo(crew, ticket, 128, 7));
    assertThrows(
        IllegalStateException.class,
        () ->
            s.reserveCargo(
                crew,
                new CargoState.Reservation(
                    ticket.id(),
                    t.id(),
                    registration,
                    entity,
                    0,
                    List.of(new CargoState.Slot(8, 64, 63))),
                128,
                8));
    assertEquals(0, s.cargoHeld(100));
  }

  @Test
  void pendingCargoAndPaidRepairCannotMutateTheSameFrame() throws Exception {
    var s = active();
    var terms = terms(pilot);
    s.acceptCargo(terms);
    var ticket = reservation(terms);
    s.reserveCargo(pilot, ticket, 128, 8);
    assertThrows(
        IllegalStateException.class,
        () -> s.buyAircraftRepair(UUID.randomUUID(), pilot, registration, .5f, 2000));
    var frame = s.aircraft(registration).orElseThrow();
    assertThrows(
        IllegalStateException.class,
        () -> s.packAircraft(UUID.randomUUID(), pilot, registration, frame.item()));
    s.settleCargo(UUID.randomUUID(), pilot, ticket.id(), false);
    s.buyAircraftRepair(UUID.randomUUID(), pilot, registration, .5f, 2000);
    assertThrows(
        IllegalStateException.class, () -> s.reserveCargo(pilot, reservation(terms), 128, 8));
  }

  @Test
  void competingPilotsCannotReserveTheLastDailyRewardTwice() throws Exception {
    var s = active();
    s.shareAircraft(UUID.randomUUID(), pilot, registration, crew, true);
    var first = terms(pilot);
    var other = terms(crew);
    s.acceptCargo(first);
    s.acceptCargo(other);
    s.reserveCargo(pilot, reservation(first), 128, 8);
    assertThrows(
        IllegalStateException.class, () -> s.reserveCargo(crew, reservation(other), 128, 8));
    assertEquals(500, s.cargoHeld(100));
    assertThrows(IllegalStateException.class, () -> s.acceptCargo(terms(pilot)));
  }

  @Test
  void afterRenameFaultsRecoverReservationAndDeliveryWithoutRepeatedPayment() throws Exception {
    active();
    var s = EconomyStore.open(directory);
    var terms = terms(pilot);
    s.acceptCargo(terms);
    var ticket = reservation(terms);
    var failed =
        EconomyStore.open(
            directory,
            (path, data) -> {
              AtomicNbt.write(path, data);
              throw new IOException("Private cargo after-rename fault");
            });
    assertThrows(IOException.class, () -> failed.reserveCargo(pilot, ticket, 128, 8));
    assertTrue(failed.quarantined());
    s = EconomyStore.open(directory);
    s.reserveCargo(pilot, ticket, 128, 8);
    assertEquals(500, s.cargoHeld(100));
    UUID ack = UUID.randomUUID();
    var failedAck =
        EconomyStore.open(
            directory,
            (path, data) -> {
              AtomicNbt.write(path, data);
              throw new IOException("Private cargo ACK after-rename fault");
            });
    assertThrows(IOException.class, () -> failedAck.settleCargo(ack, pilot, ticket.id(), true));
    s = EconomyStore.open(directory);
    s.settleCargo(ack, pilot, ticket.id(), true);
    assertEquals(500, s.cargoPaid(100));
    assertEquals(1, s.mailbox(pilot).size());
  }

  @Test
  void distinctAircraftStillShareOneFiniteDailyRewardPool() throws Exception {
    var s = active();
    var otherRegistration = UUID.randomUUID();
    var otherEntity = UUID.randomUUID();
    s.adminSet(UUID.randomUUID(), pilot, crew, 30000, "Private second cargo aircraft funds");
    var item = new CompoundTag();
    item.putString("id", "immersive_aircraft:biplane");
    item.putInt("count", 1);
    s.buyAircraft(otherRegistration, crew, "immersive_aircraft:biplane", 20000, item);
    var body = new CompoundTag();
    body.putString("id", "immersive_aircraft:biplane");
    body.putUUID("UUID", otherEntity);
    var receipt =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), crew, otherRegistration, body, "minecraft:overworld");
    s.recoverItem(new EconomyStore.Receipt(s.id(), receipt.sequence()));
    s.activateAircraft(UUID.randomUUID(), crew, otherRegistration);
    var first = terms(pilot);
    var second = terms(crew);
    s.acceptCargo(first);
    s.acceptCargo(second);
    s.reserveCargo(pilot, reservation(first), 128, 8);
    var otherTicket =
        new CargoState.Reservation(
            UUID.randomUUID(),
            second.id(),
            otherRegistration,
            otherEntity,
            0,
            List.of(new CargoState.Slot(8, 64, 64)));
    assertThrows(IllegalStateException.class, () -> s.reserveCargo(crew, otherTicket, 128, 8));
    assertTrue(s.pendingCargo(otherRegistration).isEmpty());
    assertEquals(500, s.cargoHeld(100));
  }
}
