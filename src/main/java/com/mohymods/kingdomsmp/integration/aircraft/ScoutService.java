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
 * Shared reconnaissance from genuine server-observed powered flight. No client travel proof, item
 * grant or profession XP.
 */
public final class ScoutService {
  public record Quote(
      UUID nonce,
      long revision,
      String action,
      ScoutBook.Survey survey,
      long issued,
      String text) {}

  private record Sample(
      UUID entity, Vec3 position, boolean qualified, boolean leg, int controlledTick) {}

  private static final Map<MinecraftServer, ScoutBook> BOOKS = new WeakHashMap<>();
  private static final Map<MinecraftServer, String> ERRORS = new WeakHashMap<>();
  private static final Map<ServerPlayer, Quote> QUOTES = new WeakHashMap<>();
  private static final Map<ServerPlayer, Sample> SAMPLES = new WeakHashMap<>();
  private static Method fuel, engine;

  private ScoutService() {}

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
    var path = server.getWorldPath(LevelResource.ROOT).resolve("data/kingdomsmp_air_scout.dat");
    if (java.nio.file.Files.exists(path, java.nio.file.LinkOption.NOFOLLOW_LINKS))
      try {
        book(server);
      } catch (IllegalStateException blocked) {
        com.mojang.logging.LogUtils.getLogger()
            .error("Reconnaissance evidence needs recovery; other services remain active", blocked);
      }
  }

  public static List<ScoutBook.Survey> savedMissions(MinecraftServer server) {
    var b = BOOKS.get(server);
    return b == null ? List.of() : b.all();
  }

  public static String status(MinecraftServer server) {
    return ERRORS.getOrDefault(
        server,
        BOOKS.containsKey(server)
            ? "Saved reconnaissance evidence active"
            : "No reconnaissance accepted yet");
  }

  public static ScoutBook book(MinecraftServer server) {
    if (ERRORS.containsKey(server)) throw new IllegalStateException(ERRORS.get(server));
    return BOOKS.computeIfAbsent(
        server,
        s -> {
          try {
            return ScoutBook.open(
                s.getWorldPath(LevelResource.ROOT).resolve("data"), CommerceService.store(s).id());
          } catch (IOException e) {
            ERRORS.put(
                s, "Reconnaissance evidence needs matching data recovery. Ask an administrator.");
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
            || q.survey().flight().day() != day()
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
    requireTarget(p, m);
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

  private static List<ScoutState.Region> eligible(ScoutBook.Survey survey, EconomyStore store) {
    return survey.observations().stream()
        .filter(r -> !store.surveyed(r.key()))
        .limit(survey.required())
        .toList();
  }

  public static void review(ServerPlayer p, String action) {
    close(p);
    FlightService.close(p);
    CargoService.close(p);
    com.mohymods.kingdomsmp.profession.ContractService.close(p);
    com.mohymods.kingdomsmp.fishing.FishingContractService.close(p);
    com.mohymods.kingdomsmp.bounty.CommunityContractService.close(p);
    com.mohymods.kingdomsmp.bounty.HuntService.close(p);
    var rules = ScoutRules.get(p.server);
    rules.requireEnabled();
    var store = CommerceService.store(p.server);
    if (store.scoutAward(p.getUUID(), day()).isPresent())
      throw new IllegalStateException("Today's reconnaissance reward is already saved");
    ScoutBook.Survey survey;
    String text;
    if (action.equals("scout:start")) {
      AircraftSales.require(p);
      var origin = CommerceService.counterProject(p);
      var target = headquarters(p.server);
      long budget = store.scoutBudget(day()).orElse(rules.budget());
      if (store.scoutSpent(day()) > budget - rules.reward())
        throw new IllegalStateException("Today's shared reconnaissance reward pool is exhausted");
      var home = post(origin);
      var m =
          new FlightBook.Mission(
              FlightBook.scoutMissionId(store.id(), p.getUUID(), day()),
              p.getUUID(),
              day(),
              origin.id(),
              target.id(),
              rules.airborne(),
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
      survey =
          new ScoutBook.Survey(
              m, rules.regions(), rules.distance(), home.getX(), home.getZ(), List.of());
      text =
          "Accept Royal reconnaissance for "
              + m.reward()
              + " coins in private Mailbox. Depart this airfield in a registered powered aircraft"
              + " and record "
              + survey.required()
              + " regions (16 by16blocks) not yet recognized by the kingdom, at least "
              + survey.distance()
              + " blocks from the airfield. Fly at least "
              + m.minimum()
              + " airborne blocks, land near headquarters and report to its Steward. Returning to"
              + " already recognized regions does not earn another report. One reward per"
              + " pilot/UTCday; no profession XP or unlock. Restart discards unfinished survey"
              + " proof.";
    } else if (action.equals("scout:finish")) {
      survey =
          book(p.server)
              .get(p.getUUID())
              .orElseThrow(() -> new IllegalStateException("Accept reconnaissance at Corin first"));
      var m = survey.flight();
      if (m.day() != day())
        throw new IllegalStateException(
            "Survey expired at UTC midnight; accept a new one at Corin");
      var regions = eligible(survey, store);
      if (regions.size() < survey.required()) {
        if (m.arrived()) {
          book(p.server).put(survey.reset());
          checkpoint(p.server);
        }
        throw new IllegalStateException(
            "More unrecognized regions are required. Another pilot may have reported first; restart"
                + " from Corin if this landing was already recorded.");
      }
      arrivalFrame(p, m);
      survey = survey.move(m.verify());
      book(p.server).put(survey);
      checkpoint(p.server);
      text =
          "Report "
              + survey.required()
              + " new kingdom regions and "
              + (long) m.meters()
              + " recorded airborne blocks for exactly "
              + m.reward()
              + " coins in Mailbox. First accepted reports recognize each region once for the"
              + " kingdom. Current aircraft, permissions, unknown regions and shared budget are"
              + " checked again. No items are consumed.";
    } else throw new IllegalArgumentException("Choose an available reconnaissance action");
    QUOTES.put(
        p, new Quote(UUID.randomUUID(), store.revision(), action, survey, System.nanoTime(), text));
  }

  public static String confirm(ServerPlayer p, UUID nonce) throws IOException {
    var q = QUOTES.remove(p);
    var store = CommerceService.store(p.server);
    CommerceService.verifyReceipt(p, store);
    ScoutRules.get(p.server).requireEnabled();
    if (q == null
        || !q.nonce().equals(nonce)
        || q.revision() != store.revision()
        || q.survey().flight().day() != day()
        || System.nanoTime() - q.issued() > 30_000_000_000L)
      throw new IllegalStateException(
          "Reconnaissance review expired or state changed; review again");
    if (store.scoutAward(p.getUUID(), day()).isPresent())
      throw new IllegalStateException("Today's reconnaissance reward is already saved");
    var survey = q.survey();
    var m = survey.flight();
    if (q.action().equals("scout:start")) {
      AircraftSales.require(p);
      if (!CommerceService.counterProject(p).id().equals(m.origin())
          || !headquarters(p.server).id().equals(m.target()))
        throw new IllegalStateException("Survey buildings changed");
      book(p.server).put(survey);
      checkpoint(p.server);
      SAMPLES.remove(p);
      return "Reconnaissance accepted. Fly from the airfield to record unrecognized regions; land"
          + " near headquarters and report to its Steward.";
    }
    if (!book(p.server).get(p.getUUID()).orElseThrow().equals(survey) || !m.verified())
      throw new IllegalStateException("Saved survey proof changed");
    arrivalFrame(p, m);
    var regions = eligible(survey, store);
    if (regions.size() != survey.required())
      throw new IllegalStateException(
          "Another accepted report changed these regions; review again");
    checkpoint(p.server);
    store.awardScout(
        m.id(),
        p.getUUID(),
        m.day(),
        m.reward(),
        m.budget(),
        m.registration(),
        m.generation(),
        survey.required(),
        survey.distance(),
        m.minimum(),
        (long) Math.floor(m.meters()),
        m.samples(),
        survey.originX(),
        survey.originZ(),
        regions);
    book(p.server).put(survey.move(m.posted()));
    checkpoint(p.server);
    p.server
        .getPlayerList()
        .broadcastSystemMessage(
            Component.literal(
                "Royal reconnaissance: "
                    + p.getGameProfile().getName()
                    + " recorded "
                    + survey.required()
                    + " new kingdom regions."),
            false);
    return "Reconnaissance reported once. Claim "
        + m.reward()
        + " coins from private Mailbox; the kingdom's region recognition is saved.";
  }

  public static List<CommerceView.Entry> entries(ServerPlayer p) {
    if (!AircraftAccess.available()) return List.of();
    boolean airport = AircraftSales.atCounter(p);
    var target = ConstructionService.lodge(p.server);
    var selected = CommerceService.counterProject(p);
    boolean headquarters =
        target != null && (selected == null || selected.id().equals(target.id()));
    if (!airport && !headquarters) return List.of();
    var rules = ScoutRules.get(p.server);
    if (!rules.enabled())
      return airport
          ? List.of(
              new CommerceView.Entry(
                  "scout:disabled",
                  "Royal reconnaissance",
                  "Reconnaissance is disabled by the server configuration.",
                  0,
                  "LOCKED"))
          : List.of();
    var store = CommerceService.store(p.server);
    if (store.scoutAward(p.getUUID(), day()).isPresent())
      return List.of(
          new CommerceView.Entry(
              "scout:done",
              "Royal reconnaissance",
              "Today's report is already recognized. Claim unclaimed coins in Mailbox; return"
                  + " after00:00UTC for a new survey.",
              0,
              "SCOUT_DONE"));
    ScoutBook.Survey survey = null;
    try {
      if (BOOKS.containsKey(p.server))
        survey =
            book(p.server).get(p.getUUID()).filter(s -> s.flight().day() == day()).orElse(null);
    } catch (IllegalStateException blocked) {
      return List.of(
          new CommerceView.Entry(
              "scout:blocked",
              "Reconnaissance paused",
              "Survey evidence requires matching-data recovery. Existing mail and other services"
                  + " remain available.",
              0,
              "LOCKED"));
    }
    var rows = new ArrayList<CommerceView.Entry>();
    if (survey != null) {
      var m = survey.flight();
      rows.add(
          new CommerceView.Entry(
              "scout:status",
              "Royal reconnaissance",
              eligible(survey, store).size()
                  + " / "
                  + survey.required()
                  + " unrecognized regions · "
                  + (long) m.meters()
                  + " / "
                  + m.minimum()
                  + " airborne blocks. "
                  + (m.arrived()
                      ? "Landing recorded; report to the Steward."
                      : "Explore beyond the airfield, then land near headquarters."),
              m.reward(),
              "SCOUT_ACTIVE"));
      if (headquarters && m.arrived())
        rows.add(
            new CommerceView.Entry(
                "scout:finish",
                "Report reconnaissance",
                "The first accepted report recognizes these regions for the kingdom. Current"
                    + " permissions, recorded flight and finite reward pool apply.",
                m.reward(),
                "SCOUT_FINISH"));
    }
    if (airport)
      rows.add(
          new CommerceView.Entry(
              "scout:start",
              survey == null ? "Royal reconnaissance" : "Restart reconnaissance",
              "Record "
                  + rules.regions()
                  + " new regions at least "
                  + rules.distance()
                  + " blocks from this airfield during powered flight. Kingdom regions recognized: "
                  + store.surveyedRegions()
                  + ". No items or profession XP.",
              rules.reward(),
              "SCOUT_ACCEPT"));
    return List.copyOf(rows);
  }

  public static void interruptTeleport(Entity entity) {
    var server = entity.getServer();
    if (server == null || !server.isSameThread()) return;
    var book = BOOKS.get(server);
    if (book == null) return;
    for (var survey : book.all()) {
      var m = survey.flight();
      if (m.registration() != null
          && !m.arrived()
          && (m.pilot().equals(entity.getUUID()) || entity.getUUID().equals(m.entity()))) {
        book.put(survey.reset());
        var p = server.getPlayerList().getPlayer(m.pilot());
        if (p != null) {
          SAMPLES.remove(p);
          p.displayClientMessage(
              Component.literal(
                  "Teleportation interrupted reconnaissance. Restart from the airfield."),
              true);
        }
      }
    }
  }

  private static void checkpoint(MinecraftServer server) {
    try {
      book(server).checkpoint();
    } catch (IOException error) {
      disable(server, error);
      throw new IllegalStateException(
          "Reconnaissance evidence requires recovery. Check Mailbox before retrying; other services"
              + " remain available.",
          error);
    }
  }

  private static void disable(MinecraftServer server, Exception error) {
    ERRORS.put(
        server,
        "Reconnaissance records require administrator recovery; no reconnaissance rewards are"
            + " enabled.");
    com.mojang.logging.LogUtils.getLogger().error("Reconnaissance evidence paused", error);
  }

  public static void tick(MinecraftServer server) {
    var b = BOOKS.get(server);
    var store = CommerceService.initializedStore(server).orElse(null);
    if (b == null || store == null || store.quarantined() || ERRORS.containsKey(server)) return;
    try {
      if (!ScoutRules.get(server).enabled()) return;
    } catch (RuntimeException error) {
      disable(server, error);
      return;
    }

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

  private static void observe(ServerPlayer p, ScoutBook book)
      throws IOException, ReflectiveOperationException {
    var survey = book.get(p.getUUID()).orElse(null);
    if (survey == null) return;
    var m = survey.flight();
    if (m.day() != day() || m.rewarded()) return;
    var store = CommerceService.store(p.server);
    var award = store.scoutAward(p.getUUID(), m.day());
    if (award.isPresent()) {
      if (!award.orElseThrow().id().equals(m.id()))
        throw new IllegalStateException("Survey identity differs from canonical recognition");
      if (m.verified()) {
        book.put(survey.move(m.posted()));
        book.checkpoint();
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
      survey = survey.move(m);
      book.put(survey);
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
    var now = plane.position();
    if (previous != null && now.distanceToSqr(previous.position()) > 32 * 32) {
      book.put(survey.reset());
      SAMPLES.remove(p);
      p.displayClientMessage(
          Component.literal(
              "A travel discontinuity reset reconnaissance. Restart from the airfield."),
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
      survey = survey.move(m);
      var chunk = plane.chunkPosition();
      var biome = p.serverLevel().getBiome(plane.blockPosition()).unwrapKey();
      if (biome.isPresent()) {
        var region =
            new ScoutState.Region(chunk.x, chunk.z, biome.orElseThrow().location().toString());
        if (region.distanceSquared(survey.originX(), survey.originZ())
                >= (long) survey.distance() * survey.distance()
            && !store.surveyed(region.key())
            && survey.observations().size() < 128) survey = survey.observe(region);
      }
      book.put(survey);
      leg = true;
    }
    if (plane.onGround() && leg) {
      var target = ConstructionService.store(p.server).get(m.target());
      if (m.meters() >= m.minimum()
          && m.samples() >= 8
          && eligible(survey, store).size() >= survey.required()
          && now.distanceToSqr(Vec3.atCenterOf(post(target))) <= 48 * 48) {
        m = m.arrive();
        survey = survey.move(m);
        book.put(survey);
        book.checkpoint();
        p.displayClientMessage(
            Component.literal(
                "Reconnaissance ready. Dismount and report to the headquarters Steward."),
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
              "Reconnaissance: "
                  + eligible(survey, store).size()
                  + " / "
                  + survey.required()
                  + " new regions · "
                  + (long) m.meters()
                  + " / "
                  + m.minimum()
                  + " airborne blocks"),
          true);
  }
}
