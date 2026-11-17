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
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("kingdom_smp_aircraft_fuel_tests")
@PrefixGameTestTemplate(false)
public final class AircraftFuelRuntimeTests {
  @GameTest(template = "empty", timeoutTicks = 700)
  public static void actualFuelPurchasePrivateClaimNativeBoilerAndRestart(GameTestHelper h)
      throws Exception {
    var level = h.getLevel();
    var server = level.getServer();
    var path = server.getWorldPath(LevelResource.ROOT).resolve("aircraft-fuel-proof.nbt");
    var proof = Files.exists(path) ? AtomicNbt.read(path) : new CompoundTag();
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
    var b = BlueprintRegistry.get(airport.blueprint(), airport.blueprintVersion());
    var home = airport.origin().offset(b.rotate(b.home(), airport.rotation()));
    p.teleportTo(level, home.getX() + .5, home.getY(), home.getZ() - 2.5, 0, 0);
    level.getChunkSource().move(p);
    h.runAfterDelay(
        90,
        () ->
            guard(
                h,
                () -> {
                  var npc = (ReeveEntity) level.getEntity(airport.npc());
                  CommerceService.openResident(p, npc);
                  var store = CommerceService.store(server);
                  var rules = AircraftFuel.rules(server);
                  var record =
                      proof.hasUUID("registration")
                          ? store.aircraft(proof.getUUID("registration")).orElseThrow()
                          : store.aircraft().stream()
                              .filter(v -> v.phase() == AircraftState.Phase.ACTIVE)
                              .findFirst()
                              .orElseThrow();
                  var plane =
                      level.getEntity(record.deployment().getCompound("entity").getUUID("UUID"));
                  h.assertTrue(
                      plane instanceof Container,
                      "Original registered native airframe loaded near Corin");
                  var inventory = (Container) plane;
                  var description = call(plane, "getInventoryDescription");
                  var boilers =
                      (List<?>)
                          description
                              .getClass()
                              .getMethod("getSlots", String.class)
                              .invoke(description, "boiler");
                  int boiler = (int) call(boilers.getFirst(), "index");
                  if (proof.hasUUID("mail")) {
                    h.assertTrue(
                        store.balance(p.getUUID()) == proof.getLong("balance")
                            && store.shopSold(FlightService.day(), AircraftFuel.RESOURCE)
                                == rules.packSize(),
                        "Actual second JVM retains exact single fuel purchase and stock");
                    h.assertTrue(
                        store.itemMailbox(p.getUUID()).stream()
                            .filter(v -> v.id().equals(proof.getUUID("mail")))
                            .allMatch(EconomyStore.ItemMail::claimed),
                        "Physical fuel delivery remains claimed");
                    h.assertTrue(
                        coal(p) == 0
                            && inventory.getItem(boiler).is(Items.COAL)
                            && inventory.getItem(boiler).getCount() == proof.getInt("remaining")
                            && (float) call(plane, "getFuelUtilization") > 0,
                        "Native boiler fuel and remaining paid coal persist without another"
                            + " delivery");
                    h.assertTrue(
                        !p.hasPermissions(2) && !p.getAbilities().mayfly,
                        "No operator/flight bypass");
                    h.succeed();
                    com.mojang.logging.LogUtils.getLogger()
                        .info(
                            "AIRCRAFT_FUEL_RESTART_VERIFIED: real second JVM preserves paid"
                                + " coal/native fuel, exact purse/stock/claimed mail and original"
                                + " registration");
                    return;
                  }
                  h.assertTrue(
                      coal(p) == 0
                          && inventory.getItem(boiler).isEmpty()
                          && (float) call(plane, "getFuelUtilization") == 0,
                      "Explicit private source has no player/boiler coal or prefilled native fuel");
                  long before = store.balance(p.getUUID());
                  var view =
                      CommerceService.handle(
                          p,
                          new CommerceRequest(
                              1, 1, 1, AircraftFuel.ACTION, 2304, CommerceRequest.NONE));
                  h.assertTrue(
                      view.reviewing()
                          && AircraftSales.quote(p).orElseThrow().price() == rules.price(),
                      "Manipulated request quantity cannot replace fixed server-derived fuel pack");
                  var q = AircraftSales.quote(p).orElseThrow();
                  CommerceService.handle(p, new CommerceRequest(2, 2, 1, "", 1, q.nonce()));
                  h.assertTrue(
                      store.balance(p.getUUID()) == before - rules.price()
                          && store.shopSold(FlightService.day(), AircraftFuel.RESOURCE)
                              == rules.packSize(),
                      "Real purchase commits exact price and finite coal units");
                  boolean replay = false;
                  try {
                    CommerceService.handle(p, new CommerceRequest(3, 2, 1, "", 1, q.nonce()));
                  } catch (IllegalStateException expected) {
                    replay = true;
                  }
                  h.assertTrue(
                      replay && store.balance(p.getUUID()) == before - rules.price(),
                      "Old purchase nonce cannot debit or grant twice");
                  AuctionService.review(p, 2, q.nonce().toString(), 1, 0);
                  AuctionService.confirm(p, AuctionService.quote(p).orElseThrow().nonce());
                  h.assertTrue(
                      coal(p) == rules.packSize(),
                      "Normal native player receipt claim delivers exact purchased coal");
                  boolean repeated = false;
                  try {
                    AuctionService.review(p, 2, q.nonce().toString(), 1, 0);
                  } catch (IllegalStateException expected) {
                    repeated = true;
                  }
                  h.assertTrue(
                      repeated && coal(p) == rules.packSize(),
                      "Claimed private fuel mail cannot create more coal");
                  p.teleportTo(level, plane.getX(), plane.getY(), plane.getZ() - 3.5, 0, 0);
                  level.getChunkSource().move(p);
                  plane.getClass().getMethod("openInventory", ServerPlayer.class).invoke(plane, p);
                  var menu = p.containerMenu;
                  h.assertTrue(
                      menu != p.inventoryMenu && menu.stillValid(p),
                      "Original authorized native inventory opens");
                  int source = -1;
                  for (int i = 0; i < menu.slots.size(); i++)
                    if (menu.getSlot(i).container == p.getInventory()
                        && menu.getSlot(i).getItem().is(Items.COAL)) {
                      source = i;
                      break;
                    }
                  h.assertTrue(
                      source >= 0 && menu.getSlot(boiler).mayPlace(menu.getSlot(source).getItem()),
                      "Native boiler accepts actual paid coal");
                  click(p, source);
                  click(p, boiler);
                  h.assertTrue(
                      menu.getCarried().isEmpty()
                          && coal(p) == 0
                          && inventory.getItem(boiler).getCount() == rules.packSize(),
                      "Real native container clicks transfer all paid fuel with empty cursor");
                  p.closeContainer();
                  h.assertTrue(
                      p.startRiding(plane, true), "Ordinary registered buyer boards fueled plane");
                  h.runAfterDelay(
                      4,
                      () ->
                          guard(
                              h,
                              () -> {
                                h.assertTrue(
                                    (float) call(plane, "getFuelUtilization") > 0
                                        && inventory.getItem(boiler).getCount() < rules.packSize(),
                                    "Native engine actually burns purchased coal; no fuel-value"
                                        + " injection");
                                send(p, "EnginePowerMessage", new Class<?>[] {float.class}, .5f);
                                h.assertTrue(
                                    (float) call(plane, "getEngineTarget") == .5f,
                                    "Actual native engine accepts powered throttle from paid fuel");
                                send(p, "EnginePowerMessage", new Class<?>[] {float.class}, 0f);
                                p.stopRiding();
                                proof.putUUID("registration", record.id());
                                proof.putUUID("mail", q.nonce());
                                proof.putLong("balance", store.balance(p.getUUID()));
                                proof.putInt("remaining", inventory.getItem(boiler).getCount());
                                AtomicNbt.write(path, proof);
                                server.getPlayerList().saveAll();
                                server.saveEverything(false, true, true);
                                h.succeed();
                                com.mojang.logging.LogUtils.getLogger()
                                    .info(
                                        "AIRCRAFT_FUEL_PREPARED: actual fixed-pack purchase/private"
                                            + " claim/normal boiler click/native coal consumption,"
                                            + " exact debit and finite stock");
                              }));
                }));
  }

  static int coal(ServerPlayer p) {
    int total = 0;
    for (var stack : p.getInventory().items) if (stack.is(Items.COAL)) total += stack.getCount();
    return total;
  }

  static void click(ServerPlayer p, int slot) {
    var menu = p.containerMenu;
    p.connection.handleContainerClick(
        new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(
            menu.containerId,
            menu.getStateId(),
            slot,
            0,
            net.minecraft.world.inventory.ClickType.PICKUP,
            ItemStack.EMPTY,
            new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>()));
  }

  static Object call(Object object, String method) throws Exception {
    return object.getClass().getMethod(method).invoke(object);
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

  static void guard(GameTestHelper h, Step step) {
    try {
      step.run();
    } catch (Exception error) {
      com.mojang.logging.LogUtils.getLogger()
          .error("Native purchased fuel assertion failed", error);
      h.runAfterDelay(1, () -> h.fail(error.toString()));
    }
  }
}
