package com.mohymods.kingdomsmp.economy;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AircraftEconomyTest {
  @TempDir Path directory;
  final UUID buyer = UUID.randomUUID(),
      crew = UUID.randomUUID(),
      stranger = UUID.randomUUID(),
      registration = UUID.randomUUID();

  CompoundTag item() {
    var t = new CompoundTag();
    t.putString("id", "immersive_aircraft:biplane");
    t.putInt("count", 1);
    return t;
  }

  CompoundTag body() {
    var t = new CompoundTag();
    t.putString("id", "immersive_aircraft:biplane");
    t.putUUID("UUID", UUID.randomUUID());
    var pos = new ListTag();
    pos.add(DoubleTag.valueOf(1));
    pos.add(DoubleTag.valueOf(80));
    pos.add(DoubleTag.valueOf(1));
    t.put("Pos", pos);
    return t;
  }

  EconomyStore open() throws IOException {
    return EconomyStore.open(directory);
  }

  EconomyStore bought() throws Exception {
    var s = open();
    s.adminSet(UUID.randomUUID(), stranger, buyer, 30000, "Private test funds");
    s.buyAircraft(registration, buyer, "immersive_aircraft:biplane", 20000, item());
    return s;
  }

  @Test
  void purchaseIsAtomicIdempotentBoundAndArchived() throws Exception {
    var s = bought();
    assertEquals(10000, s.balance(buyer));
    s.buyAircraft(registration, buyer, "immersive_aircraft:biplane", 20000, item());
    assertEquals(1, s.aircraft().size());
    assertEquals(1, s.itemMailbox(buyer).size());
    assertTrue(s.shopDelivery(registration));
    var original = s;
    assertThrows(
        IllegalArgumentException.class,
        () ->
            original.buyAircraft(registration, crew, "immersive_aircraft:biplane", 20000, item()));
    s.archive(stranger, "Private aircraft archive check");
    s = open();
    assertEquals(10000, s.balance(buyer));
    assertEquals(buyer, s.aircraft(registration).orElseThrow().owner());
    assertEquals(AircraftState.Phase.PACKED, s.aircraft(registration).orElseThrow().phase());
  }

  @Test
  void prepareAndRestartUseExactNativeReceiptAndNeverGuessASpawn() throws Exception {
    var s = bought();
    var pending =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), buyer, registration, body(), "minecraft:overworld");
    assertEquals(AircraftState.Phase.PACKED, s.aircraft(registration).orElseThrow().phase());
    s = open();
    assertFalse(s.recoverItem(EconomyStore.Receipt.empty()));
    assertEquals(AircraftState.Phase.PACKED, s.aircraft(registration).orElseThrow().phase());
    pending =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), buyer, registration, body(), "minecraft:overworld");
    s = open();
    assertTrue(s.recoverItem(new EconomyStore.Receipt(s.id(), pending.sequence())));
    assertEquals(pending.sequence(), s.expectedReceipt(buyer));
    assertEquals(AircraftState.Phase.STARTING, open().aircraft(registration).orElseThrow().phase());
    s.activateAircraft(UUID.randomUUID(), buyer, registration);
    assertEquals(AircraftState.Phase.ACTIVE, open().aircraft(registration).orElseThrow().phase());
    var current = s;
    assertThrows(
        IllegalStateException.class,
        () ->
            current.prepareAircraftDeployment(
                UUID.randomUUID(), buyer, registration, body(), "minecraft:overworld"));
  }

  @Test
  void crewRightsAreOwnedPersistentRevocableAndDoNotTransferRegistration() throws Exception {
    var s = bought();
    assertThrows(
        IllegalStateException.class,
        () -> s.shareAircraft(UUID.randomUUID(), crew, registration, stranger, true));
    assertThrows(
        IllegalStateException.class,
        () ->
            s.prepareAircraftDeployment(
                UUID.randomUUID(), crew, registration, body(), "minecraft:overworld"));
    s.shareAircraft(UUID.randomUUID(), buyer, registration, crew, true);
    assertTrue(open().aircraft(registration).orElseThrow().permits(crew));
    var pending =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), crew, registration, body(), "minecraft:overworld");
    s.recoverItem(new EconomyStore.Receipt(s.id(), pending.sequence()));
    s.activateAircraft(UUID.randomUUID(), buyer, registration);
    s.shareAircraft(UUID.randomUUID(), buyer, registration, crew, false);
    var record = open().aircraft(registration).orElseThrow();
    assertEquals(buyer, record.owner());
    assertFalse(record.permits(crew));
    assertTrue(record.permits(buyer));
    assertEquals(10000, s.balance(buyer));
  }

  @Test
  void repackingAdvancesGenerationAndMailsOnlyTheBuyerOnce() throws Exception {
    var s = bought();
    var p =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), buyer, registration, body(), "minecraft:overworld");
    s.recoverItem(new EconomyStore.Receipt(s.id(), p.sequence()));
    s.activateAircraft(UUID.randomUUID(), buyer, registration);
    UUID op = UUID.randomUUID();
    s.packAircraft(op, buyer, registration, item());
    s.packAircraft(op, buyer, registration, item());
    var record = open().aircraft(registration).orElseThrow();
    assertEquals(1, record.generation());
    assertEquals(AircraftState.Phase.PACKED, record.phase());
    assertEquals(2, s.itemMailbox(buyer).size());
    assertTrue(s.itemMailbox(crew).isEmpty());
    assertThrows(
        IllegalStateException.class,
        () -> s.packAircraft(UUID.randomUUID(), buyer, registration, item()));
    assertEquals(2, open().itemMailbox(buyer).size());
  }

  @Test
  void uncertainCommittedPurchaseRestoresExactlyOneDebitAndRegistration() throws Exception {
    var s = open();
    s.adminSet(UUID.randomUUID(), stranger, buyer, 30000, "Private crash test funds");
    var failing =
        EconomyStore.open(
            directory,
            (path, data) -> {
              AtomicNbt.write(path, data);
              throw new IOException("after rename");
            });
    assertThrows(
        IOException.class,
        () ->
            failing.buyAircraft(registration, buyer, "immersive_aircraft:biplane", 20000, item()));
    assertTrue(failing.quarantined());
    s = open();
    s.buyAircraft(registration, buyer, "immersive_aircraft:biplane", 20000, item());
    assertEquals(10000, s.balance(buyer));
    assertEquals(1, s.aircraft().size());
    assertEquals(1, s.itemMailbox(buyer).size());
  }

  @Test
  void unfinishedDeploymentReturnsOneCanonicalAirframeWithoutAnotherDebit() throws Exception {
    var s = bought();
    var pending =
        s.prepareAircraftDeployment(
            UUID.randomUUID(), buyer, registration, body(), "minecraft:overworld");
    s.recoverItem(new EconomyStore.Receipt(s.id(), pending.sequence()));
    assertThrows(
        IllegalStateException.class,
        () -> s.packAircraft(UUID.randomUUID(), crew, registration, item()));
    var operation = UUID.randomUUID();
    s.packAircraft(operation, buyer, registration, item());
    s.packAircraft(operation, buyer, registration, item());
    var record = open().aircraft(registration).orElseThrow();
    assertEquals(AircraftState.Phase.PACKED, record.phase());
    assertEquals(1, record.generation());
    assertTrue(record.deployment().isEmpty());
    assertEquals(10000, s.balance(buyer));
    assertEquals(2, s.itemMailbox(buyer).size());
    assertThrows(
        IllegalStateException.class,
        () -> s.activateAircraft(UUID.randomUUID(), buyer, registration));
  }
}
