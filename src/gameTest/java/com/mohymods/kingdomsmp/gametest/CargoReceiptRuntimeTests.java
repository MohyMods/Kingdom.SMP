package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.integration.aircraft.*;
import com.mohymods.kingdomsmp.persistence.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;

/** Private copied-world protocol test only. No production event registration or player service. */
@GameTestHolder("kingdom_smp_cargo_receipt_tests")
@PrefixGameTestTemplate(false)
public final class CargoReceiptRuntimeTests {
  static final String RECEIPT = "KingdomAirCargoReceipt";

  @GameTest(template = "empty", timeoutTicks = 500)
  public static void nativeInventoryReservationReceiptRecoveryAndWorldRollback(GameTestHelper h)
      throws Exception {
    var server = h.getLevel().getServer();
    var level = h.getLevel();
    var path = server.getWorldPath(LevelResource.ROOT).resolve("cargo-receipt-proof.nbt");
    var proof = Files.exists(path) ? AtomicNbt.read(path) : new CompoundTag();
    if (proof.getInt("phase") == 3) {
      var disk = EconomyStore.open(server.getWorldPath(LevelResource.ROOT).resolve("data"));
      var frame = disk.aircraft(proof.getUUID("registration")).orElseThrow();
      var body = frame.deployment().getCompound("entity");
      var actualPosition =
          net.minecraft.core.BlockPos.containing(
              proof.getDouble("bodyX"), proof.getDouble("bodyY"), proof.getDouble("bodyZ"));
      var chunk = new net.minecraft.world.level.ChunkPos(actualPosition);
      level
          .getChunkSource()
          .addRegionTicket(net.minecraft.server.level.TicketType.PORTAL, chunk, 2, actualPosition);
      level.getChunk(actualPosition);
      h.runAfterDelay(
          90,
          () ->
              guard(
                  h,
                  () -> {
                    try {
                      var entity = level.getEntity(body.getUUID("UUID"));
                      h.assertTrue(
                          entity != null,
                          "Original older native body is loaded for read-only recovery check");
                      var delivery = disk.deliveredCargo(frame.id()).orElseThrow();
                      h.assertTrue(
                          CargoInventory.matches(
                              entity,
                              delivery.terms().item(),
                              delivery.reservation().slots(),
                              false),
                          "Controlled older source remains intact before validation");
                      h.assertTrue(
                          AircraftSales.validateBody(entity)
                              && CommerceService.initializedStore(server)
                                  .orElseThrow()
                                  .quarantined(),
                          "Real production aircraft guard freezes mismatched completed receipt");
                      h.assertTrue(
                          CargoInventory.matches(
                                  entity,
                                  delivery.terms().item(),
                                  delivery.reservation().slots(),
                                  false)
                              && !entity.getPersistentData().contains(RECEIPT),
                          "Guard removes no goods and invents no replacement receipt");
                      var verified =
                          EconomyStore.open(
                              server.getWorldPath(LevelResource.ROOT).resolve("data"));
                      h.assertTrue(
                          verified.balance(delivery.terms().pilot())
                                  == proof.getLong("balance") + 500
                              && verified.cargoPaid(delivery.terms().day()) == 500
                              && verified.cargoHeld(delivery.terms().day()) == 0,
                          "Quarantine preserves exactly one historical reward with no new append");
                      h.succeed();
                      com.mojang.logging.LogUtils.getLogger()
                          .info(
                              "CARGO_RECEIPT_MISMATCH_BLOCKED: fourth actual JVM, mismatched"
                                  + " completed native source frozen without item removal, repair"
                                  + " or another reward");
                    } finally {
                      level
                          .getChunkSource()
                          .removeRegionTicket(
                              net.minecraft.server.level.TicketType.PORTAL,
                              chunk,
                              2,
                              actualPosition);
                    }
                  }));
      return;
    }
    var airport =
        ConstructionService.store(server).projects().stream()
            .filter(
                v ->
                    v.blueprint().equals(RoyalAerodromeBlueprint.ID)
                        && v.phase() == ConstructionStore.Phase.COMPLETE)
            .findFirst()
            .orElseThrow();
    var p = ClaimRuntimeTests.vanillaPlayer(level, airport.owner(), airport.ownerName());
    CommerceService.verifyReceipt(p);
    var store = CommerceService.store(server);
    var frame =
        proof.hasUUID("registration")
            ? store.aircraft(proof.getUUID("registration")).orElseThrow()
            : store.aircraft().stream()
                .filter(v -> v.phase() == AircraftState.Phase.ACTIVE)
                .findFirst()
                .orElseThrow();
    var b = BlueprintRegistry.get(airport.blueprint(), airport.blueprintVersion());
    var home = airport.origin().offset(b.rotate(b.home(), airport.rotation()));
    p.teleportTo(level, home.getX() + .5, home.getY(), home.getZ() - 2.5, 0, 0);
    level.getChunkSource().move(p);
    var pos = frame.deployment().getCompound("entity").getList("Pos", 6);
    level.getChunk(
        net.minecraft.core.BlockPos.containing(
            pos.getDouble(0), pos.getDouble(1), pos.getDouble(2)));
    h.runAfterDelay(
        90,
        () ->
            guard(
                h,
                () -> {
                  var entity =
                      level.getEntity(frame.deployment().getCompound("entity").getUUID("UUID"));
                  h.assertTrue(
                      entity instanceof Container, "Original purchased native cargo body loaded");
                  var inventory = (Container) entity;
                  var slots = new ArrayList<>(new TreeSet<>(CargoInventory.slots(entity)));
                  h.assertTrue(
                      slots.size() >= 2,
                      "Native aircraft supplies real storage slots distinct from its"
                          + " fuel/equipment");
                  int phase = proof.getInt("phase");
                  if (phase == 0) {
                    h.assertTrue(
                        inventory.getItem(slots.get(0)).isEmpty()
                            && inventory.getItem(slots.get(1)).isEmpty(),
                        "Only empty private-fixture storage receives explicit test goods");
                    inventory.setItem(slots.get(0), new ItemStack(Items.WHEAT, 40));
                    inventory.setItem(slots.get(1), new ItemStack(Items.WHEAT, 40));
                    inventory.setChanged();
                    var terms =
                        new CargoState.Terms(
                            UUID.randomUUID(),
                            p.getUUID(),
                            FlightService.day(),
                            "minecraft:wheat",
                            64,
                            128,
                            500,
                            500,
                            airport.id(),
                            ConstructionService.lodge(server).id());
                    store.acceptCargo(terms);
                    var selected = CargoInventory.select(entity, terms.item(), terms.quantity());
                    h.assertTrue(
                        selected.size() == 2
                            && selected.get(0).take() == 40
                            && selected.get(1).take() == 24,
                        "Actual native partial cargo selection is exact");
                    var ticket =
                        new CargoState.Reservation(
                            UUID.randomUUID(),
                            terms.id(),
                            frame.id(),
                            entity.getUUID(),
                            frame.generation(),
                            selected);
                    store.reserveCargo(p.getUUID(), ticket, 128, 8);
                    h.assertTrue(
                        CargoInventory.matches(entity, terms.item(), selected, false)
                            && store.cargoHeld(terms.day()) == 500
                            && store.cargoPaid(terms.day()) == 0,
                        "Reservation consumes no native goods and issues no reward");
                    proof.putUUID("registration", frame.id());
                    proof.putUUID("contract", terms.id());
                    proof.putUUID("ticket", ticket.id());
                    proof.putLong("balance", store.balance(p.getUUID()));
                    proof.putLong("day", terms.day());
                    save(
                        h,
                        p,
                        path,
                        proof,
                        1,
                        "CARGO_RESERVATION_PREPARED: actual native source untouched, finite reward"
                            + " reserved, no live cargo hooks");
                    return;
                  }
                  var task = store.cargoContract(proof.getUUID("contract")).orElseThrow();
                  var terms = task.terms();
                  var ticket =
                      phase <= 2
                          ? task.reservation()
                          : store.deliveredCargo(frame.id()).orElseThrow().reservation();
                  UUID actual =
                      entity.getPersistentData().hasUUID(RECEIPT)
                          ? entity.getPersistentData().getUUID(RECEIPT)
                          : null;
                  if (phase == 1) {
                    h.assertTrue(
                        CargoReceiptRules.decide(
                                true,
                                actual,
                                ticket.id(),
                                ticket.previousReceipt(),
                                null,
                                null,
                                CargoInventory.matches(entity, terms.item(), ticket.slots(), false))
                            == CargoReceiptRules.Action.WAIT_FOR_ACTOR,
                        "Second JVM sees reserved but unperformed source");
                    CargoInventory.consume(entity, terms.item(), ticket.slots());
                    entity.getPersistentData().putUUID(RECEIPT, ticket.id());
                    h.assertTrue(
                        inventory.getItem(ticket.slots().get(0).index()).isEmpty()
                            && inventory.getItem(ticket.slots().get(1).index()).getCount() == 16,
                        "Actual native mutation consumes exactly64 and leaves16");
                    h.assertTrue(
                        store.cargoHeld(terms.day()) == 500
                            && store.mailbox(p.getUUID()).stream()
                                .noneMatch(mail -> mail.id().equals(ticket.id())),
                        "Before ACK no private reward exists");
                    save(
                        h,
                        p,
                        path,
                        proof,
                        2,
                        "CARGO_NATIVE_DEBIT_PREPARED: actual partial-stack debit and receipt saved"
                            + " before any payment");
                    return;
                  }
                  if (phase == 2) {
                    h.assertTrue(
                        CargoReceiptRules.decide(
                                true,
                                actual,
                                ticket.id(),
                                ticket.previousReceipt(),
                                null,
                                null,
                                false)
                            == CargoReceiptRules.Action.ACKNOWLEDGE,
                        "Third JVM acknowledges its saved native receipt without another debit");
                    h.assertTrue(
                        CargoInventory.matches(entity, terms.item(), ticket.slots(), true),
                        "Original native16remaining survive restart");
                    UUID ack = UUID.randomUUID();
                    store.settleCargo(ack, p.getUUID(), ticket.id(), true);
                    store.settleCargo(ack, p.getUUID(), ticket.id(), true);
                    store.claimCoins(p.getUUID(), ticket.id());
                    h.assertTrue(
                        store.balance(p.getUUID()) == proof.getLong("balance") + 500
                            && store.cargoPaid(terms.day()) == 500
                            && store.cargoHeld(terms.day()) == 0,
                        "Exact one reward and claimed purse");
                    // Explicit private fault fixture: persist an older native body while preserving
                    // canonical history.
                    for (var source : ticket.slots())
                      inventory.setItem(
                          source.index(), new ItemStack(Items.WHEAT, source.before()));
                    inventory.setChanged();
                    entity.getPersistentData().remove(RECEIPT);
                    proof.putDouble("bodyX", entity.getX());
                    proof.putDouble("bodyY", entity.getY());
                    proof.putDouble("bodyZ", entity.getZ());
                    save(
                        h,
                        p,
                        path,
                        proof,
                        3,
                        "CARGO_WORLD_ROLLBACK_PREPARED: controlled old native source saved against"
                            + " completed canonical debit; next JVM must reconcile without paying");
                    return;
                  }
                  h.fail("Unexpected private cargo receipt phase");
                }));
  }

  static void save(
      GameTestHelper h, ServerPlayer p, Path path, CompoundTag proof, int phase, String marker)
      throws Exception {
    proof.putInt("phase", phase);
    AtomicNbt.write(path, proof);
    p.server.getPlayerList().saveAll();
    p.server.saveEverything(false, true, true);
    h.succeed();
    com.mojang.logging.LogUtils.getLogger().info(marker);
  }

  @FunctionalInterface
  interface Step {
    void run() throws Exception;
  }

  static void guard(GameTestHelper h, Step step) {
    try {
      step.run();
    } catch (Exception error) {
      com.mojang.logging.LogUtils.getLogger()
          .error("Private native cargo receipt test failed", error);
      h.runAfterDelay(1, () -> h.fail(error.toString()));
    }
  }
}
