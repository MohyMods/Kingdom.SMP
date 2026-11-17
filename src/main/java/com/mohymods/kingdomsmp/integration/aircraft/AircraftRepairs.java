package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.CommerceView;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;

/**
 * Reserved coins -> saved native entity receipt -> canonical settlement. No remote or repeated
 * heal.
 */
public final class AircraftRepairs {
  public static final String RECEIPT = "KingdomAircraftRepairReceipt";
  private static Method health, setHealth, engine;

  private AircraftRepairs() {}

  public static void bind(Class<?> vehicle) throws ReflectiveOperationException {
    health = vehicle.getMethod("getHealth");
    setHealth = vehicle.getMethod("setHealth", float.class);
    engine = Class.forName("immersive_aircraft.entity.EngineVehicle").getMethod("getEngineTarget");
  }

  public static float health(Entity entity) {
    try {
      return (float) health.invoke(entity);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Native aircraft health API unavailable", e);
    }
  }

  private static boolean authenticated(
      Entity entity, AircraftState.Registration record, EconomyStore store) {
    if (entity == null
        || !AircraftAccess.aircraft(entity)
        || !net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
            .getKey(entity.getType())
            .toString()
            .equals(record.type())) return false;
    var tag = entity.getPersistentData().getCompound(AircraftSales.MARKER);
    return tag.getAllKeys().equals(Set.of("store", "registration", "generation"))
        && tag.hasUUID("store")
        && tag.getUUID("store").equals(store.id())
        && tag.hasUUID("registration")
        && tag.getUUID("registration").equals(record.id())
        && tag.contains("generation", net.minecraft.nbt.Tag.TAG_LONG)
        && tag.getLong("generation") == record.generation();
  }

  private static boolean receiptConsistent(
      Entity entity, AircraftState.Registration record, EconomyStore store) {
    var data = entity.getPersistentData();
    if (data.contains(RECEIPT) && !data.hasUUID(RECEIPT)) return false;
    UUID received = data.hasUUID(RECEIPT) ? data.getUUID(RECEIPT) : null;
    UUID expected = store.deliveredAircraftRepair(record.id()).orElse(null);
    var pending = store.aircraftRepair(record.id()).orElse(null);
    return Objects.equals(received, expected) || pending != null && pending.id().equals(received);
  }

