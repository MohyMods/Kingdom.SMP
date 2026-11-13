package com.mohymods.kingdomsmp.integration.aircraft;

import com.google.gson.*;
import com.mohymods.kingdomsmp.economy.AircraftState;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Configurable numerical defaults; no configurable bypass of access or ownership rules. */
public final class AircraftSalesRules {
  private static final Map<MinecraftServer, Map<String, Long>> CACHE = new WeakHashMap<>();

  private AircraftSalesRules() {}

  public static Map<String, Long> prices(MinecraftServer server) {
    return CACHE.computeIfAbsent(
        server,
        s -> {
          try {
            return load(
                s.getWorldPath(LevelResource.ROOT)
                    .resolve("serverconfig/kingdomsmp-aircraft.json"));
          } catch (IOException e) {
            throw new IllegalStateException("Aircraft prices need administrator recovery", e);
          }
        });
  }

  public static Map<String, Long> load(Path file) throws IOException {
    var defaults = new TreeMap<String, Long>();
    for (String type : AircraftState.TYPES)
      defaults.put(
          type,
          switch (type.substring(type.indexOf(':') + 1)) {
            case "bamboo_hopper" -> 8000L;
            case "quadrocopter" -> 12000L;
            case "gyrodyne" -> 16000L;
            case "biplane" -> 20000L;
            case "airship" -> 30000L;
            case "cargo_airship" -> 45000L;
            default -> 60000L;
          });
    var root = new JsonObject();
    root.addProperty("schema", 1);
    var prices = new JsonObject();
    defaults.forEach(prices::addProperty);
    root.add("prices", prices);
    if (!Files.exists(file)) {
      Files.createDirectories(file.getParent());
      Files.writeString(
          file,
          new GsonBuilder().setPrettyPrinting().create().toJson(root),
          StandardOpenOption.CREATE_NEW);
    }
    try {
      var data = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
      if (!data.keySet().equals(Set.of("schema", "prices"))
          || !data.get("schema").isJsonPrimitive()
          || !data.get("schema").getAsJsonPrimitive().isNumber()
          || data.get("schema").getAsBigDecimal().intValueExact() != 1
          || !data.getAsJsonObject("prices").keySet().equals(defaults.keySet()))
        throw new IllegalArgumentException("Unknown aircraft prices");
      var result = new TreeMap<String, Long>();
      for (String type : defaults.keySet()) {
        var value = data.getAsJsonObject("prices").get(type);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
          throw new IllegalArgumentException("Price must be a number");
        long price = value.getAsBigDecimal().longValueExact();
        if (price < 1000 || price > 1_000_000_000L)
          throw new IllegalArgumentException("Aircraft price out of bounds");
        result.put(type, price);
      }
      return Map.copyOf(result);
    } catch (RuntimeException e) {
      throw new IOException("Invalid bounded aircraft prices", e);
    }
  }
}
