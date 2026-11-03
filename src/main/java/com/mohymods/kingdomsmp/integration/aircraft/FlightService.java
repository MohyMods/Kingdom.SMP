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

/**
 * One genuine server-observed daily dispatch. No client travel proof, item grant or profession XP.
 */
public final class FlightService {
  public record Quote(
      UUID nonce,
      long revision,
      String action,
      FlightBook.Mission mission,
      long issued,
      String text) {}

  private record Sample(
      UUID entity, Vec3 position, boolean qualified, boolean leg, int controlledTick) {}

  private static final Map<MinecraftServer, FlightBook> BOOKS = new WeakHashMap<>();
  private static final Map<MinecraftServer, String> ERRORS = new WeakHashMap<>();
  private static final Map<ServerPlayer, Quote> QUOTES = new WeakHashMap<>();
  private static final Map<ServerPlayer, Sample> SAMPLES = new WeakHashMap<>();
  private static Method fuel, engine;

  private FlightService() {}

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
    var path = server.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_air_dispatch.dat");
    if (java.nio.file.Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
      try {
        book(server);
      } catch (IllegalStateException blocked) {
        com.mojang.logging.LogUtils.getLogger()
            .error("Air dispatch evidence needs recovery; other services remain active", blocked);
      }
  }

  public static List<FlightBook.Mission> savedMissions(MinecraftServer server) {
    var b = BOOKS.get(server);
    return b == null ? List.of() : b.all();
  }

  public static String status(MinecraftServer server) {
    return ERRORS.getOrDefault(
        server,
        BOOKS.containsKey(server) ? "Saved dispatch evidence active" : "No dispatch accepted yet");
  }

