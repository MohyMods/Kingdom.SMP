package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
/** Server-only configurable quantities; fixed objective identities and rank bands cannot be replaced by config. */
public final class FarmerQuestRules {
    private final Map<String,Integer> goals;
    private static final Map<MinecraftServer,FarmerQuestRules> CACHE=new WeakHashMap<>();
    private FarmerQuestRules(Map<String,Integer> goals){this.goals=Map.copyOf(goals);}
    public int goal(String id){var goal=goals.get(id);if(goal==null)throw new IllegalArgumentException("Unknown Farmer goal");return goal;}
    public static void clear(MinecraftServer server){CACHE.remove(server);}
    public static FarmerQuestRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-farmer-quests.json"));}catch(IOException e){throw new IllegalStateException("Farmer quest configuration invalid",e);}});}
    public static FarmerQuestRules load(Path path)throws IOException{
        Map<String,Integer> defaults=Map.ofEntries(Map.entry("a01",32),Map.entry("a02",128),Map.entry("a03",64),Map.entry("a04",64),Map.entry("a05",8),Map.entry("a06",32),Map.entry("a07",32),Map.entry("j01",256),Map.entry("j02",256),Map.entry("j03",128),Map.entry("j05_eggs",64),Map.entry("j05_milk",32),Map.entry("j06",16),Map.entry("m03_bread",64),Map.entry("m03_cake",4),Map.entry("m03_pie",32),Map.entry("m03_soup",16),Map.entry("j04",8),Map.entry("j07",5),Map.entry("m01",1024),Map.entry("m01_category",128),Map.entry("m02",16),Map.entry("m02_category",2),Map.entry("m04",2),Map.entry("m06",128),Map.entry("m05",3));
        var json=new JsonObject();json.addProperty("schema",1);var quantities=new JsonObject();defaults.forEach(quantities::addProperty);json.add("quantities",quantities);
        if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json),StandardOpenOption.CREATE_NEW);}
        try{var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!root.keySet().equals(Set.of("schema","quantities"))||number(root,"schema")!=1||!root.getAsJsonObject("quantities").keySet().equals(defaults.keySet()))throw new IllegalArgumentException("Unknown Farmer objective configuration");var result=new HashMap<String,Integer>();for(var id:defaults.keySet()){int count=number(root.getAsJsonObject("quantities"),id);if(count<1||count>8192)throw new IllegalArgumentException("Farmer quantity outside bounded range");result.put(id,count);}if(result.get("a07")<result.get("a06"))throw new IllegalArgumentException("Bread delivery must prove the required baked bread quantity");return new FarmerQuestRules(result);}catch(RuntimeException e){throw new IOException("Invalid Farmer schema1 rules",e);}
    }
    private static int number(JsonObject json,String key){var v=json.get(key);if(v==null||!v.isJsonPrimitive()||!v.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return v.getAsBigDecimal().intValueExact();}
}
