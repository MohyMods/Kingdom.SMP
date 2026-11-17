package com.mohymods.kingdomsmp.economy;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Canonical cargo projection. Evidence and native inventories never independently issue money. */
public final class CargoState {
  public enum Stage {
    ACCEPTED,
    UNLOADING,
    DELIVERED
  }

  public record Terms(
      UUID id,
      UUID pilot,
      long day,
      String item,
      int quantity,
      int minimum,
      long reward,
      long budget,
      UUID origin,
      UUID target) {
    public Terms {
      Objects.requireNonNull(id);
      Objects.requireNonNull(pilot);
      Objects.requireNonNull(origin);
      Objects.requireNonNull(target);
      if (day < 0
          || ResourceLocation.tryParse(item) == null
          || quantity < 1
          || quantity > 8192
          || minimum < 32
          || minimum > 100000
          || reward < 1
          || reward > 1000000000L
          || budget < reward
          || budget > 1000000000L
          || origin.equals(target))
        throw new IllegalArgumentException("Invalid bounded cargo contract terms");
    }
  }

  /** Only pristine, stackable cargo is eligible; exact source counts precede any removal. */
  public record Slot(int index, int before, int take) {
    public Slot {
      if (index < 0 || index >= 256 || before < 1 || before > 64 || take < 1 || take > before)
        throw new IllegalArgumentException("Invalid native cargo source slot");
    }

    public int after() {
      return before - take;
    }
  }

  public record Reservation(
      UUID id,
      UUID contract,
      UUID registration,
      UUID entity,
      long generation,
      List<Slot> slots,
      UUID previousReceipt) {
    public Reservation(
        UUID id, UUID contract, UUID registration, UUID entity, long generation, List<Slot> slots) {
      this(id, contract, registration, entity, generation, slots, null);
    }

    public Reservation {
      Objects.requireNonNull(id);
      Objects.requireNonNull(contract);
      Objects.requireNonNull(registration);
      Objects.requireNonNull(entity);
      slots = List.copyOf(slots);
      if (generation < 0
          || slots.isEmpty()
          || slots.size() > 128
          || slots.stream().map(Slot::index).distinct().count() != slots.size())
        throw new IllegalArgumentException("Invalid native cargo reservation");
    }
  }

  public record Contract(Terms terms, Stage stage, Reservation reservation) {
    public Contract {
      Objects.requireNonNull(terms);
      Objects.requireNonNull(stage);
      if ((stage != Stage.ACCEPTED) != (reservation != null)
          || reservation != null && !reservation.contract().equals(terms.id()))
        throw new IllegalArgumentException("Inconsistent cargo contract phase");
    }
  }

  public record Delivery(Terms terms, Reservation reservation) {}

  private record PilotDay(UUID pilot, long day) {}

  private final Map<UUID, Contract> contracts = new LinkedHashMap<>();
  private final Map<PilotDay, UUID> pilots = new HashMap<>();
  private final Map<UUID, UUID> pendingFrames = new HashMap<>();
  private final Map<UUID, Delivery> deliveredReceipts = new HashMap<>();
  private final Map<Long, Long> paid = new HashMap<>(),
      held = new HashMap<>(),
      budgets = new HashMap<>();

  CargoState copy() {
    var next = new CargoState();
    next.contracts.putAll(contracts);
    next.pilots.putAll(pilots);
    next.pendingFrames.putAll(pendingFrames);
    next.deliveredReceipts.putAll(deliveredReceipts);
    next.paid.putAll(paid);
    next.held.putAll(held);
    next.budgets.putAll(budgets);
    return next;
  }

  public Optional<Contract> get(UUID id) {
    return Optional.ofNullable(contracts.get(id));
  }

  public Optional<Contract> get(UUID pilot, long day) {
    return Optional.ofNullable(pilots.get(new PilotDay(pilot, day))).flatMap(this::get);
  }

  public Optional<Reservation> pending(UUID registration) {
    return Optional.ofNullable(pendingFrames.get(registration))
        .map(contracts::get)
        .map(Contract::reservation);
  }

  public Optional<Contract> ticket(UUID ticket) {
    return contracts.values().stream()
        .filter(v -> v.stage() == Stage.UNLOADING && v.reservation().id().equals(ticket))
        .findFirst();
  }

