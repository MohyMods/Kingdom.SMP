package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.economy.CargoState;
import java.util.*;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/** Isolated container transaction; no server hooks, world writes, payment or player inventory. */
final class CargoInventoryExchange {
  private CargoInventoryExchange() {}

  static List<CargoState.Slot> select(
      Container inventory, Collection<Integer> allowed, String item, int quantity) {
    if (quantity < 1 || quantity > 8192)
      throw new IllegalArgumentException("Invalid cargo quantity");
    int remaining = quantity;
    var result = new ArrayList<CargoState.Slot>();
    for (int slot : new TreeSet<>(allowed)) {
      if (slot < 0 || slot >= inventory.getContainerSize() || slot >= 256)
        throw new IllegalArgumentException("Invalid native cargo layout");
      var stack = inventory.getItem(slot);
      if (!CargoInventory.pristine(stack, item)) continue;
      int take = Math.min(remaining, stack.getCount());
      result.add(new CargoState.Slot(slot, stack.getCount(), take));
      remaining -= take;
      if (remaining == 0) return List.copyOf(result);
    }
    throw new IllegalStateException(
        "Load " + quantity + " pristine " + item + " in native aircraft storage before reporting");
  }

  static boolean matches(
      Container inventory,
      Collection<Integer> allowed,
      String item,
      List<CargoState.Slot> sources,
      boolean after) {
    if (sources.isEmpty()
        || sources.size() > 128
        || sources.stream().map(CargoState.Slot::index).distinct().count() != sources.size())
      return false;
    for (var slot : sources) {
      if (!allowed.contains(slot.index()) || slot.index() >= inventory.getContainerSize())
        return false;
      int expected = after ? slot.after() : slot.before();
      var stack = inventory.getItem(slot.index());
      if (expected == 0) {
        if (!stack.isEmpty()) return false;
      } else if (!CargoInventory.pristine(stack, item) || stack.getCount() != expected)
        return false;
    }
    return true;
  }

  static void consume(
      Container inventory,
      Collection<Integer> allowed,
      String item,
      List<CargoState.Slot> sources) {
    if (!matches(inventory, allowed, item, sources, false))
      throw new IllegalStateException("Cargo source inventory changed; no items removed");
    var before = new ArrayList<ItemStack>();
    for (var slot : sources) before.add(inventory.getItem(slot.index()).copy());
    try {
      for (int i = 0; i < sources.size(); i++) {
        var slot = sources.get(i);
        inventory.setItem(
            slot.index(),
            slot.after() == 0 ? ItemStack.EMPTY : before.get(i).copyWithCount(slot.after()));
      }
      inventory.setChanged();
      if (!matches(inventory, allowed, item, sources, true))
        throw new IllegalStateException("Native cargo debit failed result verification");
    } catch (RuntimeException failure) {
      for (int i = 0; i < sources.size(); i++)
        try {
          inventory.setItem(sources.get(i).index(), before.get(i));
        } catch (RuntimeException rollback) {
          failure.addSuppressed(rollback);
        }
      try {
        inventory.setChanged();
      } catch (RuntimeException rollback) {
        failure.addSuppressed(rollback);
      }
      throw failure;
    }
  }
}
