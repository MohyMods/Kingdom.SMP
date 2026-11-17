package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.core.registries.*;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Explicitly authorized native unload. No entity-join hook and no automatic stale-body debit. */
public final class CargoTransactions {
  public static final String RECEIPT = "KingdomAirCargoReceipt";
  private static Method engine;

  private CargoTransactions() {}

  public static void bind() throws ReflectiveOperationException {
    engine = Class.forName("immersive_aircraft.entity.EngineVehicle").getMethod("getEngineTarget");
  }

  public static boolean authenticated(
      Entity e, AircraftState.Registration frame, EconomyStore store) {
    if (!AircraftAccess.aircraft(e)
        || !e.isAlive()
        || frame.phase() != AircraftState.Phase.ACTIVE
        || !BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(frame.type())
        || !frame.deployment().getCompound("entity").getUUID("UUID").equals(e.getUUID()))
      return false;
    var marker = e.getPersistentData().getCompound(AircraftSales.MARKER);
    return marker.getAllKeys().equals(Set.of("store", "registration", "generation"))
        && marker.hasUUID("store")
        && marker.getUUID("store").equals(store.id())
        && marker.hasUUID("registration")
        && marker.getUUID("registration").equals(frame.id())
        && marker.contains("generation", Tag.TAG_LONG)
        && marker.getLong("generation") == frame.generation();
  }

  public static Entity body(MinecraftServer server, AircraftState.Registration frame) {
    var level =
        server.getLevel(
            net.minecraft.resources.ResourceKey.create(
                Registries.DIMENSION,
                net.minecraft.resources.ResourceLocation.parse(
                    frame.deployment().getString("dimension"))));
    return level == null
        ? null
        : level.getEntity(frame.deployment().getCompound("entity").getUUID("UUID"));
  }

  public static UUID receipt(Entity entity) {
    var data = entity.getPersistentData();
    if (data.contains(RECEIPT) && !data.hasUUID(RECEIPT))
      throw new IllegalStateException("Malformed native cargo receipt; restore matching data");
    return data.hasUUID(RECEIPT) ? data.getUUID(RECEIPT) : null;
  }

  /** A mismatch blocks use; even a recognized old source requires explicit recovery review. */
  public static CargoReceiptRules.Action state(
      Entity entity, AircraftState.Registration frame, EconomyStore store) {
    if (!authenticated(entity, frame, store)) return CargoReceiptRules.Action.REFUSE;
    var pending = store.pendingCargo(frame.id()).orElse(null);
    var delivered = store.deliveredCargo(frame.id()).orElse(null);
    boolean before =
        pending != null
            && CargoInventory.matches(
                entity,
                store.cargoContract(pending.contract()).orElseThrow().terms().item(),
                pending.slots(),
                false);
    return CargoReceiptRules.decide(
        true,
        receipt(entity),
        pending == null ? null : pending.id(),
        pending == null ? null : pending.previousReceipt(),
        delivered == null ? null : delivered.reservation().id(),
        delivered == null ? null : delivered.reservation().previousReceipt(),
        before);
  }

  public static boolean blocked(
      Entity entity, AircraftState.Registration frame, EconomyStore store) {
    if (store.pendingCargo(frame.id()).isEmpty()
        && store.deliveredCargoReceipt(frame.id()).isEmpty()
        && !entity.getPersistentData().contains(RECEIPT)) return false;
    try {
      var action = state(entity, frame, store);
      if (action == CargoReceiptRules.Action.READY) return false;
      if (action == CargoReceiptRules.Action.WAIT_FOR_ACTOR
          || action == CargoReceiptRules.Action.ACKNOWLEDGE) return true;
      throw new IllegalStateException(
          "Cargo receipt/history mismatch; explicit matching-data recovery required");
    } catch (RuntimeException failure) {
      store.quarantine();
      com.mojang.logging.LogUtils.getLogger()
          .error("Cargo authority paused without modifying goods", failure);
      return true;
    }
  }

  public static void requireCounter(ServerPlayer p, CargoState.Terms terms) {
    CommerceService.requireResidentCounter(p);
    String denial = AircraftAccess.denial(p.server);
    if (!denial.isEmpty()) throw new IllegalStateException(denial);
    var target = CommerceService.counterProject(p);
    if (target == null) target = ConstructionService.lodge(p.server);
    if (target == null
        || !target.id().equals(terms.target())
        || target.phase() != ConstructionStore.Phase.COMPLETE)
      throw new IllegalStateException(
          "Visit the assigned headquarters Steward before unloading air cargo");
  }

