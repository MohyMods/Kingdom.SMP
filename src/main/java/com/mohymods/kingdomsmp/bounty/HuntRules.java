package com.mohymods.kingdomsmp.bounty;

import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** Numerical defaults are configurable; Tier3, resident, natural-spawn and kill checks are not. */
public record HuntRules(int goal, long reward, long pool) {
    private static final Map<MinecraftServer,HuntRules> CACHE = new WeakHashMap<>();
    public HuntRules {
        if (goal < 1 || goal > 256 || reward < 1 || reward > 100_000 || pool < Math.multiplyExact(10, reward) || pool > 1_000_000_000)
            throw new IllegalArgumentException("Hunt pool must fund at least ten players; invalid hunt quantities");
    }
    public static void clear(MinecraftServer server) { CACHE.remove(server); }
    public static HuntRules get(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, s -> {
            try { return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-hunts.json")); }
            catch (IOException e) { throw new IllegalStateException("Invalid hunt configuration", e); }
        });
    }
    public static HuntRules load(Path file) throws IOException {
        if (!Files.exists(file)) {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "{\n  \"schema\": 1,\n  \"kills\": 8,\n  \"reward\": 160,\n  \"dailyPool\": 2000\n}\n", StandardOpenOption.CREATE_NEW);
        }
        try {
            var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            if (!json.keySet().equals(Set.of("schema", "kills", "reward", "dailyPool")) || number(json,"schema") != 1)
                throw new IllegalArgumentException("Invalid hunt schema");
            return new HuntRules(Math.toIntExact(number(json,"kills")), number(json,"reward"), number(json,"dailyPool"));
        } catch (RuntimeException e) { throw new IOException("Invalid hunt configuration", e); }
    }
    private static long number(JsonObject json, String key) {
        var value = json.get(key);
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException(key);
        return value.getAsBigDecimal().longValueExact();
    }
}
