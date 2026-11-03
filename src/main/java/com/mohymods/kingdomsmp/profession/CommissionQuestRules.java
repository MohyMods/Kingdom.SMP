package com.mohymods.kingdomsmp.profession;

import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/** Bounded balancing defaults, separate from the immutable approved objective identities. */
public final class CommissionQuestRules {
    private static final Map<MinecraftServer, CommissionQuestRules> CACHE = new WeakHashMap<>();
    private final Map<String,Integer> goals;
    private CommissionQuestRules(Map<String,Integer> goals) { this.goals = Map.copyOf(goals); }
    public int goal(String id) { CommissionQuests.get(id); return goals.get(id); }
    public static void clear(MinecraftServer server) { CACHE.remove(server); }
    public static CommissionQuestRules get(MinecraftServer server) {
        return CACHE.computeIfAbsent(server, s -> {
            try { return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-commission-quests.json")); }
            catch (IOException e) { throw new IllegalStateException("Commission quest configuration is invalid; no XP awarded", e); }
        });
    }
    public static CommissionQuestRules load(Path path) throws IOException {
        var defaults = new JsonObject(); defaults.addProperty("schema",1);
        var quantities = new JsonObject(); defaults.add("goals", quantities);
        for (var id : CommissionQuests.ids()) quantities.addProperty(id,CommissionQuests.get(id).goal());
        if (!Files.exists(path)) {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);
        }
        try {
            var data = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (!data.keySet().equals(Set.of("schema","goals")) || number(data,"schema") != 1) throw new IllegalArgumentException("Unknown schema");
            var rows=data.getAsJsonObject("goals");
            if (!quantities.keySet().containsAll(rows.keySet())||!rows.keySet().containsAll(Set.of("kingdom_smp:builder/a07","kingdom_smp:lumberjack/a07"))) throw new IllegalArgumentException("Missing or unknown commission goal");
            Map<String,Integer> result=new HashMap<>();
            for (var id : quantities.keySet()) {
                int n=rows.has(id)?number(rows,id):CommissionQuests.get(id).goal();
                if(n<1||n>365) throw new IllegalArgumentException("Commission periods must be 1..365");
                result.put(id,n);
            }
            return new CommissionQuestRules(result);
        } catch (RuntimeException e) { throw new IOException("Invalid commission goals",e); }
    }
    private static int number(JsonObject data,String key) {
        var value=data.get(key);
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Numeric goal required");
        return value.getAsBigDecimal().intValueExact();
    }
}
