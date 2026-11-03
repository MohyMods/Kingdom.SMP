package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

/**
 * Saved travel evidence only. The economy journal alone authorizes a finite once-per-day reward.
 */
public final class FlightBook {
  public record Mission(
      UUID id,
      UUID pilot,
      long day,
      UUID origin,
      UUID target,
      int minimum,
      long reward,
      long budget,
      UUID registration,
      UUID entity,
      long generation,
      double meters,
      int samples,
      boolean arrived,
      boolean verified,
      boolean rewarded) {
    public Mission {
      Objects.requireNonNull(id);
      Objects.requireNonNull(pilot);
      Objects.requireNonNull(origin);
      Objects.requireNonNull(target);
      new FlightRules(minimum, reward, budget);
      if (origin.equals(target)
          || day < 0
          || !Double.isFinite(meters)
          || meters < 0
          || meters > 1_000_000
          || samples < 0
          || samples > 2_000_000
          || registration == null
              && (entity != null || generation != -1 || meters != 0 || samples != 0)
          || registration != null && (entity == null || generation < 0)
          || arrived && (registration == null || meters < minimum || samples < 8)
          || verified && !arrived
          || rewarded && !verified)
        throw new IllegalArgumentException("Invalid saved dispatch evidence");
    }

    public Mission assign(UUID registration, UUID entity, long generation) {
      if (this.registration != null) return this;
      return new Mission(
          id,
          pilot,
          day,
          origin,
          target,
          minimum,
          reward,
          budget,
          registration,
          entity,
          generation,
          0,
          0,
          false,
          false,
          false);
    }

    public Mission reset() {
      return new Mission(
          id, pilot, day, origin, target, minimum, reward, budget, null, null, -1, 0, 0, false,
          false, false);
    }

    public Mission fly(UUID registration, UUID entity, long generation, double increment) {
      if (verified || rewarded) return this;
      if (!Double.isFinite(increment) || increment < 0 || increment > 32)
        throw new IllegalArgumentException("Invalid server flight step");
      if (this.registration != null
          && (!this.registration.equals(registration)
              || !this.entity.equals(entity)
              || this.generation != generation))
        throw new IllegalStateException("Dispatch aircraft changed; restart the dispatch");
      return new Mission(
          id,
          pilot,
          day,
          origin,
          target,
          minimum,
          reward,
          budget,
          registration,
          entity,
          generation,
          Math.min(1_000_000, meters + increment),
          Math.min(2_000_000, samples + 1),
          false,
          false,
          false);
    }

    public Mission arrive() {
      return new Mission(
          id,
          pilot,
          day,
          origin,
          target,
          minimum,
          reward,
          budget,
          registration,
          entity,
          generation,
          meters,
          samples,
          true,
          false,
          false);
    }

    public Mission verify() {
      return new Mission(
          id,
          pilot,
          day,
          origin,
          target,
          minimum,
          reward,
          budget,
          registration,
          entity,
          generation,
          meters,
          samples,
          arrived,
          true,
          false);
    }

    public Mission posted() {
      return new Mission(
          id,
          pilot,
          day,
          origin,
          target,
          minimum,
          reward,
          budget,
          registration,
          entity,
          generation,
          meters,
          samples,
          arrived,
          true,
          true);
    }
  }

  private final Path file;
  private final String purpose;
  private final UUID economy, identity;
  private final Map<UUID, Mission> missions = new LinkedHashMap<>();
  private boolean dirty;

  private FlightBook(Path file, UUID economy, UUID identity, String purpose) {
    this.file = file;
    this.purpose = purpose;
    this.economy = economy;
    this.identity = identity;
  }

  public static UUID missionId(UUID economy, UUID pilot, long day) {
    return missionId(economy, pilot, day, "air_dispatch");
  }

  public static UUID cargoMissionId(UUID economy, UUID pilot, long day) {
    return missionId(economy, pilot, day, "air_cargo");
  }

