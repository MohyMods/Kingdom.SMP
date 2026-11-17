package com.mohymods.kingdomsmp.integration.aircraft;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;

/** Physical native-compatible refueling supplies; never edits an engine's fuel value remotely. */
public final class AircraftFuel {
  public static final String RESOURCE = "kingdom_smp:aircraft_fuel/coal", ACTION = "air:fuel";

  public record Rules(int packSize, long unitPrice, int dailyStock) {
    public Rules {
      if (packSize < 1
          || packSize > 64
          || unitPrice < 1
          || unitPrice > 1000000
          || dailyStock < packSize
          || dailyStock > 65536
          || dailyStock % packSize != 0)
        throw new IllegalArgumentException("Invalid bounded aircraft fuel supply policy");
    }

    public long price() {
      return Math.multiplyExact(packSize, unitPrice);
    }

    public ItemStack stack() {
      return new ItemStack(Items.COAL, packSize);
    }
  }

  private static final Map<MinecraftServer, Rules> CACHE = new WeakHashMap<>();

  private AircraftFuel() {}

  public static Rules rules(MinecraftServer server) {
    return CACHE.computeIfAbsent(
        server,
        s -> {
          try {
            return load(
                s.getWorldPath(LevelResource.ROOT)
                    .resolve("serverconfig/kingdomsmp-aircraft-fuel.json"));
          } catch (IOException failure) {
            throw new IllegalStateException(
                "Aircraft fuel supply settings need administrator recovery", failure);
          }
        });
  }

  public static Rules load(Path file) throws IOException {
    if (!Files.exists(file)) {
      Files.createDirectories(file.getParent());
      Files.writeString(
          file,
          "{\"schema\":1,\"packSize\":16,\"unitPrice\":8,\"dailyStock\":128}\n",
          StandardOpenOption.CREATE_NEW);
    }
    try {
      var data = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
      if (!data.keySet().equals(Set.of("schema", "packSize", "unitPrice", "dailyStock"))
          || number(data, "schema") != 1)
        throw new IllegalArgumentException("Unknown aircraft fuel supply schema");
      return new Rules(
          Math.toIntExact(number(data, "packSize")),
          number(data, "unitPrice"),
          Math.toIntExact(number(data, "dailyStock")));
    } catch (RuntimeException failure) {
      throw new IOException("Invalid aircraft fuel supply settings", failure);
    }
  }

  private static long number(JsonObject data, String key) {
    var value = data.get(key);
    if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
      throw new IllegalArgumentException(key);
    return value.getAsBigDecimal().longValueExact();
  }
}