  private static Entity body(MinecraftServer server, AircraftState.Registration record) {
    if (record.phase() != AircraftState.Phase.ACTIVE) return null;
    var level =
        server.getLevel(
            net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(
                    record.deployment().getString("dimension"))));
    return level == null
        ? null
        : level.getEntity(record.deployment().getCompound("entity").getUUID("UUID"));
  }

  public static Entity available(ServerPlayer p, UUID registration, boolean pending) {
    AircraftSales.require(p);
    var store = CommerceService.store(p.server);
    var record = store.aircraft(registration).orElseThrow();
    Entity entity = body(p.server, record);
    if (!record.owner().equals(p.getUUID())
        || entity == null
        || !entity.isAlive()
        || entity.level() != p.serverLevel()
        || !AircraftAccess.aircraft(entity)
        || p.distanceToSqr(entity) > 64
        || !p.serverLevel().hasChunkAt(entity.blockPosition())
        || !entity.getPassengers().isEmpty()
        || !entity.onGround()
        || !Double.isFinite(entity.getDeltaMovement().lengthSqr())
        || entity.getDeltaMovement().lengthSqr() > .01)
      throw new IllegalStateException(
          "Park your registered unoccupied aircraft on the ground within eight blocks while"
              + " visiting Corin.");
    if (!authenticated(entity, record, store) || !receiptConsistent(entity, record, store))
      throw new IllegalStateException(
          "Aircraft body does not match its typed canonical registration");
    String denial = PlacementProtection.entityInteraction(p, entity);
    if (denial != null) throw new IllegalStateException(denial);
    if (!pending && store.aircraftRepair(registration).isPresent())
      throw new IllegalStateException("This aircraft already has a paid repair awaiting recovery");
    try {
      if (engine.getDeclaringClass().isInstance(entity)
          && (!Float.isFinite((float) engine.invoke(entity))
              || (float) engine.invoke(entity) > .001f))
        throw new IllegalStateException("Stop the aircraft engine before a workshop repair.");
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException("Native engine state unavailable", e);
    }
    return entity;
  }

  public static long price(ServerPlayer p, UUID registration) {
    var entity = available(p, registration, false);
    float condition = health(entity);
    if (condition >= 1f)
      throw new IllegalStateException(
          "This aircraft is already at full health. No repair payment is needed.");
    return AircraftState.repairPrice(condition, AircraftRepairRules.rate(p.server));
  }

  public static String purchase(ServerPlayer p, UUID nonce, UUID registration, long price)
      throws IOException {
    var entity = available(p, registration, false);
    float before = health(entity);
    long rate = AircraftRepairRules.rate(p.server);
    if (before >= 1f)
      throw new IllegalStateException(
          "This aircraft is already at full health. No repair payment was taken.");
    if (AircraftState.repairPrice(before, rate) != price)
      throw new IllegalStateException("Aircraft condition or repair price changed; review again.");
    var store = CommerceService.store(p.server);
    store.buyAircraftRepair(nonce, p.getUUID(), registration, before, rate);
    finish(p.server, store.aircraftRepair(registration).orElseThrow());
    return store.aircraftRepair(registration).isEmpty()
        ? "Workshop repair complete and saved. You can still repair your aircraft by hand."
        : "Repair funds reserved. Return to Corin with your parked aircraft to resume the work"
            + " without another payment.";
  }

  private static boolean received(Entity entity, UUID ticket) {
    return entity.getPersistentData().hasUUID(RECEIPT)
        && entity.getPersistentData().getUUID(RECEIPT).equals(ticket);
  }

  public static void finish(MinecraftServer server, AircraftState.Repair repair)
      throws IOException {
    var store = CommerceService.store(server);
    var record = store.aircraft(repair.registration()).orElseThrow();
    if (record.phase() != AircraftState.Phase.ACTIVE || record.generation() != repair.generation())
      return;
    var entity = body(server, record);
    if (!authenticated(entity, record, store) || !entity.getUUID().equals(repair.entity())) return;
    if (!receiptConsistent(entity, record, store))
      throw new IOException(
          "Aircraft service receipt differs from the canonical history; restore matching world and"
              + " journal data");
    if (received(entity, repair.id())) {
      store.settleAircraftRepair(UUID.randomUUID(), repair.actor(), repair.id(), false);
      return;
    }
    var actor = server.getPlayerList().getPlayer(repair.actor());
    if (actor == null || actor.hasDisconnected()) return;
    try {
      if (available(actor, record.id(), true) != entity) return;
    } catch (IllegalStateException unavailable) {
      return;
    }
    try {
      setHealth.invoke(entity, 1f);
    } catch (ReflectiveOperationException e) {
      throw new IOException("Native repair API failed after reservation", e);
    }
    entity.getPersistentData().putUUID(RECEIPT, repair.id());
    server.saveEverything(false, true, true);
    store.settleAircraftRepair(UUID.randomUUID(), repair.actor(), repair.id(), false);
  }

  public static AircraftState.Repair refundable(ServerPlayer p, UUID registration) {
    AircraftSales.require(p);
    var store = CommerceService.store(p.server);
    var repair =
        store
            .aircraftRepair(registration)
            .orElseThrow(() -> new IllegalStateException("No paid repair is pending"));
    if (!repair.actor().equals(p.getUUID()))
      throw new IllegalStateException("These repair funds belong to another player.");
    var record = store.aircraft(registration).orElseThrow();
    var entity = body(p.server, record);
    if (!authenticated(entity, record, store)
        || record.generation() != repair.generation()
        || !entity.getUUID().equals(repair.entity()))
      throw new IllegalStateException(
          "Load the original aircraft before inspecting an undelivered repair; absence never"
              + " authorizes a refund");
    if (!receiptConsistent(entity, record, store))
      throw new IllegalStateException(
          "Aircraft repair history does not match its save. Ask an administrator to check matching"
              + " backups.");
    if (received(entity, repair.id()))
      throw new IllegalStateException(
          "The repair already reached this aircraft; finish its saved receipt instead of refunding"
              + " it");
    return repair;
  }

  public static String refund(ServerPlayer p, UUID operation, UUID registration)
      throws IOException {
    var repair = refundable(p, registration);
    CommerceService.store(p.server).settleAircraftRepair(operation, p.getUUID(), repair.id(), true);
    return "Undelivered workshop repair canceled; exactly "
        + repair.price()
        + " reserved coins returned once.";
  }

  public static List<CommerceView.Entry> entries(
      ServerPlayer p, AircraftState.Registration record) {
    var pending = CommerceService.store(p.server).aircraftRepair(record.id());
    if (pending.isPresent()) {
      var repair = pending.orElseThrow();
      boolean refund = false;
      try {
        refundable(p, record.id());
        refund = true;
      } catch (IllegalStateException unavailable) {
      }
      return List.of(
          new CommerceView.Entry(
              "air:repaircancel:" + record.id(),
              "Reserved repair · " + record.id().toString().substring(0, 8),
              repair.price()
                  + " coins reserved for this aircraft. Completed repairs cannot be refunded. Bring"
                  + " the original aircraft back into loaded chunks so Corin can check unfinished"
                  + " work.",
              0,
              refund ? "AIRCRAFT_REPAIR_CANCEL" : "IN_PROGRESS"));
    }
    try {
      long price = price(p, record.id());
      return List.of(
          new CommerceView.Entry(
              "air:repair:" + record.id(),
              "Repair "
                  + record.type().substring(record.type().indexOf(':') + 1).replace('_', ' ')
                  + " · "
                  + record.id().toString().substring(0, 8),
              "Health: "
                  + Math.round(health(body(p.server, record)) * 100)
                  + "%. Restore to full at the workshop. Park unoccupied with"
                  + " engine off within eight blocks while visiting Corin; current claims apply."
                  + " You can still repair your aircraft by hand.",
              price,
              "AIRCRAFT_REPAIR"));
    } catch (IllegalStateException | IllegalArgumentException unavailable) {
      return List.of();
    }
  }

  public static void recover(MinecraftServer server) {
    var store = CommerceService.initializedStore(server).orElse(null);
    if (store == null || store.quarantined()) return;
    for (var repair : store.aircraftRepairs())
      try {
        finish(server, repair);
      } catch (IOException | RuntimeException e) {
        store.quarantine();
        com.mojang.logging.LogUtils.getLogger()
            .error("Paid aircraft repair awaits matching entity/journal recovery", e);
        return;
      }
  }
}
