package com.mohymods.kingdomsmp.fishing;
import com.google.gson.*;
import com.mohymods.kingdomsmp.bounty.CommunityContractRules.Terms;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
/** Finite numerical weekly orders; no new profession or implicit Farmer quest credit. */
public record FishingContractRules(List<Terms> orders){
 public FishingContractRules{orders=List.copyOf(orders);}
 private static final Map<MinecraftServer,FishingContractRules> CACHE=new WeakHashMap<>();
 public static void clear(MinecraftServer s){CACHE.remove(s);}
 public static FishingContractRules get(MinecraftServer s){return CACHE.computeIfAbsent(s,v->{try{return load(v.getWorldPath(LevelResource.ROOT).resolve("serverconfig/kingdomsmp-fishing-contracts.json"));}catch(IOException e){throw new IllegalStateException("Fishing contracts require configuration recovery",e);}});}
 public static FishingContractRules load(Path path)throws IOException{
  var json=new JsonObject();json.addProperty("schema",1);json.addProperty("weeklyRewardPool",1600);var rows=new JsonObject();for(var id:List.of("bluegill","brown_trout","catfish")){var row=new JsonObject();row.addProperty("quantity",id.equals("bluegill")?64:32);row.addProperty("coinsPerFish",id.equals("bluegill")?4:id.equals("brown_trout")?6:9);rows.add(id,row);}json.add("orders",rows);
  if(!Files.exists(path)){Files.createDirectories(path.getParent());Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(json),StandardOpenOption.CREATE_NEW);}
  try{var data=JsonParser.parseString(Files.readString(path)).getAsJsonObject();if(!data.keySet().equals(Set.of("schema","weeklyRewardPool","orders"))||number(data,"schema")!=1||!data.getAsJsonObject("orders").keySet().equals(rows.keySet()))throw new IllegalArgumentException("Invalid fishing contract schema");long pool=number(data,"weeklyRewardPool");if(pool<1||pool>1_000_000_000)throw new IllegalArgumentException("Invalid weekly pool");long required=0;List<Terms> terms=new ArrayList<>();for(var id:List.of("bluegill","brown_trout","catfish")){var row=data.getAsJsonObject("orders").getAsJsonObject(id);if(!row.keySet().equals(Set.of("quantity","coinsPerFish")))throw new IllegalArgumentException("Unexpected fishing order field");long quantity=number(row,"quantity"),unit=number(row,"coinsPerFish");if(quantity<10||quantity>65536||unit<1||unit>100)throw new IllegalArgumentException("Fishing order bounds");required=Math.addExact(required,quantity*unit);terms.add(new Terms("kingdom_smp:fishing_contract/"+id,switch(id){case "bluegill"->"Quayside bluegill provisions";case "brown_trout"->"River trout order";default->"Deep-water catfish order";},"aquaculture:"+id,(int)quantity,(int)unit,pool));}if(required>pool)throw new IllegalArgumentException("Weekly pool must fund all fishing orders");return new FishingContractRules(terms);}catch(RuntimeException e){throw new IOException("Invalid fishing contract configuration",e);}
 }
 private static long number(JsonObject object,String key){var value=object.get(key);if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException(key);return value.getAsBigDecimal().longValueExact();}
}
