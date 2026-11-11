package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
public final class BrewQuestRules {
 private static final Map<MinecraftServer,Map<String,Integer>> CACHE=new WeakHashMap<>();
 private BrewQuestRules(){}
 public static int goal(MinecraftServer server,String id){BrewQuests.get(id);return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-brewing-quests.json"));}catch(java.io.IOException e){throw new IllegalStateException("Brewing objectives paused for invalid configuration",e);}}).get(id);}
 public static Map<String,Integer> load(Path file)throws java.io.IOException{var root=new JsonObject();root.addProperty("schema",1);var goals=new JsonObject();root.add("goals",goals);BrewQuests.ids().forEach(id->goals.addProperty(id,12));if(!Files.exists(file)){Files.createDirectories(file.toAbsolutePath().getParent());Files.writeString(file,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardOpenOption.CREATE_NEW);}try{var saved=JsonParser.parseString(Files.readString(file)).getAsJsonObject();if(!saved.keySet().equals(Set.of("schema","goals"))||!saved.get("schema").isJsonPrimitive()||!saved.get("schema").getAsJsonPrimitive().isNumber()||saved.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Invalid schema");var rows=saved.getAsJsonObject("goals");if(!BrewQuests.ids().containsAll(rows.keySet()))throw new IllegalArgumentException("Unknown brewing route");Map<String,Integer> result=new HashMap<>();for(var id:BrewQuests.ids()){var value=rows.has(id)?rows.get(id):goals.get(id);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required");int n=value.getAsBigDecimal().intValueExact();if(n<1||n>36)throw new IllegalArgumentException("Brewing delivery must fit36inventory slots");result.put(id,n);}return Map.copyOf(result);}catch(RuntimeException e){throw new java.io.IOException("Invalid bounded brewing objectives",e);}}
}
