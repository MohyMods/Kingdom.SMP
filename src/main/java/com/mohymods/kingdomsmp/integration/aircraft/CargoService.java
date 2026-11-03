package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.building.*;
import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.network.*;
import com.mohymods.kingdomsmp.npc.ReeveEntity;
import com.mohymods.kingdomsmp.persistence.ConstructionStore;
import com.mohymods.kingdomsmp.placement.PlacementProtection;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;

/** One genuine server-observed daily cargo. No client travel proof, item grant or profession XP. */
public final class CargoService {
  public record Quote(
      UUID nonce,
      long revision,
      String action,
      FlightBook.Mission mission,
      CargoState.Terms terms,
      long issued,
      String text) {}

  private record Sample(
      UUID entity, Vec3 position, boolean qualified, boolean leg, int controlledTick) {}

  private static final Map<MinecraftServer, FlightBook> BOOKS = new WeakHashMap<>();
  private static final Map<MinecraftServer, String> ERRORS = new WeakHashMap<>();
  private static final Map<ServerPlayer, Quote> QUOTES = new WeakHashMap<>();
  private static final Map<ServerPlayer, Sample> SAMPLES = new WeakHashMap<>();
  private static Method fuel, engine;

  private CargoService() {}

  public static void bind() throws ReflectiveOperationException {
    var type = Class.forName("immersive_aircraft.entity.EngineVehicle");
    fuel = type.getMethod("getFuelUtilization");
    engine = type.getMethod("getEngineTarget");
  }

  public static long day() {
    return Math.floorDiv(System.currentTimeMillis(), 86_400_000L);
  }