  public static FlightBook book(MinecraftServer server) {
    if (ERRORS.containsKey(server)) throw new IllegalStateException(ERRORS.get(server));
    return BOOKS.computeIfAbsent(
        server,
        s -> {
          try {
            return FlightBook.open(
                s.getWorldPath(LevelResource.ROOT).resolve("data"), CommerceService.store(s).id());
          } catch (IOException e) {
            ERRORS.put(
                s, "Air dispatch evidence needs matching data recovery. Ask an administrator.");
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
            || q.mission().day() != day()
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
          "Maintain the kingdom headquarters and its Steward before accepting a dispatch.");
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
      throw new IllegalStateException("The dispatch headquarters changed, is damaged or unloaded.");
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
          "Your dispatch aircraft or shared access changed; restart at Corin.");
    var entity = p.serverLevel().getEntity(m.entity());
    if (entity == null
        || !registeredFrame(p, m, entity)
        || !entity.onGround()
        || !entity.getPassengers().isEmpty()
        || entity.position().distanceToSqr(Vec3.atCenterOf(post(headquarters(p.server)))) > 48 * 48
        || !AircraftSales.denial(p, entity).isEmpty())
      throw new IllegalStateException(
          "Park the original dispatch aircraft within 48 blocks of headquarters and dismount before"
              + " reporting.");
    String denial = PlacementProtection.entityInteraction(p, entity);
    if (denial != null) throw new IllegalStateException(denial);
    return entity;
  }

  public static void review(ServerPlayer p, String action) {
    close(p);CargoService.close(p);
    com.mohymods.kingdomsmp.profession.ContractService.close(p);
    com.mohymods.kingdomsmp.fishing.FishingContractService.close(p);
    com.mohymods.kingdomsmp.bounty.CommunityContractService.close(p);
    com.mohymods.kingdomsmp.bounty.HuntService.close(p);
    var store = CommerceService.store(p.server);
    long today = day();
    if (store.flightAward(p.getUUID(), today).isPresent())
      throw new IllegalStateException(
          "You already received today's dispatch reward. It resets at 00:00 UTC.");
    FlightBook.Mission m;
    String text;
    if (action.equals("flight:start")) {
      AircraftSales.require(p);
      var origin = CommerceService.counterProject(p);
      var target = headquarters(p.server);
      var rules = FlightRules.get(p.server);
      long budget = store.flightBudget(today).orElse(rules.dailyBudget());
      if (store.flightSpent(today) > budget - rules.reward())
        throw new IllegalStateException("Today's shared air-dispatch reward pool is exhausted.");
      m =
          new FlightBook.Mission(
              FlightBook.missionId(store.id(), p.getUUID(), today),
              p.getUUID(),
              today,
              origin.id(),
              target.id(),
              rules.minimumBlocks(),
              rules.reward(),
              budget,
              null,
              null,
              -1,
              0,
              0,
              false,
              false,
              false);
      text =
          "Accept today's Royal Air Dispatch for "
              + m.reward()
              + " coins in private Mailbox. Board a buyer-registered powered aircraft here; shared"
              + " crew access is accepted. Fly at least "
              + m.minimum()
              + " airborne blocks, land within 48 blocks of headquarters, then visit its Steward to"
              + " report. One reward per player per UTC day; shared pool "
              + store.flightSpent(today)
              + "/"
              + budget
              + ". No profession XP or unlock is granted. Restarting discards unfinished flight"
              + " evidence. Teleporting interrupts an unfinished dispatch.";
    } else if (action.equals("flight:finish")) {
      m =
          book(p.server)
              .get(p.getUUID())
              .orElseThrow(() -> new IllegalStateException("Accept a dispatch at Corin first."));
      if (m.day() != today)
        throw new IllegalStateException(
            "This dispatch expired at UTC midnight. Accept today's dispatch at Corin.");
      requireTarget(p, m);
      arrivalFrame(p, m);
      if (store.flightSpent(today) > m.budget() - m.reward())
        throw new IllegalStateException("Today's shared dispatch reward pool is exhausted.");
      m = m.verify();
      book(p.server).put(m);
      try {
        book(p.server).checkpoint();
      } catch (IOException e) {
        disable(p.server, e);
        throw new IllegalStateException(
            "Flight proof could not be safely saved. No reward was issued.", e);
      }
      text =
          "Report the verified Royal Air Dispatch for exactly "
              + m.reward()
              + " coins in private Mailbox. "
              + (long) Math.floor(m.meters())
              + " airborne blocks and a headquarters landing were recorded. No items are consumed."
              + " The daily reward and shared budget commit together once.";
    } else throw new IllegalArgumentException("Choose an available air dispatch action");
    QUOTES.put(
        p, new Quote(UUID.randomUUID(), store.revision(), action, m, System.nanoTime(), text));
  }

  public static String confirm(ServerPlayer p, UUID nonce) throws IOException {
    var q = QUOTES.remove(p);
    var store = CommerceService.store(p.server);
    CommerceService.verifyReceipt(p, store);
    if (q == null
        || !q.nonce().equals(nonce)
        || q.revision() != store.revision()
        || q.mission().day() != day()
        || System.nanoTime() - q.issued() > 30_000_000_000L)
      throw new IllegalStateException(
          "Dispatch review expired or reward state changed. Review again.");
    if (store.flightAward(p.getUUID(), day()).isPresent())
      throw new IllegalStateException("Today's dispatch reward is already saved.");
    if (q.action().equals("flight:start")) {
      AircraftSales.require(p);
      if (!CommerceService.counterProject(p).id().equals(q.mission().origin())
          || !headquarters(p.server).id().equals(q.mission().target()))
        throw new IllegalStateException("Dispatch buildings changed; review again.");
      book(p.server).put(q.mission());
      checkpoint(p.server);
      SAMPLES.remove(p);
      return "Royal Air Dispatch accepted. Board a registered powered aircraft at the airfield; fly"
                 + " the distance and land near headquarters before reporting to its Steward.";
    }
    var m = book(p.server).get(p.getUUID()).orElseThrow();
    if (!m.equals(q.mission()) || !m.verified())
      throw new IllegalStateException("Flight evidence changed; review the report again.");
    requireTarget(p, m);
    arrivalFrame(p, m);
    checkpoint(p.server);
    store.awardFlight(
        m.id(),
        p.getUUID(),
        m.day(),
        m.reward(),
        m.budget(),
        m.registration(),
        m.generation(),
        m.minimum(),
        (long) Math.floor(m.meters()),
        m.samples(),
        m.origin(),
        m.target());
    book(p.server).put(m.posted());
    checkpoint(p.server);
    com.mojang.logging.LogUtils.getLogger()
        .info(
            "ROYAL_AIR_DISPATCH operation={} pilot={} aircraft={} meters={} reward={}",
            m.id(),
            p.getUUID(),
            m.registration(),
            m.meters(),
            m.reward());
    return "Dispatch reported once. Claim "
        + m.reward()
        + " coins from your private Mailbox; today's recognition and shared budget are saved.";
  }

  public static List<CommerceView.Entry> entries(ServerPlayer p) {
    if (!AircraftAccess.available()) return List.of();
    var store = CommerceService.store(p.server);
    boolean airport = AircraftSales.atCounter(p);
    var target = ConstructionService.lodge(p.server);
    var selected = CommerceService.counterProject(p);
    boolean headquarters =
        target != null && (selected == null || selected.id().equals(target.id()));
    if (!airport && !headquarters) return List.of();
    if (!airport && !BOOKS.containsKey(p.server) && !ERRORS.containsKey(p.server)) return List.of();
    if (store.flightAward(p.getUUID(), day()).isPresent())
      return List.of(
          new CommerceView.Entry(
              "flight:done",
              "Royal Air Dispatch",
              "Today's reward is already saved. Claim any unclaimed coins from Mailbox. Resets at"
                  + " 00:00 UTC.",
              0,
              "FLIGHT_DONE"));
    var rows = new ArrayList<CommerceView.Entry>();
    FlightBook.Mission m;
    try { m = book(p.server).get(p.getUUID()).filter(v -> v.day() == day()).orElse(null); }
    catch (IllegalStateException blocked) { return List.of(new CommerceView.Entry("flight:blocked", "Air dispatch paused", "Flight evidence needs matching-data recovery by an administrator. Other services and existing Mailbox deliveries remain available.", 0, "LOCKED")); }

    if (m != null) {
      rows.add(
          new CommerceView.Entry(
              "flight:status",
              "Royal Air Dispatch",
              (long) Math.floor(m.meters())
                  + " / "
                  + m.minimum()
                  + " airborne blocks · "
                  + (m.arrived()
                      ? "Landing recorded. Report to the Steward."
                      : "Fly the original registered aircraft; land near headquarters.")
                  + " Reward in Mailbox; no profession XP.",
              m.reward(),
              "FLIGHT_ACTIVE"));
      if (headquarters && m.arrived())
        rows.add(
            new CommerceView.Entry(
                "flight:finish",
                "Report your dispatch",
                "Visit the headquarters Steward with the original aircraft parked nearby. Current"
                    + " permissions and saved flight evidence are checked before payment.",
                m.reward(),
                "FLIGHT_FINISH"));
    }
    if (airport) {
      var rules = FlightRules.get(p.server);
      long budget = store.flightBudget(day()).orElse(rules.dailyBudget());
      rows.add(
          new CommerceView.Entry(
              "flight:start",
              m == null ? "Royal Air Dispatch" : "Restart unfinished dispatch",
              "Fly a registered powered aircraft from this airfield, cover "
                  + rules.minimumBlocks()
                  + " airborne blocks; land near headquarters. Pool "
                  + store.flightSpent(day())
                  + "/"
                  + budget
                  + ". One reward per pilot/UTC day. Restart clears progress.",
              rules.reward(),
              store.flightSpent(day()) > budget - rules.reward() ? "LOCKED" : "FLIGHT_ACCEPT"));
    }
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
                  "Teleportation interrupted your unfinished air dispatch. Restart at the"
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
          "Air dispatch evidence requires recovery. Check Mailbox before retrying; other services"
              + " remain available.",
          error);
    }
  }

  private static void disable(MinecraftServer server, Exception error) {
    ERRORS.put(
        server,
        "Air dispatch records require administrator recovery; no dispatch rewards are enabled.");
    com.mojang.logging.LogUtils.getLogger().error("Air dispatch evidence paused", error);
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
    var awarded = store.flightAward(p.getUUID(), m.day());
    if (awarded.isPresent()) {
      if (!awarded.orElseThrow().id().equals(m.id()))
        throw new IllegalStateException("Dispatch identity differs from the journal");
      if (m.verified()) {
        b.put(m.posted());
        b.checkpoint();
      }
      return;
    }
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
    var previous = SAMPLES.get(p);
    Vec3 now = plane.position();
    if (previous != null && now.distanceToSqr(previous.position()) > 32 * 32) {
      b.put(m.reset());
      SAMPLES.remove(p);
      p.displayClientMessage(
          Component.literal(
              "A travel discontinuity interrupted the dispatch. Restart at the airfield."),
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
                "Royal Air Dispatch ready. Dismount and report to the headquarters Steward."),
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
              "Royal Air Dispatch: "
                  + (long) Math.floor(m.meters())
                  + " / "
                  + m.minimum()
                  + " airborne blocks · Land near headquarters"),
          true);
  }
}
