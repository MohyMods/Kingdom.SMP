package com.mohymods.kingdomsmp.integration.aircraft;

import com.google.gson.*;
import com.mohymods.kingdomsmp.economy.EconomyStore;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;

/** Six native, craftable nonweapon upgrades. No invented higher-tier or profession permissions. */
public final class AircraftEquipment {
  public record Part(String path, String title, long defaultPrice) {
    public String id() {
      return "immersive_aircraft:" + path;
    }

    public String resource() {
      return "kingdom_smp:aircraft_equipment/" + path;
    }
  }

  public static final List<Part> PARTS =
      List.of(
          new Part("enhanced_propeller", "Enhanced propeller", 2000),
          new Part("eco_engine", "Eco engine", 2500),
          new Part("steel_boiler", "Steel boiler", 2500),
          new Part("industrial_gears", "Industrial gears", 1500),
          new Part("improved_landing_gear", "Improved landing gear", 1500),
          new Part("gyroscope", "Gyroscope", 1000));

  public record Rules(Map<String, Long> prices, int dailyStock) {
    public Rules {
      prices = Map.copyOf(prices);
    }

    public long price(Part part) {
      return prices.get(part.id());
    }
  }

  private static final Map<MinecraftServer, Rules> CACHE = new WeakHashMap<>();

  private AircraftEquipment() {}

  public static Part part(String id) {
    return PARTS.stream()
        .filter(p -> p.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unknown native aircraft equipment"));
  }

  public static ItemStack stack(Part part) {
    var id = ResourceLocation.parse(part.id());
    if (!BuiltInRegistries.ITEM.containsKey(id))
      throw new IllegalStateException("Native aircraft equipment is unavailable");
    return new ItemStack(BuiltInRegistries.ITEM.get(id));
  }

  public static Rules rules(MinecraftServer server) {
    return CACHE.computeIfAbsent(
        server,
        s -> {
          try {
            return load(
                s.getWorldPath(LevelResource.ROOT)
                    .resolve("serverconfig/kingdomsmp-aircraft-equipment.json"));
          } catch (IOException e) {
            throw new IllegalStateException("Aircraft equipment settings need recovery", e);
          }
        });
  }

  public static Rules load(Path file) throws IOException {
    var defaults = new JsonObject();
    defaults.addProperty("schema", 1);
    defaults.addProperty("dailyStock", 8);
    var prices = new JsonObject();
    for (var p : PARTS) prices.addProperty(p.id(), p.defaultPrice());
    defaults.add("prices", prices);
    if (!Files.exists(file)) {
      Files.createDirectories(file.getParent());
      Files.writeString(
          file,
          new GsonBuilder().setPrettyPrinting().create().toJson(defaults),
          StandardOpenOption.CREATE_NEW);
    }
    try {
      var data = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
      if (!data.keySet().equals(Set.of("schema", "dailyStock", "prices"))
          || number(data, "schema") != 1
          || !data.getAsJsonObject("prices").keySet().equals(prices.keySet()))
        throw new IllegalArgumentException("Unknown aircraft equipment settings");
      long stock = number(data, "dailyStock");
      if (stock < 1 || stock > 64)
        throw new IllegalArgumentException("Invalid bounded daily stock");
      var result = new TreeMap<String, Long>();
      for (var p : PARTS) {
        long price = number(data.getAsJsonObject("prices"), p.id());
        if (price < 1 || price > 1_000_000_000L)
          throw new IllegalArgumentException("Invalid equipment price");
        result.put(p.id(), price);
      }
      return new Rules(result, (int) stock);
    } catch (RuntimeException e) {
      throw new IOException("Invalid bounded native aircraft equipment configuration", e);
    }
  }

  private static long number(JsonObject data, String key) {
    var value = data.get(key);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
      throw new IllegalArgumentException("Expected numeric setting");
    return value.getAsBigDecimal().longValueExact();
  }

  public static boolean canonicalDelivery(EconomyStore store, UUID id) {
    return store
        .shopItemResource(id)
        .filter(resource -> PARTS.stream().anyMatch(p -> p.resource().equals(resource)))
        .isPresent();
  }

  public static boolean pristine(ItemStack item) {
    return item.getCount() == 1
        && PARTS.stream()
            .anyMatch(p -> BuiltInRegistries.ITEM.getKey(item.getItem()).toString().equals(p.id()))
        && ItemStack.isSameItemSameComponents(item, new ItemStack(item.getItem()));
  }
}
