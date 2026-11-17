package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.integration.aircraft.*;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import com.mohymods.kingdomsmp.persistence.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("kingdom_smp_aircraft_repair_tests")
@PrefixGameTestTemplate(false)
public final class AircraftRepairRuntimeTests {
  @GameTest(template = "empty", timeoutTicks = 1400)
  public static void actualReservedRepairNativeReceiptAndRestart(GameTestHelper h)
      throws Exception {
    var server = h.getLevel().getServer();
    var level = h.getLevel();
    var path = server.getWorldPath(LevelResource.ROOT).resolve("aircraft-repair-proof.nbt");
    var proof = Files.exists(path) ? AtomicNbt.read(path) : new CompoundTag();
    // Explicit private health-stability fixture; native production defaults are not changed.
    var config =
        Class.forName("immersive_aircraft.config.Config").getMethod("getInstance").invoke(null);
    config.getClass().getField("regenerateHealthEveryNTicks").setInt(config, 0);
    var project =
        ConstructionService.store(server).projects().stream()
            .filter(
                p ->
                    p.blueprint().equals(RoyalAerodromeBlueprint.ID)
                        && p.phase() == ConstructionStore.Phase.COMPLETE)
            .findFirst()
            .orElseThrow();
    var p = ClaimRuntimeTests.vanillaPlayer(level, project.owner(), project.ownerName());
    var b = BlueprintRegistry.get(project.blueprint(), 1);
    var home = project.origin().offset(b.rotate(b.home(), project.rotation()));
    p.teleportTo(level, home.getX() + .5, home.getY(), home.getZ() - 2.5, 0, 0);
    level.getChunkSource().move(p);
    h.runAfterDelay(
        90,
        () ->
            guard(
                h,
                () -> {
                  var npc = (ReeveEntity) level.getEntity(project.npc());
                  CommerceService.openResident(p, npc);
                  var store = CommerceService.store(server);
                  var record =
                      proof.hasUUID("registration")
                          ? store.aircraft(proof.getUUID("registration")).orElseThrow()
                          : store.aircraft().stream()
                              .filter(
                                  r ->
                                      r.owner().equals(p.getUUID())
                                          && r.phase() == AircraftState.Phase.ACTIVE)
                              .findFirst()
                              .orElseThrow();
                  Entity plane =
                      level.getEntity(record.deployment().getCompound("entity").getUUID("UUID"));
                  h.assertTrue(
                      plane != null,
                      "Original purchased native airframe is loaded; no replacement generated");
                  if (proof.getInt("phase") == 2) {
                    h.assertTrue(
                        store.balance(p.getUUID()) == proof.getLong("balance")
                            && store.aircraftRepairEscrow() == 0
                            && store.aircraftRepair(record.id()).isEmpty(),
                        "Third JVM settles the original paid native receipt without another debit");
                    h.assertTrue(
                        plane
                                .getPersistentData()
                                .getUUID(AircraftRepairs.RECEIPT)
                                .equals(proof.getUUID("ticket"))
                            && AircraftRepairs.health(plane) == .75f,
                        "Already delivered receipt acknowledges without healing again after"
                            + " subsequent staged damage");
                    h.assertTrue(
                        !store.quarantined() && AircraftSales.denial(p, plane).isEmpty(),
                        "Successful real restart leaves the original owner aircraft usable and"
                            + " economy healthy");
                    h.succeed();
                    com.mojang.logging.LogUtils.getLogger()
                        .info(
                            "AIRCRAFT_REPAIR_RECEIPT_VERIFIED: third JVM recovered saved entity"
                                + " receipt after actual failed journal settlement, no double"
                                + " heal/debit, native health .75 retained");
                    return;
                  }
                  if (proof.getInt("phase") == 1) {
                    h.runAfterDelay(
                        60,
                        () ->
                            guard(
                                h,
                                () -> {
                                  var current = CommerceService.store(server);
                                  h.assertTrue(
                                      current.balance(p.getUUID()) == proof.getLong("balance")
                                          && current.aircraftRepair(record.id()).isEmpty()
                                          && AircraftRepairs.health(plane) == 1f,
                                      "Second JVM resumes the saved undelivered paid repair once"
                                          + " through actual Corin/airframe access");
                                  setHealth(plane, .5f);
                                  AircraftSales.review(p, "air:repair:" + record.id());
                                  var q = AircraftSales.quote(p).orElseThrow();
                                  AircraftRepairFault.failSettlementBeforeRename(server);
                                  boolean failed = false;
                                  try {
                                    AircraftSales.confirm(p, q.nonce());
                                  } catch (java.io.IOException expected) {
                                    failed = true;
                                  }
                                  h.assertTrue(
                                      failed
                                          && CommerceService.initializedStore(server)
                                              .orElseThrow()
                                              .quarantined()
                                          && AircraftRepairs.health(plane) == 1f
                                          && plane
                                              .getPersistentData()
                                              .getUUID(AircraftRepairs.RECEIPT)
                                              .equals(q.nonce()),
                                      "Real reserved payment and native saved repair precede an"
                                          + " injected actual journal-write failure");
                                  var disk =
                                      EconomyStore.open(
                                          server.getWorldPath(LevelResource.ROOT).resolve("data"));
                                  h.assertTrue(
                                      disk.aircraftRepair(record.id())
                                          .orElseThrow()
                                          .id()
                                          .equals(q.nonce()),
                                      "Canonical disk still has the original pending repair after"
                                          + " failed settlement rename");
                                  setHealth(plane, .75f);
                                  server.saveEverything(false, true, true);
                                  proof.putInt("phase", 2);
                                  proof.putUUID("ticket", q.nonce());
                                  proof.putLong("balance", disk.balance(p.getUUID()));
                                  AtomicNbt.write(path, proof);
                                  h.succeed();
                                  com.mojang.logging.LogUtils.getLogger()
                                      .info(
                                          "AIRCRAFT_REPAIR_RECEIPT_PREPARED: native world receipt"
                                              + " saved, actual journal ACK write"
                                              + " refused/quarantined, subsequent explicit .75"
                                              + " damage saved for no-repeat-heal restart proof");
                                }));
                    return;
                  }
                  park(h, p, npc, plane);
                  h.runAfterDelay(
                      30,
                      () ->
                          guard(
                              h,
                              () -> {
                                h.assertTrue(
                                    plane.onGround(),
                                    "Original actual native airframe settles on existing airfield"
                                        + " ground");
                                setHealth(plane, .5f);
                                var originalMarker =
                                    plane
                                        .getPersistentData()
                                        .getCompound(AircraftSales.MARKER)
                                        .copy();
                                plane
                                    .getPersistentData()
                                    .getCompound(AircraftSales.MARKER)
                                    .putString("generation", "1");
                                boolean malformed = false;
                                try {
                                  AircraftSales.review(p, "air:repair:" + record.id());
                                } catch (IllegalStateException expected) {
                                  malformed = true;
                                }
                                h.assertTrue(
                                    malformed
                                        && CommerceService.store(server)
                                            .aircraftRepair(record.id())
                                            .isEmpty(),
                                    "Malformed native body registration is refused before a repair"
                                        + " reservation");
                                plane.getPersistentData().put(AircraftSales.MARKER, originalMarker);
                                plane
                                    .getPersistentData()
                                    .putUUID(AircraftRepairs.RECEIPT, UUID.randomUUID());
                                boolean mismatched = false;
                                try {
                                  AircraftSales.review(p, "air:repair:" + record.id());
                                } catch (IllegalStateException expected) {
                                  mismatched = true;
                                }
                                h.assertTrue(
                                    mismatched
                                        && CommerceService.store(server)
                                            .aircraftRepair(record.id())
                                            .isEmpty(),
                                    "Unknown aircraft service receipt refuses a mismatched world"
                                        + " before payment");
                                plane.getPersistentData().remove(AircraftRepairs.RECEIPT);
                                var blocker =
                                    ClaimRuntimeTests.vanillaPlayer(
                                        level,
                                        UUID.nameUUIDFromBytes(
                                            "OfflinePlayer:RepairBlocker"
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                                        "RepairBlocker");
                                long claimBaseline =
                                    CommerceService.store(server).balance(p.getUUID());
                                AircraftSales.review(p, "air:repair:" + record.id());
                                var ftbQuote = AircraftSales.quote(p).orElseThrow();
                                ClaimRuntimeTests.ftbUnclaim(level, npc.blockPosition());
                                ClaimRuntimeTests.ftbClaim(blocker, npc.blockPosition());
                                boolean ftbDenied = false;
                                try {
                                  AircraftSales.confirm(p, ftbQuote.nonce());
                                } catch (IllegalStateException expected) {
                                  ftbDenied = true;
                                }
                                h.assertTrue(
                                    ftbDenied
                                        && CommerceService.store(server).balance(p.getUUID())
                                            == claimBaseline,
                                    "FTB revocation after repair review precedes every charge or"
                                        + " native heal");
                                ClaimRuntimeTests.ftbUnclaim(level, npc.blockPosition());
                                ClaimRuntimeTests.ftbClaim(p, npc.blockPosition());
                                AircraftSales.review(p, "air:repair:" + record.id());
                                var opacQuote = AircraftSales.quote(p).orElseThrow();
                                ClaimRuntimeTests.opacUnclaim(level, npc.blockPosition());
                                ClaimRuntimeTests.opacClaim(
                                    level, blocker.getUUID(), npc.blockPosition());
                                boolean opacDenied = false;
                                try {
                                  AircraftSales.confirm(p, opacQuote.nonce());
                                } catch (IllegalStateException expected) {
                                  opacDenied = true;
                                }
                                h.assertTrue(
                                    opacDenied
                                        && CommerceService.store(server).balance(p.getUUID())
                                            == claimBaseline,
                                    "OPAC independently refuses post-review repair payment");
                                ClaimRuntimeTests.opacUnclaim(level, npc.blockPosition());
                                ClaimRuntimeTests.opacClaim(
                                    level, p.getUUID(), npc.blockPosition());
                                server.getPlayerList().remove(blocker);
                                setHealth(plane, .5f);
                                long before = CommerceService.store(server).balance(p.getUUID());
                                AircraftSales.review(p, "air:repair:" + record.id());
                                var stale = AircraftSales.quote(p).orElseThrow();
                                setHealth(plane, .25f);
                                boolean changed = false;
                                try {
                                  AircraftSales.confirm(p, stale.nonce());
                                } catch (IllegalStateException expected) {
                                  changed = true;
                                }
                                h.assertTrue(
                                    changed
                                        && CommerceService.store(server).balance(p.getUUID())
                                            == before,
                                    "Changed native condition refuses stale repair price before"
                                        + " payment");
                                setHealth(plane, .5f);
                                AircraftSales.review(p, "air:repair:" + record.id());
                                var q = AircraftSales.quote(p).orElseThrow();
                                AircraftSales.confirm(p, q.nonce());
                                var actual = CommerceService.store(server);
                                h.assertTrue(
                                    actual.balance(p.getUUID()) == before - q.price()
                                        && actual.aircraftRepair(record.id()).isEmpty()
                                        && AircraftRepairs.health(plane) == 1f
                                        && plane
                                            .getPersistentData()
                                            .getUUID(AircraftRepairs.RECEIPT)
                                            .equals(q.nonce()),
                                    "Normal actual counter repair debits once, heals native entity"
                                        + " and settles only after durable receipt");
                                // A real undelivered reservation can refund only its own buyer
                                // once.
                                setHealth(plane, .5f);
                                long refundBaseline = actual.balance(p.getUUID());
                                UUID canceled = UUID.randomUUID();
                                actual.buyAircraftRepair(
                                    canceled,
                                    p.getUUID(),
                                    record.id(),
                                    .5f,
                                    AircraftRepairRules.rate(server));
                                h.assertTrue(
                                    !AircraftInventoryAccess.entity(p, plane)
                                        && AircraftSales.validateBody(plane),
                                    "Pending workshop reservation prevents native cargo access and"
                                        + " movement until resolved");
                                AircraftSales.review(p, "air:repaircancel:" + record.id());
                                var cancelQuote = AircraftSales.quote(p).orElseThrow();
                                AircraftSales.confirm(p, cancelQuote.nonce());
                                h.assertTrue(
                                    actual.balance(p.getUUID()) == refundBaseline
                                        && actual.aircraftRepair(record.id()).isEmpty()
                                        && AircraftRepairs.health(plane) == .5f,
                                    "Reviewed undelivered service refunds exact held coins without"
                                        + " healing");
                                boolean replayed = false;
                                try {
                                  AircraftSales.confirm(p, cancelQuote.nonce());
                                } catch (IllegalStateException expected) {
                                  replayed = true;
                                }
                                h.assertTrue(
                                    replayed && actual.balance(p.getUUID()) == refundBaseline,
                                    "Repair refund review cannot return a second payment");
                                // Durable unpaid-work window: payment committed, no native entity
                                // receipt; actor logs out.
                                setHealth(plane, .5f);
                                UUID pending = UUID.randomUUID();
                                actual.buyAircraftRepair(
                                    pending,
                                    p.getUUID(),
                                    record.id(),
                                    .5f,
                                    AircraftRepairRules.rate(server));
                                CommerceService.checkpoint(p);
                                server.getPlayerList().remove(p);
                                server.saveEverything(false, true, true);
                                h.assertTrue(
                                    actual.aircraftRepair(record.id()).isPresent()
                                        && AircraftRepairs.health(plane) == .5f,
                                    "Offline actor keeps a real paid but undelivered repair pending"
                                        + " across clean shutdown");
                                proof.putUUID("registration", record.id());
                                proof.putInt("phase", 1);
                                proof.putLong("balance", actual.balance(project.owner()));
                                AtomicNbt.write(path, proof);
                                h.succeed();
                                com.mojang.logging.LogUtils.getLogger()
                                    .info(
                                        "AIRCRAFT_REPAIR_PENDING_PREPARED: real native counter paid"
                                            + " heal/receipt, changed-price refusal, original"
                                            + " damaged UUID with canonical pending reservation and"
                                            + " logged-out actor ready for restart");
                              }));
                }));
  }

  private static void park(GameTestHelper h, ServerPlayer p, Entity npc, Entity plane)
      throws Exception {
    var level = p.serverLevel();
    for (int radius = 4; radius <= 7; radius++)
      for (int dx = -radius; dx <= radius; dx++)
        for (int dz = -radius; dz <= radius; dz++) {
          if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
          var site = npc.blockPosition().offset(dx, 0, dz);
          var center =
              new net.minecraft.world.phys.Vec3(site.getX() + .5, site.getY(), site.getZ() + .5);
          var box = plane.getDimensions(plane.getPose()).makeBoundingBox(center);
          if (p.position().distanceToSqr(center) > 49
              || !level
                  .getBlockState(site.below())
                  .isFaceSturdy(level, site.below(), net.minecraft.core.Direction.UP)
              || !level.noCollision(plane, box)) continue;
          plane.moveTo(center.x, center.y, center.z, 0, 0);
          plane.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
          plane.setOnGround(true);
          return;
        }
    h.fail("No legitimate unoccupied repair parking point near existing Corin counter");
  }

  private static void setHealth(Entity entity, float value) throws Exception {
    entity.getClass().getMethod("setHealth", float.class).invoke(entity, value);
  }

  @FunctionalInterface
  interface Step {
    void run() throws Exception;
  }

  private static void guard(GameTestHelper h, Step step) {
    try {
      step.run();
    } catch (Exception e) {
      com.mojang.logging.LogUtils.getLogger().error("Native aircraft repair assertion failed", e);
      h.runAfterDelay(1, () -> h.fail(e.toString()));
    }
  }
}