  public static void initialize(MinecraftServer server) {
    BOOKS.remove(server);
    ERRORS.remove(server);
    QUOTES.keySet().removeIf(p -> p.server == server);
    SAMPLES.keySet().removeIf(p -> p.server == server);
    var path = server.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_air_cargo.dat");
    if (java.nio.file.Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
      try {
        book(server);
      } catch (IllegalStateException blocked) {
        com.mojang.logging.LogUtils.getLogger()
            .error("Air cargo evidence needs recovery; other services remain active", blocked);
      }
  }

  public static List<FlightBook.Mission> savedMissions(MinecraftServer server) {
    var b = BOOKS.get(server);
    return b == null ? List.of() : b.all();
  }

  public static List<String> inspection(MinecraftServer server) {
    var store = CommerceService.store(server);
    long today = day();
    var rows = new ArrayList<String>();
    rows.add(
        "Air cargo: UTC day "
            + today
            + " | paid "
            + store.cargoPaid(today)
            + " | held "
            + store.cargoHeld(today)
            + " | "
            + status(server));
    for (var task : store.pendingCargo().stream().limit(64).toList())
      rows.add(
          "Cargo reservation "
              + task.reservation().id()
              + " | pilot "
              + task.terms().pilot()
              + " | registration "
              + task.reservation().registration()
              + " | goods "
              + task.terms().quantity()
              + " "
              + task.terms().item()
              + " | reward held "
              + task.terms().reward());
    for (var m : savedMissions(server).stream().limit(64).toList()) {
      var task = store.cargoContract(m.id()).orElse(null);
      if (task == null) {
        rows.add("Cargo evidence " + m.id() + " | missing canonical contract");
        continue;
      }
      String goods = "unloaded";
      if (AircraftAccess.available() && m.registration() != null) {
        var record = store.aircraft(m.registration()).orElse(null);
        if (record != null) {
          var entity = CargoTransactions.body(server, record);
          if (entity != null && CargoTransactions.authenticated(entity, record, store))
            goods = Integer.toString(CargoInventory.count(entity, task.terms().item()));
        }
      }
      rows.add(
          "Cargo "
              + m.id()
              + " | pilot "
              + m.pilot()
              + " | "
              + task.stage()
              + " | flight "
              + (long) m.meters()
              + "/"
              + m.minimum()
              + " | landed "
              + m.arrived()
              + " | native goods "
              + goods
              + " "
              + task.terms().item()
              + " | reward "
              + task.terms().reward());
    }
    return List.copyOf(rows);
  }

  public static String status(MinecraftServer server) {
    return ERRORS.getOrDefault(
        server,
        BOOKS.containsKey(server) ? "Saved cargo evidence active" : "No cargo accepted yet");
  }

  public static FlightBook book(MinecraftServer server) {
    if (ERRORS.containsKey(server)) throw new IllegalStateException(ERRORS.get(server));
    return BOOKS.computeIfAbsent(
        server,
        s -> {
          try {
            return FlightBook.openCargo(
                s.getWorldPath(LevelResource.ROOT).resolve("data"), CommerceService.store(s).id());
          } catch (IOException e) {
            ERRORS.put(s, "Air cargo evidence needs matching data recovery. Ask an administrator.");
            throw new IllegalStateException(ERRORS.get(s), e);
          }
        });
  }

  public static void close(ServerPlayer p) {
    QUOTES.remove(p);
  }

  public static void logout(ServerPlayer p) {
    close(p);
    SAMPLES.remove(p);
    var b = BOOKS.get(p.server);
    if (b != null)
      try {
        b.checkpoint();
      } catch (IOException e) {
        disable(p.server, e);
      }
  }

  public static Optional<Quote> quote(ServerPlayer p) {
    var q = QUOTES.get(p);
    if (q != null
        && (q.revision() != CommerceService.store(p.server).revision()
            || !q.action().equals("cargo:resume")
                && !q.action().equals("cargo:cancel")
                && q.mission().day() != day()
            || System.nanoTime() - q.issued() > 30_000_000_000L)) {
      close(p);
      q = null;
    }
    return Optional.ofNullable(q);
  }

  private static ConstructionStore.Project headquarters(MinecraftServer server) {
    var target = ConstructionService.lodge(server);
    if (target == null || target.phase() != ConstructionStore.Phase.COMPLETE || target.npcDead())
      throw new IllegalStateException(
          "Maintain the kingdom headquarters and its Steward before accepting a cargo.");
    return target;
  }

  private static net.minecraft.core.BlockPos post(ConstructionStore.Project project) {
    var b = BlueprintRegistry.get(project.blueprint(), project.blueprintVersion());
    return project.origin().offset(b.rotate(b.home(), project.rotation()));
  }

  private static void requireTarget(ServerPlayer p, FlightBook.Mission m) {
    CommerceService.requireResidentCounter(p);
    String denial = AircraftAccess.denial(p.server);
    if (!denial.isEmpty()) throw new IllegalStateException(denial);
    var target = headquarters(p.server);
    if (!target.id().equals(m.target())
        || !ConstructionService.structureIntact(p.serverLevel(), target))
      throw new IllegalStateException("The cargo headquarters changed, is damaged or unloaded.");
    var npc = p.serverLevel().getEntity(target.npc());
    if (!(npc instanceof ReeveEntity resident)
        || !ConstructionService.registeredReeve(resident)
        || p.distanceToSqr(npc) > 64
        || !p.hasLineOfSight(npc))
      throw new IllegalStateException(
          "Visit the assigned headquarters Steward within eight blocks and clear sight to report"
              + " your flight.");
    denial = PlacementProtection.entityInteraction(p, npc);
    if (denial != null) throw new IllegalStateException(denial);
  }

  private static boolean registeredFrame(ServerPlayer p, FlightBook.Mission m, Entity entity) {
    if (!AircraftAccess.aircraft(entity) || !entity.getUUID().equals(m.entity())) return false;
    var marker = entity.getPersistentData().getCompound(AircraftSales.MARKER);
    return marker.hasUUID("store")
        && marker.hasUUID("registration")
        && marker.contains("generation", net.minecraft.nbt.Tag.TAG_LONG)
        && marker.getUUID("store").equals(CommerceService.store(p.server).id())
        && marker.getUUID("registration").equals(m.registration())
        && marker.getLong("generation") == m.generation()
        && AircraftSales.denial(p, entity).isEmpty();
  }

  private static Entity arrivalFrame(ServerPlayer p, FlightBook.Mission m) {
    if (m.registration() == null || !m.arrived() || m.meters() < m.minimum() || m.samples() < 8)
      throw new IllegalStateException(
          "Complete the powered flight distance and land near headquarters before reporting.");
    var record = CommerceService.store(p.server).aircraft(m.registration()).orElseThrow();
    if (record.phase() != AircraftState.Phase.ACTIVE
        || record.generation() != m.generation()
        || !record.permits(p.getUUID()))
      throw new IllegalStateException(
          "Your cargo aircraft or shared access changed; restart at Corin.");
    var entity = p.serverLevel().getEntity(m.entity());
    if (entity == null
        || !registeredFrame(p, m, entity)
        || !entity.onGround()
        || !entity.getPassengers().isEmpty()
        || entity.position().distanceToSqr(Vec3.atCenterOf(post(headquarters(p.server)))) > 48 * 48
        || !AircraftSales.denial(p, entity).isEmpty())
      throw new IllegalStateException(
          "Park the original cargo aircraft within 48 blocks of headquarters and dismount before"
              + " reporting.");
    String denial = PlacementProtection.entityInteraction(p, entity);
    if (denial != null) throw new IllegalStateException(denial);
    return entity;
  }

  private static CargoState.Contract current(ServerPlayer p) {
    var store = CommerceService.store(p.server);
    return store.pendingCargo().stream()
        .filter(c -> c.terms().pilot().equals(p.getUUID()))
        .findFirst()
        .orElseGet(() -> store.cargo(p.getUUID(), day()).orElse(null));
  }

  private static FlightBook.Mission voyage(CargoState.Terms t) {
    return new FlightBook.Mission(
        t.id(),
        t.pilot(),
        t.day(),
        t.origin(),
        t.target(),
        t.minimum(),
        t.reward(),
        t.budget(),
        null,
        null,
        -1,
        0,
        0,
        false,
        false,
        false);
  }

  public static void review(ServerPlayer p, String action) {
    close(p);ScoutService.close(p);
    FlightService.close(p);
    com.mohymods.kingdomsmp.profession.ContractService.close(p);
    com.mohymods.kingdomsmp.fishing.FishingContractService.close(p);
    com.mohymods.kingdomsmp.bounty.CommunityContractService.close(p);
    com.mohymods.kingdomsmp.bounty.HuntService.close(p);
    CargoRules.get(p.server).requireEnabled();
    var store = CommerceService.store(p.server);
    var task = current(p);
    CargoState.Terms terms;
    FlightBook.Mission m;
    String text;
    if (action.equals("cargo:start")) {
      AircraftSales.require(p);
      if (task != null && task.stage() != CargoState.Stage.ACCEPTED)
        throw new IllegalStateException(
            "Finish the saved unloading or wait until the next UTC day");
      if (task == null) {
        var r = CargoRules.get(p.server);
        var origin = CommerceService.counterProject(p);
        var target = headquarters(p.server);
        terms =
            new CargoState.Terms(
                FlightBook.cargoMissionId(store.id(), p.getUUID(), day()),
                p.getUUID(),
                day(),
                r.item(),
                r.quantity(),
                r.minimum(),
                r.reward(),
                store.cargoBudget(day()).orElse(r.budget()),
                origin.id(),
                target.id());
      } else terms = task.terms();
      if (store.cargoPaid(terms.day()) + store.cargoHeld(terms.day())
          > terms.budget() - terms.reward())
        throw new IllegalStateException("Today's shared cargo reward pool is exhausted");
      m = voyage(terms);
      text =
          "Accept Royal Air Cargo for "
              + terms.reward()
              + " coins in Mailbox. Carry "
              + terms.quantity()
              + " unmodified "
              + net.minecraft.core.registries.BuiltInRegistries.ITEM
                  .get(net.minecraft.resources.ResourceLocation.parse(terms.item()))
                  .getDescription()
                  .getString()
              + " in native aircraft storage for the entire powered flight of at least "
              + terms.minimum()
              + " airborne blocks. Depart here, land near headquarters, stop the engine and report"
              + " to its Steward. The goods are permanently consumed only after an exact unloading"
              + " review. One reward per UTC day; no profession XP. Restarting clears unfinished"
              + " transport proof.";
    } else {
      if (task == null)
        throw new IllegalStateException("Accept an air cargo contract at Corin first");
      terms = task.terms();
      m =
          book(p.server)
              .get(p.getUUID())
              .filter(v -> v.id().equals(terms.id()))
              .orElseGet(() -> voyage(terms));
      if (action.equals("cargo:finish")) {
        if (task.stage() != CargoState.Stage.ACCEPTED || terms.day() != day())
          throw new IllegalStateException("Cargo contract is no longer available today");
        arrivalFrame(p, m);
        var frame = CargoTransactions.available(p, terms, m.registration(), false);
        CargoInventory.select(frame, terms.item(), terms.quantity());
        m = m.verify();
        book(p.server).put(m);
        checkpoint(p.server);
        text =
            "Unload exactly "
                + terms.quantity()
                + " transported "
                + net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .get(net.minecraft.resources.ResourceLocation.parse(terms.item()))
                    .getDescription()
                    .getString()
                + " from the original native aircraft storage for "
                + terms.reward()
                + " coins in private Mailbox. These goods are permanently consumed. Current"
                + " permissions, exact source stacks and daily budget are checked before the"
                + " durable debit and once-only payment.";
      } else if (action.equals("cargo:resume") || action.equals("cargo:cancel")) {
        if (task.stage() != CargoState.Stage.UNLOADING)
          throw new IllegalStateException("No cargo unloading is pending");
        CargoTransactions.available(p, terms, task.reservation().registration(), true);
        text =
            action.equals("cargo:resume")
                ? "Resume this saved cargo unloading without accepting another contract or paying"
                      + " twice. A saved receipt is acknowledged without another item debit; an"
                      + " untouched source is consumed only under the original reservation and"
                      + " current permissions."
                : "Cancel only a verified unperformed unloading. Exact original goods and receipt"
                      + " are required. No coins are issued or items removed; delivered or"
                      + " uncertain states cannot be canceled.";
      } else throw new IllegalArgumentException("Choose an available cargo action");
    }
    QUOTES.put(
        p,
        new Quote(UUID.randomUUID(), store.revision(), action, m, terms, System.nanoTime(), text));
  }

  public static String confirm(ServerPlayer p, UUID nonce) throws IOException {
    var q = QUOTES.remove(p);
    var store = CommerceService.store(p.server);
    CommerceService.verifyReceipt(p, store);
    CargoRules.get(p.server).requireEnabled();
    if (q == null
        || !q.nonce().equals(nonce)
        || q.revision() != store.revision()
        || System.nanoTime() - q.issued() > 30_000_000_000L
        || !q.action().equals("cargo:resume")
            && !q.action().equals("cargo:cancel")
            && q.terms().day() != day())
      throw new IllegalStateException("Cargo review expired or state changed; review again");
    if (q.action().equals("cargo:start")) {
      AircraftSales.require(p);
      if (!CommerceService.counterProject(p).id().equals(q.terms().origin())
          || !headquarters(p.server).id().equals(q.terms().target()))
        throw new IllegalStateException("Cargo buildings changed");
      var task = store.cargo(p.getUUID(), q.terms().day()).orElse(null);
      if (task != null && task.stage() != CargoState.Stage.ACCEPTED)
        throw new IllegalStateException("Cargo is already delivered or unloading");
      store.acceptCargo(q.terms());
      book(p.server).put(q.mission());
      checkpoint(p.server);
      SAMPLES.remove(p);
      return "Air cargo accepted. Load the goods in native storage before departing; keep them"
                 + " aboard through the flight.";
    }
    var task = store.cargoContract(q.terms().id()).orElseThrow();
    if (!task.terms().equals(q.terms()))
      throw new IllegalStateException("Canonical cargo terms changed");
    if (q.action().equals("cargo:cancel")) {
      CargoTransactions.cancelUndelivered(p, task.terms().id());
      return "Unperformed unloading canceled. Goods remain in the aircraft; no reward was issued.";
    }
    if (q.action().equals("cargo:resume")) {
      CargoTransactions.available(p, task.terms(), task.reservation().registration(), true);
      CargoTransactions.finish(p.server, task);
    } else {
      var m = book(p.server).get(p.getUUID()).orElseThrow();
      if (!m.equals(q.mission()) || !m.verified())
        throw new IllegalStateException("Cargo transport proof changed");
      arrivalFrame(p, m);
      CargoTransactions.begin(
          p, m.id(), m.registration(), (long) Math.floor(m.meters()), m.samples());
    }
    var saved = store.cargoContract(q.terms().id()).orElseThrow();
    if (saved.stage() == CargoState.Stage.DELIVERED) {
      var m =
          book(p.server)
              .get(p.getUUID())
              .filter(v -> v.id().equals(saved.terms().id()))
              .orElse(null);
      if (m != null && m.verified()) {
        book(p.server).put(m.posted());
        checkpoint(p.server);
      }
      return "Air cargo delivered once. Claim "
          + saved.terms().reward()
          + " coins from your private Mailbox.";
    }
    return "Unloading reservation saved. Return to the Steward with the original parked aircraft to"
               + " resume without another reward.";
  }

  public static List<CommerceView.Entry> entries(ServerPlayer p) {
    if (!AircraftAccess.available()) return List.of();
    boolean airport = AircraftSales.atCounter(p);
    var target = ConstructionService.lodge(p.server);
    var selected = CommerceService.counterProject(p);
    boolean atHeadquarters =
        target != null && (selected == null || selected.id().equals(target.id()));
    if (!airport && !atHeadquarters) return List.of();
    var rules = CargoRules.get(p.server);
    if (!rules.enabled())
      return airport
          ? List.of(
              new CommerceView.Entry(
                  "cargo:disabled",
                  "Royal Air Cargo",
                  "Air cargo deliveries are disabled by the server configuration.",
                  0,
                  "LOCKED"))
          : List.of();
    var task = current(p);
    if (task != null && task.stage() == CargoState.Stage.DELIVERED)
      return List.of(
          new CommerceView.Entry(
              "cargo:done",
              "Royal Air Cargo",
              "Today's cargo reward is already saved. Claim any unclaimed coins from Mailbox.",
              0,
              "CARGO_DONE"));
    if (task != null && task.stage() == CargoState.Stage.UNLOADING) {
      var rows = new ArrayList<CommerceView.Entry>();
      rows.add(
          new CommerceView.Entry(
              "cargo:pending",
              "Saved cargo unloading",
              "Visit the assigned headquarters Steward with the original parked aircraft. No"
                  + " absent-body or uncertain-state cancellation is allowed.",
              task.terms().reward(),
              "CARGO_PENDING"));
      if (atHeadquarters) {
        rows.add(
            new CommerceView.Entry(
                "cargo:resume",
                "Resume saved unloading",
                "Review the existing receipt or exact untouched source. No second contract or"
                    + " repeated payout.",
                task.terms().reward(),
                "CARGO_RESUME"));
        rows.add(
            new CommerceView.Entry(
                "cargo:cancel",
                "Cancel unperformed unloading",
                "Only the original exact goods with no delivered receipt permit cancellation. No"
                    + " items or coins are granted.",
                0,
                "CARGO_CANCEL"));
      }
      return List.copyOf(rows);
    }
    var rows = new ArrayList<CommerceView.Entry>();
    FlightBook.Mission m = null;
    try {
      if (task != null)
        m =
            book(p.server)
                .get(p.getUUID())
                .filter(v -> v.id().equals(task.terms().id()))
                .orElse(null);
    } catch (IllegalStateException error) {
      return List.of(
          new CommerceView.Entry(
              "cargo:blocked",
              "Air cargo paused",
              "Cargo evidence needs matching-data recovery; other services remain available.",
              0,
              "LOCKED"));
    }
    if (m != null) {
      rows.add(
          new CommerceView.Entry(
              "cargo:status",
              "Royal Air Cargo",
              (long) m.meters()
                  + " / "
                  + m.minimum()
                  + " airborne blocks. Keep "
                  + task.terms().quantity()
                  + " goods in native storage. "
                  + (m.arrived()
                      ? "Landing recorded; stop the engine and report to the Steward."
                      : "Depart from Corin and land near headquarters."),
              m.reward(),
              "CARGO_ACTIVE"));
      if (atHeadquarters && m.arrived())
        rows.add(
            new CommerceView.Entry(
                "cargo:finish",
                "Review cargo unloading",
                "The exact transported goods are consumed from original native aircraft storage"
                    + " only after confirmation; payment arrives in private Mailbox.",
                m.reward(),
                "CARGO_FINISH"));
    }
    if (airport)
      rows.add(
          new CommerceView.Entry(
              "cargo:start",
              m == null ? "Royal Air Cargo" : "Restart cargo transport",
              "Load "
                  + (task == null ? rules.quantity() : task.terms().quantity())
                  + " goods in native storage, fly from this airfield and deliver to the"
                  + " headquarters Steward. One reward per UTC day. Restart discards flight proof.",
              task == null ? rules.reward() : task.terms().reward(),
              "CARGO_ACCEPT"));
    return List.copyOf(rows);
  }

  public static void interruptTeleport(Entity entity) {
    var server = entity.getServer();
    if (server == null || !server.isSameThread()) return;
    var b = BOOKS.get(server);
    if (b == null) return;
    for (var m : b.all())
      if (m.registration() != null
          && !m.arrived()
          && (m.pilot().equals(entity.getUUID()) || entity.getUUID().equals(m.entity()))) {
        b.put(m.reset());
        var p = server.getPlayerList().getPlayer(m.pilot());
        if (p != null) {
          SAMPLES.remove(p);
          p.displayClientMessage(
              Component.literal(
                  "Teleportation interrupted your unfinished air cargo. Restart at the"
                      + " airfield."),
              true);
        }
      }
  }

  private static void checkpoint(MinecraftServer server) {
    try {
      book(server).checkpoint();
    } catch (IOException error) {
      disable(server, error);
      throw new IllegalStateException(
          "Air cargo evidence requires recovery. Check Mailbox before retrying; other services"
              + " remain available.",
          error);
    }
  }

  private static void disable(MinecraftServer server, Exception error) {
    ERRORS.put(
        server, "Air cargo records require administrator recovery; no cargo rewards are enabled.");
    com.mojang.logging.LogUtils.getLogger().error("Air cargo evidence paused", error);
  }

  public static void tick(MinecraftServer server) {
    var b = BOOKS.get(server);
    var store = CommerceService.initializedStore(server).orElse(null);
    if (b == null || store == null || store.quarantined() || ERRORS.containsKey(server)) return;
    for (var p : List.copyOf(server.getPlayerList().getPlayers()))
      try {
        observe(p, b);
      } catch (IOException | ReflectiveOperationException | RuntimeException e) {
        disable(server, e);
        break;
      }
    if (server.getTickCount() % 400 == 0)
      try {
        b.checkpoint();
      } catch (IOException e) {
        disable(server, e);
      }
  }

  private static void observe(ServerPlayer p, FlightBook b)
      throws IOException, ReflectiveOperationException {
    var m = b.get(p.getUUID()).orElse(null);
    if (m == null || m.day() != day() || m.rewarded()) return;
    var store = CommerceService.store(p.server);
    var task =
        store
            .cargoContract(m.id())
            .orElseThrow(
                () -> new IllegalStateException("Cargo voyage lacks canonical accepted terms"));
    if (task.stage() == CargoState.Stage.DELIVERED) {
      if (m.verified()) {
        b.put(m.posted());
        b.checkpoint();
      }
      return;
    }
    if (task.stage() == CargoState.Stage.UNLOADING) return;
    if (m.verified()) return;
    if (p.hasDisconnected()
        || !p.isAlive()
        || p.isSpectator()
        || p.getAbilities().mayfly
        || p.serverLevel() != p.server.overworld()
        || p.server.getTickCount() % 20 == 0 && !AircraftAccess.denial(p.server).isEmpty()) {
      SAMPLES.remove(p);
      return;
    }
    Entity plane = p.getVehicle();
    boolean controlling =
        AircraftAccess.aircraft(plane)
            && plane.getControllingPassenger() == p
            && AircraftSales.denial(p, plane).isEmpty();
    if (m.registration() == null) {
      if (!controlling) return;
      if (CargoInventory.count(plane, task.terms().item()) < task.terms().quantity()) return;
      var data = plane.getPersistentData().getCompound(AircraftSales.MARKER);
      if (!data.hasUUID("registration")) return;
      var origin = ConstructionService.store(p.server).get(m.origin());
      if (plane.position().distanceToSqr(Vec3.atCenterOf(post(origin))) > 48 * 48) return;
      var record = store.aircraft(data.getUUID("registration")).orElseThrow();
      m = m.assign(record.id(), plane.getUUID(), record.generation());
      b.put(m);
    }
    if (!controlling) {
      plane = p.serverLevel().getEntity(m.entity());
      var old = SAMPLES.get(p);
      if (plane == null
          || old == null
          || !old.entity().equals(m.entity())
          || p.server.getTickCount() - old.controlledTick() > 40) {
        SAMPLES.remove(p);
        return;
      }
    }
    if (!registeredFrame(p, m, plane)) {
      SAMPLES.remove(p);
      return;
    }
    if (CargoInventory.count(plane, task.terms().item()) < task.terms().quantity()) {
      b.put(m.reset());
      SAMPLES.remove(p);
      p.displayClientMessage(
          Component.literal("Cargo left native storage. Reload and restart from the airfield."),
          true);
      return;
    }
    var previous = SAMPLES.get(p);
    Vec3 now = plane.position();
    if (previous != null && now.distanceToSqr(previous.position()) > 32 * 32) {
      b.put(m.reset());
      SAMPLES.remove(p);
      p.displayClientMessage(
          Component.literal(
              "A travel discontinuity interrupted the cargo. Restart at the airfield."),
          true);
      return;
    }
    boolean powered =
        controlling
            && engine.getDeclaringClass().isInstance(plane)
            && (float) engine.invoke(plane) > 0
            && (float) fuel.invoke(plane) > 0;
    boolean high =
        powered
            && !plane.onGround()
            && p.serverLevel()
                    .clip(
                        new ClipContext(
                            now,
                            now.add(0, -3, 0),
                            ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.ANY,
                            plane))
                    .getType()
                == HitResult.Type.MISS;
    boolean leg = previous != null && previous.leg();
    if (high) {
      double distance =
          previous != null && previous.qualified()
              ? Math.hypot(now.x - previous.position().x, now.z - previous.position().z)
              : 0;
      m = m.fly(m.registration(), m.entity(), m.generation(), distance);
      b.put(m);
      leg = true;
    }
    if (plane.onGround() && leg) {
      var target = ConstructionService.store(p.server).get(m.target());
      if (m.meters() >= m.minimum()
          && m.samples() >= 8
          && now.distanceToSqr(Vec3.atCenterOf(post(target))) <= 48 * 48) {
        m = m.arrive();
        b.put(m);
        b.checkpoint();
        p.displayClientMessage(
            Component.literal(
                "Royal Air Cargo ready. Dismount and report to the headquarters Steward."),
            true);
      }
      leg = false;
    }
    SAMPLES.put(
        p,
        new Sample(
            m.entity(),
            now,
            high,
            leg,
            controlling
                ? p.server.getTickCount()
                : previous == null ? 0 : previous.controlledTick()));
    if (controlling && p.server.getTickCount() % 40 == 0)
      p.displayClientMessage(
          Component.literal(
              "Royal Air Cargo: "
                  + (long) Math.floor(m.meters())
                  + " / "
                  + m.minimum()
                  + " airborne blocks · Land near headquarters"),
          true);
  }
}
