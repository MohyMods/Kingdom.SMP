package com.mohymods.kingdomsmp.economy;

import java.util.*;
import net.minecraft.nbt.*;

/** Rebuilt only from the authenticated economy journal; items merely reference this authority. */
public final class AircraftState {
  public enum Phase {
    PACKED,
    STARTING,
    ACTIVE
  }

  public record Registration(
      UUID id,
      UUID owner,
      String type,
      long generation,
      Phase phase,
      Set<UUID> crew,
      CompoundTag item,
      CompoundTag deployment) {
    public Registration {
      Objects.requireNonNull(id);
      Objects.requireNonNull(owner);
      Objects.requireNonNull(phase);
      crew = Set.copyOf(crew);
      item = item.copy();
      deployment = deployment.copy();
    }

    @Override
    public CompoundTag item() {
      return item.copy();
    }

    @Override
    public CompoundTag deployment() {
      return deployment.copy();
    }

    public boolean permits(UUID actor) {
      return owner.equals(actor) || crew.contains(actor);
    }
  }

  public record Repair(
      UUID id,
      UUID registration,
      UUID actor,
      UUID entity,
      long generation,
      float health,
      long rate,
      long price) {}

  private final Map<UUID, Repair> repairs = new LinkedHashMap<>();
  private final Map<UUID, UUID> deliveredRepairs = new HashMap<>();

  public Optional<UUID> deliveredRepair(UUID registration) {
    return Optional.ofNullable(deliveredRepairs.get(registration));
  }

  public List<Repair> repairs() {
    return List.copyOf(repairs.values());
  }

  public Optional<Repair> repair(UUID registration) {
    return Optional.ofNullable(repairs.get(registration));
  }

  public Optional<Repair> repairTicket(UUID ticket) {
    return repairs.values().stream().filter(r -> r.id().equals(ticket)).findFirst();
  }

  public static long repairPrice(float health, long rate) {
    if (!Float.isFinite(health) || health <= 0 || health >= 1 || rate < 1 || rate > 1_000_000_000L)
      throw new IllegalArgumentException("Invalid damaged-airframe repair terms");
    return java.math.BigDecimal.ONE
        .subtract(new java.math.BigDecimal(Float.toString(health)))
        .multiply(java.math.BigDecimal.valueOf(rate))
        .setScale(0, java.math.RoundingMode.CEILING)
        .longValueExact();
  }

  void buyRepair(
      UUID id,
      UUID actor,
      UUID registration,
      UUID entity,
      long generation,
      float health,
      long rate,
      long price) {
    var record = required(registration);
    if (!record.owner().equals(actor)
        || record.phase() != Phase.ACTIVE
        || record.generation() != generation
        || !record.deployment().getCompound("entity").getUUID("UUID").equals(entity)
        || repairs.containsKey(registration)
        || cargo.pending(registration).isPresent()
        || repairPrice(health, rate) != price)
      throw new IllegalStateException("Aircraft repair is unavailable or already reserved");
    repairs.put(
        registration, new Repair(id, registration, actor, entity, generation, health, rate, price));
  }

  Repair settleRepair(UUID ticket, UUID actor, boolean delivered) {
    var repair =
        repairTicket(ticket)
            .orElseThrow(() -> new IllegalStateException("No pending aircraft repair"));
    if (!repair.actor().equals(actor))
      throw new IllegalStateException("Repair reservation belongs to another player");
    repairs.remove(repair.registration());
    if (delivered) deliveredRepairs.put(repair.registration(), repair.id());
    return repair;
  }

  public record FlightKey(UUID pilot, long day) {}

  public record FlightAward(
      UUID id, UUID pilot, long day, long reward, long budget, UUID registration) {}

  private final Map<FlightKey, FlightAward> flightAwards = new HashMap<>();
  private final Map<Long, Long> flightSpent = new HashMap<>(), flightBudgets = new HashMap<>();

  public Optional<FlightAward> flightAward(UUID pilot, long day) {
    return Optional.ofNullable(flightAwards.get(new FlightKey(pilot, day)));
  }

  public long flightSpent(long day) {
    return flightSpent.getOrDefault(day, 0L);
  }

  public OptionalLong flightBudget(long day) {
    var value = flightBudgets.get(day);
    return value == null ? OptionalLong.empty() : OptionalLong.of(value);
  }

  void awardFlight(
      UUID id,
      UUID pilot,
      long day,
      long reward,
      long budget,
      UUID registration,
      long generation,
      int minimum,
      long meters,
      int samples) {
    var record = required(registration);
    if (day < 0
        || flightAward(pilot, day).isPresent()
        || !record.permits(pilot)
        || record.phase() != Phase.ACTIVE
        || record.generation() != generation
        || minimum < 32
        || minimum > 100_000
        || meters < minimum
        || meters > 1_000_000
        || samples < 8
        || samples > 2_000_000
        || reward < 1
        || budget < reward
        || budget > 1_000_000_000L
        || flightSpent(day) > budget - reward
        || flightBudgets.containsKey(day) && flightBudgets.get(day) != budget
        || !flightBudgets.isEmpty() && day < Collections.max(flightBudgets.keySet()))
      throw new IllegalStateException(
          "Dispatch completion, daily quota or shared reward pool is unavailable");
    flightAwards.put(
        new FlightKey(pilot, day), new FlightAward(id, pilot, day, reward, budget, registration));
    flightSpent.put(day, Math.addExact(flightSpent(day), reward));
    flightBudgets.putIfAbsent(day, budget);
  }