  private static UUID missionId(UUID economy, UUID pilot, long day, String purpose) {
    if (day < 0) throw new IllegalArgumentException("Invalid UTC day");
    return UUID.nameUUIDFromBytes(
        ("kingdom_smp:" + purpose + "/" + economy + "/" + pilot + "/" + day)
            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  public Optional<Mission> get(UUID pilot) {
    return Optional.ofNullable(missions.get(pilot));
  }

  public List<Mission> all() {
    return List.copyOf(missions.values());
  }

  public void put(Mission mission) {
    if (!mission.id().equals(missionId(economy, mission.pilot(), mission.day(), purpose))
        || !missions.containsKey(mission.pilot()) && missions.size() >= 128)
      throw new IllegalArgumentException("Invalid dispatch identity or player limit");
    missions.put(mission.pilot(), mission);
    dirty = true;
  }

  public static FlightBook open(Path directory, UUID economy) throws IOException {
    return open(directory, economy, "air_dispatch");
  }

  public static FlightBook openCargo(Path directory, UUID economy) throws IOException {
    return open(directory, economy, "air_cargo");
  }

  private static FlightBook open(Path directory, UUID economy, String purpose) throws IOException {
    var file = directory.resolve("kingdomsmp_" + purpose + ".dat");
    var seal = directory.resolve("kingdomsmp_" + purpose + ".created");
    boolean exists = Files.exists(file, LinkOption.NOFOLLOW_LINKS),
        marked = Files.exists(seal, LinkOption.NOFOLLOW_LINKS);
    if (exists != marked)
      throw new IOException("Dispatch evidence/identity mismatch; restore matching data");
    if (!exists) {
      var book = new FlightBook(file, economy, UUID.randomUUID(), purpose);
      var marker = new CompoundTag();
      marker.putUUID("economy", economy);
      marker.putUUID("identity", book.identity);
      AtomicNbt.write(seal, marker);
      book.dirty = true;
      book.checkpoint();
      return book;
    }
    var marker = AtomicNbt.read(seal);
    var data = AtomicNbt.read(file);
    if (!marker.getAllKeys().equals(Set.of("economy", "identity"))
        || !marker.hasUUID("economy")
        || !marker.hasUUID("identity")
        || !marker.getUUID("economy").equals(economy)
        || !data.getAllKeys().equals(Set.of("schema", "economy", "identity", "missions"))
        || !data.contains("schema", Tag.TAG_INT)
        || data.getInt("schema") != 1
        || !data.hasUUID("economy")
        || !data.getUUID("economy").equals(economy)
        || !data.hasUUID("identity")
        || !data.getUUID("identity").equals(marker.getUUID("identity"))
        || !(data.get("missions") instanceof ListTag rows)
        || rows.size() > 128
        || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND)
      throw new IOException("Malformed or foreign air dispatch evidence");
    var book = new FlightBook(file, economy, data.getUUID("identity"), purpose);
    try {
      for (var value : rows) {
        var mission = decode((CompoundTag) value);
        if (book.missions.containsKey(mission.pilot()))
          throw new IllegalArgumentException("Duplicate dispatch pilot");
        book.put(mission);
      }
      book.dirty = false;
      return book;
    } catch (RuntimeException e) {
      throw new IOException("Invalid saved dispatch mission", e);
    }
  }

  public void checkpoint() throws IOException {
    if (!dirty) return;
    var data = new CompoundTag();
    data.putInt("schema", 1);
    data.putUUID("economy", economy);
    data.putUUID("identity", identity);
    var rows = new ListTag();
    missions.values().stream()
        .sorted(Comparator.comparing(m -> m.pilot().toString()))
        .forEach(m -> rows.add(encode(m)));
    data.put("missions", rows);
    AtomicNbt.write(file, data);
    if (!AtomicNbt.read(file).equals(data))
      throw new IOException("Dispatch evidence checkpoint failed verification");
    dirty = false;
  }

  private static CompoundTag encode(Mission m) {
    var t = new CompoundTag();
    t.putUUID("id", m.id());
    t.putUUID("pilot", m.pilot());
    t.putLong("day", m.day());
    t.putUUID("origin", m.origin());
    t.putUUID("target", m.target());
    t.putInt("minimum", m.minimum());
    t.putLong("reward", m.reward());
    t.putLong("budget", m.budget());
    if (m.registration() != null) {
      t.putUUID("registration", m.registration());
      t.putUUID("entity", m.entity());
    }
    t.putLong("generation", m.generation());
    t.putDouble("meters", m.meters());
    t.putInt("samples", m.samples());
    t.putBoolean("arrived", m.arrived());
    t.putBoolean("verified", m.verified());
    t.putBoolean("rewarded", m.rewarded());
    return t;
  }

  private static Mission decode(CompoundTag t) {
    var fields =
        new HashSet<>(
            Set.of(
                "id",
                "pilot",
                "day",
                "origin",
                "target",
                "minimum",
                "reward",
                "budget",
                "generation",
                "meters",
                "samples",
                "arrived",
                "verified",
                "rewarded"));
    if (t.contains("registration")) {
      fields.add("registration");
      fields.add("entity");
      if (!t.hasUUID("registration") || !t.hasUUID("entity"))
        throw new IllegalArgumentException("Invalid dispatch aircraft identity");
    }
    if (!t.getAllKeys().equals(fields))
      throw new IllegalArgumentException("Unknown dispatch evidence fields");
    for (String k : List.of("id", "pilot", "origin", "target"))
      if (!t.hasUUID(k)) throw new IllegalArgumentException("Missing dispatch UUID");
    for (String k : List.of("day", "reward", "budget", "generation"))
      if (!t.contains(k, Tag.TAG_LONG))
        throw new IllegalArgumentException("Invalid dispatch integer");
    for (String k : List.of("minimum", "samples"))
      if (!t.contains(k, Tag.TAG_INT)) throw new IllegalArgumentException("Invalid dispatch count");
    if (!t.contains("meters", Tag.TAG_DOUBLE)
        || !t.contains("arrived", Tag.TAG_BYTE)
        || !t.contains("verified", Tag.TAG_BYTE)
        || !t.contains("rewarded", Tag.TAG_BYTE))
      throw new IllegalArgumentException("Invalid travel proof");
    return new Mission(
        t.getUUID("id"),
        t.getUUID("pilot"),
        t.getLong("day"),
        t.getUUID("origin"),
        t.getUUID("target"),
        t.getInt("minimum"),
        t.getLong("reward"),
        t.getLong("budget"),
        t.hasUUID("registration") ? t.getUUID("registration") : null,
        t.hasUUID("entity") ? t.getUUID("entity") : null,
        t.getLong("generation"),
        t.getDouble("meters"),
        t.getInt("samples"),
        t.getBoolean("arrived"),
        t.getBoolean("verified"),
        t.getBoolean("rewarded"));
  }
}
