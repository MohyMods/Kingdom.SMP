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

  public static final Set<String> TYPES =
      Set.of(
          "immersive_aircraft:biplane",
          "immersive_aircraft:airship",
          "immersive_aircraft:cargo_airship",
          "immersive_aircraft:gyrodyne",
          "immersive_aircraft:quadrocopter",
          "immersive_aircraft:bamboo_hopper",
          "immersive_aircraft:warship");
  private final Map<UUID, Registration> records = new LinkedHashMap<>();

  AircraftState copy() {
    var next = new AircraftState();
    next.records.putAll(records);
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
        || old.phase() != Phase.ACTIVE && old.phase() != Phase.STARTING
        || !item.getString("id").equals(old.type())
        || item.getInt("count") != 1)
      throw new IllegalStateException("Aircraft cannot be returned");
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
