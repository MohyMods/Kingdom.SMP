package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.integration.aircraft.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import com.mohymods.kingdomsmp.persistence.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.*;

/**
 * Actual native powered aircraft, ordinary movement packets, real counter handlers and JVM
 * restarts.
 */
@GameTestHolder("kingdom_smp_flight_tests")
@PrefixGameTestTemplate(false)
public final class FlightRuntimeTests {
  @GameTest(template = "empty", timeoutTicks = 1800)
  public static void poweredDispatchAndDurableDailyReward(GameTestHelper h) throws Exception {
    var level = h.getLevel();
    var server = level.getServer();
    var path = server.getWorldPath(LevelResource.ROOT).resolve("air-dispatch-proof.nbt");
    var proof = Files.exists(path) ? AtomicNbt.read(path) : new CompoundTag();
    var airport =
        ConstructionService.store(server).projects().stream()
            .filter(
                v ->
                    v.blueprint().equals(RoyalAerodromeBlueprint.ID)
                        && v.phase() == ConstructionStore.Phase.COMPLETE)
            .findFirst()
            .orElseThrow();
    var headquarters = ConstructionService.lodge(server);
    var home = post(airport);
    var target = post(headquarters);
    // The copied private flat-void fixture needs a landing pad; add only into air, outside
    // buildings.
    var pad = net.minecraft.core.BlockPos.containing(target.x + 14, target.y - 1, target.z + 14);
    for (var cell :
        net.minecraft.core.BlockPos.betweenClosed(pad.offset(-3, 0, -3), pad.offset(3, 0, 3)))
      if (level.getBlockState(cell).isAir())
        level.setBlock(cell, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 18);
    var p = ClaimRuntimeTests.vanillaPlayer(level, airport.owner(), airport.ownerName());
    var store = CommerceService.store(server);
    CommerceService.verifyReceipt(p);
    if (proof.getInt("phase") == 2) {
      h.assertTrue(
          store.balance(p.getUUID()) == proof.getLong("balance")
              && store.flightSpent(FlightService.day()) == 500,
          "Third actual JVM retains exactly one claimed daily reward");
      var m = FlightService.book(server).get(p.getUUID()).orElseThrow();
      h.assertTrue(
          m.rewarded() && m.arrived() && m.verified(), "Saved evidence retains complete dispatch");
      h.assertTrue(
          store.flightAward(p.getUUID(), m.day()).orElseThrow().id().equals(m.id()),
          "Canonical reward and evidence agree after reload");
      h.assertTrue(
          !p.getAbilities().mayfly && !p.hasPermissions(2), "No flight or administrator exemption");
      h.succeed();
      com.mojang.logging.LogUtils.getLogger()
          .info(
              "FLIGHT_RESTART_VERIFIED: third dedicated JVM retained one reward, exact purse,"
                  + " original registration and completed proof");
      return;
    }
    if (proof.getInt("phase") == 0) {
      p.teleportTo(level, home.x, home.y, home.z - 3, 0, 0);
      level.getChunkSource().move(p);
    }
    var record =
        proof.hasUUID("registration")
            ? store.aircraft(proof.getUUID("registration")).orElseThrow()
            : store.aircraft().stream()
                .filter(v -> v.phase() == AircraftState.Phase.ACTIVE)
                .findFirst()
                .orElseThrow();
    var saved = record.deployment().getCompound("entity");
    level.getChunk(
        net.minecraft.core.BlockPos.containing(
            saved.getList("Pos", 6).getDouble(0),
            saved.getList("Pos", 6).getDouble(1),
            saved.getList("Pos", 6).getDouble(2)));
    h.runAfterDelay(
        90,
        () ->
            guard(
                h,
                () -> {
                  var plane = level.getEntity(saved.getUUID("UUID"));
                  h.assertTrue(plane != null, "Original purchased native airframe loads");
                  var npc = (ReeveEntity) level.getEntity(airport.npc());
                  if (proof.getInt("phase") == 0) CommerceService.openResident(p, npc);
                  if (proof.getInt("phase") == 1) {
                    var persisted = FlightService.book(server).get(p.getUUID()).orElseThrow();
                    h.assertTrue(
                        persisted.meters() >= 40
                            && !persisted.rewarded()
                            && store.flightSpent(FlightService.day()) == 0,
                        "Real restart retains unfinished flight without payment");
                  } else {
                    h.assertTrue(
                        CommerceService.snapshot(p, 1, 8, "").entries().stream()
                            .anyMatch(v -> v.state().equals("FLIGHT_ACCEPT")),
                        "Actual Corin Contracts tab offers dispatch");
                    review(h, p, "flight:start");
                    var blocker =
                        ClaimRuntimeTests.vanillaPlayer(
                            level,
                            UUID.nameUUIDFromBytes(
                                "OfflinePlayer:FlightBlocker"
                                    .getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            "FlightBlocker");
                    var q = FlightService.quote(p).orElseThrow();
                    ClaimRuntimeTests.ftbUnclaim(level, npc.blockPosition());
                    ClaimRuntimeTests.ftbClaim(blocker, npc.blockPosition());
                    denied(
                        h,
                        () -> FlightService.confirm(p, q.nonce()),
                        "Current FTB revocation prevents dispatch acceptance");
                    ClaimRuntimeTests.ftbUnclaim(level, npc.blockPosition());
                    ClaimRuntimeTests.ftbClaim(p, npc.blockPosition());
                    review(h, p, "flight:start");
                    var opacQuote = FlightService.quote(p).orElseThrow();
                    ClaimRuntimeTests.opacUnclaim(level, npc.blockPosition());
                    ClaimRuntimeTests.opacClaim(level, blocker.getUUID(), npc.blockPosition());
                    denied(
                        h,
                        () -> FlightService.confirm(p, opacQuote.nonce()),
                        "Current OPAC revocation independently prevents dispatch acceptance");
                    ClaimRuntimeTests.opacUnclaim(level, npc.blockPosition());
                    ClaimRuntimeTests.opacClaim(level, p.getUUID(), npc.blockPosition());
                    review(h, p, "flight:start");
                    confirm(h, p);
                    denied(
                        h,
                        () -> FlightService.review(p, "flight:finish"),
                        "An unflown dispatch cannot be reported");
                    proof.putLong("balance", store.balance(p.getUUID()));
                    proof.putUUID("registration", record.id());
                  }
                  // Explicit controlled starting position only; subsequent distance uses actual
                  // vanilla packets.
                  if (proof.getInt("phase") == 0) {
                    plane.moveTo(home.x + 16, home.y + 16, home.z);
                    plane.setDeltaMovement(Vec3.ZERO);
                    plane.setOnGround(false);
                    plane.verticalCollisionBelow = false;
                    p.teleportTo(level, plane.getX(), plane.getY() + 1, plane.getZ(), 0, 0);
                    level.getChunkSource().move(p);
                    ack(p);
                    h.assertTrue(
                        p.startRiding(plane, true),
                        "Ordinary buyer boards original registered plane");
                  } else {
                    h.assertTrue(
                        p.getVehicle() == plane,
                        "Real saved pilot reattaches to the original aircraft");
                    ack(p);
                  }
                  var desc = call(plane, "getInventoryDescription");
                  var fuelSlots =
                      (List<?>)
                          desc.getClass()
                              .getMethod("getSlots", String.class)
                              .invoke(desc, "boiler");
                  int slot = (int) call(fuelSlots.getFirst(), "index");
                  ((Container) plane).setItem(slot, new ItemStack(Items.COAL, 16));
                  h.runAfterDelay(
                      4,
                      () ->
                          guard(
                              h,
                              () -> {
                                h.assertTrue(
                                    (float) call(plane, "getFuelUtilization") > 0,
                                    "Native engine genuinely consumes coal");
                                send(p, "EnginePowerMessage", new Class<?>[] {float.class}, .5f);
                                var b = FlightService.book(server);
                                if (proof.getInt("phase")
                                    == 1) { // Login/reposition teleport correctly reset old partial
                                  // proof; resuming starts a new takeoff.
                                  h.assertTrue(
                                      b.get(p.getUUID()).orElseThrow().meters() >= 40,
                                      "Uninterrupted mounted restart retains earned partial"
                                          + " evidence");
                                }
                                if (proof.getInt("phase") == 0) {
                                  h.assertTrue(
                                      b.get(p.getUUID()).orElseThrow().registration() != null,
                                      "Actual boarding binds dispatch to canonical native frame");
                                  plane.teleportTo(plane.getX(), plane.getY(), plane.getZ());
                                  h.assertTrue(
                                      b.get(p.getUUID()).orElseThrow().registration() == null,
                                      "Real entity teleport interrupts proof");
                                  FlightService.tick(server);
                                  p.teleportTo(level, p.getX(), p.getY(), p.getZ(), 0, 0);
                                  h.assertTrue(
                                      b.get(p.getUUID()).orElseThrow().registration() == null,
                                      "Real player teleport interrupts proof");
                                  ack(p);
                                  h.assertTrue(
                                      p.startRiding(plane, true),
                                      "Pilot can reboard after interruption");
                                }
                                fly(h, p, plane, home, target, path, proof, 0);
                              }));
                }));
  }

  static void fly(
      GameTestHelper h,
      ServerPlayer p,
      Entity plane,
      Vec3 home,
      Vec3 target,
      Path path,
      CompoundTag proof,
      int step) {
    h.runAfterDelay(
        1,
        () ->
            guard(
                h,
                () -> {
                  var b = FlightService.book(p.server);
                  var m = b.get(p.getUUID()).orElseThrow();
                  if (proof.getInt("phase") == 0 && m.meters() >= 40) {
                    b.checkpoint();
                    proof.putInt("phase", 1);
                    AtomicNbt.write(path, proof);
                    p.server.getPlayerList().saveAll();
                    p.server.saveEverything(false, true, true);
                    h.succeed();
                    com.mojang.logging.LogUtils.getLogger()
                        .info(
                            "FLIGHT_PARTIAL_PREPARED: real registered powered flight retained"
                                + " partial evidence without reward before restart");
                    return;
                  }
                  if (step > 1200)
                    throw new IllegalStateException("Native dispatch route did not complete: " + m);
                  if (m.arrived()) {
                    send(p, "EnginePowerMessage", new Class<?>[] {float.class}, 0f);
                    p.stopRiding();
                    p.teleportTo(p.serverLevel(), target.x, target.y, target.z - 3, 0, 0);
                    p.serverLevel().getChunkSource().move(p);
                    h.runAfterDelay(
                        20,
                        () ->
                            guard(
                                h,
                                () -> {
                                  var npc =
                                      (ReeveEntity)
                                          p.serverLevel()
                                              .getEntity(ConstructionService.lodge(p.server).npc());
                                  CommerceService.openResident(p, npc);
                                  
                                  var blocker=ClaimRuntimeTests.vanillaPlayer(p.serverLevel(),UUID.nameUUIDFromBytes("OfflinePlayer:FlightBlocker".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"FlightBlocker");
                                  FlightService.review(p,"flight:finish");var deniedFtb=FlightService.quote(p).orElseThrow();ClaimRuntimeTests.ftbUnclaim(p.serverLevel(),npc.blockPosition());ClaimRuntimeTests.ftbClaim(blocker,npc.blockPosition());denied(h,()->FlightService.confirm(p,deniedFtb.nonce()),"FTB claim revocation after flight report review prevents payment");ClaimRuntimeTests.ftbUnclaim(p.serverLevel(),npc.blockPosition());ClaimRuntimeTests.ftbClaim(p,npc.blockPosition());
                                  FlightService.review(p,"flight:finish");var deniedOpac=FlightService.quote(p).orElseThrow();ClaimRuntimeTests.opacUnclaim(p.serverLevel(),npc.blockPosition());ClaimRuntimeTests.opacClaim(p.serverLevel(),blocker.getUUID(),npc.blockPosition());denied(h,()->FlightService.confirm(p,deniedOpac.nonce()),"OPAC claim revocation independently prevents payment after report review");ClaimRuntimeTests.opacUnclaim(p.serverLevel(),npc.blockPosition());ClaimRuntimeTests.opacClaim(p.serverLevel(),p.getUUID(),npc.blockPosition());
                                  h.assertTrue(CommerceService.store(p.server).flightSpent(FlightService.day())==0,"Permission failures issued no private reward or daily spend");
                                  if (Boolean.getBoolean("kingdom.testStageFlightReview")) {
                                    p.server.getPlayerList().saveAll();
                                    p.server.saveEverything(false, true, true);
                                    h.succeed();
                                    com.mojang.logging.LogUtils.getLogger()
                                        .info(
                                            "FLIGHT_REVIEW_PREPARED: verified real arrival awaiting"
                                                + " ordinary client Steward report, no reward"
                                                + " issued");
                                    return;
                                  }
                                  review(h, p, "flight:finish");
                                  var q = FlightService.quote(p).orElseThrow();
                                  confirm(h, p);
                                  var store = CommerceService.store(p.server);
                                  h.assertTrue(
                                      store.balance(p.getUUID()) == proof.getLong("balance")
                                          && store.flightSpent(FlightService.day()) == 500,
                                      "Report creates one private-mail reward without changing"
                                          + " purse");
                                  boolean refused = false;
                                  try {
                                    CommerceService.handle(
                                        p, new CommerceRequest(9, 2, 8, "", 1, q.nonce()));
                                  } catch (IllegalStateException expected) {
                                    refused = true;
                                  }
                                  h.assertTrue(
                                      refused && store.flightSpent(FlightService.day()) == 500,
                                      "Repeated stale network confirmation cannot pay twice");
                                  store.claimCoins(p.getUUID(), m.id());
                                  h.assertTrue(
                                      store.balance(p.getUUID()) == proof.getLong("balance") + 500,
                                      "Existing canonical mailbox claim credits exactly once");
                                  proof.putInt("phase", 2);
                                  proof.putLong("balance", store.balance(p.getUUID()));
                                  AtomicNbt.write(path, proof);
                                  p.server.getPlayerList().saveAll();
                                  p.server.saveEverything(false, true, true);
                                  h.succeed();
                                  com.mojang.logging.LogUtils.getLogger()
                                      .info(
                                          "FLIGHT_REWARD_PREPARED: server-observed powered flight,"
                                              + " real landing, headquarters Steward report, replay"
                                              + " refusal and exact once-only mail claim");
                                }));
                    return;
                  }
                  // A controlled client route with genuine per-tick vanilla vehicle handling, not
                  // injected proof.
                  Vec3 destination =
                      step < 100
                          ? new Vec3(home.x + 110, home.y + 20, home.z)
                          : new Vec3(target.x + 14, Math.max(home.y, target.y) + 20, target.z + 14);
                  if (step >= 100
                      && Math.hypot(plane.getX() - destination.x, plane.getZ() - destination.z)
                          < 2) {
                    destination = new Vec3(destination.x, target.y, destination.z);
                  }
                  var delta = destination.subtract(plane.position());
                  if (delta.length() > 1.5) delta = delta.normalize().scale(1.5);
                  Vec3 next = plane.position().add(delta);
                  p.connection.tick();
                  ack(p);
                  var buffer =
                      new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
                  buffer.writeDouble(next.x);
                  buffer.writeDouble(next.y);
                  buffer.writeDouble(next.z);
                  buffer.writeFloat(0);
                  buffer.writeFloat(0);
                  var packet =
                      net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket.STREAM_CODEC
                          .decode(buffer);
                  buffer.release();
                  plane.setOnGround(false);
                  plane.verticalCollisionBelow = false;
                  p.connection.handleMoveVehicle(packet);
                  // Landing collision uses an existing supported safe ground cell; no
                  // structure/terrain replacement.
                  if (step >= 100
                      && plane
                              .position()
                              .distanceToSqr(new Vec3(target.x + 14, target.y, target.z + 14))
                          < 4) {
                    var below = plane.blockPosition().below();
                    if (p.serverLevel()
                        .getBlockState(below)
                        .isFaceSturdy(p.serverLevel(), below, net.minecraft.core.Direction.UP))
                      plane.setOnGround(true);
                  }
                  fly(h, p, plane, home, target, path, proof, step + 1);
                }));
  }

  static void denied(GameTestHelper h, Step action, String reason) throws Exception {
    boolean blocked = false;
    try {
      action.run();
    } catch (IllegalStateException expected) {
      blocked = true;
    }
    h.assertTrue(blocked, reason);
  }

  static Vec3 post(ConstructionStore.Project v) {
    var b = BlueprintRegistry.get(v.blueprint(), v.blueprintVersion());
    var pos = v.origin().offset(b.rotate(b.home(), v.rotation()));
    return new Vec3(pos.getX() + .5, pos.getY(), pos.getZ() + .5);
  }

  static void review(GameTestHelper h, ServerPlayer p, String action) {
    var v =
        CommerceService.handle(p, new CommerceRequest(2, 1, 8, action, 1, CommerceRequest.NONE));
    h.assertTrue(v.reviewing(), "Real dispatch review: " + v.notice());
  }

  static void confirm(GameTestHelper h, ServerPlayer p) {
    var q = FlightService.quote(p).orElseThrow();
    var v = CommerceService.handle(p, new CommerceRequest(3, 2, 8, "", 1, q.nonce()));
    h.assertTrue(
        !v.reviewing() && !v.notice().isBlank(), "Real dispatch confirmation: " + v.notice());
  }

  static void ack(ServerPlayer p) throws Exception {
    var f =
        net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField(
            "awaitingPositionFromClient");
    f.setAccessible(true);
    if (f.get(p.connection) != null) {
      var i =
          net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField(
              "awaitingTeleport");
      i.setAccessible(true);
      p.connection.handleAcceptTeleportPacket(
          new net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket(
              i.getInt(p.connection)));
    }
  }

  static Object call(Object target, String name) throws Exception {
    return target.getClass().getMethod(name).invoke(target);
  }

  static void send(ServerPlayer p, String name, Class<?>[] types, Object... args) throws Exception {
    var packet =
        Class.forName("immersive_aircraft.network.c2s." + name)
            .getConstructor(types)
            .newInstance(args);
    packet.getClass().getMethod("receiveServer", ServerPlayer.class).invoke(packet, p);
  }

  @FunctionalInterface
  interface Step {
    void run() throws Exception;
  }

  static void guard(GameTestHelper h, Step s) {
    try {
      s.run();
    } catch (Exception e) {
      com.mojang.logging.LogUtils.getLogger().error("Actual flight dispatch assertion failed", e);
      h.runAfterDelay(1, () -> h.fail(e.toString()));
    }
  }
}
