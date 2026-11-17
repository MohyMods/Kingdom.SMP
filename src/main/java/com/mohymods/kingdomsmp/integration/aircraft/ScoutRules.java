package com.mohymods.kingdomsmp.integration.aircraft;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Optional reconnaissance defaults; no profession objective, building or dimension unlock. */
public record ScoutRules(
    boolean enabled, int regions, int distance, int airborne, long reward, long budget) {
  private static final Map<MinecraftServer, ScoutRules> CACHE = new WeakHashMap<>();

  public ScoutRules {
    if (regions < 1 || regions > 32 || distance < 32 || distance > 1000000)
      throw new IllegalArgumentException("Invalid bounded scouting goal");
    new FlightRules(airborne, reward, budget);
  }

  public static ScoutRules get(MinecraftServer server) {
    return CACHE.computeIfAbsent(
        server,
        s -> {
          try {
            return load(
                s.getWorldPath(LevelResource.ROOT)
                    .resolve("serverconfig/kingdomsmp-air-scout.json"));
          } catch (IOException e) {
            throw new IllegalStateException(
                "Reconnaissance settings need administrator recovery", e);
          }
        });
  }

  public static ScoutRules load(Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectories(path.getParent());
      Files.writeString(
          path,
          "{\"schema\":1,\"enabled\":true,\"regions\":6,\"minimumDistance\":256,\"airborneBlocks\":512,\"reward\":300,\"dailyBudget\":3000}\n",
          StandardOpenOption.CREATE_NEW);
    }
    try {
      var d = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
      if (!d.keySet()
              .equals(
                  Set.of(
                      "schema",
                      "enabled",
                      "regions",
                      "minimumDistance",
                      "airborneBlocks",
                      "reward",
                      "dailyBudget"))
          || number(d, "schema") != 1
          || !d.get("enabled").isJsonPrimitive()
          || !d.getAsJsonPrimitive("enabled").isBoolean())
        throw new IllegalArgumentException("Unknown reconnaissance schema");
      return new ScoutRules(
          d.get("enabled").getAsBoolean(),
          Math.toIntExact(number(d, "regions")),
          Math.toIntExact(number(d, "minimumDistance")),
          Math.toIntExact(number(d, "airborneBlocks")),
          number(d, "reward"),
          number(d, "dailyBudget"));
    } catch (RuntimeException e) {
      throw new IOException("Invalid scouting policy", e);
    }
  }

  private static long number(JsonObject d, String key) {
    var v = d.get(key);
    if (!v.isJsonPrimitive() || !v.getAsJsonPrimitive().isNumber())
      throw new IllegalArgumentException(key);
    return v.getAsBigDecimal().longValueExact();
  }

  public void requireEnabled() {
    if (!enabled)
      throw new IllegalStateException(
          "Reconnaissance contracts are disabled by the server configuration");
  }
}
