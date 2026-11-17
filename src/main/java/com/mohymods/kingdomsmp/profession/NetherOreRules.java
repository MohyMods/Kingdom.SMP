package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Approved Nether quantities in an independent configuration; existing bytes are never rewritten. */
public final class NetherOreRules {
 private static final Map<MinecraftServer,NetherOreRules> CACHE=new WeakHashMap<>();private final Map<String,Integer> goals;
 private NetherOreRules(Map<String,Integer> goals){this.goals=Map.copyOf(goals);}
 public int goal(String id){if(!NetherOres.contains(id))throw new IllegalArgumentException("Unknown Nether objective");return goals.get(id);}
 public static NetherOreRules get(MinecraftServer server){return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-nether-mining-quests.json"));}catch(IOException e){throw new IllegalStateException("Invalid Nether mining configuration",e);}});}
 public static NetherOreRules load(Path path)throws IOException{if(!Files.exists(path)){var root=new JsonObject();root.addProperty("schema",1);var rows=new JsonObject();NetherOres.GOALS.keySet().stream().sorted().forEach(id->rows.addProperty(id,NetherOres.GOALS.get(id)));root.add("goals",rows);Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(root),StandardOpenOption.CREATE_NEW);}try{var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!(root.keySet().equals(Set.of("schema","goals"))||root.keySet().equals(Set.of("schema","goals","copperAreaSeparationChunks")))||integer(root,"schema")!=1||!root.getAsJsonObject("goals").keySet().equals(NetherOres.GOALS.keySet()))throw new IllegalArgumentException("Unknown Nether configuration");/* Preserve early shared-rule fixture files without applying unrelated copper rules to Nether rewards. */if(root.has("copperAreaSeparationChunks")){int old=integer(root,"copperAreaSeparationChunks");if(old<1||old>8)throw new IllegalArgumentException("Invalid legacy extension");}var result=new HashMap<String,Integer>();for(String id:NetherOres.GOALS.keySet()){int n=integer(root.getAsJsonObject("goals"),id);if(n<1||n>100000)throw new IllegalArgumentException("Nether quantity outside1..100000");result.put(id,n);}return new NetherOreRules(result);}catch(RuntimeException e){throw new IOException("Malformed Nether mining configuration",e);}}
 private static int integer(JsonObject row,String key){var value=row.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer required");return value.getAsBigDecimal().intValueExact();}
}
