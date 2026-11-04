package com.mohymods.kingdomsmp.economy;

import java.util.*;
import net.minecraft.nbt.*;

/** Strict journal shape for a bounded native inventory transaction. */
final class CargoData {
  private CargoData() {}

  static CompoundTag encode(CargoState.Reservation r) {
    var data = new CompoundTag();
    data.putUUID("registration", r.registration());
    data.putUUID("entity", r.entity());
    data.putLong("generation", r.generation());
    var rows = new ListTag();
    for (var slot : r.slots()) {
      var row = new CompoundTag();
      row.putInt("index", slot.index());
      row.putInt("before", slot.before());
      row.putInt("take", slot.take());
      rows.add(row);
    }
    data.put("slots", rows);
    if (r.previousReceipt() != null) data.putUUID("previous", r.previousReceipt());
    return data;
  }

  static CargoState.Reservation decode(UUID id, UUID contract, CompoundTag data) {
    var fields =
        new HashSet<>(Set.of("registration", "entity", "generation", "slots", "meters", "samples"));
    if (data.contains("previous")) {
      fields.add("previous");
      if (!data.hasUUID("previous"))
        throw new IllegalArgumentException("Invalid prior cargo receipt");
    }
    if (!data.getAllKeys().equals(fields)
        || !data.hasUUID("registration")
        || !data.hasUUID("entity")
        || !data.contains("generation", Tag.TAG_LONG)
        || !data.contains("meters", Tag.TAG_LONG)
        || !data.contains("samples", Tag.TAG_INT)
        || !(data.get("slots") instanceof ListTag rows)
        || rows.isEmpty()
        || rows.getElementType() != Tag.TAG_COMPOUND
        || rows.size() > 128)
      throw new IllegalArgumentException("Malformed native cargo reservation");
    var slots = new ArrayList<CargoState.Slot>();
    for (var value : rows) {
      var row = (CompoundTag) value;
      if (!row.getAllKeys().equals(Set.of("index", "before", "take"))
          || !row.contains("index", Tag.TAG_INT)
          || !row.contains("before", Tag.TAG_INT)
          || !row.contains("take", Tag.TAG_INT))
        throw new IllegalArgumentException("Malformed native cargo slot");
      slots.add(new CargoState.Slot(row.getInt("index"), row.getInt("before"), row.getInt("take")));
    }
    return new CargoState.Reservation(
        id,
        contract,
        data.getUUID("registration"),
        data.getUUID("entity"),
        data.getLong("generation"),
        slots,
        data.hasUUID("previous") ? data.getUUID("previous") : null);
  }
}
