package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.nbt.*;

/** Saved server observations; only the economy journal recognizes new regions or authorizes pay. */
public final class ScoutBook {
  public record Survey(
      FlightBook.Mission flight,
      int required,
      int distance,
      int originX,
      int originZ,
      List<ScoutState.Region> observations) {
    public Survey {
      Objects.requireNonNull(flight);
      new ScoutRules(false, required, distance, flight.minimum(), flight.reward(), flight.budget());
      observations = List.copyOf(observations);
      if (Math.abs((long) originX) > 30000000
          || Math.abs((long) originZ) > 30000000
          || observations.size() > 128
          || observations.stream().map(ScoutState.Region::key).distinct().count()
              != observations.size()
          || observations.stream()
              .anyMatch(r -> r.distanceSquared(originX, originZ) < (long) distance * distance)
          || flight.registration() == null && !observations.isEmpty())
        throw new IllegalArgumentException("Invalid saved reconnaissance evidence");
    }

    public Survey move(FlightBook.Mission changed) {
      return new Survey(
          changed,
          required,
          distance,
          originX,
          originZ,
          changed.registration() == null ? List.of() : observations);
    }

    public Survey reset() {
      return move(flight.reset());
    }

    public Survey observe(ScoutState.Region region) {
      if (observations.stream().anyMatch(r -> r.key() == region.key())) return this;
      if (observations.size() >= 128)
        throw new IllegalStateException(
            "Survey evidence is full; restart from Corin to survey different regions");
      var next = new ArrayList<>(observations);
      next.add(region);
      return new Survey(flight, required, distance, originX, originZ, next);
    }
  }

  private final Path file;
  private final UUID economy, identity;
  private final Map<UUID, Survey> records = new LinkedHashMap<>();
  private boolean dirty;

  private ScoutBook(Path file, UUID economy, UUID identity) {
    this.file = file;
    this.economy = economy;
    this.identity = identity;
  }

  public Optional<Survey> get(UUID pilot) {
    return Optional.ofNullable(records.get(pilot));
  }

  public List<Survey> all() {
    return List.copyOf(records.values());
  }

  public void put(Survey survey) {
    var m = survey.flight();
    if (!m.id().equals(FlightBook.scoutMissionId(economy, m.pilot(), m.day()))
        || !records.containsKey(m.pilot()) && records.size() >= 128)
      throw new IllegalArgumentException("Invalid survey identity/player limit");
    records.put(m.pilot(), survey);
    dirty = true;
  }

  public static ScoutBook open(Path directory, UUID economy) throws IOException {
    var file = directory.resolve("kingdomsmp_air_scout.dat");
    var marker = directory.resolve("kingdomsmp_air_scout.created");
    boolean exists = Files.exists(file, LinkOption.NOFOLLOW_LINKS),
        marked = Files.exists(marker, LinkOption.NOFOLLOW_LINKS);
    if (exists != marked)
      throw new IOException("Scouting evidence/identity mismatch; restore matching data");
    if (!exists) {
      var book = new ScoutBook(file, economy, UUID.randomUUID());
      var seal = new CompoundTag();
      seal.putUUID("economy", economy);
      seal.putUUID("identity", book.identity);
      AtomicNbt.write(marker, seal);
      book.dirty = true;
      book.checkpoint();
      return book;
    }
    var seal = AtomicNbt.read(marker);
    var d = AtomicNbt.read(file);
    if (!seal.getAllKeys().equals(Set.of("economy", "identity"))
        || !seal.hasUUID("economy")
        || !seal.hasUUID("identity")
        || !seal.getUUID("economy").equals(economy)
        || !d.getAllKeys().equals(Set.of("schema", "economy", "identity", "surveys"))
        || !d.contains("schema", Tag.TAG_INT)
        || d.getInt("schema") != 1
        || !d.hasUUID("economy")
        || !d.getUUID("economy").equals(economy)
        || !d.hasUUID("identity")
        || !d.getUUID("identity").equals(seal.getUUID("identity"))
        || !(d.get("surveys") instanceof ListTag rows)
        || rows.size() > 128
        || !rows.isEmpty() && rows.getElementType() != Tag.TAG_COMPOUND)
      throw new IOException("Malformed or foreign scouting evidence");
    var book = new ScoutBook(file, economy, d.getUUID("identity"));
    try {
      for (var value : rows) {
        var row = (CompoundTag) value;
        if (!row.getAllKeys()
                .equals(
                    Set.of("flight", "required", "distance", "originX", "originZ", "observations"))
            || !row.contains("flight", Tag.TAG_COMPOUND))
          throw new IllegalArgumentException("Invalid survey fields");
        for (String key : List.of("required", "distance", "originX", "originZ"))
          if (!row.contains(key, Tag.TAG_INT))
            throw new IllegalArgumentException("Invalid survey integer");
        var survey =
            new Survey(
                FlightBook.decode(row.getCompound("flight")),
                row.getInt("required"),
                row.getInt("distance"),
                row.getInt("originX"),
                row.getInt("originZ"),
                ScoutData.decode(row.get("observations")));
        if (book.records.containsKey(survey.flight().pilot()))
          throw new IllegalArgumentException("Duplicate survey pilot");
        book.put(survey);
      }
      book.dirty = false;
      return book;
    } catch (RuntimeException e) {
      throw new IOException("Invalid saved scouting observations", e);
    }
  }

  public void checkpoint() throws IOException {
    if (!dirty) return;
    var d = new CompoundTag();
    d.putInt("schema", 1);
    d.putUUID("economy", economy);
    d.putUUID("identity", identity);
    var rows = new ListTag();
    for (var survey :
        records.values().stream()
            .sorted(Comparator.comparing(s -> s.flight().pilot().toString()))
            .toList()) {
      var row = new CompoundTag();
      row.put("flight", FlightBook.encode(survey.flight()));
      row.putInt("required", survey.required());
      row.putInt("distance", survey.distance());
      row.putInt("originX", survey.originX());
      row.putInt("originZ", survey.originZ());
      row.put("observations", ScoutData.encode(survey.observations()));
      rows.add(row);
    }
    d.put("surveys", rows);
    AtomicNbt.write(file, d);
    if (!AtomicNbt.read(file).equals(d))
      throw new IOException("Scouting evidence checkpoint failed verification");
    dirty = false;
  }
}
