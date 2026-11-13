package com.mohymods.kingdomsmp.integration.aircraft;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Numerical dispatch defaults. No new profession quest, building or dimension unlock. */
public record FlightRules(int minimumBlocks, long reward, long dailyBudget) {
  private static final Map<MinecraftServer, FlightRules> CACHE = new WeakHashMap<>();

  public FlightRules {
    if (minimumBlocks < 32
        || minimumBlocks > 100_000
        || reward < 1
        || reward > 1_000_000_000L
        || dailyBudget < reward
        || dailyBudget > 1_000_000_000L)
      throw new IllegalArgumentException("Invalid bounded air dispatch terms");
  }

  public static FlightRules get(MinecraftServer server) {
    return CACHE.computeIfAbsent(
        server,
        s -> {
          try {
            return load(
                s.getWorldPath(LevelResource.ROOT)
                    .resolve("serverconfig/kingdomsmp-air-dispatch.json"));
          } catch (IOException e) {
            throw new IllegalStateException("Air dispatch settings need administrator recovery", e);
          }
        });
  }

  public static FlightRules load(Path file) throws IOException {
    if (!Files.exists(file)) {
      Files.createDirectories(file.getParent());
      Files.writeString(
          file,
          "{\"schema\":1,\"minimumBlocks\":128,\"reward\":500,\"dailyBudget\":5000}\n",
          StandardOpenOption.CREATE_NEW);
    }
    try {
      var root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
      if (!root.keySet().equals(Set.of("schema", "minimumBlocks", "reward", "dailyBudget"))
          || number(root, "schema") != 1)
        throw new IllegalArgumentException("Unknown dispatch settings");
      return new FlightRules(
          Math.toIntExact(number(root, "minimumBlocks")),
          number(root, "reward"),
          number(root, "dailyBudget"));
    } catch (RuntimeException e) {
      throw new IOException("Invalid bounded air dispatch schema", e);
    }
  }

  private static long number(JsonObject root, String key) {
    var n = root.get(key);
    if (!n.isJsonPrimitive() || !n.getAsJsonPrimitive().isNumber())
      throw new IllegalArgumentException(key);
    return n.getAsBigDecimal().longValueExact();
  }
}
