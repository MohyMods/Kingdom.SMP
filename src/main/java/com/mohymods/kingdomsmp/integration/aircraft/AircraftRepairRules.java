package com.mohymods.kingdomsmp.integration.aircraft;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Numerical convenience-service price only; native food repairs remain available unchanged. */
public final class AircraftRepairRules {
  private static final Map<MinecraftServer, Long> CACHE = new WeakHashMap<>();

  private AircraftRepairRules() {}

  public static long rate(MinecraftServer server) {
    return CACHE.computeIfAbsent(
        server,
        s -> {
          try {
            return load(
                s.getWorldPath(LevelResource.ROOT)
                    .resolve("serverconfig/kingdomsmp-aircraft-repairs.json"));
          } catch (IOException e) {
            throw new IllegalStateException("Aircraft repair prices need recovery", e);
          }
        });
  }

  public static long load(Path file) throws IOException {
    if (!Files.exists(file)) {
      Files.createDirectories(file.getParent());
      Files.writeString(
          file, "{\"schema\":1,\"fullRepairRate\":2000}\n", StandardOpenOption.CREATE_NEW);
    }
    try {
      var root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
      if (!root.keySet().equals(Set.of("schema", "fullRepairRate")) || number(root, "schema") != 1)
        throw new IllegalArgumentException("Unknown repair settings");
      long rate = number(root, "fullRepairRate");
      if (rate < 1 || rate > 1_000_000_000L)
        throw new IllegalArgumentException("Repair rate outside bounds");
      return rate;
    } catch (RuntimeException e) {
      throw new IOException("Invalid numerical aircraft repair settings", e);
    }
  }

  private static long number(JsonObject root, String key) {
    var n = root.get(key);
    if (!n.isJsonPrimitive() || !n.getAsJsonPrimitive().isNumber())
      throw new IllegalArgumentException(key);
    return n.getAsBigDecimal().longValueExact();
  }
}
