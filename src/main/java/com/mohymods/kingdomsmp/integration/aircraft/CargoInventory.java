package com.mohymods.kingdomsmp.integration.aircraft;

import com.mohymods.kingdomsmp.economy.CargoState;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/**
 * Exact pristine native-storage slot transaction. No player inventory or client slot assertions.
 */
public final class CargoInventory {
  private CargoInventory() {}

  public static Set<Integer> slots(Entity entity) {
    if (entity.getServer() == null || !entity.getServer().isSameThread())
      throw new IllegalStateException("Native cargo inspection requires the server thread");
    if (!AircraftAccess.aircraft(entity) || !(entity instanceof Container inventory))
      throw new IllegalStateException("Load the original native cargo aircraft");
    try {
      var description = entity.getClass().getMethod("getInventoryDescription").invoke(entity);
      var rows =
          (List<?>)
              description
                  .getClass()
                  .getMethod("getSlots", String.class)
                  .invoke(description, "inventory");
      var result = new LinkedHashSet<Integer>();
      for (var row : rows) {
        int index = (int) row.getClass().getMethod("index").invoke(row);
        if (index < 0
            || index >= inventory.getContainerSize()
            || index >= 256
            || !result.add(index))
          throw new IllegalStateException("Native cargo slot layout changed");
      }
      return Set.copyOf(result);
    } catch (ReflectiveOperationException error) {
      throw new IllegalStateException("Verified native cargo description unavailable", error);
    }
  }

  public static boolean pristine(ItemStack stack, String item) {
    return !stack.isEmpty()
        && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item)
        && stack.getComponentsPatch().isEmpty()
        && stack.getMaxStackSize() == 64
        && stack.getCount() >= 1
        && stack.getCount() <= 64;
  }

  public static int count(Entity entity, String item) {
    var allowed = slots(entity);
    int total = 0;
    var inventory = (Container) entity;
    for (int index : allowed) {
      var stack = inventory.getItem(index);
      if (pristine(stack, item)) total = Math.addExact(total, stack.getCount());
    }
    return total;
  }

  public static List<CargoState.Slot> select(Entity entity, String item, int quantity) {
    var allowed = slots(entity);
    return CargoInventoryExchange.select((Container) entity, allowed, item, quantity);
  }

  public static boolean matches(
      Entity entity, String item, List<CargoState.Slot> sources, boolean after) {
    var allowed = slots(entity);
    return CargoInventoryExchange.matches((Container) entity, allowed, item, sources, after);
  }

  public static void consume(Entity entity, String item, List<CargoState.Slot> sources) {
    var allowed = slots(entity);
    CargoInventoryExchange.consume((Container) entity, allowed, item, sources);
  }
}
