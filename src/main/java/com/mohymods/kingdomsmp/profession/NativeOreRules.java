package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Configurable approved quantities; invalid or unknown input never resets saved progression. */
public final class NativeOreRules {
    private static final Map<MinecraftServer,NativeOreRules> CACHE=new WeakHashMap<>();private final Map<String,Integer> goals;private final int separation;
    private NativeOreRules(Map<String,Integer> goals,int separation){this.goals=Map.copyOf(goals);this.separation=separation;}public int separation(){return separation;}public int goal(String id){if(!NativeOreQuests.contains(id))throw new IllegalArgumentException("Unvalidated mining goal");return goals.get(id);}
    public static void clear(MinecraftServer server){CACHE.remove(server);}
    public static NativeOreRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-mining-quests.json"));}catch(IOException e){throw new IllegalStateException("Invalid native mining objective configuration",e);}});}
    public static NativeOreRules load(Path path)throws IOException{
        var root=new JsonObject();root.addProperty("schema",1);root.addProperty("copperAreaSeparationChunks",2);var rows=new JsonObject();root.add("goals",rows);NativeOreQuests.GOALS.keySet().stream().sorted().forEach(id->rows.addProperty(id,NativeOreQuests.GOALS.get(id)));
        if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardOpenOption.CREATE_NEW);}
        try{var configured=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!(configured.keySet().equals(Set.of("schema","goals"))||configured.keySet().equals(Set.of("schema","goals","copperAreaSeparationChunks")))||number(configured,"schema")!=1||!rows.keySet().containsAll(configured.getAsJsonObject("goals").keySet())||!configured.getAsJsonObject("goals").keySet().containsAll(Set.of("kingdom_smp:miner/a01","kingdom_smp:miner/a02")))throw new IllegalArgumentException("Unknown mining rules");Map<String,Integer> goals=new HashMap<>();for(var id:rows.keySet()){int n=configured.getAsJsonObject("goals").has(id)?number(configured.getAsJsonObject("goals"),id):NativeOreQuests.GOALS.get(id);if(n<1||n>100000)throw new IllegalArgumentException("Mining goal outside1..100000");goals.put(id,n);}int separation=configured.has("copperAreaSeparationChunks")?number(configured,"copperAreaSeparationChunks"):2;if(separation<1||separation>8)throw new IllegalArgumentException("Copper area separation outside1..8chunks");return new NativeOreRules(goals,separation);}catch(RuntimeException e){throw new IOException("Malformed mining objective configuration",e);}
    }
    private static int number(JsonObject row,String key){var value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required");return value.getAsBigDecimal().intValueExact();}
}