  public List<Contract> pending() {
    return contracts.values().stream().filter(v -> v.stage() == Stage.UNLOADING).toList();
  }

  public Optional<UUID> deliveredReceipt(UUID registration) {
    return Optional.ofNullable(deliveredReceipts.get(registration)).map(v -> v.reservation().id());
  }

  public Optional<Delivery> delivered(UUID registration) {
    return Optional.ofNullable(deliveredReceipts.get(registration));
  }

  public long paid(long day) {
    return paid.getOrDefault(day, 0L);
  }

  public long held(long day) {
    return held.getOrDefault(day, 0L);
  }

  public OptionalLong budget(long day) {
    var value = budgets.get(day);
    return value == null ? OptionalLong.empty() : OptionalLong.of(value);
  }

  void packed(UUID registration) {
    if (pendingFrames.containsKey(registration))
      throw new IllegalStateException("Native cargo unloading is pending");
    deliveredReceipts.remove(registration);
  }

  void accept(Terms terms) {
    if (contracts.containsKey(terms.id())
        || pilots.containsKey(new PilotDay(terms.pilot(), terms.day()))
        || budgets.containsKey(terms.day()) && budgets.get(terms.day()) != terms.budget()
        || !budgets.isEmpty() && terms.day() < Collections.max(budgets.keySet()))
      throw new IllegalStateException("Cargo contract already exists or daily terms changed");
    contracts.put(terms.id(), new Contract(terms, Stage.ACCEPTED, null));
    pilots.put(new PilotDay(terms.pilot(), terms.day()), terms.id());
    budgets.putIfAbsent(terms.day(), terms.budget());
  }

  void reserve(
      UUID pilot,
      Reservation reservation,
      AircraftState.Registration frame,
      boolean repairPending,
      long meters,
      int samples) {
    var old = required(reservation.contract());
    var terms = old.terms();
    if (!terms.pilot().equals(pilot)
        || old.stage() != Stage.ACCEPTED
        || !frame.id().equals(reservation.registration())
        || frame.phase() != AircraftState.Phase.ACTIVE
        || frame.generation() != reservation.generation()
        || !frame.permits(pilot)
        || repairPending
        || !frame.deployment().getCompound("entity").getUUID("UUID").equals(reservation.entity())
        || pendingFrames.containsKey(frame.id())
        || !Objects.equals(reservation.previousReceipt(), deliveredReceipt(frame.id()).orElse(null))
        || meters < terms.minimum()
        || meters > 1000000
        || samples < 8
        || samples > 2000000
        || reservation.slots().stream().mapToInt(Slot::take).sum() != terms.quantity()
        || paid(terms.day()) + held(terms.day()) > terms.budget() - terms.reward())
      throw new IllegalStateException(
          "Cargo proof, aircraft, source inventory or daily budget changed");
    held.put(terms.day(), Math.addExact(held(terms.day()), terms.reward()));
    pendingFrames.put(frame.id(), terms.id());
    contracts.put(terms.id(), new Contract(terms, Stage.UNLOADING, reservation));
  }

  Terms settle(UUID ticket, UUID pilot, boolean delivered) {
    var old =
        contracts.values().stream()
            .filter(v -> v.stage() == Stage.UNLOADING && v.reservation().id().equals(ticket))
            .findFirst()
            .orElseThrow();
    var terms = old.terms();
    var reservation = old.reservation();
    if (!terms.pilot().equals(pilot) || old.stage() != Stage.UNLOADING)
      throw new IllegalStateException("Cargo settlement is not authorized");
    held.put(terms.day(), Math.subtractExact(held(terms.day()), terms.reward()));
    pendingFrames.remove(reservation.registration());
    if (delivered) {
      paid.put(terms.day(), Math.addExact(paid(terms.day()), terms.reward()));
      deliveredReceipts.put(reservation.registration(), new Delivery(terms, reservation));
    }
    contracts.put(
        terms.id(),
        new Contract(
            terms, delivered ? Stage.DELIVERED : Stage.ACCEPTED, delivered ? reservation : null));
    return terms;
  }

  private Contract required(UUID id) {
    return get(id).orElseThrow(() -> new IllegalStateException("Unknown cargo contract"));
  }
}
