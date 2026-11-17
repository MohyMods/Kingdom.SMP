package com.mohymods.kingdomsmp.profession;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
/** Per-world defaults; the reviewed goal is pinned in the canonical award for stable replay. */
public final class MerchantQuestRules {
 private static final Map<MinecraftServer,Map<String,Integer>> CACHE=new WeakHashMap<>();
 private MerchantQuestRules(){}
 public static int goal(MinecraftServer server,String id){MerchantTransactionQuests.get(id);return CACHE.computeIfAbsent(server,s->{try{return load(s.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-merchant-objectives.json"));}catch(java.io.IOException e){throw new IllegalStateException("Merchant objective configuration invalid; transactions remain usable",e);}}).get(id);}
 public static Map<String,Integer> load(Path path)throws java.io.IOException{
  var defaults=new JsonObject();defaults.addProperty("schema",1);var values=new JsonObject();defaults.add("goals",values);for(var id:MerchantTransactionQuests.ids())values.addProperty(id,MerchantTransactionQuests.goal(id));
  if(!Files.exists(path)){Files.createDirectories(path.toAbsolutePath().getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(defaults),StandardOpenOption.CREATE_NEW);}
  try{var root=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!root.keySet().equals(Set.of("schema","goals"))||root.get("schema").getAsBigDecimal().intValueExact()!=1)throw new IllegalArgumentException("Invalid schema");var rows=root.getAsJsonObject("goals");if(!MerchantTransactionQuests.ids().containsAll(rows.keySet()))throw new IllegalArgumentException("Unknown goal");Map<String,Integer> result=new HashMap<>();for(var id:MerchantTransactionQuests.ids()){var value=rows.has(id)?rows.get(id):values.get(id);if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("Integer goal required");int goal=value.getAsBigDecimal().intValueExact();validate(id,goal);result.put(id,goal);}return Map.copyOf(result);}catch(RuntimeException e){throw new java.io.IOException("Expected six bounded integer Merchant goals",e);}
 }
 public static void validate(String id,int goal){MerchantTransactionQuests.get(id);if(goal<1||goal>(id.equals(MerchantTransactionQuests.SUPPLY)?4:(id.equals(MerchantTransactionQuests.ORDER)||id.equals(MerchantTransactionQuests.EXCHANGE))?1:1000000))throw new IllegalArgumentException("Invalid Merchant goal");}
}
