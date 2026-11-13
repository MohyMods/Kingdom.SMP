package com.mohymods.kingdomsmp.integration.aircraft;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Validated numerical defaults; existing explicit administrator settings remain authoritative. */
public record CargoRules(
    boolean enabled, String item, int quantity, int minimum, long reward, long budget) {
  private static final Map<MinecraftServer, CargoRules> CACHE = new WeakHashMap<>();

  public CargoRules {
    if (item == null
        || item.length() > 128
        || net.minecraft.resources.ResourceLocation.tryParse(item) == null
        || quantity < 1
        || quantity > 8192) throw new IllegalArgumentException("Invalid cargo item/quantity");
    new FlightRules(minimum, reward, budget);
  }

  public static CargoRules get(MinecraftServer server) {
    return CACHE.computeIfAbsent(
        server,
        s -> {
          try {
            return load(
                s.getWorldPath(LevelResource.ROOT)
                    .resolve("serverconfig/kingdomsmp-air-cargo.json"));
          } catch (IOException e) {
            throw new IllegalStateException("Air cargo settings require administrator recovery", e);
          }
        });
  }

  public static CargoRules load(Path file) throws IOException {
    if (!Files.exists(file)) {
      Files.createDirectories(file.getParent());
      Files.writeString(
          file,
          "{\"schema\":1,\"enabled\":true,\"item\":\"minecraft:wheat\",\"quantity\":64,\"minimumBlocks\":128,\"reward\":500,\"dailyBudget\":5000}\n",
          StandardOpenOption.CREATE_NEW);
    }
    try {
      var d = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
      if (!d.keySet()
              .equals(
                  Set.of(
                      "schema",
                      "enabled",
                      "item",
                      "quantity",
                      "minimumBlocks",
                      "reward",
                      "dailyBudget"))
          || number(d, "schema") != 1
          || !d.get("enabled").isJsonPrimitive()
          || !d.getAsJsonPrimitive("enabled").isBoolean()
          || !d.get("item").isJsonPrimitive()
          || !d.getAsJsonPrimitive("item").isString())
        throw new IllegalArgumentException("Invalid cargo schema");
      return new CargoRules(
          d.get("enabled").getAsBoolean(),
          d.get("item").getAsString(),
          Math.toIntExact(number(d, "quantity")),
          Math.toIntExact(number(d, "minimumBlocks")),
          number(d, "reward"),
          number(d, "dailyBudget"));
    } catch (RuntimeException error) {
      throw new IOException("Invalid cargo settings", error);
    }
  }

  private static long number(JsonObject d, String key) {
    var n = d.get(key);
    if (!n.isJsonPrimitive() || !n.getAsJsonPrimitive().isNumber())
      throw new IllegalArgumentException(key);
    return n.getAsBigDecimal().longValueExact();
  }

  public void requireEnabled() {
    if (!enabled)
      throw new IllegalStateException(
          "Air cargo deliveries are disabled by the server configuration");
    var key = net.minecraft.resources.ResourceLocation.parse(item);
    if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key)
        || net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key).getDefaultMaxStackSize()
            != 64)
      throw new IllegalStateException(
          "Configured air cargo must be a registered stackable resource");
  }
}
