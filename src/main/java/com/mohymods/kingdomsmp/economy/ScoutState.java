package com.mohymods.kingdomsmp.economy;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Shared reconnaissance recognition rebuilt only from the canonical financial journal. */
public final class ScoutState {
  public record Region(int x, int z, String biome) {
    public Region {
      if (Math.abs((long) x) > 1875000
          || Math.abs((long) z) > 1875000
          || biome == null
          || biome.length() > 128
          || ResourceLocation.tryParse(biome) == null)
        throw new IllegalArgumentException("Invalid surveyed Overworld region");
    }

    public long key() {
      return (x & 0xffffffffL) | ((long) z << 32);
    }

    public long distanceSquared(int originX, int originZ) {
      long dx = (long) x * 16 + 8 - originX, dz = (long) z * 16 + 8 - originZ;
      return dx * dx + dz * dz;
    }
  }

  public record Recognition(UUID operation, UUID pilot, long day, Region region) {}

  public record Award(
      UUID id,
      UUID pilot,
      long day,
      long reward,
      long budget,
      UUID registration,
      List<Region> regions) {
    public Award {
      regions = List.copyOf(regions);
    }
  }

  private record PilotDay(UUID pilot, long day) {}

  private final Map<Long, Recognition> known = new LinkedHashMap<>();
  private final Map<PilotDay, Award> awards = new HashMap<>();
  private final Map<Long, Long> spent = new HashMap<>(), budgets = new HashMap<>();

  ScoutState copy() {
    var next = new ScoutState();
    next.known.putAll(known);
    next.awards.putAll(awards);
    next.spent.putAll(spent);
    next.budgets.putAll(budgets);
    return next;
  }

  public boolean known(long key) {
    return known.containsKey(key);
  }

  public int count() {
    return known.size();
  }

  public Optional<Recognition> recognition(long key) {
    return Optional.ofNullable(known.get(key));
  }

  public Optional<Award> award(UUID pilot, long day) {
    return Optional.ofNullable(awards.get(new PilotDay(pilot, day)));
  }

  public long spent(long day) {
    return spent.getOrDefault(day, 0L);
  }

  public OptionalLong budget(long day) {
    var value = budgets.get(day);
    return value == null ? OptionalLong.empty() : OptionalLong.of(value);
  }

  void award(
      UUID id,
      UUID pilot,
      long day,
      long reward,
      long budget,
      AircraftState.Registration frame,
      long generation,
      int required,
      int distance,
      int minimum,
      long meters,
      int samples,
      int originX,
      int originZ,
      List<Region> regions) {
    regions = List.copyOf(regions);
    if (day < 0
        || award(pilot, day).isPresent()
        || frame.phase() != AircraftState.Phase.ACTIVE
        || frame.generation() != generation
        || !frame.permits(pilot)
        || required < 1
        || required > 32
        || distance < 32
        || distance > 1000000
        || minimum < 32
        || minimum > 100000
        || meters < minimum
        || meters > 1000000
        || samples < 8
        || samples > 2000000
        || Math.abs((long) originX) > 30000000
        || Math.abs((long) originZ) > 30000000
        || regions.size() != required
        || regions.stream().map(Region::key).distinct().count() != regions.size()
        || regions.stream()
            .anyMatch(
                r ->
                    known(r.key())
                        || r.distanceSquared(originX, originZ) < (long) distance * distance)
        || reward < 1
        || reward > 1000000000L
        || budget < reward
        || budget > 1000000000L
        || spent(day) > budget - reward
        || budgets.containsKey(day) && budgets.get(day) != budget
        || !budgets.isEmpty() && day < Collections.max(budgets.keySet()))
      throw new IllegalStateException(
          "Reconnaissance evidence, new regions, current aircraft or daily budget changed");
    for (var region : regions) known.put(region.key(), new Recognition(id, pilot, day, region));
    awards.put(
        new PilotDay(pilot, day), new Award(id, pilot, day, reward, budget, frame.id(), regions));
    spent.put(day, Math.addExact(spent(day), reward));
    budgets.putIfAbsent(day, budget);
  }
}
