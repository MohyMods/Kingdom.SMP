package com.mohymods.kingdomsmp.gametest;

import com.mohymods.kingdomsmp.economy.*;
import com.mohymods.kingdomsmp.persistence.AtomicNbt;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Test mod only. No split packages and no production fault-injection API or option. */
public final class AircraftRepairFault {
  private AircraftRepairFault() {}

  @SuppressWarnings("unchecked")
  public static void failSettlementBeforeRename(MinecraftServer server) throws Exception {
    Class<?> writer = Class.forName("com.mohymods.kingdomsmp.economy.EconomyStore$Writer");
    var callback =
        Proxy.newProxyInstance(
            writer.getClassLoader(),
            new Class<?>[] {writer},
            (proxy, method, args) -> {
              if (!method.getName().equals("write"))
                throw new UnsupportedOperationException("Unexpected private fault callback method");
              var tag = (CompoundTag) args[1];
              var rows = tag.getList("history", Tag.TAG_COMPOUND);
              if (!rows.isEmpty()
                  && rows.getCompound(rows.size() - 1)
                      .getString("kind")
                      .equals("AIRCRAFT_REPAIR_APPLY"))
                throw new IOException(
                    "Explicit private native repair settlement fault before rename");
              AtomicNbt.write((Path) args[0], tag);
              return null;
            });
    var open = EconomyStore.class.getDeclaredMethod("open", Path.class, writer);
    if (!open.trySetAccessible())
      throw new IllegalStateException("Private test journal writer unavailable");
    var replacement =
        (EconomyStore)
            open.invoke(null, server.getWorldPath(LevelResource.ROOT).resolve("data"), callback);
    var field = CommerceService.class.getDeclaredField("STORES");
    if (!field.trySetAccessible())
      throw new IllegalStateException("Private test store map unavailable");
    ((Map<MinecraftServer, EconomyStore>) field.get(null)).put(server, replacement);
  }
}
