package com.mohymods.kingdomsmp.integration.aircraft;

import static org.junit.jupiter.api.Assertions.*;

import com.mohymods.kingdomsmp.economy.CargoState;
import java.util.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import org.junit.jupiter.api.*;

class CargoInventoryExchangeTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.tryDetectVersion();
    Bootstrap.bootStrap();
  }

  SimpleContainer source() {
    var inv = new SimpleContainer(12);
    inv.setItem(8, new ItemStack(Items.WHEAT, 40));
    inv.setItem(9, new ItemStack(Items.WHEAT, 40));
    inv.setItem(2, new ItemStack(Items.COAL, 16));
    return inv;
  }

  @Test
  void splitStackDebitConsumesExactlyTheReservedGoodsAndPreservesFuel() {
    var inv = source();
    var slots = CargoInventoryExchange.select(inv, Set.of(8, 9), "minecraft:wheat", 64);
    assertEquals(List.of(new CargoState.Slot(8, 40, 40), new CargoState.Slot(9, 40, 24)), slots);
    CargoInventoryExchange.consume(inv, Set.of(8, 9), "minecraft:wheat", slots);
    assertTrue(inv.getItem(8).isEmpty());
    assertEquals(16, inv.getItem(9).getCount());
    assertEquals(16, inv.getItem(2).getCount());
    assertTrue(CargoInventoryExchange.matches(inv, Set.of(8, 9), "minecraft:wheat", slots, true));
    assertThrows(
        IllegalStateException.class,
        () -> CargoInventoryExchange.consume(inv, Set.of(8, 9), "minecraft:wheat", slots));
    assertEquals(16, inv.getItem(9).getCount());
  }

  @Test
  void changedLaterSlotPreventsAnyEarlierMutation() {
    var inv = source();
    var slots = CargoInventoryExchange.select(inv, Set.of(8, 9), "minecraft:wheat", 64);
    inv.getItem(9).shrink(1);
    assertThrows(
        IllegalStateException.class,
        () -> CargoInventoryExchange.consume(inv, Set.of(8, 9), "minecraft:wheat", slots));
    assertEquals(40, inv.getItem(8).getCount());
    assertEquals(39, inv.getItem(9).getCount());
  }

  @Test
  void customItemsNonStorageSlotsAndDuplicatesNeverBecomeOrdinaryGoods() {
    var inv = source();
    inv.getItem(9).set(DataComponents.CUSTOM_NAME, Component.literal("Player keepsake"));
    assertThrows(
        IllegalStateException.class,
        () -> CargoInventoryExchange.select(inv, Set.of(8, 9), "minecraft:wheat", 64));
    assertThrows(
        IllegalStateException.class,
        () ->
            CargoInventoryExchange.consume(
                inv, Set.of(9), "minecraft:wheat", List.of(new CargoState.Slot(8, 40, 40))));
    var repeated = List.of(new CargoState.Slot(8, 40, 20), new CargoState.Slot(8, 40, 20));
    assertThrows(
        IllegalStateException.class,
        () -> CargoInventoryExchange.consume(inv, Set.of(8), "minecraft:wheat", repeated));
    assertEquals(40, inv.getItem(8).getCount());
    assertTrue(inv.getItem(9).has(DataComponents.CUSTOM_NAME));
  }

  @Test
  void aMidDebitContainerFailureRestoresTheOriginalSelectedStacks() {
    class Faulty extends SimpleContainer {
      boolean fail;

      Faulty() {
        super(12);
      }

      @Override
      public void setItem(int slot, ItemStack stack) {
        if (fail && slot == 9) {
          fail = false;
          throw new IllegalStateException("Private second-slot fault");
        }
        super.setItem(slot, stack);
      }
    }
    var inv = new Faulty();
    inv.setItem(8, new ItemStack(Items.WHEAT, 40));
    inv.setItem(9, new ItemStack(Items.WHEAT, 40));
    var slots = CargoInventoryExchange.select(inv, Set.of(8, 9), "minecraft:wheat", 64);
    inv.fail = true;
    assertThrows(
        IllegalStateException.class,
        () -> CargoInventoryExchange.consume(inv, Set.of(8, 9), "minecraft:wheat", slots));
    assertEquals(40, inv.getItem(8).getCount());
    assertEquals(40, inv.getItem(9).getCount());
  }
}
