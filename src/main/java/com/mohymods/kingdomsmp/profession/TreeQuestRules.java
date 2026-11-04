package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Configurable approved quantities; invalid or unknown input never resets saved progression. */
public final class TreeQuestRules {
    private static final Map<MinecraftServer,TreeQuestRules> CACHE=new WeakHashMap<>();private final Map<String,Integer> goals;
    private TreeQuestRules(Map<String,Integer> goals){this.goals=Map.copyOf(goals);}public int goal(String id){TreeQuestService.get(id);return goals.get(id);}
    public static void clear(MinecraftServer server){CACHE.remove(server);}
    public static TreeQuestRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-tree-quests.json"));}catch(IOException e){throw new IllegalStateException("Invalid native tree objective configuration",e);}});}
    public static TreeQuestRules load(Path path)throws IOException{
        var root=new JsonObject();root.addProperty("schema",1);var rows=new JsonObject();root.add("goals",rows);TreeQuestService.ids().stream().sorted().forEach(id->rows.addProperty(id,TreeQuestService.get(id).goal()));
        if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardOpenOption.CREATE_NEW);}
        try{var configured=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!configured.keySet().equals(Set.of("schema","goals"))||number(configured,"schema")!=1||!rows.keySet().containsAll(configured.getAsJsonObject("goals").keySet())||!configured.getAsJsonObject("goals").keySet().containsAll(Set.of("kingdom_smp:lumberjack/a01","kingdom_smp:lumberjack/a02","kingdom_smp:lumberjack/a03","kingdom_smp:lumberjack/a04")))throw new IllegalArgumentException("Unknown tree rules");Map<String,Integer> goals=new HashMap<>();for(var id:rows.keySet()){int n=configured.getAsJsonObject("goals").has(id)?number(configured.getAsJsonObject("goals"),id):TreeQuestService.get(id).goal();if(n<1||n>100000||id.endsWith("j01")&&n<2)throw new IllegalArgumentException("Tree goal outside1..100000");goals.put(id,n);}return new TreeQuestRules(goals);}catch(RuntimeException e){throw new IOException("Malformed tree objective configuration",e);}
    }
    private static int number(JsonObject row,String key){var value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required");return value.getAsBigDecimal().intValueExact();}
}