  public static Entity available(
      ServerPlayer p, CargoState.Terms terms, UUID registration, boolean pending) {
    requireCounter(p, terms);
    var store = CommerceService.store(p.server);
    var frame = store.aircraft(registration).orElseThrow();
    var entity = body(p.server, frame);
    if (entity == null
        || !authenticated(entity, frame, store)
        || !frame.permits(p.getUUID())
        || !terms.pilot().equals(p.getUUID())
        || entity.level() != p.serverLevel()
        || !entity.onGround()
        || !entity.getPassengers().isEmpty()
        || !Double.isFinite(entity.getDeltaMovement().lengthSqr())
        || entity.getDeltaMovement().lengthSqr() > .01
        || store.aircraftRepair(registration).isPresent()
        || !pending && store.pendingCargo(registration).isPresent())
      throw new IllegalStateException(
          "Park the original authorized aircraft unoccupied with the engine off near headquarters");
    var project = ConstructionService.store(p.server).get(terms.target());
    var b = BlueprintRegistry.get(project.blueprint(), project.blueprintVersion());
    var home = project.origin().offset(b.rotate(b.home(), project.rotation()));
    if (entity.position().distanceToSqr(Vec3.atCenterOf(home)) > 48 * 48)
      throw new IllegalStateException("Park the aircraft within 48 blocks of headquarters");
    String denial = PlacementProtection.entityInteraction(p, entity);
    if (denial != null) throw new IllegalStateException(denial);
    try {
      float throttle = (float) engine.invoke(entity);
      if (!Float.isFinite(throttle) || throttle > .001f)
        throw new IllegalStateException("Stop the aircraft engine before unloading cargo");
    } catch (ReflectiveOperationException failure) {
      throw new IllegalStateException("Native engine unavailable", failure);
    }
    return entity;
  }

  /**
   * Trusted server mission code supplies recorded flight evidence; there is no packet/command
   * entry.
   */
  public static UUID begin(
      ServerPlayer p, UUID contract, UUID registration, long meters, int samples)
      throws IOException {
    var store = CommerceService.store(p.server);
    CommerceService.verifyReceipt(p, store);
    var task = store.cargoContract(contract).orElseThrow();
    if (task.stage() != CargoState.Stage.ACCEPTED || task.terms().day() != FlightService.day())
      throw new IllegalStateException("Cargo contract is not available for today's unloading");
    var entity = available(p, task.terms(), registration, false);
    var frame = store.aircraft(registration).orElseThrow();
    if (state(entity, frame, store) != CargoReceiptRules.Action.READY)
      throw new IllegalStateException("Inspect matching cargo receipts before another unload");
    var selected = CargoInventory.select(entity, task.terms().item(), task.terms().quantity());
    var r =
        new CargoState.Reservation(
            UUID.randomUUID(),
            contract,
            registration,
            entity.getUUID(),
            frame.generation(),
            selected,
            store.deliveredCargoReceipt(registration).orElse(null));
    store.reserveCargo(p.getUUID(), r, meters, samples);
    finish(p.server, store.cargoContract(contract).orElseThrow());
    return r.id();
  }

  public static void finish(MinecraftServer server, CargoState.Contract task) throws IOException {
    if (task.stage() != CargoState.Stage.UNLOADING) return;
    var store = CommerceService.store(server);
    var r = task.reservation();
    var frame = store.aircraft(r.registration()).orElseThrow();
    var entity = body(server, frame);
    if (entity == null
        || !authenticated(entity, frame, store)
        || frame.generation() != r.generation()
        || !entity.getUUID().equals(r.entity())) return;
    var decision = state(entity, frame, store);
    if (decision == CargoReceiptRules.Action.ACKNOWLEDGE) {
      store.settleCargo(UUID.randomUUID(), task.terms().pilot(), r.id(), true);
      return;
    }
    if (decision != CargoReceiptRules.Action.WAIT_FOR_ACTOR)
      throw new IOException(
          "Native cargo source/receipt requires matching-data recovery; no guessed debit or"
              + " payout");
    var actor = server.getPlayerList().getPlayer(task.terms().pilot());
    if (actor == null || actor.hasDisconnected()) return;
    try {
      if (available(actor, task.terms(), frame.id(), true) != entity) return;
    } catch (IllegalStateException unavailable) {
      return;
    }
    CargoInventory.consume(entity, task.terms().item(), r.slots());
    entity.getPersistentData().putUUID(RECEIPT, r.id());
    if (!server.saveEverything(false, true, true))
      throw new IOException("Native cargo world checkpoint was not performed");
    store.settleCargo(UUID.randomUUID(), task.terms().pilot(), r.id(), true);
  }

  public static void cancelUndelivered(ServerPlayer p, UUID contract) throws IOException {
    var store = CommerceService.store(p.server);
    var task = store.cargoContract(contract).orElseThrow();
    if (task.stage() != CargoState.Stage.UNLOADING)
      throw new IllegalStateException("No native cargo unloading is pending");
    var r = task.reservation();
    var entity = available(p, task.terms(), r.registration(), true);
    var frame = store.aircraft(r.registration()).orElseThrow();
    if (state(entity, frame, store) != CargoReceiptRules.Action.WAIT_FOR_ACTOR)
      throw new IllegalStateException(
          "Unloading is delivered or uncertain; absence never authorizes cancellation");
    store.settleCargo(UUID.randomUUID(), p.getUUID(), r.id(), false);
  }
}