  public static final Set<String> TYPES =
      Set.of(
          "immersive_aircraft:biplane",
          "immersive_aircraft:airship",
          "immersive_aircraft:cargo_airship",
          "immersive_aircraft:gyrodyne",
          "immersive_aircraft:quadrocopter",
          "immersive_aircraft:bamboo_hopper",
          "immersive_aircraft:warship");
  CargoState cargo = new CargoState();
  ScoutState scouts = new ScoutState();

  private final Map<UUID, Registration> records = new LinkedHashMap<>();

  AircraftState copy() {
    var next = new AircraftState();
    next.records.putAll(records);
    next.cargo=cargo.copy();
    next.scouts=scouts.copy();
    next.repairs.putAll(repairs);
    next.deliveredRepairs.putAll(deliveredRepairs);
    next.flightAwards.putAll(flightAwards);
    next.flightSpent.putAll(flightSpent);
    next.flightBudgets.putAll(flightBudgets);
    return next;
  }

  public List<Registration> records() {
    return List.copyOf(records.values());
  }

  public Optional<Registration> get(UUID id) {
    return Optional.ofNullable(records.get(id));
  }

  void buy(UUID id, UUID owner, String type, CompoundTag item) {
    if (!TYPES.contains(type)
        || !type.equals(item.getString("id"))
        || item.getInt("count") != 1
        || records.containsKey(id)
        || records.size() >= 4096) throw new IllegalArgumentException("Invalid aircraft purchase");
    records.put(
        id, new Registration(id, owner, type, 0, Phase.PACKED, Set.of(), item, new CompoundTag()));
  }

  void deploy(UUID id, UUID actor, CompoundTag data) {
    var old = required(id);
    if (!old.permits(actor)
        || old.phase() != Phase.PACKED
        || !data.getAllKeys().equals(Set.of("entity", "dimension", "actor")))
      throw new IllegalStateException("Aircraft plan is unavailable or belongs to another buyer");
    var entity = data.getCompound("entity");
    if (!data.hasUUID("actor")
        || !data.getUUID("actor").equals(actor)
        || !entity.hasUUID("UUID")
        || !entity.getString("id").equals(old.type())
        || entity.contains("Passengers")
        || !data.contains("dimension", Tag.TAG_STRING)
        || !data.getString("dimension").matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
      throw new IllegalArgumentException("Invalid native aircraft deployment");
    records.put(
        id,
        new Registration(
            id,
            old.owner(),
            old.type(),
            old.generation(),
            Phase.STARTING,
            old.crew(),
            old.item(),
            data));
  }

  void activate(UUID id, UUID owner) {
    var old = required(id);
    if (!old.owner().equals(owner) || old.phase() != Phase.STARTING)
      throw new IllegalStateException("Aircraft activation is not pending");
    records.put(
        id,
        new Registration(
            id,
            old.owner(),
            old.type(),
            old.generation(),
            Phase.ACTIVE,
            old.crew(),
            old.item(),
            old.deployment()));
  }

  void pack(UUID id, UUID owner, CompoundTag item) {
    var old = required(id);
    if (!old.owner().equals(owner)
        || repairs.containsKey(id)
        || old.phase() != Phase.ACTIVE && old.phase() != Phase.STARTING
        || !item.getString("id").equals(old.type())
        || item.getInt("count") != 1)
      throw new IllegalStateException("Aircraft cannot be returned");
    cargo.packed(id);
    deliveredRepairs.remove(
        id); // A legitimately packed next-generation body has no old service receipt.
    records.put(
        id,
        new Registration(
            id,
            owner,
            old.type(),
            Math.addExact(old.generation(), 1),
            Phase.PACKED,
            old.crew(),
            item,
            new CompoundTag()));
  }

  void share(UUID id, UUID owner, UUID target, boolean allow) {
    var old = required(id);
    if (!old.owner().equals(owner) || owner.equals(target))
      throw new IllegalStateException("Only the buyer may change shared aircraft access");
    var crew = new HashSet<>(old.crew());
    if (allow) {
      if (!crew.add(target) || crew.size() > 32)
        throw new IllegalStateException("Aircraft crew limit or already shared");
    } else if (!crew.remove(target)) throw new IllegalStateException("Access was already removed");
    records.put(
        id,
        new Registration(
            id,
            owner,
            old.type(),
            old.generation(),
            old.phase(),
            crew,
            old.item(),
            old.deployment()));
  }

  private Registration required(UUID id) {
    return get(id)
        .orElseThrow(() -> new IllegalStateException("No authoritative aircraft registration"));
  }
}
